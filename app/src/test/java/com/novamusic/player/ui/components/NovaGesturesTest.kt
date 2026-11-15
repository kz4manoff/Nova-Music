package com.novamusic.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaGesturesTest {

    @Test
    fun `system edge exclusion protects left and right edges`() {
        val width = 1080f
        val edge = 72f

        assertTrue(isWithinSystemEdgeExclusion(startX = 0f, layoutWidthPx = width, edgePx = edge))
        assertTrue(isWithinSystemEdgeExclusion(startX = 72f, layoutWidthPx = width, edgePx = edge))
        assertFalse(isWithinSystemEdgeExclusion(startX = 73f, layoutWidthPx = width, edgePx = edge))
        assertFalse(isWithinSystemEdgeExclusion(startX = 540f, layoutWidthPx = width, edgePx = edge))
        assertFalse(isWithinSystemEdgeExclusion(startX = 1007f, layoutWidthPx = width, edgePx = edge))
        assertTrue(isWithinSystemEdgeExclusion(startX = 1008f, layoutWidthPx = width, edgePx = edge))
        assertTrue(isWithinSystemEdgeExclusion(startX = 1080f, layoutWidthPx = width, edgePx = edge))
    }

    @Test
    fun `system edge exclusion ignores invalid narrow containers`() {
        assertFalse(isWithinSystemEdgeExclusion(startX = 10f, layoutWidthPx = 100f, edgePx = 60f))
        assertFalse(isWithinSystemEdgeExclusion(startX = 10f, layoutWidthPx = 1080f, edgePx = 0f))
    }

    @Test
    fun `resolveDragAxis discriminates horizontal vs vertical and ignores slop jitter`() {
        val slop = 24f

        assertNull(
            resolveDragAxis(
                dx = 12f,
                dy = 10f,
                touchSlop = slop,
                enableHorizontal = true,
                enableVertical = true
            )
        )

        // Diagonal ambiguous drag should not lock prematurely
        assertNull(
            resolveDragAxis(
                dx = 30f,
                dy = 28f,
                touchSlop = slop,
                enableHorizontal = true,
                enableVertical = true
            )
        )

        // Clear horizontal swipe locks to HORIZONTAL
        assertEquals(
            DragAxis.HORIZONTAL,
            resolveDragAxis(
                dx = -44f,
                dy = 12f,
                touchSlop = slop,
                enableHorizontal = true,
                enableVertical = true
            )
        )

        // Clear downward swipe locks to VERTICAL
        assertEquals(
            DragAxis.VERTICAL,
            resolveDragAxis(
                dx = 10f,
                dy = 48f,
                touchSlop = slop,
                enableHorizontal = true,
                enableVertical = true
            )
        )
    }

    @Test
    fun `resolveDragAxis respects enabled axes to prevent accidental cross-axis triggers`() {
        val slop = 24f

        // Vertical drag inside horizontal-only artwork stage must return null
        assertNull(
            resolveDragAxis(
                dx = 8f,
                dy = 60f,
                touchSlop = slop,
                enableHorizontal = true,
                enableVertical = false
            )
        )

        // Horizontal drag inside vertical-only NowPlaying root must return null
        assertNull(
            resolveDragAxis(
                dx = 60f,
                dy = 10f,
                touchSlop = slop,
                enableHorizontal = false,
                enableVertical = true
            )
        )
    }

    @Test
    fun `vertical dismiss triggers on distance or fast downward fling`() {
        val distanceThreshold = 384f
        val velocityThreshold = 2700f

        // Exceeds distance threshold
        assertTrue(
            shouldTriggerVerticalDismiss(
                offsetYPx = 390f,
                velocityYPx = 100f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )

        // Fast downward flick beyond 28% travel triggers dismiss
        assertTrue(
            shouldTriggerVerticalDismiss(
                offsetYPx = 120f,
                velocityYPx = 2900f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )

        // Too short and slow -> snap back
        assertFalse(
            shouldTriggerVerticalDismiss(
                offsetYPx = 120f,
                velocityYPx = 400f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )

        // Upward drag never dismisses
        assertFalse(
            shouldTriggerVerticalDismiss(
                offsetYPx = -200f,
                velocityYPx = -3000f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
    }

    @Test
    fun `mini player expand triggers on upward swipe or upward fling`() {
        val expandThreshold = 108f
        val velocityThreshold = 1860f

        assertTrue(
            shouldTriggerMiniPlayerExpand(
                offsetYPx = -112f,
                velocityYPx = -200f,
                distanceThresholdPx = expandThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
        assertTrue(
            shouldTriggerMiniPlayerExpand(
                offsetYPx = -40f,
                velocityYPx = -2100f,
                distanceThresholdPx = expandThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
        assertFalse(
            shouldTriggerMiniPlayerExpand(
                offsetYPx = 60f,
                velocityYPx = 1200f,
                distanceThresholdPx = expandThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
    }

    @Test
    fun `horizontal track action maps left swipe to NEXT and right swipe to PREVIOUS`() {
        val distanceThreshold = 216f
        val velocityThreshold = 2100f

        // Swipe left -> next track
        assertEquals(
            SwipeDirection.NEXT,
            resolveHorizontalTrackAction(
                offsetXPx = -220f,
                velocityXPx = -150f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
        // Quick left flick
        assertEquals(
            SwipeDirection.NEXT,
            resolveHorizontalTrackAction(
                offsetXPx = -80f,
                velocityXPx = -2300f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
        // Swipe right -> previous track
        assertEquals(
            SwipeDirection.PREVIOUS,
            resolveHorizontalTrackAction(
                offsetXPx = 225f,
                velocityXPx = 100f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
        // Small accidental horizontal jitter -> none
        assertEquals(
            SwipeDirection.NONE,
            resolveHorizontalTrackAction(
                offsetXPx = 45f,
                velocityXPx = 200f,
                distanceThresholdPx = distanceThreshold,
                velocityThresholdPx = velocityThreshold
            )
        )
    }
}
