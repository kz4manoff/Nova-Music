package com.novamusic.player.wrapped

import com.novamusic.player.database.entities.ListeningEventEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure-Kotlin listening validation rules and statistical aggregation engine for NOVA WRAPPED.
 * Every metric is computed strictly from persisted ListeningEventEntity rows — zero fabricated values.
 */
object WrappedAnalyticsEngine {

    private const val STANDARD_PLAY_THRESHOLD_MS = 30_000L
    private const val MIN_SHORT_TRACK_PLAY_MS = 1_000L
    private const val MIN_RELIABLE_ARTWORK_FALLBACK = ""

    private val invalidGenreTokens = setOf(
        "",
        "unknown",
        "<unknown>",
        "unknown genre",
        "none",
        "null",
        "n/a",
        "other",
        "misc"
    )

    private val invalidArtistTokens = setOf(
        "",
        "unknown",
        "<unknown>",
        "unknown artist"
    )

    private val invalidAlbumTokens = setOf(
        "",
        "unknown",
        "<unknown>",
        "unknown album"
    )

    /**
     * Play count threshold rule:
     * - Count play after >= 30s of active listening, OR >= 50% of tracks shorter than 60s.
     */
    fun computePlayThresholdMs(trackDurationMs: Long): Long {
        if (trackDurationMs <= 0L) return STANDARD_PLAY_THRESHOLD_MS
        if (trackDurationMs < STANDARD_PLAY_THRESHOLD_MS * 2L) {
            return (trackDurationMs / 2L).coerceAtLeast(MIN_SHORT_TRACK_PLAY_MS)
        }
        return STANDARD_PLAY_THRESHOLD_MS
    }

