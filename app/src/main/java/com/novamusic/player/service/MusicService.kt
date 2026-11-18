package com.novamusic.player.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.novamusic.player.AppContainer
import com.novamusic.player.MainActivity
import com.novamusic.player.NovaMusicApplication
import com.novamusic.player.R
import com.novamusic.player.audio.SmartShuffleEngine
import com.novamusic.player.data.model.AudioFocusBehavior
import com.novamusic.player.data.model.AudioOutputType
import com.novamusic.player.data.model.AudioQualityMode
import com.novamusic.player.data.model.NotificationAppearance
import com.novamusic.player.data.model.PlaybackMode
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.preferences.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer

    @Volatile
    private var currentSettings: AppSettings = AppSettings()

    @Volatile
    private var currentPlaybackMode: PlaybackMode = PlaybackMode.NORMAL

    private var hasUserChangedMode = false
    private var isApplyingModeTransition = false
    private var modeTransitionJob: Job? = null
    private var widgetTickerJob: Job? = null
    private var smartOriginalQueueIds: List<Long> = emptyList()
    private val recentSessionSongIds = ArrayDeque<Long>()

    private val cyclePlaybackModeCommand =
        SessionCommand(PlaybackSessionContract.ACTION_CYCLE_PLAYBACK_MODE, Bundle.EMPTY)
    private val setPlaybackModeCommand =
        SessionCommand(PlaybackSessionContract.ACTION_SET_PLAYBACK_MODE, Bundle.EMPTY)
    private val widgetPlayPauseCommand =
        SessionCommand(PlaybackSessionContract.ACTION_WIDGET_PLAY_PAUSE, Bundle.EMPTY)
    private val widgetNextCommand =
        SessionCommand(PlaybackSessionContract.ACTION_WIDGET_NEXT, Bundle.EMPTY)
    private val widgetPreviousCommand =
        SessionCommand(PlaybackSessionContract.ACTION_WIDGET_PREVIOUS, Bundle.EMPTY)

    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
                currentSettings.pauseOnHeadphonesDisconnect &&
                ::exoPlayer.isInitialized &&
                exoPlayer.playWhenReady
            ) {
                exoPlayer.pause()
            }
        }
    }
    private var noisyReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        val container = (application as NovaMusicApplication).container

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        ContextCompat.registerReceiver(
            this,
            audioBecomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_EXPORTED
        )
        noisyReceiverRegistered = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    container.equalizerManager.attachToSession(audioSessionId)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    persistCurrentPosition(container)
                    stopWidgetTicker()
                } else {
                    startWidgetTicker(container)
                }
                publishWidgetSnapshotFromService(container, forceFullUpdate = true)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId?.toLongOrNull()
                if (songId != null && songId > 0L) {
                    recordRecentSessionSong(songId)
                }
                publishWidgetSnapshotFromService(container, forceFullUpdate = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                publishWidgetSnapshotFromService(container, forceFullUpdate = false)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (!isApplyingModeTransition) {
                    val derived = deriveExternalPlaybackMode(
                        shuffleEnabled = shuffleModeEnabled,
                        repeatMode = exoPlayer.repeatMode
                    )
                    if (derived != currentPlaybackMode) {
                        hasUserChangedMode = true
                        currentPlaybackMode = derived
                        if (derived != PlaybackMode.SMART_SHUFFLE) {
                            smartOriginalQueueIds = emptyList()
                        }
                        publishPlaybackModeToSession(derived)
                        persistModeOnly(container)
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                if (!isApplyingModeTransition) {
                    val derived = deriveExternalPlaybackMode(
                        shuffleEnabled = exoPlayer.shuffleModeEnabled,
                        repeatMode = repeatMode
                    )
                    if (derived != currentPlaybackMode) {
                        hasUserChangedMode = true
                        currentPlaybackMode = derived
                        if (derived != PlaybackMode.SMART_SHUFFLE) {
                            smartOriginalQueueIds = emptyList()
                        }
                        publishPlaybackModeToSession(derived)
                        persistModeOnly(container)
                    }
                }
            }
        })

        if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId > 0) {
            container.equalizerManager.attachToSession(exoPlayer.audioSessionId)
        }

        container.audioOutputManager.start()
        serviceScope.launch {
            container.audioOutputManager.preferredAudioDeviceInfo.collectLatest { preferredDevice ->
                exoPlayer.setPreferredAudioDevice(preferredDevice)
            }
        }
        serviceScope.launch {
            container.audioOutputManager.externalDeviceConnectedEvents.collect { device ->
                val isHeadphoneDevice = device.type == AudioOutputType.WIRED_HEADPHONES ||
                    device.type == AudioOutputType.BLUETOOTH_HEADPHONES ||
                    device.type == AudioOutputType.USB_AUDIO
                if (isHeadphoneDevice &&
                    currentSettings.resumeOnHeadphonesConnect &&
                    ::exoPlayer.isInitialized &&
                    exoPlayer.mediaItemCount > 0 &&
                    !exoPlayer.isPlaying
                ) {
                    if (exoPlayer.playbackState == Player.STATE_IDLE) {
                        exoPlayer.prepare()
                    }
                    exoPlayer.play()
                }
            }
        }

        serviceScope.launch {
            container.preferencesRepository.appSettings.collectLatest { settings ->
                currentSettings = settings
                exoPlayer.pauseAtEndOfMediaItems = !settings.gaplessPlayback
                val handleFocus = settings.audioFocusBehavior != AudioFocusBehavior.IGNORE_TRANSIENT
                exoPlayer.setAudioAttributes(audioAttributes, handleFocus)
                exoPlayer.setWakeMode(
                    when (settings.audioQuality) {
                        AudioQualityMode.LOSSLESS_DIRECT -> C.WAKE_MODE_LOCAL
                        AudioQualityMode.HIGH_RES -> C.WAKE_MODE_LOCAL
                        AudioQualityMode.BALANCED -> C.WAKE_MODE_NONE
                    }
                )
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(cyclePlaybackModeCommand)
                    .add(setPlaybackModeCommand)
                    .add(widgetPlayPauseCommand)
                    .add(widgetNextCommand)
                    .add(widgetPreviousCommand)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableSessionCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    PlaybackSessionContract.ACTION_CYCLE_PLAYBACK_MODE -> {
                        hasUserChangedMode = true
                        applyPlaybackMode(
                            container = container,
                            targetMode = currentPlaybackMode.next(),
                            restoreOnly = false
                        )
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    PlaybackSessionContract.ACTION_SET_PLAYBACK_MODE -> {
                        hasUserChangedMode = true
                        val modeName = args.getString(PlaybackSessionContract.EXTRA_PLAYBACK_MODE)
                        val targetMode = PlaybackMode.fromStorageKey(modeName) ?: currentPlaybackMode
                        val restoreOnly = args.getBoolean(PlaybackSessionContract.EXTRA_RESTORE_ONLY, false)
                        val overrideOriginals = args.getLongArray(PlaybackSessionContract.EXTRA_ORIGINAL_QUEUE_IDS)
                            ?.toList()
                        applyPlaybackMode(
                            container = container,
                            targetMode = targetMode,
                            restoreOnly = restoreOnly,
                            overrideOriginalQueueIds = overrideOriginals
                        )
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    PlaybackSessionContract.ACTION_WIDGET_PLAY_PAUSE,
                    PlaybackSessionContract.ACTION_WIDGET_NEXT,
                    PlaybackSessionContract.ACTION_WIDGET_PREVIOUS -> {
                        return handleWidgetTransportCommand(
                            action = customCommand.customAction,
                            container = container
                        )
                    }
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaItemsWithStartPosition> {
                val future = SettableFuture.create<MediaItemsWithStartPosition>()
                serviceScope.launch {
                    try {
                        if (container.musicRepository.allSongs.value.isEmpty()) {
                            container.musicRepository.refreshLibrary()
                        }
                        val allSongs = container.musicRepository.allSongs.value
                        val byId = allSongs.associateBy { it.id }
                        val saved = container.preferencesRepository.getPlaybackPersistenceState()
                        val queueSongs = saved.queueSongIds.mapNotNull { byId[it] }
                            .ifEmpty { listOfNotNull(byId[saved.lastSongId]) }

                        if (queueSongs.isEmpty()) {
                            future.setException(IllegalStateException("No saved playback queue"))
                            return@launch
                        }

                        container.audioPlayerManager.registerSongs(queueSongs)
                        val startIdx = queueSongs.indexOfFirst { it.id == saved.lastSongId }.coerceAtLeast(0)
                        val includeArt = currentSettings.showNotificationArtwork
                        val mediaItems = queueSongs.map { song ->
                            val metaBuilder = MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .setAlbumTitle(song.album)
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            if (includeArt) {
                                metaBuilder.setArtworkUri(song.artworkUri)
                            }
                            MediaItem.Builder()
                                .setMediaId(song.id.toString())
                                .setUri(song.uri)
                                .setMediaMetadata(metaBuilder.build())
                                .build()
                        }

                        applyPlaybackMode(
                            container = container,
                            targetMode = saved.playbackMode,
                            restoreOnly = true,
                            overrideOriginalQueueIds = saved.originalQueueSongIds
                        )

                        future.set(
                            MediaItemsWithStartPosition(
                                mediaItems,
                                startIdx,
                                saved.lastPositionMs.coerceAtLeast(0L)
                            )
                        )
                    } catch (t: Throwable) {
                        future.setException(t)
                    }
                }
                return future
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        publishPlaybackModeToSession(currentPlaybackMode)

        serviceScope.launch {
            val saved = container.preferencesRepository.getPlaybackPersistenceState()
            if (!hasUserChangedMode && exoPlayer.mediaItemCount == 0) {
                applyPlaybackMode(
                    container = container,
                    targetMode = saved.playbackMode,
                    restoreOnly = true,
                    overrideOriginalQueueIds = saved.originalQueueSongIds
                )
            }
        }

        val defaultProvider = DefaultMediaNotificationProvider.Builder(this)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_nova_notification)
            }

        val customProvider = object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val base = defaultProvider.createNotification(
                    mediaSession,
                    customLayout,
                    actionFactory,
                    onNotificationChangedCallback
                )
                val s = currentSettings
                val notification = base.notification
                notification.visibility = if (s.notificationsEnabled && s.lockScreenControls) {
                    Notification.VISIBILITY_PUBLIC
                } else {
                    Notification.VISIBILITY_SECRET
                }
                if (!s.notificationsEnabled || s.notificationAppearance == NotificationAppearance.COMPACT) {
                    notification.priority = Notification.PRIORITY_MIN
                }
                return base
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean = defaultProvider.handleCustomCommand(session, action, extras)
        }
        setMediaNotificationProvider(customProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val container = (application as NovaMusicApplication).container
        persistCurrentPosition(container)
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val container = (application as NovaMusicApplication).container
        persistCurrentPosition(container)
        publishWidgetSnapshotFromService(
            container = container,
            forceIsPlaying = false,
            forceFullUpdate = true
        )
        stopWidgetTicker()
        if (noisyReceiverRegistered) {
            runCatching { unregisterReceiver(audioBecomingNoisyReceiver) }
            noisyReceiverRegistered = false
        }
        modeTransitionJob?.cancel()
        serviceScope.cancel()
        container.equalizerManager.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun applyPlaybackMode(
        container: AppContainer,
        targetMode: PlaybackMode,
        restoreOnly: Boolean,
        overrideOriginalQueueIds: List<Long>? = null
    ) {
        if (!::exoPlayer.isInitialized) return
        val previousMode = currentPlaybackMode
        modeTransitionJob?.cancel()

        if (overrideOriginalQueueIds != null) {
            smartOriginalQueueIds = overrideOriginalQueueIds
        }

        if (restoreOnly) {
            currentPlaybackMode = targetMode
            applyExoPlayerFlags(targetMode)
            if (targetMode != PlaybackMode.SMART_SHUFFLE) {
                smartOriginalQueueIds = emptyList()
            }
            publishPlaybackModeToSession(targetMode)
            persistModeOnly(container)
            return
        }

        if (targetMode == PlaybackMode.SMART_SHUFFLE) {
            // Update session indicator immediately so taps feel instantaneous.
            currentPlaybackMode = PlaybackMode.SMART_SHUFFLE
            applyExoPlayerFlags(PlaybackMode.SMART_SHUFFLE)
            publishPlaybackModeToSession(PlaybackMode.SMART_SHUFFLE)

            modeTransitionJob = serviceScope.launch {
                val (historyMap, favoriteIds) = withContext(Dispatchers.IO) {
                    val h = runCatching { container.musicRepository.getPlaybackHistoryMap() }.getOrDefault(emptyMap())
                    val f = runCatching { container.musicRepository.getFavoriteSongIdSet() }.getOrDefault(emptySet())
                    h to f
                }

                val itemCount = exoPlayer.mediaItemCount
                if (itemCount > 1) {
                    val itemsSnapshot = (0 until itemCount).map { exoPlayer.getMediaItemAt(it) }
                    val songsSnapshot = itemsSnapshot.map { resolveSongForMediaItem(container, it) }
                    val currentIdx = exoPlayer.currentMediaItemIndex.coerceIn(0, itemsSnapshot.lastIndex)

                    if (smartOriginalQueueIds.isEmpty()) {
                        smartOriginalQueueIds = songsSnapshot.map { it.id }
                    }

                    val orderedIndices = SmartShuffleEngine.buildSmartOrderIndices(
                        currentIndex = currentIdx,
                        queue = songsSnapshot,
                        historyMap = historyMap,
                        favoriteIds = favoriteIds,
                        recentSessionSongIds = recentSessionSongIds.toList()
                    )

                    if (orderedIndices.size == itemsSnapshot.size) {
                        reorderAroundAnchorZero(
                            currentIdx = currentIdx,
                            orderedIndices = orderedIndices,
                            itemsSnapshot = itemsSnapshot
                        )
                    }
                } else if (itemCount == 1 && smartOriginalQueueIds.isEmpty()) {
                    exoPlayer.getMediaItemAt(0).mediaId.toLongOrNull()?.let { singleId ->
                        smartOriginalQueueIds = listOf(singleId)
                    }
                }

                persistCurrentPosition(container)
            }
            return
        }

        // Transitioning out of SMART_SHUFFLE (e.g., SMART_SHUFFLE -> NORMAL) or between standard modes.
        if (previousMode == PlaybackMode.SMART_SHUFFLE && smartOriginalQueueIds.isNotEmpty() && exoPlayer.mediaItemCount > 1) {
            val itemCount = exoPlayer.mediaItemCount
            val itemsSnapshot = (0 until itemCount).map { exoPlayer.getMediaItemAt(it) }
            val currentIds = itemsSnapshot.map { it.mediaId.toLongOrNull() ?: -1L }
            val currentIdx = exoPlayer.currentMediaItemIndex.coerceIn(0, itemsSnapshot.lastIndex)
            val (restoredIndices, newAnchorPos) = SmartShuffleEngine.restoreSequentialOrderIndices(
                currentIndex = currentIdx,
                currentQueueIds = currentIds,
                originalOrderIds = smartOriginalQueueIds
            )
            if (restoredIndices.size == itemsSnapshot.size) {
                restoreOrderKeepingAnchorUninterrupted(
                    currentIdx = currentIdx,
                    restoredIndices = restoredIndices,
                    newAnchorPos = newAnchorPos,
                    itemsSnapshot = itemsSnapshot
                )
            }
        }

        smartOriginalQueueIds = emptyList()
        currentPlaybackMode = targetMode
        applyExoPlayerFlags(targetMode)
        publishPlaybackModeToSession(targetMode)
        persistCurrentPosition(container)
    }

    /**
     * Reorders [exoPlayer] to match [orderedIndices] (whose element 0 is [currentIdx]) without
     * reloading or interrupting the currently playing MediaItem.
     */
    private fun reorderAroundAnchorZero(
        currentIdx: Int,
        orderedIndices: List<Int>,
        itemsSnapshot: List<MediaItem>
    ) {
        if (currentIdx > 0) {
            exoPlayer.moveMediaItem(currentIdx, 0)
        }
        val currentCount = exoPlayer.mediaItemCount
        if (currentCount > 1) {
            exoPlayer.removeMediaItems(1, currentCount)
        }
        val upcoming = orderedIndices.drop(1).map { itemsSnapshot[it] }
        if (upcoming.isNotEmpty()) {
            exoPlayer.addMediaItems(1, upcoming)
        }
    }

    /**
     * Restores sequential order around the currently playing item at [currentIdx] so the live
     * track continues playing smoothly at [newAnchorPos] with zero audio glitch or restart.
     */
    private fun restoreOrderKeepingAnchorUninterrupted(
        currentIdx: Int,
        restoredIndices: List<Int>,
        newAnchorPos: Int,
        itemsSnapshot: List<MediaItem>
    ) {
        val count = exoPlayer.mediaItemCount
        if (currentIdx + 1 < count) {
            exoPlayer.removeMediaItems(currentIdx + 1, count)
        }
        if (currentIdx > 0) {
            exoPlayer.removeMediaItems(0, currentIdx)
        }
        // Only the anchor item remains at index 0.
        val beforeItems = restoredIndices.take(newAnchorPos).map { itemsSnapshot[it] }
        if (beforeItems.isNotEmpty()) {
            exoPlayer.addMediaItems(0, beforeItems)
        }
        val afterItems = restoredIndices.drop(newAnchorPos + 1).map { itemsSnapshot[it] }
        if (afterItems.isNotEmpty()) {
            exoPlayer.addMediaItems(beforeItems.size + 1, afterItems)
        }
    }

    private fun applyExoPlayerFlags(mode: PlaybackMode) {
        isApplyingModeTransition = true
        try {
            when (mode) {
                PlaybackMode.NORMAL -> {
                    exoPlayer.shuffleModeEnabled = false
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                }

                PlaybackMode.SHUFFLE -> {
                    exoPlayer.shuffleModeEnabled = true
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                }

                PlaybackMode.REPEAT_ALL -> {
                    exoPlayer.shuffleModeEnabled = false
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                }

                PlaybackMode.REPEAT_ONE -> {
                    exoPlayer.shuffleModeEnabled = false
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                }

                PlaybackMode.SMART_SHUFFLE -> {
                    exoPlayer.shuffleModeEnabled = false
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                }
            }
        } finally {
            isApplyingModeTransition = false
        }
    }

    private fun publishPlaybackModeToSession(mode: PlaybackMode) {
        val session = mediaSession ?: return
        session.setSessionExtras(
            bundleOf(PlaybackSessionContract.EXTRA_PLAYBACK_MODE to mode.name)
        )
        session.setCustomLayout(ImmutableList.of(buildPlaybackModeCommandButton(mode)))
    }

    private fun buildPlaybackModeCommandButton(mode: PlaybackMode): CommandButton {
        val (iconRes, label) = when (mode) {
            PlaybackMode.NORMAL -> R.drawable.ic_mode_normal to "Normal"
            PlaybackMode.SHUFFLE -> R.drawable.ic_mode_shuffle to "Shuffle"
            PlaybackMode.REPEAT_ALL -> R.drawable.ic_mode_repeat_all to "Repeat All"
            PlaybackMode.REPEAT_ONE -> R.drawable.ic_mode_repeat_one to "Repeat One"
            PlaybackMode.SMART_SHUFFLE -> R.drawable.ic_mode_smart_shuffle to "Smart Shuffle"
        }
        return CommandButton.Builder()
            .setSessionCommand(cyclePlaybackModeCommand)
            .setIconResId(iconRes)
            .setDisplayName(label)
            .setEnabled(true)
            .build()
    }

    private fun deriveExternalPlaybackMode(shuffleEnabled: Boolean, repeatMode: Int): PlaybackMode {
        return when (repeatMode) {
            Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ONE
            Player.REPEAT_MODE_ALL -> PlaybackMode.REPEAT_ALL
            else -> if (shuffleEnabled) PlaybackMode.SHUFFLE else PlaybackMode.NORMAL
        }
    }

    private fun recordRecentSessionSong(songId: Long) {
        if (recentSessionSongIds.lastOrNull() == songId) return
        recentSessionSongIds.addLast(songId)
        while (recentSessionSongIds.size > 25) {
            recentSessionSongIds.removeFirst()
        }
    }

    private fun resolveSongForMediaItem(container: AppContainer, mediaItem: MediaItem): Song {
        container.audioPlayerManager.findSong(mediaItem.mediaId)?.let { return it }
        val longId = mediaItem.mediaId.toLongOrNull() ?: -1L
        if (longId > 0L) {
            container.musicRepository.allSongs.value.firstOrNull { it.id == longId }?.let { return it }
        }
        val meta = mediaItem.mediaMetadata
        return Song(
            id = longId,
            title = meta.title?.toString().orEmpty().ifBlank { "Unknown Track" },
            artist = meta.artist?.toString().orEmpty().ifBlank { "Unknown Artist" },
            artistId = -1L,
            album = meta.albumTitle?.toString().orEmpty().ifBlank { "Unknown Album" },
            albumId = -1L,
            durationMs = 0L,
            uri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
            artworkUri = meta.artworkUri ?: Uri.EMPTY,
            path = "",
            folderPath = "",
            folderName = "",
            mimeType = "",
            sizeBytes = 0L,
            dateAddedSec = 0L,
            trackNumber = 0,
            year = 0,
            genre = meta.genre?.toString().orEmpty().ifBlank { "Unknown" }
        )
    }

    private fun persistModeOnly(container: AppContainer) {
        val mode = currentPlaybackMode
        val originals = smartOriginalQueueIds
        serviceScope.launch {
            container.preferencesRepository.savePlaybackMode(
                playbackMode = mode,
                originalQueueIds = originals
            )
        }
    }

    private fun persistCurrentPosition(container: AppContainer) {
        if (!::exoPlayer.isInitialized) return
        val songId = exoPlayer.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val queueIds = (0 until exoPlayer.mediaItemCount).mapNotNull {
            exoPlayer.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        val mode = currentPlaybackMode
        val originals = smartOriginalQueueIds
        serviceScope.launch {
            container.preferencesRepository.savePlaybackState(
                songId = songId,
                positionMs = pos,
                queueIds = queueIds,
                playbackMode = mode,
                originalQueueIds = originals
            )
        }
    }

    private fun handleWidgetTransportCommand(
        action: String,
        container: AppContainer
    ): ListenableFuture<SessionResult> {
        if (!::exoPlayer.isInitialized) {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
        }
        if (exoPlayer.mediaItemCount > 0) {
            executeWidgetActionOnPlayer(action, container)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        val future = SettableFuture.create<SessionResult>()
        serviceScope.launch {
            try {
                val restored = restoreQueueFromPersistenceOrLibrary(container)
                if (restored && exoPlayer.mediaItemCount > 0) {
                    executeWidgetActionOnPlayer(action, container)
                } else {
                    publishWidgetSnapshotFromService(container, forceFullUpdate = true)
                }
                future.set(SessionResult(SessionResult.RESULT_SUCCESS))
            } catch (_: Throwable) {
                future.set(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
            }
        }
        return future
    }

    private suspend fun restoreQueueFromPersistenceOrLibrary(container: AppContainer): Boolean {
        if (container.musicRepository.allSongs.value.isEmpty()) {
            runCatching { container.musicRepository.refreshLibrary() }
        }
        val allSongs = container.musicRepository.allSongs.value
        if (allSongs.isEmpty()) return false

        val byId = allSongs.associateBy { it.id }
        val saved = container.preferencesRepository.getPlaybackPersistenceState()
        val queueSongs = saved.queueSongIds.mapNotNull { byId[it] }
            .ifEmpty { listOfNotNull(byId[saved.lastSongId]) }
            .ifEmpty { allSongs.take(100) }

        if (queueSongs.isEmpty()) return false

        container.audioPlayerManager.registerSongs(queueSongs)
        val startIdx = queueSongs.indexOfFirst { it.id == saved.lastSongId }.coerceAtLeast(0)
        val startPos = if (queueSongs.getOrNull(startIdx)?.id == saved.lastSongId) {
            saved.lastPositionMs.coerceAtLeast(0L)
        } else {
            0L
        }
        val includeArt = currentSettings.showNotificationArtwork
        val mediaItems = queueSongs.map { song ->
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setAlbumArtist(song.artist)
                .setGenre(song.genre)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            if (includeArt) {
                metaBuilder.setArtworkUri(song.artworkUri)
            }
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(metaBuilder.build())
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, startIdx, startPos)
        applyPlaybackMode(
            container = container,
            targetMode = saved.playbackMode,
            restoreOnly = true,
            overrideOriginalQueueIds = saved.originalQueueSongIds
        )
        exoPlayer.prepare()
        return true
    }

    private fun executeWidgetActionOnPlayer(action: String, container: AppContainer) {
        when (action) {
            PlaybackSessionContract.ACTION_WIDGET_PLAY_PAUSE -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    when (exoPlayer.playbackState) {
                        Player.STATE_IDLE -> if (exoPlayer.mediaItemCount > 0) exoPlayer.prepare()
                        Player.STATE_ENDED -> {
                            if (exoPlayer.mediaItemCount > 0) {
                                exoPlayer.seekToDefaultPosition(0)
                            }
                        }
                    }
                    exoPlayer.play()
                }
            }

            PlaybackSessionContract.ACTION_WIDGET_NEXT -> {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                } else if (exoPlayer.mediaItemCount > 0) {
                    exoPlayer.seekToDefaultPosition(0)
                }
                if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                    exoPlayer.prepare()
                }
                exoPlayer.play()
            }

            PlaybackSessionContract.ACTION_WIDGET_PREVIOUS -> {
                if (exoPlayer.currentPosition > 3_500L) {
                    exoPlayer.seekTo(0L)
                } else if (exoPlayer.hasPreviousMediaItem()) {
                    exoPlayer.seekToPreviousMediaItem()
                } else {
                    exoPlayer.seekTo(0L)
                }
            }
        }
        publishWidgetSnapshotFromService(container, forceFullUpdate = true)
    }

    private fun startWidgetTicker(container: AppContainer) {
        widgetTickerJob?.cancel()
        if (!container.widgetCoordinator.hasActiveWidgets()) return
        widgetTickerJob = serviceScope.launch {
            while (isActive && ::exoPlayer.isInitialized && exoPlayer.isPlaying) {
                if (!container.widgetCoordinator.hasActiveWidgets()) break
                val duration = exoPlayer.duration.takeIf { it > 0L }
                    ?: exoPlayer.currentMediaItem?.let { resolveSongForMediaItem(container, it).durationMs }
                    ?: 0L
                container.widgetCoordinator.onProgressTick(
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                    durationMs = duration
                )
                delay(1000L)
            }
        }
    }

    private fun stopWidgetTicker() {
        widgetTickerJob?.cancel()
        widgetTickerJob = null
    }

    private fun publishWidgetSnapshotFromService(
        container: AppContainer,
        forceIsPlaying: Boolean? = null,
        forceFullUpdate: Boolean = false
    ) {
        if (!::exoPlayer.isInitialized || !container.widgetCoordinator.hasActiveWidgets()) return
        val currentItem = exoPlayer.currentMediaItem
        val song = currentItem?.let { resolveSongForMediaItem(container, it) }
        val duration = exoPlayer.duration.takeIf { it > 0L } ?: song?.durationMs ?: 0L
        container.widgetCoordinator.onServicePlaybackSnapshot(
            songId = song?.id ?: -1L,
            title = song?.title,
            artist = song?.artist,
            album = song?.album,
            artworkUri = song?.artworkUri,
            isPlaying = forceIsPlaying ?: exoPlayer.isPlaying,
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            forceFullUpdate = forceFullUpdate
        )
    }
}
