package com.novamusic.player.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithSongs>> = repository.playlistsWithSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSongs: StateFlow<List<Song>> = repository.allSongs

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = repository.favoriteSongIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playerState: StateFlow<PlayerUiState> = playerManager.playerState

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, newName) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun moveSongInPlaylist(playlist: PlaylistWithSongs, fromIdx: Int, toIdx: Int) {
        if (fromIdx !in playlist.songs.indices || toIdx !in playlist.songs.indices || fromIdx == toIdx) return
        val mutable = playlist.songs.map { it.id }.toMutableList()
        val moved = mutable.removeAt(fromIdx)
        mutable.add(toIdx, moved)
        viewModelScope.launch {
            repository.reorderPlaylistSongs(playlist.id, mutable)
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isNotEmpty()) {
            playerManager.playQueue(songs, startIndex, shuffle)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }
}
