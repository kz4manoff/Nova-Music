package com.novamusic.player.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.AudioOutputManager
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.audio.EqualizerManager
import com.novamusic.player.audio.LyricsParser
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.AudioRoutingUiState
import com.novamusic.player.data.model.LyricsResult
import com.novamusic.player.data.model.LyricsSource
import com.novamusic.player.data.model.OutputSwitchOutcome
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager,
    private val preferences: PreferencesRepository,
    equalizerManager: EqualizerManager,
    private val audioOutputManager: AudioOutputManager
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = playerManager.playerState
    val audioRoutingState: StateFlow<AudioRoutingUiState> = audioOutputManager.routingState

    val isEqualizerEnabled: StateFlow<Boolean> = equalizerManager.state
        .map { it.isEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteSongIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<PlaylistWithSongs>> = repository.playlistsWithSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artworkStyle: StateFlow<ArtworkStyle> = preferences.appSettings
        .map { it.artworkStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtworkStyle.ROUNDED_CARD)

    val highResArtwork: StateFlow<Boolean> = preferences.appSettings
        .map { it.highResArtwork }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _lyricsResult = MutableStateFlow(
        LyricsResult(emptyList(), isSynchronized = false, source = LyricsSource.NONE)
    )
    val lyricsResult: StateFlow<LyricsResult> = _lyricsResult.asStateFlow()

    private val _rawLyricsForEdit = MutableStateFlow("")
    val rawLyricsForEdit: StateFlow<String> = _rawLyricsForEdit.asStateFlow()

    private val _currentAudioInfo = MutableStateFlow<AudioFileInfo?>(null)
    val currentAudioInfo: StateFlow<AudioFileInfo?> = _currentAudioInfo.asStateFlow()

    private val _trackInfoDialog = MutableStateFlow<AudioFileInfo?>(null)
    val trackInfoDialog: StateFlow<AudioFileInfo?> = _trackInfoDialog.asStateFlow()

    val activeLyricIndex: StateFlow<Int> = combine(
        _lyricsResult,
        playerState
    ) { lyrics, player ->
        if (!lyrics.isSynchronized) -1
        else LyricsParser.findActiveLineIndex(lyrics.lines, player.positionMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    init {
        viewModelScope.launch {
            playerState
                .map { it.currentSong }
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { song ->
                    _currentAudioInfo.value = if (song != null) {
                        runCatching { repository.getAudioFileInfo(song) }.getOrNull()
                    } else {
                        null
                    }
                }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun pausePlayback() = playerManager.pausePlayback()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun skipToNext() = playerManager.skipToNext()
    fun skipToPrevious() = playerManager.skipToPrevious()
    fun cyclePlaybackMode() = playerManager.cyclePlaybackMode()
    fun setPlaybackMode(mode: com.novamusic.player.data.model.PlaybackMode) = playerManager.setPlaybackMode(mode)
    fun jumpToQueueIndex(index: Int) = playerManager.jumpToQueueIndex(index)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)
    fun moveQueueItem(from: Int, to: Int) = playerManager.moveQueueItem(from, to)
    fun playNext(song: Song) = playerManager.playNext(song)
    fun addToQueue(song: Song) = playerManager.addToQueue(song)

    fun shareSong(context: Context, song: Song) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = song.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, song.uri)
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            val chooser = Intent.createChooser(shareIntent, "Share \"${song.title}\"").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // Smart Audio Device Management & Output Switching
    fun selectAudioOutput(deviceId: String?): OutputSwitchOutcome = audioOutputManager.selectOutputDevice(deviceId)
    fun resetAudioOutputToAuto() {
        audioOutputManager.selectOutputDevice(null)
    }
    fun dismissDeviceBanner() = audioOutputManager.dismissBanner()
    fun clearAudioStatusNotice() = audioOutputManager.clearStatusNotice()
    fun refreshAudioDevices() = audioOutputManager.refreshPermissionsAndDevices()

    fun openSystemSoundOutputPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_VOLUME)
        } else {
            Intent(Settings.ACTION_SOUND_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
    }

    fun openBluetoothSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun cycleArtworkStyle() {
        val styles = ArtworkStyle.entries
        val next = styles[(artworkStyle.value.ordinal + 1) % styles.size]
        viewModelScope.launch { preferences.setArtworkStyle(next) }
    }

    fun setArtworkStyle(style: ArtworkStyle) {
        viewModelScope.launch { preferences.setArtworkStyle(style) }
    }

    fun startSleepTimer(minutes: Int) = playerManager.startSleepTimer(minutes)
    fun setStopAfterCurrentTrack() = playerManager.setStopAfterCurrentTrack()
    fun cancelSleepTimer() = playerManager.cancelSleepTimer()

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addSongToPlaylist(id, songId)
        }
    }

    fun openTrackInfoDialog(song: Song) {
        viewModelScope.launch {
            _trackInfoDialog.value = _currentAudioInfo.value ?: repository.getAudioFileInfo(song)
        }
    }

    fun clearTrackInfoDialog() {
        _trackInfoDialog.value = null
    }

    fun loadLyricsForSong(context: Context, song: Song) {
        viewModelScope.launch {
            val manual = repository.getManualLyrics(song.id)
            _rawLyricsForEdit.value = manual.orEmpty()
            _lyricsResult.value = LyricsParser.loadLyrics(context, song, manual)
        }
    }

    fun saveManualLyrics(context: Context, song: Song, rawText: String) {
        viewModelScope.launch {
            repository.saveManualLyrics(song.id, rawText)
            _rawLyricsForEdit.value = rawText
            _lyricsResult.value = LyricsParser.loadLyrics(context, song, rawText.ifBlank { null })
        }
    }

    fun importLrcFile(context: Context, song: Song, uri: Uri) {
        viewModelScope.launch {
            val content = LyricsParser.readLrcFromUri(context, uri)
            if (!content.isNullOrBlank()) {
                repository.saveManualLyrics(song.id, content)
                _rawLyricsForEdit.value = content
                _lyricsResult.value = LyricsParser.loadLyrics(context, song, content)
            }
        }
    }
}
