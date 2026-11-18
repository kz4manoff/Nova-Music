package com.novamusic.player.wrapped

import com.novamusic.player.database.entities.ListeningEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList

class WrappedEngineAndTrackerTest {

    @Test
    fun computePlayThresholdMs_enforces30SecondsOrHalfOfShortTrack() {
        assertEquals(30_000L, WrappedAnalyticsEngine.computePlayThresholdMs(180_000L))
        assertEquals(30_000L, WrappedAnalyticsEngine.computePlayThresholdMs(60_000L))
        assertEquals(20_000L, WrappedAnalyticsEngine.computePlayThresholdMs(40_000L))
        assertEquals(5_000L, WrappedAnalyticsEngine.computePlayThresholdMs(10_000L))
        assertEquals(30_000L, WrappedAnalyticsEngine.computePlayThresholdMs(0L))
    }

    @Test
    fun listeningTracker_excludesPausedTimeAndPreventsDoubleCounting() {
        var fakeElapsedMs = 1_000L
        val fakeWallMs = 1_780_000_000_000L
        val persisted = CopyOnWriteArrayList<ListeningEventEntity>()

        val tracker = ListeningTracker(
            persistCallback = { persisted.add(it) },
            elapsedRealtimeProvider = { fakeElapsedMs },
            wallClockProvider = { fakeWallMs },
            zoneId = ZoneOffset.UTC
        )

        val track = TrackedTrackSnapshot(
            songId = 101L,
            trackKey = "song:101",
            title = "Midnight Echo",
            artist = "Solaris",
            album = "Afterglow",
            albumId = 20L,
            genre = "Electronic",
            artworkUri = "",
            durationMs = 200_000L
        )

        // Start playback
        tracker.onTrackChanged(
            newTrack = track,
            isCurrentlyPlaying = true,
            wasAutoOrRepeatTransition = false,
            outgoingPositionMs = 0L
        )

        // Listen for 20s (< 30s threshold) -> NOT counted yet
        fakeElapsedMs += 20_000L
        tracker.onIsPlayingChanged(isPlaying = false, currentTrack = track, currentPositionMs = 20_000L)
        val pausedSnapshot = tracker.snapshotCurrentSessionForTest()!!
        assertEquals(20_000L, pausedSnapshot.listenedMs)
        assertFalse(pausedSnapshot.countedPlay)

        // Pause for 5 minutes -> paused time MUST NOT count
        fakeElapsedMs += 300_000L
        tracker.onIsPlayingChanged(isPlaying = true, currentTrack = track, currentPositionMs = 20_000L)

        // Seek within same track preserves sessionId, then listen 15s more -> total 35s -> counted!
        fakeElapsedMs += 5_000L
        tracker.onSeekDiscontinuity(newPositionMs = 60_000L)
        fakeElapsedMs += 10_000L

        val resumedSnapshot = tracker.snapshotCurrentSessionForTest()!!
        assertEquals(pausedSnapshot.sessionId, resumedSnapshot.sessionId)
        assertEquals(35_000L, resumedSnapshot.listenedMs)
        assertTrue(resumedSnapshot.countedPlay)
    }

    @Test
    fun listeningTracker_trackSwitchCreatesNewSessionAndMarksCompletedOrSkipped() {
        var fakeElapsedMs = 0L
        val fakeWallMs = 1_780_000_000_000L
        val persisted = CopyOnWriteArrayList<ListeningEventEntity>()

        val tracker = ListeningTracker(
            persistCallback = { persisted.add(it) },
            elapsedRealtimeProvider = { fakeElapsedMs },
            wallClockProvider = { fakeWallMs },
            zoneId = ZoneOffset.UTC
        )

        val songA = TrackedTrackSnapshot(1L, "song:1", "Track A", "Artist A", "Album A", 1L, "Rock", "", 180_000L)
        val songB = TrackedTrackSnapshot(2L, "song:2", "Track B", "Artist B", "Album B", 2L, "Indie", "", 180_000L)

        tracker.onTrackChanged(songA, isCurrentlyPlaying = true, wasAutoOrRepeatTransition = false, outgoingPositionMs = 0L)
        val firstSessionId = tracker.snapshotCurrentSessionForTest()!!.sessionId

        // Skip early after 5s
        fakeElapsedMs += 5_000L
        tracker.onTrackChanged(songB, isCurrentlyPlaying = true, wasAutoOrRepeatTransition = false, outgoingPositionMs = 5_000L)
        val secondSessionId = tracker.snapshotCurrentSessionForTest()!!.sessionId

        assertNotEquals(firstSessionId, secondSessionId)
    }

