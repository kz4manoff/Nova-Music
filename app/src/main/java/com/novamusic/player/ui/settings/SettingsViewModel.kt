package com.novamusic.player.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.novamusic.player.audio.AudioOutputManager
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.audio.EqualizerManager
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.AudioFocusBehavior
import com.novamusic.player.data.model.AudioQualityMode
import com.novamusic.player.data.model.AudioRoutingUiState
import com.novamusic.player.data.model.EqualizerState
import com.novamusic.player.data.model.MusicFolder
import com.novamusic.player.data.model.NavBarAppearance
import com.novamusic.player.data.model.NotificationAppearance
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.RescanInterval
import com.novamusic.player.data.model.ThemeMode
import com.novamusic.player.data.preferences.AppSettings
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import com.novamusic.player.database.entities.CustomPresetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val context: Context,
    private val preferences: PreferencesRepository,
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager,
    private val equalizerManager: EqualizerManager,
    private val audioOutputManager: AudioOutputManager
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = preferences.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val eqState: StateFlow<EqualizerState> = equalizerManager.state
    val builtInPresets: List<String> = EqualizerManager.BUILT_IN_PRESETS.keys.toList()
    val customPresets: StateFlow<List<CustomPresetEntity>> = repository.customEqualizerPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isScanning: StateFlow<Boolean> = repository.isScanning
    val playerState: StateFlow<PlayerUiState> = playerManager.playerState
    val audioRoutingState: StateFlow<AudioRoutingUiState> = audioOutputManager.routingState

    val availableFolders: StateFlow<List<MusicFolder>> = repository.availableFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalSongsCount: StateFlow<Int> = repository.allSongs
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val totalLibraryBytes: StateFlow<Long> = repository.totalLibraryBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                directorySizeBytes(context.cacheDir) + directorySizeBytes(context.codeCacheDir)
            }
            _cacheSizeBytes.value = bytes
        }
    }

    private fun directorySizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += if (child.isDirectory) directorySizeBytes(child) else child.length()
        }
        return total
    }

    // 1. General
    fun setLanguage(tag: String) {
        viewModelScope.launch {
            preferences.setLanguageTag(tag)
            val locales = if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColors(enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { preferences.setHapticFeedback(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferences.setKeepScreenOn(enabled) }
    }

    // 2. Appearance
    fun setAccentColor(accent: AccentColorOption) {
        viewModelScope.launch { preferences.setAccentColor(accent) }
    }

    fun setArtworkStyle(style: ArtworkStyle) {
        viewModelScope.launch { preferences.setArtworkStyle(style) }
    }

    fun setBlurIntensity(intensity: Float) {
        viewModelScope.launch { preferences.setBlurIntensity(intensity) }
    }

    fun setPlayerAnimationStyle(style: PlayerAnimationStyle) {
        viewModelScope.launch { preferences.setPlayerAnimationStyle(style) }
    }

    fun setNavBarAppearance(appearance: NavBarAppearance) {
        viewModelScope.launch { preferences.setNavBarAppearance(appearance) }
    }

    fun setInterfaceAnimations(enabled: Boolean) {
        viewModelScope.launch { preferences.setInterfaceAnimations(enabled) }
    }

    // 3. Playback
    fun setAudioQuality(quality: AudioQualityMode) {
        viewModelScope.launch { preferences.setAudioQuality(quality) }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { preferences.setGaplessPlayback(enabled) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { preferences.setCrossfadeSeconds(seconds) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setReplayGainEnabled(enabled) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { preferences.setResumePlayback(enabled) }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoplayEnabled(enabled) }
    }

    fun setAudioFocusBehavior(behavior: AudioFocusBehavior) {
        viewModelScope.launch { preferences.setAudioFocusBehavior(behavior) }
    }

    fun startSleepTimer(minutes: Int) = playerManager.startSleepTimer(minutes)
    fun setStopAfterCurrentTrack() = playerManager.setStopAfterCurrentTrack()
    fun cancelSleepTimer() = playerManager.cancelSleepTimer()

    // 4. Sound & Equalizer (Inline DSP controls)
    fun setEqEnabled(enabled: Boolean) = equalizerManager.setEnabled(enabled)
    fun applyBuiltInPreset(presetName: String) = equalizerManager.applyBuiltInPreset(presetName)
    fun applyCustomPreset(preset: CustomPresetEntity) {
        val bands = preset.bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        equalizerManager.applyCustomPreset(
            name = preset.name,
            bandLevelsMillibels = bands,
            bass = preset.bassBoost,
            virt = preset.virtualizer,
            loudness = preset.loudness,
            preampDb = preset.preampDb
        )
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) = equalizerManager.setBandLevel(bandIndex, levelMillibels)
    fun setBassBoost(strength: Int) = equalizerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = equalizerManager.setVirtualizer(strength)
    fun setLoudnessGain(gainMillibels: Int) = equalizerManager.setLoudnessGain(gainMillibels)
    fun setPreampGain(gainDb: Float) = equalizerManager.setPreampGain(gainDb)
    fun resetEqualizer() = equalizerManager.resetEqualizer()

    // 5. Library & Storage
    fun rescanLibrary() {
        viewModelScope.launch {
            repository.refreshLibrary()
            refreshCacheSize()
        }
    }

    fun setRescanInterval(interval: RescanInterval) {
        viewModelScope.launch { preferences.setRescanInterval(interval) }
    }

    fun toggleExcludedFolder(folderPath: String) {
        viewModelScope.launch { preferences.toggleExcludedFolder(folderPath) }
    }

    fun clearExcludedFolders() {
        viewModelScope.launch { preferences.setExcludedFolders(emptySet()) }
    }

    fun setMinTrackDurationSec(seconds: Int) {
        viewModelScope.launch {
            preferences.setMinTrackDurationSec(seconds)
            repository.refreshLibrary()
        }
    }

    fun setHighResArtwork(enabled: Boolean) {
        viewModelScope.launch { preferences.setHighResArtwork(enabled) }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val loader = context.imageLoader
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            }
            refreshCacheSize()
        }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearPlaybackHistory() }
    }

    // 6. Notifications
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    fun setLockScreenControls(enabled: Boolean) {
        viewModelScope.launch { preferences.setLockScreenControls(enabled) }
    }

    fun setShowNotificationArtwork(enabled: Boolean) {
        viewModelScope.launch { preferences.setShowNotificationArtwork(enabled) }
    }

    fun setNotificationAppearance(appearance: NotificationAppearance) {
        viewModelScope.launch { preferences.setNotificationAppearance(appearance) }
    }
}
