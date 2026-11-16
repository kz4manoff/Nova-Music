package com.novamusic.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.novamusic.player.AppContainer
import com.novamusic.player.ui.equalizer.EqualizerViewModel
import com.novamusic.player.ui.home.HomeViewModel
import com.novamusic.player.ui.library.LibraryViewModel
import com.novamusic.player.ui.player.PlayerViewModel
import com.novamusic.player.ui.playlists.PlaylistsViewModel
import com.novamusic.player.ui.settings.SettingsViewModel

class NovaViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(container.musicRepository, container.audioPlayerManager, container.preferencesRepository) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(container.musicRepository, container.audioPlayerManager, container.preferencesRepository) as T
            }
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(
                    container.musicRepository,
                    container.audioPlayerManager,
                    container.preferencesRepository,
                    container.equalizerManager,
                    container.audioOutputManager
                ) as T
            }
            modelClass.isAssignableFrom(EqualizerViewModel::class.java) -> {
                EqualizerViewModel(container.equalizerManager, container.musicRepository) as T
            }
            modelClass.isAssignableFrom(PlaylistsViewModel::class.java) -> {
                PlaylistsViewModel(container.musicRepository, container.audioPlayerManager) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    context = container.appContext,
                    preferences = container.preferencesRepository,
                    repository = container.musicRepository,
                    playerManager = container.audioPlayerManager,
                    equalizerManager = container.equalizerManager,
                    audioOutputManager = container.audioOutputManager,
                    updateManager = container.updateManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
