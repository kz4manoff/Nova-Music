package com.novamusic.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.data.model.Album
import com.novamusic.player.data.model.Artist
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val allSongsCount: Int = 0,
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<PlaylistWithSongs> = emptyList(),
    val isScanning: Boolean = false,
    val highResArtwork: Boolean = true
)

private data class HomePartA(
    val allSongs: List<Song>,
    val recentlyPlayed: List<Song>,
    val recentlyAdded: List<Song>,
    val mostPlayed: List<Song>,
    val favorites: List<Song>
)

private data class HomePartB(
    val favoriteIds: Set<Long>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<PlaylistWithSongs>,
    val isScanning: Boolean,
    val highResArtwork: Boolean
)

class HomeViewModel(
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = playerManager.playerState

    private val partA = combine(
        repository.allSongs,
        repository.recentlyPlayedSongs,
        repository.recentlyAddedSongs,
        repository.mostPlayedSongs,
        repository.favoriteSongs
    ) { allSongs, recent, added, most, favs ->
        HomePartA(allSongs, recent, added, most, favs)
    }

    private val partB = combine(
        repository.favoriteSongIds,
        repository.albums,
        repository.artists,
        repository.playlistsWithSongs,
        repository.isScanning
    ) { favIds, albums, artists, playlists, scanning ->
        listOf(favIds, albums, artists, playlists, scanning)
    }.combine(preferencesRepository.appSettings) { list, settings ->
        @Suppress("UNCHECKED_CAST")
        HomePartB(
            favoriteIds = list[0] as Set<Long>,
            albums = (list[1] as List<Album>).take(20),
            artists = (list[2] as List<Artist>).take(20),
            playlists = list[3] as List<PlaylistWithSongs>,
            isScanning = list[4] as Boolean,
            highResArtwork = settings.highResArtwork
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(partA, partB) { a, b ->
        HomeUiState(
            allSongsCount = a.allSongs.size,
            recentlyPlayed = a.recentlyPlayed,
            recentlyAdded = a.recentlyAdded,
            mostPlayed = a.mostPlayed,
            favoriteSongs = a.favorites,
            favoriteIds = b.favoriteIds,
            albums = b.albums,
            artists = b.artists,
            playlists = b.playlists,
            isScanning = b.isScanning,
            highResArtwork = b.highResArtwork
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun playSong(song: Song, contextQueue: List<Song>) {
        playerManager.playSong(song, contextQueue.ifEmpty { listOf(song) })
    }

    fun shuffleAll() {
        val songs = repository.allSongs.value
        if (songs.isNotEmpty()) {
            playerManager.playQueue(songs, startIndex = songs.indices.random(), shuffle = true)
        }
    }

    fun playPlaylist(playlist: PlaylistWithSongs) {
        if (playlist.songs.isNotEmpty()) {
            playerManager.playQueue(playlist.songs, startIndex = 0, shuffle = false)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun refreshLibrary() {
        viewModelScope.launch { repository.refreshLibrary() }
    }
}
