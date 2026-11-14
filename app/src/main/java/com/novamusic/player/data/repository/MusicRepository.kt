package com.novamusic.player.data.repository

import android.net.Uri
import com.novamusic.player.data.model.Album
import com.novamusic.player.data.model.Artist
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.Genre
import com.novamusic.player.data.model.MusicFolder
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.scanner.MusicScanner
import com.novamusic.player.database.NovaDatabase
import com.novamusic.player.database.entities.CustomPresetEntity
import com.novamusic.player.database.entities.FavoriteFolderEntity
import com.novamusic.player.database.entities.FavoriteSongEntity
import com.novamusic.player.database.entities.PlaylistEntity
import com.novamusic.player.database.entities.PlaylistSongCrossRef
import com.novamusic.player.database.entities.TrackLyricsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val scanner: MusicScanner,
    private val database: NovaDatabase,
    private val preferences: PreferencesRepository
) {
    private val favoriteDao = database.favoriteDao()
    private val historyDao = database.historyDao()
    private val playlistDao = database.playlistDao()
    private val folderDao = database.folderDao()
    private val presetDao = database.equalizerPresetDao()
    private val lyricsDao = database.lyricsDao()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val favoriteSongIds: Flow<Set<Long>> = favoriteDao.observeFavoriteSongIds().map { it.toSet() }

    val favoriteSongs: Flow<List<Song>> = combine(_allSongs, favoriteDao.observeFavoriteSongIds()) { songs, ids ->
        val byId = songs.associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }

    val recentlyPlayedSongs: Flow<List<Song>> = combine(_allSongs, historyDao.observeRecentlyPlayed(30)) { songs, history ->
        val byId = songs.associateBy { it.id }
        history.mapNotNull { byId[it.songId] }
    }

    val mostPlayedSongs: Flow<List<Song>> = combine(_allSongs, historyDao.observeMostPlayed(30)) { songs, history ->
        val byId = songs.associateBy { it.id }
        history.mapNotNull { byId[it.songId] }
    }

    val recentlyAddedSongs: Flow<List<Song>> = _allSongs.map { songs ->
        songs.sortedByDescending { it.dateAddedSec }.take(30)
    }

    val albums: Flow<List<Album>> = _allSongs.map { songs ->
        songs.groupBy { it.albumId }
            .values
            .map { albumSongs ->
                val first = albumSongs.first()
                Album(
                    id = first.albumId,
                    name = first.album,
                    artist = first.artist,
                    artworkUri = first.artworkUri,
                    songCount = albumSongs.size,
                    year = albumSongs.maxOfOrNull { it.year } ?: 0
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val artists: Flow<List<Artist>> = _allSongs.map { songs ->
        songs.groupBy { it.artist }
            .values
            .map { artistSongs ->
                val first = artistSongs.first()
                Artist(
                    id = first.artistId,
                    name = first.artist,
                    albumCount = artistSongs.distinctBy { it.albumId }.size,
                    songCount = artistSongs.size,
                    artworkUri = first.artworkUri
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val folders: Flow<List<MusicFolder>> = combine(_allSongs, folderDao.observeFavoriteFolderPaths()) { songs, favPaths ->
        val favSet = favPaths.toSet()
        songs.groupBy { it.folderPath }
            .values
            .map { folderSongs ->
                val first = folderSongs.first()
                MusicFolder(
                    path = first.folderPath,
                    name = first.folderName,
                    songCount = folderSongs.size,
                    isFavorite = first.folderPath in favSet
                )
            }
            .sortedWith(compareByDescending<MusicFolder> { it.isFavorite }.thenBy { it.name.lowercase() })
    }

    val playlistsWithSongs: Flow<List<PlaylistWithSongs>> = combine(
        playlistDao.observePlaylists(),
        playlistDao.observeAllPlaylistSongs(),
        _allSongs
    ) { playlists, refs, songs ->
        val songMap = songs.associateBy { it.id }
        val refsByPlaylist = refs.groupBy { it.playlistId }
        playlists.map { playlist ->
            val orderedSongs = refsByPlaylist[playlist.id]
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { songMap[it.songId] }
            PlaylistWithSongs(
                id = playlist.id,
                name = playlist.name,
                createdAt = playlist.createdAt,
                songs = orderedSongs
            )
        }
    }

    val customEqualizerPresets: Flow<List<CustomPresetEntity>> = presetDao.observeCustomPresets()

    suspend fun refreshLibrary() {
        if (_isScanning.value) return
        _isScanning.value = true
        try {
            val minDuration = preferences.appSettings.first().minTrackDurationSec
            val (songs, genreList) = scanner.scanDeviceMusic(minDurationSec = minDuration)
            _allSongs.value = songs
            _genres.value = genreList
        } finally {
            _isScanning.value = false
        }
    }

    fun sortSongs(songs: List<Song>, order: SortOrder, direction: SortDirection): List<Song> {
        val comparator: Comparator<Song> = when (order) {
            SortOrder.NAME -> compareBy { it.title.lowercase() }
            SortOrder.ARTIST -> compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            SortOrder.ALBUM -> compareBy<Song> { it.album.lowercase() }.thenBy { it.trackNumber }
            SortOrder.DURATION -> compareBy { it.durationMs }
            SortOrder.DATE_ADDED -> compareBy { it.dateAddedSec }
        }
        return if (direction == SortDirection.ASCENDING) {
            songs.sortedWith(comparator)
        } else {
            songs.sortedWith(comparator.reversed())
        }
    }

    suspend fun toggleFavorite(songId: Long) {
        if (favoriteDao.isFavorite(songId)) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.addFavorite(FavoriteSongEntity(songId = songId))
        }
    }

    suspend fun recordSongPlayed(songId: Long) {
        if (songId > 0L) {
            historyDao.recordPlay(songId)
        }
    }

    suspend fun clearPlaybackHistory() {
        historyDao.clearHistory()
    }

    suspend fun toggleFavoriteFolder(folderPath: String, folderName: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            folderDao.removeFavoriteFolder(folderPath)
        } else {
            folderDao.addFavoriteFolder(FavoriteFolderEntity(folderPath = folderPath, folderName = folderName))
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val maxPos = playlistDao.getMaxPosition(playlistId)
        playlistDao.insertPlaylistSong(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = maxPos + 1
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) {
        playlistDao.reorderPlaylist(playlistId, orderedSongIds)
    }

    suspend fun saveCustomPreset(
        name: String,
        bandLevelsMillibels: List<Int>,
        bassBoost: Int,
        virtualizer: Int,
        loudness: Int,
        preampDb: Float
    ) {
        presetDao.insertPreset(
            CustomPresetEntity(
                name = name.trim(),
                bandLevelsCsv = bandLevelsMillibels.joinToString(","),
                bassBoost = bassBoost,
                virtualizer = virtualizer,
                loudness = loudness,
                preampDb = preampDb
            )
        )
    }

    suspend fun deleteCustomPreset(presetId: Long) {
        presetDao.deletePreset(presetId)
    }

    fun observeManualLyrics(songId: Long): Flow<TrackLyricsEntity?> = lyricsDao.observeLyricsForSong(songId)

    suspend fun getManualLyrics(songId: Long): String? = lyricsDao.getLyricsForSong(songId)?.rawLyrics

    suspend fun saveManualLyrics(songId: Long, rawLyrics: String) {
        if (rawLyrics.isBlank()) {
            lyricsDao.deleteLyrics(songId)
        } else {
            lyricsDao.saveLyrics(TrackLyricsEntity(songId = songId, rawLyrics = rawLyrics))
        }
    }

    suspend fun getAudioFileInfo(song: Song): AudioFileInfo = scanner.getAudioFileInfo(song)

    suspend fun resolveSafUriToSong(uri: Uri): Song? = scanner.resolveSafUriToSong(uri)

    suspend fun scanSafTree(treeUri: Uri): List<Song> = scanner.scanSafTree(treeUri)
}
