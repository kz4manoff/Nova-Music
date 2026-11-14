package com.novamusic.player.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.novamusic.player.database.dao.EqualizerPresetDao
import com.novamusic.player.database.dao.FavoriteDao
import com.novamusic.player.database.dao.FolderDao
import com.novamusic.player.database.dao.HistoryDao
import com.novamusic.player.database.dao.LyricsDao
import com.novamusic.player.database.dao.PlaylistDao
import com.novamusic.player.database.entities.CustomPresetEntity
import com.novamusic.player.database.entities.FavoriteFolderEntity
import com.novamusic.player.database.entities.FavoriteSongEntity
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
        TrackLyricsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun folderDao(): FolderDao
    abstract fun equalizerPresetDao(): EqualizerPresetDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_music.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
