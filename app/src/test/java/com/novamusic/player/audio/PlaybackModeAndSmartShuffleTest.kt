package com.novamusic.player.audio

import com.novamusic.player.data.model.PlaybackMode
import com.novamusic.player.data.model.RepeatModeOption
import com.novamusic.player.database.entities.PlaybackHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackModeAndSmartShuffleTest {

    private fun testTrack(
        id: Long,
        artist: String? = "Artist $id",
        artistId: Long = id,
        album: String? = "Album $id",
        albumId: Long = id,
        genre: String? = "Electronic",
        folderPath: String? = "/music/default",
        year: Int = 2024
    ): SmartShuffleTrack = SmartShuffleTrack(
        id = id,
        title = "Track $id",
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        genre = genre,
        folderPath = folderPath,
        year = year
    )

    @Test
    fun `playback mode cycles in exact 5-step sequence`() {
        var mode = PlaybackMode.SEQUENTIAL
        val visited = mutableListOf(mode)
        repeat(5) {
            mode = mode.next()
            visited += mode
        }
        assertEquals(
            listOf(
                PlaybackMode.SEQUENTIAL,
                PlaybackMode.SHUFFLE,
                PlaybackMode.REPEAT_ALL,
                PlaybackMode.REPEAT_ONE,
                PlaybackMode.SMART_SHUFFLE,
                PlaybackMode.SEQUENTIAL
            ),
            visited
        )
    }

    @Test
    fun `legacy shuffle and repeat preferences migrate cleanly to PlaybackMode`() {
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackMode.fromLegacy(false, RepeatModeOption.OFF))
        assertEquals(PlaybackMode.SHUFFLE, PlaybackMode.fromLegacy(true, RepeatModeOption.OFF))
        assertEquals(PlaybackMode.REPEAT_ALL, PlaybackMode.fromLegacy(false, RepeatModeOption.ALL))
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackMode.fromLegacy(false, RepeatModeOption.ONE))
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackMode.fromLegacy(true, RepeatModeOption.ONE))
    }

    @Test
    fun `smart shuffle keeps currently playing song anchored at index zero`() {
        val songs = (1L..6L).map { testTrack(id = it) }
        val current = songs[3]

        val smart = SmartShuffleEngine.buildSmartQueueTracks(
            currentTrack = current,
            queue = songs,
            random = Random(42)
        )

        assertEquals(songs.size, smart.size)
        assertEquals(current.id, smart.first().id)
        assertEquals(songs.map { it.id }.toSet(), smart.map { it.id }.toSet())
    }

    @Test
    fun `smart shuffle prioritizes related genre and metadata over unrelated tracks`() {
        val current = testTrack(id = 1L, artist = "Daft Punk", artistId = 10L, album = "Discovery", albumId = 100L, genre = "French House", year = 2001)
        val relatedGenre = testTrack(id = 2L, artist = "Justice", artistId = 20L, album = "Cross", albumId = 200L, genre = "French House", year = 2007)
        val relatedEra = testTrack(id = 3L, artist = "Modjo", artistId = 30L, album = "Modjo", albumId = 300L, genre = "French House", year = 2001)
        val unrelated = testTrack(id = 4L, artist = "Mozart", artistId = 40L, album = "Requiem", albumId = 400L, genre = "Classical", year = 1791)

        val smart = SmartShuffleEngine.buildSmartQueueTracks(
            currentTrack = current,
            queue = listOf(unrelated, relatedEra, current, relatedGenre),
            random = Random(7)
        )

        assertEquals(1L, smart.first().id)
        // Unrelated classical track should rank after French House tracks
        assertEquals(4L, smart.last().id)
    }

    @Test
    fun `smart shuffle penalizes recently played songs to avoid immediate repetition`() {
        val now = 1_750_000_000_000L
        val current = testTrack(id = 1L, artist = "Alpha", artistId = 1L, genre = "Ambient")
        val freshTrack = testTrack(id = 2L, artist = "Beta", artistId = 2L, genre = "Ambient")
        val justPlayedTrack = testTrack(id = 3L, artist = "Gamma", artistId = 3L, genre = "Ambient")

        val history = mapOf(
            3L to PlaybackHistoryEntity(songId = 3L, lastPlayedAt = now - 60_000L, playCount = 5)
        )

        val smart = SmartShuffleEngine.buildSmartQueueTracks(
            currentTrack = current,
            queue = listOf(current, justPlayedTrack, freshTrack),
            historyMap = history,
            recentSessionSongIds = listOf(3L),
            nowMs = now,
            random = Random(123)
        )

        assertEquals(listOf(1L, 2L, 3L), smart.map { it.id })
    }

    @Test
    fun `smart shuffle spreads artists to avoid consecutive same-artist runs when alternatives exist`() {
        val s1 = testTrack(id = 1L, artist = "Artist A", artistId = 10L, genre = "Indie")
        val s2 = testTrack(id = 2L, artist = "Artist A", artistId = 10L, genre = "Indie")
        val s3 = testTrack(id = 3L, artist = "Artist A", artistId = 10L, genre = "Indie")
        val s4 = testTrack(id = 4L, artist = "Artist B", artistId = 20L, genre = "Indie")
        val s5 = testTrack(id = 5L, artist = "Artist C", artistId = 30L, genre = "Indie")

        val smart = SmartShuffleEngine.buildSmartQueueTracks(
            currentTrack = s1,
            queue = listOf(s1, s2, s3, s4, s5),
            random = Random(99)
        )

        // Track right after Artist A (s1) must NOT be Artist A when Artist B/C are available
        assertNotEquals("Artist A", smart[1].artist)
        assertTrue(smart.drop(1).take(2).any { it.artist != "Artist A" })
    }

    @Test
    fun `smart shuffle falls back safely with sparse or unknown placeholder metadata`() {
        val tracks = (1L..5L).map {
            testTrack(
                id = it,
                artist = "Unknown Artist",
                artistId = -1L,
                album = "Unknown Album",
                albumId = -1L,
                genre = "Unknown",
                year = 0
            )
        }

        val smart = SmartShuffleEngine.buildSmartQueueTracks(
            currentTrack = tracks[2],
            queue = tracks,
            random = Random(5)
        )

        assertEquals(5, smart.size)
        assertEquals(3L, smart.first().id)
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), smart.map { it.id }.toSet())
    }

    @Test
    fun `smart shuffle handles null blank and partial metadata safely without throwing`() {
        val tracks = listOf(
            testTrack(id = 1L, artist = null, artistId = 1L, album = null, albumId = 1L, genre = null, folderPath = null, year = 0),
            testTrack(id = 2L, artist = "   ", artistId = 2L, album = "", albumId = 2L, genre = "  ", folderPath = "", year = 0),
            testTrack(id = 3L, artist = "<unknown>", artistId = 3L, album = "Local File", albumId = 3L, genre = "Local", folderPath = "/music", year = 0),
            testTrack(id = 4L, artist = "Real Artist", artistId = 4L, album = null, albumId = -1L, genre = "Synthwave", folderPath = "/music", year = 2022)
        )

        val smart = SmartShuffleEngine.buildSmartQueueTracks(
            currentTrack = tracks[0],
            queue = tracks,
            random = Random(11)
        )

        assertEquals(4, smart.size)
        assertEquals(1L, smart.first().id)
        assertEquals(setOf(1L, 2L, 3L, 4L), smart.map { it.id }.toSet())
    }

    @Test
    fun `restoring sequential order recovers original sequence and preserves active song`() {
        val originalOrder = listOf(10L, 20L, 30L, 40L, 50L)
        val smartShuffledQueue = listOf(30L, 50L, 10L, 40L, 20L)

        val (restoredIndices, anchorPos) = SmartShuffleEngine.restoreSequentialOrderIndices(
            currentIndex = 0, // playing song 30L at index 0
            currentQueueIds = smartShuffledQueue,
            originalOrderIds = originalOrder
        )

        val restoredIds = restoredIndices.map { smartShuffledQueue[it] }
        assertEquals(originalOrder, restoredIds)
        assertEquals(2, anchorPos)
        assertFalse(restoredIds.isEmpty())
    }
}