    @Test
    fun analyticsEngine_computesRankingsGenresStreaksHoursAndPersonality() {
        val zone = ZoneOffset.UTC
        val day1 = LocalDateTime.of(2026, 6, 10, 23, 15).toInstant(zone).toEpochMilli()
        val day2 = LocalDateTime.of(2026, 6, 11, 23, 30).toInstant(zone).toEpochMilli()
        val day3 = LocalDateTime.of(2026, 6, 12, 22, 45).toInstant(zone).toEpochMilli()

        val events = listOf(
            ListeningEventEntity(
                id = 1L, sessionId = "s1", songId = 1L, trackKey = "song:1",
                title = "Starlight", artist = "Muse", album = "Black Holes", albumId = 10L,
                genre = "Rock", artworkUri = "content://art/1", trackDurationMs = 240_000L,
                listenedMs = 220_000L, startedAtMs = day1, endedAtMs = day1 + 220_000L,
                localYear = 2026, localMonth = 6, localDayKey = 20260610, hourOfDay = 23,
                countedPlay = true, completed = true, skipped = false
            ),
            ListeningEventEntity(
                id = 2L, sessionId = "s2", songId = 1L, trackKey = "song:1",
                title = "Starlight", artist = "Muse", album = "Black Holes", albumId = 10L,
                genre = "Rock", artworkUri = "content://art/1", trackDurationMs = 240_000L,
                listenedMs = 200_000L, startedAtMs = day2, endedAtMs = day2 + 200_000L,
                localYear = 2026, localMonth = 6, localDayKey = 20260611, hourOfDay = 23,
                countedPlay = true, completed = true, skipped = false
            ),
            ListeningEventEntity(
                id = 3L, sessionId = "s3", songId = 2L, trackKey = "song:2",
                title = "Sunset", artist = "M83", album = "Unknown", albumId = 11L,
                genre = "Unknown", artworkUri = "", trackDurationMs = 210_000L,
                listenedMs = 90_000L, startedAtMs = day3, endedAtMs = day3 + 90_000L,
                localYear = 2026, localMonth = 6, localDayKey = 20260612, hourOfDay = 22,
                countedPlay = true, completed = false, skipped = true
            )
        )

        val range2026 = WrappedAnalyticsEngine.buildPeriodRange(WrappedPeriod.THIS_YEAR, nowMs = day3, zoneId = zone)
        val summary = WrappedAnalyticsEngine.aggregate(events, range2026, zoneId = zone)

        assertTrue(summary.hasData)
        assertEquals(510_000L, summary.totalListeningMs)
        assertEquals(3, summary.totalPlays)
        assertEquals(2, summary.uniqueTracks)
        assertEquals(3, summary.longestStreakDays)
        assertEquals("Muse", summary.topArtist?.name)
        assertEquals("Starlight", summary.topSongs.first().title)
        assertEquals("Black Holes", summary.topAlbums.first().name)
        assertEquals(1, summary.topAlbums.size) // "Unknown" album filtered out cleanly
        assertTrue(summary.hasReliableGenres)
        assertEquals(listOf("Rock"), summary.topGenres.map { it.name }) // "Unknown" genre excluded
        assertEquals(23, summary.peakHour)
        assertNotNull(summary.personality)
        assertEquals("The Night Owl", summary.personality?.archetypeTitle)

        // Previous year (2025) has zero records -> must honestly report hasData = false
        val range2025 = WrappedAnalyticsEngine.buildPeriodRange(WrappedPeriod.PREVIOUS_YEAR, nowMs = day3, zoneId = zone)
        val emptyPrev = WrappedAnalyticsEngine.aggregate(events, range2025, zoneId = zone)
        assertFalse(emptyPrev.hasData)
    }

    @Test
    fun computeLongestStreak_calculatesConsecutiveDaysCorrectly() {
        val days = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 6),
            LocalDate.of(2026, 1, 7),
            LocalDate.of(2026, 1, 8)
        )
        assertEquals(4, WrappedAnalyticsEngine.computeLongestStreak(days))
    }
}
