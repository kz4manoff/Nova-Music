package com.novamusic.player.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.novamusic.player.database.entities.CustomPresetEntity
import com.novamusic.player.database.entities.FavoriteFolderEntity
import com.novamusic.player.database.entities.FavoriteSongEntity
import com.novamusic.player.database.entities.PlaybackHistoryEntity
import com.novamusic.player.database.entities.PlaylistEntity
import com.novamusic.player.database.entities.PlaylistSongCrossRef
import com.novamusic.player.database.entities.TrackLyricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT songId FROM favorite_songs ORDER BY addedAt DESC")
    fun observeFavoriteSongIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 40): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 40): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE songId = :songId LIMIT 1")
    suspend fun getHistoryForSong(songId: Long): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: PlaybackHistoryEntity)

    @Transaction
    suspend fun recordPlay(songId: Long) {
        val existing = getHistoryForSong(songId)
        val updated = if (existing != null) {
            existing.copy(
                lastPlayedAt = System.currentTimeMillis(),
                playCount = existing.playCount + 1
            )
        } else {
            PlaybackHistoryEntity(
                songId = songId,
                lastPlayedAt = System.currentTimeMillis(),
                playCount = 1
            )
        }
        upsertHistory(updated)
    }

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistSongs(playlistId: Long): Flow<List<PlaylistSongCrossRef>>

    @Query("SELECT * FROM playlist_songs ORDER BY position ASC")
    fun observeAllPlaylistSongs(): Flow<List<PlaylistSongCrossRef>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId ->
            updateSongPosition(playlistId, songId, index)
        }
    }
}

@Dao
interface FolderDao {
    @Query("SELECT folderPath FROM favorite_folders ORDER BY addedAt DESC")
    fun observeFavoriteFolderPaths(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteFolder(entity: FavoriteFolderEntity)

    @Query("DELETE FROM favorite_folders WHERE folderPath = :folderPath")
    suspend fun removeFavoriteFolder(folderPath: String)
}

@Dao
interface EqualizerPresetDao {
    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAt DESC")
    fun observeCustomPresets(): Flow<List<CustomPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CustomPresetEntity): Long

    @Query("DELETE FROM custom_eq_presets WHERE id = :presetId")
    suspend fun deletePreset(presetId: Long)
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM track_lyrics WHERE songId = :songId LIMIT 1")
    fun observeLyricsForSong(songId: Long): Flow<TrackLyricsEntity?>

    @Query("SELECT * FROM track_lyrics WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSong(songId: Long): TrackLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(entity: TrackLyricsEntity)

    @Query("DELETE FROM track_lyrics WHERE songId = :songId")
    suspend fun deleteLyrics(songId: Long)
}
