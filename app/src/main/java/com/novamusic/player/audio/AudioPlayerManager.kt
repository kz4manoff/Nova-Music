package com.novamusic.player.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.RepeatModeOption
import com.novamusic.player.data.model.SleepTimerState
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.preferences.AppSettings
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import com.novamusic.player.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerManager(
    private val context: Context,
    private val repository: MusicRepository,
    private val preferences: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val songRegistry = mutableMapOf<String, Song>()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var lastRecordedSongId: Long = -1L
    private var hasRestoredSession = false
    private var pendingPlaybackAction: (() -> Unit)? = null
    private var consecutiveErrors = 0
    private var currentSettings: AppSettings = AppSettings()

    init {
        scope.launch {
            var prevShowArtwork = currentSettings.showNotificationArtwork
            preferences.appSettings.collect { settings ->
                currentSettings = settings
                if (prevShowArtwork != settings.showNotificationArtwork) {
                    prevShowArtwork = settings.showNotificationArtwork
                    refreshMediaItemMetadataArtwork(settings.showNotificationArtwork)
                }
                applyTargetPlaybackVolume()
            }
        }
    }

    private fun targetVolume(): Float = if (currentSettings.replayGainEnabled) 0.84f else 1.0f

    private fun applyTargetPlaybackVolume() {
        val controller = mediaController ?: return
        if (crossfadeJob?.isActive != true) {
            controller.volume = targetVolume()
        }
    }

    private fun runFadeInIfNeeded() {
        val controller = mediaController ?: return
        val fadeSec = currentSettings.crossfadeSeconds
        val endVol = targetVolume()
        crossfadeJob?.cancel()
        if (fadeSec <= 0) {
            controller.volume = endVol
            return
        }
        val durationMs = (fadeSec * 600L).coerceIn(450L, 4500L)
        crossfadeJob = scope.launch {
            val steps = 16
            val stepMs = durationMs / steps
            for (i in 1..steps) {
                val frac = i.toFloat() / steps.toFloat()
                controller.volume = (endVol * (0.18f + 0.82f * frac)).coerceIn(0f, 1f)
                delay(stepMs)
            }
            controller.volume = endVol
        }
    }

    private fun refreshMediaItemMetadataArtwork(showArtwork: Boolean) {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        for (i in 0 until count) {
            val item = controller.getMediaItemAt(i)
            val song = songRegistry[item.mediaId] ?: continue
            controller.replaceMediaItem(i, song.toMediaItem(showArtwork))
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncStateFromController()
            if (isPlaying) {
                consecutiveErrors = 0
                _playerState.value = _playerState.value.copy(playbackError = null)
                applyTargetPlaybackVolume()
                startPositionTicker()
            } else {
                stopPositionTicker()
                persistPlaybackSnapshot()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val sleep = _playerState.value.sleepTimer
            if (sleep.stopAfterCurrentTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                mediaController?.pause()
                _playerState.value = _playerState.value.copy(
                    sleepTimer = SleepTimerState()
                )
            } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                runFadeInIfNeeded()
            }
            syncStateFromController()
            val songId = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            if (songId > 0L && songId != lastRecordedSongId) {
                lastRecordedSongId = songId
                scope.launch {
                    repository.recordSongPlayed(songId)
                }
            }
            persistPlaybackSnapshot()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                consecutiveErrors = 0
            } else if (playbackState == Player.STATE_ENDED &&
                currentSettings.autoplayEnabled &&
                mediaController?.repeatMode == Player.REPEAT_MODE_OFF
            ) {
                val all = repository.allSongs.value
                val currentId = _playerState.value.currentSong?.id
                val candidates = all.filter { it.id != currentId }
                val nextUp = candidates.randomOrNull()
                if (nextUp != null) {
                    addToQueue(nextUp)
                    mediaController?.seekToNextMediaItem()
                    mediaController?.prepare()
                    mediaController?.play()
                }
            }
            syncStateFromController()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncStateFromController()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncStateFromController()
            persistPlaybackSnapshot()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncStateFromController()
            persistPlaybackSnapshot()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.value = _playerState.value.copy(
                playbackError = error.localizedMessage ?: "Unable to play this audio file"
            )
            val controller = mediaController ?: return
            consecutiveErrors++
            val itemCount = controller.mediaItemCount
            if (itemCount > 1 && controller.hasNextMediaItem() && consecutiveErrors < itemCount) {
                controller.seekToNextMediaItem()
                controller.prepare()
                controller.play()
            } else {
                controller.pause()
                syncStateFromController()
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                controller.addListener(playerListener)
                syncStateFromController()
                if (controller.isPlaying) {
                    startPositionTicker()
                }
                pendingPlaybackAction?.let { action ->
                    pendingPlaybackAction = null
                    action()
                }
                scope.launch {
                    val allSongs = repository.allSongs.value
                    if (allSongs.isNotEmpty()) {
                        restoreLastSessionIfNeeded(allSongs)
                    }
                }
            } catch (_: Throwable) {
                controllerFuture = null
                mediaController = null
            }
        }, MoreExecutors.directExecutor())
    }

    fun registerSongs(songs: List<Song>) {
        songs.forEach { songRegistry[it.id.toString()] = it }
        syncStateFromController()
    }

    suspend fun restoreLastSessionIfNeeded(allSongs: List<Song>) {
        registerSongs(allSongs)
        if (hasRestoredSession) return
        val controller = mediaController ?: return
        if (controller.mediaItemCount > 0) {
            hasRestoredSession = true
            return
        }

        val settings = preferences.appSettings.first()
        if (!settings.resumePlaybackOnStart) {
            hasRestoredSession = true
            return
        }

        val saved = preferences.getPlaybackPersistenceState()
        if (saved.lastSongId <= 0L && saved.queueSongIds.isEmpty()) {
            hasRestoredSession = true
            return
        }

        val byId = allSongs.associateBy { it.id }
        val queueSongs = saved.queueSongIds.mapNotNull { byId[it] }
            .ifEmpty { listOfNotNull(byId[saved.lastSongId]) }

        if (queueSongs.isEmpty()) {
            hasRestoredSession = true
            return
        }

        val startIndex = queueSongs.indexOfFirst { it.id == saved.lastSongId }.coerceAtLeast(0)
        val items = queueSongs.map { it.toMediaItem(settings.showNotificationArtwork) }
        controller.setMediaItems(items, startIndex, saved.lastPositionMs.coerceAtLeast(0L))
        controller.shuffleModeEnabled = saved.shuffleEnabled
        controller.repeatMode = saved.repeatMode.toMedia3RepeatMode()
        controller.volume = targetVolume()
        controller.prepare()
        controller.playWhenReady = false
        hasRestoredSession = true
        syncStateFromController()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val effectiveQueue = if (queue.any { it.id == song.id }) queue else listOf(song) + queue
        registerSongs(effectiveQueue)
        val startIndex = effectiveQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        val controller = mediaController
        if (controller == null) {
            pendingPlaybackAction = { playSong(song, effectiveQueue) }
            connect()
            return
        }

        val mediaItems = effectiveQueue.map { it.toMediaItem(currentSettings.showNotificationArtwork) }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        runFadeInIfNeeded()
        controller.play()
        lastRecordedSongId = song.id
        _playerState.value = _playerState.value.copy(playbackError = null)
        scope.launch {
            repository.recordSongPlayed(song.id)
        }
        syncStateFromController()
        persistPlaybackSnapshot()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        registerSongs(songs)
        val controller = mediaController ?: run {
            pendingPlaybackAction = { playQueue(songs, startIndex, shuffle) }
            connect()
            return
        }
        controller.shuffleModeEnabled = shuffle
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        controller.setMediaItems(songs.map { it.toMediaItem(currentSettings.showNotificationArtwork) }, safeIndex, 0L)
        controller.prepare()
        runFadeInIfNeeded()
        controller.play()
        val firstSong = songs[safeIndex]
        lastRecordedSongId = firstSong.id
        _playerState.value = _playerState.value.copy(playbackError = null)
        scope.launch { repository.recordSongPlayed(firstSong.id) }
        syncStateFromController()
        persistPlaybackSnapshot()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: run {
            connect()
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            when (controller.playbackState) {
                Player.STATE_IDLE -> if (controller.mediaItemCount > 0) controller.prepare()
                Player.STATE_ENDED -> {
                    if (controller.mediaItemCount > 0) {
                        controller.seekToDefaultPosition(0)
                    }
                }
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _playerState.value = _playerState.value.copy(positionMs = clamped)
        persistPlaybackSnapshot()
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekToDefaultPosition(0)
        }
        if (controller.playbackState == Player.STATE_ENDED || controller.playbackState == Player.STATE_IDLE) {
            controller.prepare()
            controller.play()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3_500L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        syncStateFromController()
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncStateFromController()
    }

    fun playNext(song: Song) {
        registerSongs(listOf(song))
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            playSong(song)
            return
        }
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
        controller.addMediaItem(insertIndex, song.toMediaItem(currentSettings.showNotificationArtwork))
        syncStateFromController()
        persistPlaybackSnapshot()
    }

    fun addToQueue(song: Song) {
        registerSongs(listOf(song))
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            playSong(song)
            return
        }
        controller.addMediaItem(song.toMediaItem(currentSettings.showNotificationArtwork))
        syncStateFromController()
        persistPlaybackSnapshot()
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
            syncStateFromController()
            persistPlaybackSnapshot()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        if (fromIndex in 0 until count && toIndex in 0 until count && fromIndex != toIndex) {
            controller.moveMediaItem(fromIndex, toIndex)
            syncStateFromController()
            persistPlaybackSnapshot()
        }
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.prepare()
            controller.play()
            syncStateFromController()
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(sleepTimer = SleepTimerState())
            return
        }
        val totalMs = minutes * 60_000L
        _playerState.value = _playerState.value.copy(
            sleepTimer = SleepTimerState(isActive = true, remainingMs = totalMs, stopAfterCurrentTrack = false)
        )
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _playerState.value = _playerState.value.copy(
                    sleepTimer = SleepTimerState(isActive = true, remainingMs = remaining, stopAfterCurrentTrack = false)
                )
            }
            if (isActive) {
                mediaController?.pause()
                _playerState.value = _playerState.value.copy(sleepTimer = SleepTimerState())
            }
        }
    }

    fun setStopAfterCurrentTrack() {
        sleepTimerJob?.cancel()
        _playerState.value = _playerState.value.copy(
            sleepTimer = SleepTimerState(isActive = true, remainingMs = 0L, stopAfterCurrentTrack = true)
        )
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _playerState.value = _playerState.value.copy(sleepTimer = SleepTimerState())
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            var persistCounter = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L }
                        ?: _playerState.value.currentSong?.durationMs
                        ?: 0L
                    val crossfadeWindowMs = currentSettings.crossfadeSeconds * 1000L
                    if (crossfadeWindowMs > 0L && dur > crossfadeWindowMs + 1500L && controller.hasNextMediaItem()) {
                        val remainingMs = dur - pos
                        if (remainingMs in 1L..crossfadeWindowMs) {
                            val fadeOutFrac = (remainingMs.toFloat() / crossfadeWindowMs.toFloat()).coerceIn(0.15f, 1f)
                            controller.volume = targetVolume() * fadeOutFrac
                        }
                    }
                    _playerState.value = _playerState.value.copy(
                        positionMs = pos,
                        durationMs = dur,
                        isPlaying = controller.isPlaying
                    )
                    persistCounter++
                    if (persistCounter >= 15) {
                        persistCounter = 0
                        persistPlaybackSnapshot()
                    }
                }
                delay(300L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun syncStateFromController() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        val queue = (0 until count).mapNotNull { idx ->
            val item = controller.getMediaItemAt(idx)
            songRegistry[item.mediaId]
        }
        val currentIdx = controller.currentMediaItemIndex
        val currentItem = controller.currentMediaItem
        val currentSong = currentItem?.let { songRegistry[it.mediaId] }

        val duration = controller.duration.takeIf { it > 0L }
            ?: currentSong?.durationMs
            ?: 0L

        _playerState.value = _playerState.value.copy(
            currentSong = currentSong,
            queue = queue,
            currentQueueIndex = currentIdx,
            isPlaying = controller.isPlaying,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            shuffleEnabled = controller.shuffleModeEnabled,
            repeatMode = controller.repeatMode.toRepeatModeOption()
        )
    }

    private fun persistPlaybackSnapshot() {
        val controller = mediaController ?: return
        val currentSongId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val pos = controller.currentPosition.coerceAtLeast(0L)
        val queueIds = (0 until controller.mediaItemCount).mapNotNull {
            controller.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        val shuffle = controller.shuffleModeEnabled
        val repeat = controller.repeatMode.toRepeatModeOption()

        scope.launch {
            preferences.savePlaybackState(
                songId = currentSongId,
                positionMs = pos,
                queueIds = queueIds,
                shuffleEnabled = shuffle,
                repeatMode = repeat
            )
        }
    }

    private fun Song.toMediaItem(showArtwork: Boolean = true): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setAlbumArtist(artist)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        if (showArtwork) {
            metaBuilder.setArtworkUri(artworkUri)
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    private fun Int.toRepeatModeOption(): RepeatModeOption = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
        Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
        else -> RepeatModeOption.OFF
    }

    private fun RepeatModeOption.toMedia3RepeatMode(): Int = when (this) {
        RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
        RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
        RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
    }
}
