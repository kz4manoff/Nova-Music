package com.novamusic.player.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.ThemeMode
import com.novamusic.player.data.preferences.AppSettings
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: PreferencesRepository,
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = preferences.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val isScanning: StateFlow<Boolean> = repository.isScanning
    val playerState: StateFlow<PlayerUiState> = playerManager.playerState

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setAccentColor(accent: AccentColorOption) {
        viewModelScope.launch { preferences.setAccentColor(accent) }
    }

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

    fun setArtworkStyle(style: ArtworkStyle) {
        viewModelScope.launch { preferences.setArtworkStyle(style) }
    }

    fun setHighResArtwork(enabled: Boolean) {
        viewModelScope.launch { preferences.setHighResArtwork(enabled) }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { preferences.setGaplessPlayback(enabled) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { preferences.setResumePlayback(enabled) }
    }

    fun setMinTrackDurationSec(seconds: Int) {
        viewModelScope.launch {
            preferences.setMinTrackDurationSec(seconds)
            repository.refreshLibrary()
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch { repository.refreshLibrary() }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearPlaybackHistory() }
    }

    fun startSleepTimer(minutes: Int) = playerManager.startSleepTimer(minutes)
    fun setStopAfterCurrentTrack() = playerManager.setStopAfterCurrentTrack()
    fun cancelSleepTimer() = playerManager.cancelSleepTimer()
}
