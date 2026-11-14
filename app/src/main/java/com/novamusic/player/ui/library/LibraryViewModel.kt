package com.novamusic.player.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.data.model.Album
import com.novamusic.player.data.model.Artist
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.Genre
import com.novamusic.player.data.model.MusicFolder
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    GENRES("Genres")
}

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val playlists: List<PlaylistWithSongs> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val highResArtwork: Boolean = true,
    val safPickedSongs: List<Song> = emptyList(),
    val safFolderName: String? = null
)

class LibraryViewModel(
    private val repository: MusicRepository,
    private val playerManager: AudioPlayerManager,
    private val preferences: PreferencesRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = playerManager.playerState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _safPickedSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _safFolderName = MutableStateFlow<String?>(null)

    private val _selectedTrackInfo = MutableStateFlow<Pair<Song, AudioFileInfo>?>(null)
    val selectedTrackInfo: StateFlow<Pair<Song, AudioFileInfo>?> = _selectedTrackInfo.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allSongs,
        repository.albums,
        repository.artists,
        repository.folders,
        repository.genres
    ) { songs, albums, artists, folders, genres ->
        LibraryCollections(songs, albums, artists, folders, genres)
    }.combine(
        combine(
            repository.playlistsWithSongs,
            repository.favoriteSongIds,
            preferences.appSettings,
            repository.isScanning,
            _searchQuery
        ) { playlists, favIds, settings, scanning, query ->
            LibraryMeta(playlists, favIds, settings, scanning, query)
        }
    ) { collections, meta ->
        val q = meta.query.trim().lowercase()
        val sortedSongs = repository.sortSongs(collections.songs, meta.settings.sortOrder, meta.settings.sortDirection)
        val filteredSongs = if (q.isBlank()) sortedSongs else sortedSongs.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.folderName.lowercase().contains(q) ||
                it.genre.lowercase().contains(q)
        }
        val filteredAlbums = if (q.isBlank()) collections.albums else collections.albums.filter {
            it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
        val filteredArtists = if (q.isBlank()) collections.artists else collections.artists.filter {
            it.name.lowercase().contains(q)
        }
        val filteredFolders = if (q.isBlank()) collections.folders else collections.folders.filter {
            it.name.lowercase().contains(q) || it.path.lowercase().contains(q)
        }
        val filteredGenres = if (q.isBlank()) collections.genres else collections.genres.filter {
            it.name.lowercase().contains(q)
        }

        LibraryUiState(
            songs = filteredSongs,
            albums = filteredAlbums,
            artists = filteredArtists,
            folders = filteredFolders,
            genres = filteredGenres,
            playlists = meta.playlists,
            favoriteIds = meta.favIds,
            sortOrder = meta.settings.sortOrder,
            sortDirection = meta.settings.sortDirection,
            searchQuery = meta.query,
            isScanning = meta.scanning,
            highResArtwork = meta.settings.highResArtwork,
            safPickedSongs = _safPickedSongs.value,
            safFolderName = _safFolderName.value
        )
    }.combine(_safPickedSongs) { state, safSongs ->
        state.copy(safPickedSongs = safSongs, safFolderName = _safFolderName.value)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    private data class LibraryCollections(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val folders: List<MusicFolder>,
        val genres: List<Genre>
    )

    private data class LibraryMeta(
        val playlists: List<PlaylistWithSongs>,
        val favIds: Set<Long>,
        val settings: com.novamusic.player.data.preferences.AppSettings,
        val scanning: Boolean,
        val query: String
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSort(order: SortOrder, direction: SortDirection) {
        viewModelScope.launch {
            preferences.setSort(order, direction)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repository.refreshLibrary()
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        playerManager.playSong(song, queue)
    }

    fun playAll(songs: List<Song>, shuffle: Boolean = false) {
        if (songs.isNotEmpty()) {
            val startIdx = if (shuffle) songs.indices.random() else 0
            playerManager.playQueue(songs, startIdx, shuffle)
        }
    }

    fun playNext(song: Song) = playerManager.playNext(song)

    fun addToQueue(song: Song) = playerManager.addToQueue(song)

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun toggleFavoriteFolder(folder: MusicFolder) {
        viewModelScope.launch {
            repository.toggleFavoriteFolder(folder.path, folder.name, folder.isFavorite)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addSongToPlaylist(id, songId)
        }
    }

    fun loadTrackInfo(song: Song) {
        viewModelScope.launch {
            val info = repository.getAudioFileInfo(song)
            _selectedTrackInfo.value = song to info
        }
    }

    fun clearTrackInfo() {
        _selectedTrackInfo.value = null
    }

    fun openSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            val songs = repository.scanSafTree(treeUri)
            _safPickedSongs.value = songs
            _safFolderName.value = songs.firstOrNull()?.folderName ?: "Picked Folder"
        }
    }

    fun openSafAudioFile(fileUri: Uri) {
        viewModelScope.launch {
            val song = repository.resolveSafUriToSong(fileUri)
            if (song != null) {
                playerManager.playSong(song, listOf(song))
            }
        }
    }

    fun clearSafFolder() {
        _safPickedSongs.value = emptyList()
        _safFolderName.value = null
    }

    fun songsForAlbum(albumId: Long): List<Song> {
        return repository.allSongs.value
            .filter { it.albumId == albumId }
            .sortedBy { it.trackNumber }
    }

    fun songsForArtist(artistName: String): List<Song> {
        return repository.allSongs.value
            .filter { it.artist.equals(artistName, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
    }

    fun songsForFolder(folderPath: String): List<Song> {
        return repository.allSongs.value
            .filter { it.folderPath == folderPath }
            .sortedBy { it.title.lowercase() }
    }

    fun songsForGenre(genreName: String): List<Song> {
        return repository.allSongs.value
            .filter { it.genre.equals(genreName, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
    }
}