    fun buildPeriodRange(
        period: WrappedPeriod,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WrappedPeriodRange {
        val nowDate = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

        return when (period) {
            WrappedPeriod.THIS_MONTH -> {
                val ym = YearMonth.from(nowDate)
                val startMs = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMs = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                WrappedPeriodRange(
                    period = period,
                    startMs = startMs,
                    endMs = endMs,
                    displayTitle = "This Month",
                    subtitle = ym.format(monthFormatter)
                )
            }

            WrappedPeriod.LAST_MONTH -> {
                val ym = YearMonth.from(nowDate).minusMonths(1)
                val startMs = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMs = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                WrappedPeriodRange(
                    period = period,
                    startMs = startMs,
                    endMs = endMs,
                    displayTitle = "Last Month",
                    subtitle = ym.format(monthFormatter)
                )
            }

            WrappedPeriod.THIS_YEAR -> {
                val year = nowDate.year
                val startMs = LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMs = LocalDate.of(year + 1, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                WrappedPeriodRange(
                    period = period,
                    startMs = startMs,
                    endMs = endMs,
                    displayTitle = "$year Wrapped",
                    subtitle = "Jan 1 – Dec 31, $year"
                )
            }

            WrappedPeriod.PREVIOUS_YEAR -> {
                val year = nowDate.year - 1
                val startMs = LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMs = LocalDate.of(year + 1, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                WrappedPeriodRange(
                    period = period,
                    startMs = startMs,
                    endMs = endMs,
                    displayTitle = "$year Wrapped",
                    subtitle = "Jan 1 – Dec 31, $year"
                )
            }

            WrappedPeriod.ALL_TIME -> {
                WrappedPeriodRange(
                    period = period,
                    startMs = 0L,
                    endMs = Long.MAX_VALUE,
                    displayTitle = "All Time Wrapped",
                    subtitle = "Complete NOVA MUSIC history"
                )
            }
        }
    }

    fun aggregate(
        events: List<ListeningEventEntity>,
        range: WrappedPeriodRange,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WrappedSummaryStats {
        val inRange = events.filter {
            it.startedAtMs in range.startMs..range.endMs && it.listenedMs > 0L
        }
        val countedEvents = inRange.filter { it.countedPlay }

        if (countedEvents.isEmpty() && inRange.sumOf { it.listenedMs } < STANDARD_PLAY_THRESHOLD_MS) {
            return WrappedSummaryStats(periodRange = range, hasData = false)
        }

        val effectiveSource = if (countedEvents.isNotEmpty()) countedEvents else inRange
        val totalListeningMs = inRange.sumOf { it.listenedMs.coerceAtLeast(0L) }
        val totalPlays = countedEvents.size
        val uniqueTracks = effectiveSource.map { it.trackKey }.distinct().size
        val validArtistList = effectiveSource
            .map { it.artist.trim() }
            .filter { !isInvalidToken(it, invalidArtistTokens) }
        val uniqueArtists = validArtistList.map { it.lowercase(Locale.ROOT) }.distinct().size

        val completedPlays = inRange.count { it.completed }
        val skippedCount = inRange.count { it.skipped }

        // Top Artist (ranked by active listening time, then play count)
        val artistGroups = effectiveSource
            .filter { !isInvalidToken(it.artist.trim(), invalidArtistTokens) }
            .groupBy { it.artist.trim().lowercase(Locale.ROOT) }
        val topArtist = artistGroups.values
            .map { group ->
                val displayName = group.first().artist.trim()
                val art = group.firstOrNull { it.artworkUri.isNotBlank() }?.artworkUri ?: MIN_RELIABLE_ARTWORK_FALLBACK
                val ms = group.sumOf { it.listenedMs }
                val plays = group.count { it.countedPlay }.coerceAtLeast(if (countedEvents.isEmpty()) group.size else 0)
                val uniq = group.map { it.trackKey }.distinct().size
                WrappedTopArtist(
                    name = displayName,
                    artworkUri = art,
                    totalListeningMs = ms,
                    playCount = plays,
                    uniqueTracksCount = uniq,
                    sharePercent = if (totalListeningMs > 0L) {
                        ((ms * 100.0) / totalListeningMs).roundToInt().coerceIn(1, 100)
                    } else 0
                )
            }
            .sortedWith(
                compareByDescending<WrappedTopArtist> { it.totalListeningMs }
                    .thenByDescending { it.playCount }
            )
            .firstOrNull()

        // Top 5 Songs (ranked by counted play count, then active listening time)
        val topSongs = effectiveSource
            .groupBy { it.trackKey }
            .values
            .map { group ->
                val latest = group.maxByOrNull { it.endedAtMs } ?: group.first()
                val art = group.firstOrNull { it.artworkUri.isNotBlank() }?.artworkUri ?: MIN_RELIABLE_ARTWORK_FALLBACK
                val plays = group.count { it.countedPlay }
                val ms = group.sumOf { it.listenedMs }
                WrappedTopSong(
                    rank = 0,
                    songId = latest.songId,
                    trackKey = latest.trackKey,
                    title = latest.title.ifBlank { "Unknown Track" },
                    artist = latest.artist.ifBlank { "Unknown Artist" },
                    album = latest.album,
                    artworkUri = art,
                    playCount = plays,
                    totalListeningMs = ms
                )
            }
            .sortedWith(
                compareByDescending<WrappedTopSong> { it.playCount }
                    .thenByDescending { it.totalListeningMs }
            )
            .take(5)
            .mapIndexed { idx, song -> song.copy(rank = idx + 1) }

        // Top Albums (excluding blank/unknown album placeholders)
        val topAlbums = effectiveSource
            .filter { !isInvalidToken(it.album.trim(), invalidAlbumTokens) }
            .groupBy {
                val cleanAlbum = it.album.trim().lowercase(Locale.ROOT)
                val cleanArtist = it.artist.trim().lowercase(Locale.ROOT)
                "$cleanAlbum||$cleanArtist"
            }
            .values
            .map { group ->
                val rep = group.firstOrNull { it.artworkUri.isNotBlank() } ?: group.first()
                WrappedTopAlbum(
                    rank = 0,
                    albumId = rep.albumId,
                    name = rep.album.trim(),
                    artist = rep.artist.trim().ifBlank { "Unknown Artist" },
                    artworkUri = rep.artworkUri,
                    playCount = group.count { it.countedPlay },
                    totalListeningMs = group.sumOf { it.listenedMs }
                )
            }
            .sortedWith(
                compareByDescending<WrappedTopAlbum> { it.totalListeningMs }
                    .thenByDescending { it.playCount }
            )
            .take(4)
            .mapIndexed { idx, album -> album.copy(rank = idx + 1) }

        // Reliable Genres
        val genreEvents = effectiveSource.filter { isReliableGenre(it.genre) }
        val totalGenreMs = genreEvents.sumOf { it.listenedMs }.coerceAtLeast(0L)
        val topGenres = if (genreEvents.isNotEmpty() && totalGenreMs > 0L) {
            genreEvents
                .groupBy { it.genre.trim().lowercase(Locale.ROOT) }
                .values
                .map { group ->
                    val ms = group.sumOf { it.listenedMs }
                    WrappedGenreStat(
                        name = formatGenreDisplayName(group.first().genre.trim()),
                        percent = ((ms * 100.0) / totalGenreMs).roundToInt().coerceAtLeast(1),
                        totalListeningMs = ms,
                        playCount = group.count { it.countedPlay }
                    )
                }
                .sortedByDescending { it.totalListeningMs }
                .take(5)
        } else {
            emptyList()
        }

        // 24-Hour Listening Activity Histogram
        val hourBucketsMs = LongArray(24)
        val hourBucketsPlays = IntArray(24)
        val dayOfWeekMs = LongArray(8) // 1 = Monday .. 7 = Sunday

        for (event in inRange) {
            val h = event.hourOfDay.coerceIn(0, 23)
            hourBucketsMs[h] += event.listenedMs
            if (event.countedPlay) hourBucketsPlays[h]++

            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.startedAtMs), zoneId)
            val dow = ldt.dayOfWeek.value
            dayOfWeekMs[dow] += event.listenedMs
        }

        val hourlyActivity = (0..23).map { hour ->
            WrappedHourStat(
                hourOfDay = hour,
                totalListeningMs = hourBucketsMs[hour],
                playCount = hourBucketsPlays[hour]
            )
        }
        val peakHour = hourlyActivity.maxByOrNull { it.totalListeningMs }?.hourOfDay ?: 0
        val peakWindowLabel = describeTimeWindow(peakHour)

        val peakDowValue = (1..7).maxByOrNull { dayOfWeekMs[it] } ?: 1
        val mostActiveDayLabel = DayOfWeek.of(peakDowValue)
            .getDisplayName(TextStyle.FULL, Locale.getDefault())

        // Streaks & Active Days
        val distinctDays = inRange
            .mapNotNull { parseLocalDayKey(it.localDayKey) }
            .distinct()
            .sorted()
        val longestStreakDays = computeLongestStreak(distinctDays)

        val personality = buildPersonality(
            inRange = inRange,
            hourBucketsMs = hourBucketsMs,
            totalListeningMs = totalListeningMs,
            totalPlays = totalPlays,
            uniqueTracks = uniqueTracks,
            completedPlays = completedPlays,
            topArtist = topArtist,
            longestStreakDays = longestStreakDays,
            peakWindowLabel = peakWindowLabel
        )

        val primaryArtworkUri = topSongs.firstOrNull { it.artworkUri.isNotBlank() }?.artworkUri
            ?: topArtist?.artworkUri?.takeIf { it.isNotBlank() }
            ?: topAlbums.firstOrNull { it.artworkUri.isNotBlank() }?.artworkUri
            ?: ""

        return WrappedSummaryStats(
            periodRange = range,
            hasData = true,
            totalListeningMs = totalListeningMs,
            totalPlays = totalPlays,
            totalSessions = inRange.size,
            uniqueTracks = uniqueTracks,
            uniqueArtists = uniqueArtists,
            completedPlays = completedPlays,
            skippedCount = skippedCount,
            activeDaysCount = distinctDays.size,
            longestStreakDays = longestStreakDays,
            topArtist = topArtist,
            topSongs = topSongs,
            topAlbums = topAlbums,
            hasReliableGenres = topGenres.isNotEmpty(),
            topGenres = topGenres,
            hourlyActivity = hourlyActivity,
            peakHour = peakHour,
            peakWindowLabel = peakWindowLabel,
            mostActiveDayLabel = mostActiveDayLabel,
            personality = personality,
            primaryArtworkUri = primaryArtworkUri
        )
    }

    private fun buildPersonality(
        inRange: List<ListeningEventEntity>,
        hourBucketsMs: LongArray,
        totalListeningMs: Long,
        totalPlays: Int,
        uniqueTracks: Int,
        completedPlays: Int,
        topArtist: WrappedTopArtist?,
        longestStreakDays: Int,
        peakWindowLabel: String
    ): WrappedPersonality {
        if (totalListeningMs <= 0L) {
            return WrappedPersonality(
                archetypeTitle = "The Quiet Listener",
                badgeLabel = "JUST STARTING",
                description = "Your NOVA MUSIC listening profile is taking shape as you discover your favorite tracks.",
                highlights = emptyList()
            )
        }

        val nightMs = (listOf(22, 23, 0, 1, 2, 3, 4).sumOf { hourBucketsMs[it] })
        val nightPct = ((nightMs * 100.0) / totalListeningMs).roundToInt()

        val morningMs = (5..10).sumOf { hourBucketsMs[it] }
        val morningPct = ((morningMs * 100.0) / totalListeningMs).roundToInt()

        val completionRate = if (inRange.isNotEmpty()) {
            ((completedPlays * 100.0) / inRange.size).roundToInt()
        } else 0

        val playsPerTrack = if (uniqueTracks > 0) totalPlays.toDouble() / uniqueTracks.toDouble() else 0.0
        val topArtistShare = topArtist?.sharePercent ?: 0

        return when {
            nightPct >= 38 -> WrappedPersonality(
                archetypeTitle = "The Night Owl",
                badgeLabel = "AFTER DARK",
                description = "Your headphones come alive when the city quiets down, turning late hours into your personal soundstage.",
                highlights = listOf(
                    "$nightPct% of your listening happened between 10 PM and 5 AM",
                    "Peak listening window: $peakWindowLabel",
                    "$uniqueTracks distinct tracks explored"
                )
            )

            morningPct >= 35 -> WrappedPersonality(
                archetypeTitle = "The Dawn Chaser",
                badgeLabel = "EARLY FREQUENCY",
                description = "You set the tone of every morning with music before the rest of the world wakes up.",
                highlights = listOf(
                    "$morningPct% of your listening took place before 11 AM",
                    "Peak listening window: $peakWindowLabel",
                    "$longestStreakDays-day listening streak"
                )
            )

            topArtist != null && topArtistShare >= 42 -> WrappedPersonality(
                archetypeTitle = "The Devotee",
                badgeLabel = "LOYAL LISTENER",
                description = "When an artist resonates with you, you dive straight into their entire universe on repeat.",
                highlights = listOf(
                    "${topArtist.name} accounted for $topArtistShare% of your listening time",
                    "${topArtist.playCount} plays across ${topArtist.uniqueTracksCount} tracks",
                    "Completion rate: $completionRate%"
                )
            )

            completionRate >= 72 && inRange.size >= 4 -> WrappedPersonality(
                archetypeTitle = "The Deep Diver",
                badgeLabel = "FULL SESSION",
                description = "No frantic skipping here—you let tracks breathe from opening note to final fade-out.",
                highlights = listOf(
                    "$completionRate% of your tracks played cleanly to completion",
                    "$totalPlays counted plays across $uniqueTracks tracks",
                    "Peak listening window: $peakWindowLabel"
                )
            )

            playsPerTrack >= 2.5 && uniqueTracks >= 3 -> WrappedPersonality(
                archetypeTitle = "The Replay Architect",
                badgeLabel = "ON HEAVY ROTATION",
                description = "You know exactly which anthems hit hardest and aren't afraid to keep them in relentless rotation.",
                highlights = listOf(
                    String.format(Locale.US, "%.1fx average plays per unique track", playsPerTrack),
                    "$totalPlays total plays across $uniqueTracks favorites",
                    "Longest streak: $longestStreakDays days"
                )
            )

            uniqueTracks >= 12 && playsPerTrack < 1.8 -> WrappedPersonality(
                archetypeTitle = "The Sonic Explorer",
                badgeLabel = "CURIOUS EAR",
                description = "Your queue is an ever-moving horizon—constantly jumping across fresh tracks, albums, and moods.",
                highlights = listOf(
                    "$uniqueTracks unique tracks played",
                    "Peak listening window: $peakWindowLabel",
                    "$completionRate% track completion rate"
                )
            )

            else -> WrappedPersonality(
                archetypeTitle = "The Rhythm Purist",
                badgeLabel = "STEADY PULSE",
                description = "Balanced, intentional, and consistent—music weaves seamlessly through every part of your day.",
                highlights = listOf(
                    "$totalPlays plays across $uniqueTracks distinct tracks",
                    "Peak listening window: $peakWindowLabel",
                    "Longest streak: $longestStreakDays days"
                )
            )
        }
    }

    fun isReliableGenre(raw: String?): Boolean {
        val cleaned = raw?.trim().orEmpty()
        if (isInvalidToken(cleaned, invalidGenreTokens)) return false
        // Exclude bare numeric ID3v1 tags like "12" or "(12)"
        if (cleaned.all { it.isDigit() || it == '(' || it == ')' }) return false
        return cleaned.length >= 2
    }

    private fun formatGenreDisplayName(raw: String): String {
        return raw.split(" ", "/", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
            .ifBlank { raw }
    }

    private fun isInvalidToken(value: String, invalidSet: Set<String>): Boolean {
        return value.lowercase(Locale.ROOT) in invalidSet
    }

    fun formatHourLabel(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        return when {
            h == 0 -> "12 AM"
            h < 12 -> "$h AM"
            h == 12 -> "12 PM"
            else -> "${h - 12} PM"
        }
    }

    fun describeTimeWindow(hour: Int): String = when (hour) {
        in 5..8 -> "Early Morning (5–9 AM)"
        in 9..11 -> "Late Morning (9 AM–12 PM)"
        in 12..16 -> "Afternoon Flow (12–5 PM)"
        in 17..20 -> "Golden Evening (5–9 PM)"
        in 21..23 -> "Late Night (9 PM–12 AM)"
        else -> "Midnight Hours (12–5 AM)"
    }

    fun computeLocalDayKey(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val d = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
        return d.year * 10_000 + d.monthValue * 100 + d.dayOfMonth
    }

    private fun parseLocalDayKey(dayKey: Int): LocalDate? {
        if (dayKey < 19700101) return null
        val y = dayKey / 10_000
        val m = (dayKey % 10_000) / 100
        val d = dayKey % 100
        return runCatching { LocalDate.of(y, m, d) }.getOrNull()
    }

    fun computeLongestStreak(sortedUniqueDays: List<LocalDate>): Int {
        if (sortedUniqueDays.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until sortedUniqueDays.size) {
            val prev = sortedUniqueDays[i - 1]
            val cur = sortedUniqueDays[i]
            if (prev.plusDays(1) == cur) {
                current++
                best = max(best, current)
            } else {
                current = 1
            }
        }
        return best
    }
}
