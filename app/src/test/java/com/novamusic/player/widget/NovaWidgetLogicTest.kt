package com.novamusic.player.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaWidgetLogicTest {

    @Test
    fun selectWidgetSize_usesDefaultWhenOptionsMissing() {
        assertEquals(
            NovaWidgetSize.MEDIUM,
            NovaWidgetLogic.selectWidgetSize(NovaWidgetSize.MEDIUM, minHeightDp = 0, maxHeightDp = 0)
        )
        assertEquals(
            NovaWidgetSize.LARGE,
            NovaWidgetLogic.selectWidgetSize(NovaWidgetSize.LARGE, minHeightDp = -1, maxHeightDp = 0)
        )
    }

    @Test
    fun selectWidgetSize_switchesBetweenMediumAndLargeByHeightThreshold() {
        assertEquals(
            NovaWidgetSize.MEDIUM,
            NovaWidgetLogic.selectWidgetSize(NovaWidgetSize.LARGE, minHeightDp = 110, maxHeightDp = 140)
        )
        assertEquals(
            NovaWidgetSize.LARGE,
            NovaWidgetLogic.selectWidgetSize(NovaWidgetSize.MEDIUM, minHeightDp = 150, maxHeightDp = 185)
        )
        assertEquals(
            NovaWidgetSize.LARGE,
            NovaWidgetLogic.selectWidgetSize(NovaWidgetSize.MEDIUM, minHeightDp = 170, maxHeightDp = 0)
        )
    }

    @Test
    fun computeProgress_handlesBoundsAndZeroDuration() {
        assertEquals(0, NovaWidgetLogic.computeProgress(positionMs = 5_000L, durationMs = 0L))
        assertEquals(0, NovaWidgetLogic.computeProgress(positionMs = -100L, durationMs = 10_000L))
        assertEquals(500, NovaWidgetLogic.computeProgress(positionMs = 50_000L, durationMs = 100_000L))
        assertEquals(1000, NovaWidgetLogic.computeProgress(positionMs = 120_000L, durationMs = 100_000L))
    }

    @Test
    fun metadataFallbacks_handleNullAndBlankValuesGracefully() {
        assertEquals(
            "No track playing",
            NovaWidgetLogic.resolveTitle(null, emptyFallback = "No track playing")
        )
        assertEquals(
            "Midnight City",
            NovaWidgetLogic.resolveTitle("  Midnight City  ", emptyFallback = "No track playing")
        )
        assertEquals(
            "Tap to open NOVA MUSIC",
            NovaWidgetLogic.resolveArtist(
                artist = "Ignored",
                hasTrack = false,
                emptyStateSubtitle = "Tap to open NOVA MUSIC"
            )
        )
        assertEquals(
            "Unknown Artist",
            NovaWidgetLogic.resolveArtist(
                artist = "  ",
                hasTrack = true,
                emptyStateSubtitle = "Tap to open NOVA MUSIC"
            )
        )
    }

    @Test
    fun widgetStateHasTrack_reflectsSongIdOrTitle() {
        assertFalse(NovaWidgetState().hasTrack)
        assertTrue(NovaWidgetState(songId = 42L).hasTrack)
        assertTrue(NovaWidgetState(title = "Track").hasTrack)
    }
}
