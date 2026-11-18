package com.novamusic.player.wrapped

import androidx.compose.runtime.Immutable

enum class WrappedPeriod(val shortLabel: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    PREVIOUS_YEAR("Last Year"),
    ALL_TIME("All Time")
}

@Immutable
data class WrappedPeriodRange(
    val period: WrappedPeriod,
    val startMs: Long,
    val endMs: Long,
    val displayTitle: String,
    val subtitle: String
)

@Immutable
data class WrappedTopArtist(
    val name: String,
    val artworkUri: String,
    val totalListeningMs: Long,
    val playCount: Int,
    val uniqueTracksCount: Int,
    val sharePercent: Int
)

@Immutable
data class WrappedTopSong(
    val rank: Int,
    val songId: Long,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String,
    val playCount: Int,
    val totalListeningMs: Long
)

@Immutable
data class WrappedTopAlbum(
    val rank: Int,
    val albumId: Long,
    val name: String,
    val artist: String,
    val artworkUri: String,
    val playCount: Int,
    val totalListeningMs: Long
)

@Immutable
data class WrappedGenreStat(
    val name: String,
    val percent: Int,
    val totalListeningMs: Long,
    val playCount: Int
)

@Immutable
data class WrappedHourStat(
    val hourOfDay: Int, // 0..23
    val totalListeningMs: Long,
    val playCount: Int
)

@Immutable
data class WrappedPersonality(
    val archetypeTitle: String,
    val badgeLabel: String,
    val description: String,
    val highlights: List<String>
)

@Immutable
data class WrappedSummaryStats(
    val periodRange: WrappedPeriodRange,
    val hasData: Boolean,
    val totalListeningMs: Long = 0L,
    val totalPlays: Int = 0,
    val totalSessions: Int = 0,
    val uniqueTracks: Int = 0,
    val uniqueArtists: Int = 0,
    val completedPlays: Int = 0,
    val skippedCount: Int = 0,
    val activeDaysCount: Int = 0,
    val longestStreakDays: Int = 0,
    val topArtist: WrappedTopArtist? = null,
    val topSongs: List<WrappedTopSong> = emptyList(),
    val topAlbums: List<WrappedTopAlbum> = emptyList(),
    val hasReliableGenres: Boolean = false,
    val topGenres: List<WrappedGenreStat> = emptyList(),
    val hourlyActivity: List<WrappedHourStat> = emptyList(),
    val peakHour: Int = 0,
    val peakWindowLabel: String = "",
    val mostActiveDayLabel: String = "",
    val personality: WrappedPersonality? = null,
    val primaryArtworkUri: String = ""
)
