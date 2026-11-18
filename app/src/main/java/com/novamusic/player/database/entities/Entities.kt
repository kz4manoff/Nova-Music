package com.novamusic.player.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val songId: Long,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_folders")
data class FavoriteFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_eq_presets")
data class CustomPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bandLevelsCsv: String,
    val bassBoost: Int,
    val virtualizer: Int,
    val loudness: Int,
    val preampDb: Float,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "track_lyrics")
data class TrackLyricsEntity(
    @PrimaryKey val songId: Long,
    val rawLyrics: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "listening_events",
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["startedAtMs"]),
        Index(value = ["localYear", "localMonth"])
    ]
)
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: String,
    val songId: Long,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val artworkUri: String,
    val trackDurationMs: Long,
    val listenedMs: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val localYear: Int,
    val localMonth: Int,
    val localDayKey: Int,
    val hourOfDay: Int,
    val countedPlay: Boolean,
    val completed: Boolean,
    val skipped: Boolean
)
