package com.novamusic.player.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.novamusic.player.database.dao.EqualizerPresetDao
import com.novamusic.player.database.dao.FavoriteDao
import com.novamusic.player.database.dao.FolderDao
import com.novamusic.player.database.dao.HistoryDao
import com.novamusic.player.database.dao.ListeningDao
import com.novamusic.player.database.dao.LyricsDao
import com.novamusic.player.database.dao.PlaylistDao
import com.novamusic.player.database.entities.CustomPresetEntity
import com.novamusic.player.database.entities.FavoriteFolderEntity
import com.novamusic.player.database.entities.FavoriteSongEntity
import com.novamusic.player.database.entities.ListeningEventEntity
import com.novamusic.player.database.entities.PlaybackHistoryEntity
import com.novamusic.player.database.entities.PlaylistEntity
import com.novamusic.player.database.entities.PlaylistSongCrossRef
import com.novamusic.player.database.entities.TrackLyricsEntity

@Database(
    entities = [
        FavoriteSongEntity::class,
        PlaybackHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteFolderEntity::class,
        CustomPresetEntity::class,
        TrackLyricsEntity::class,
        ListeningEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun folderDao(): FolderDao
    abstract fun equalizerPresetDao(): EqualizerPresetDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun listeningDao(): ListeningDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `listening_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `trackKey` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `albumId` INTEGER NOT NULL,
                        `genre` TEXT NOT NULL,
                        `artworkUri` TEXT NOT NULL,
                        `trackDurationMs` INTEGER NOT NULL,
                        `listenedMs` INTEGER NOT NULL,
                        `startedAtMs` INTEGER NOT NULL,
                        `endedAtMs` INTEGER NOT NULL,
                        `localYear` INTEGER NOT NULL,
                        `localMonth` INTEGER NOT NULL,
                        `localDayKey` INTEGER NOT NULL,
                        `hourOfDay` INTEGER NOT NULL,
                        `countedPlay` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `skipped` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_listening_events_sessionId` ON `listening_events` (`sessionId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_listening_events_startedAtMs` ON `listening_events` (`startedAtMs`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_listening_events_localYear_localMonth` ON `listening_events` (`localYear`, `localMonth`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_music.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
