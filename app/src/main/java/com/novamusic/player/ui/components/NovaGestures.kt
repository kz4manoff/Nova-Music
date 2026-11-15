package com.novamusic.player.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class DragAxis {
    HORIZONTAL,
    VERTICAL
}

enum class SwipeDirection {
    NONE,
    NEXT,
    PREVIOUS
}

@Immutable
data class NovaGestureConfig(
    val verticalDismissDistanceDp: Dp = 128.dp,
    val verticalDismissVelocityDpPerSec: Float = 900f,
    val horizontalSwipeDistanceDp: Dp = 72.dp,
    val horizontalSwipeVelocityDpPerSec: Float = 700f,
    val miniPlayerExpandDistanceDp: Dp = 36.dp,
    val miniPlayerDismissDistanceDp: Dp = 44.dp,
    val miniPlayerSwipeVelocityDpPerSec: Float = 620f,
    val edgeExclusionDp: Dp = 24.dp,
    val directionLockRatio: Float = 1.25f
)

val DefaultNovaGestureConfig = NovaGestureConfig()

/**
 * Pure gesture helpers (tested directly via unit tests as well as runtime Compose gestures).
 */
internal fun isWithinSystemEdgeExclusion(
    startX: Float,
    layoutWidthPx: Float,
    edgePx: Float
): Boolean {
    if (edgePx <= 0f || layoutWidthPx <= edgePx * 2f) return false
    return startX <= edgePx || startX >= (layoutWidthPx - edgePx)
}

internal fun resolveDragAxis(
    dx: Float,
    dy: Float,
    touchSlop: Float,
    enableHorizontal: Boolean,
    enableVertical: Boolean,
    lockRatio: Float = 1.25f
): DragAxis? {
    val absDx = abs(dx)
    val absDy = abs(dy)
    if (absDx < touchSlop && absDy < touchSlop) return null

    if (enableHorizontal && enableVertical) {
        return when {
            absDx >= touchSlop && absDx > absDy * lockRatio -> DragAxis.HORIZONTAL
            absDy >= touchSlop && absDy > absDx * lockRatio -> DragAxis.VERTICAL
            absDx > absDy * 1.6f -> DragAxis.HORIZONTAL
            absDy > absDx * 1.6f -> DragAxis.VERTICAL
            else -> null
        }
    }
    if (enableHorizontal) {
        return if (absDx >= touchSlop && absDx > absDy * lockRatio) DragAxis.HORIZONTAL else null
    }
    if (enableVertical) {
        return if (absDy >= touchSlop && absDy > absDx * lockRatio) DragAxis.VERTICAL else null
    }
    return null
}

internal fun shouldTriggerVerticalDismiss(
    offsetYPx: Float,
    velocityYPx: Float,
    distanceThresholdPx: Float,
    velocityThresholdPx: Float
): Boolean {
    return offsetYPx >= distanceThresholdPx ||
        (offsetYPx > distanceThresholdPx * 0.28f && velocityYPx >= velocityThresholdPx)
}

internal fun shouldTriggerMiniPlayerExpand(
    offsetYPx: Float,
    velocityYPx: Float,
    distanceThresholdPx: Float,
    velocityThresholdPx: Float
): Boolean {
    return offsetYPx <= -distanceThresholdPx ||
        (offsetYPx < -distanceThresholdPx * 0.30f && velocityYPx <= -velocityThresholdPx)
}

internal fun resolveHorizontalTrackAction(
    offsetXPx: Float,
    velocityXPx: Float,
    distanceThresholdPx: Float,
    velocityThresholdPx: Float
): SwipeDirection {
    return when {
        offsetXPx <= -distanceThresholdPx ||
            (offsetXPx < -distanceThresholdPx * 0.30f && velocityXPx <= -velocityThresholdPx) -> SwipeDirection.NEXT
        offsetXPx >= distanceThresholdPx ||
            (offsetXPx > distanceThresholdPx * 0.30f && velocityXPx >= velocityThresholdPx) -> SwipeDirection.PREVIOUS
        else -> SwipeDirection.NONE
    }
}

/**
 * Direction-locked iOS-style drag detector:
 * - Respects Android system navigation edge zones (edgeExclusionDp).
 * - Waits for child consumption on PointerEventPass.Main so buttons, seek bars, and scrollable
 *   lists retain priority without gesture conflicts.
 * - Discriminates cleanly between horizontal and vertical gestures using touch slop + ratio lock.
 * - Tracks pointer velocity for responsive iOS flick/fling gestures.
 */
fun Modifier.novaDirectionalDrag(
    key: Any? = Unit,
    enableHorizontal: Boolean = false,
    enableVertical: Boolean = false,
    edgeExclusionDp: Dp = DefaultNovaGestureConfig.edgeExclusionDp,
    directionLockRatio: Float = DefaultNovaGestureConfig.directionLockRatio,
    onHorizontalDrag: (deltaX: Float) -> Unit = {},
    onHorizontalEnd: (velocityX: Float) -> Unit = {},
    onHorizontalCancel: () -> Unit = {},
    onVerticalDrag: (deltaY: Float) -> Unit = {},
    onVerticalEnd: (velocityY: Float) -> Unit = {},
    onVerticalCancel: () -> Unit = {}
): Modifier = composed {
    val latestOnHDrag by rememberUpdatedState(onHorizontalDrag)
    val latestOnHEnd by rememberUpdatedState(onHorizontalEnd)
    val latestOnHCancel by rememberUpdatedState(onHorizontalCancel)
    val latestOnVDrag by rememberUpdatedState(onVerticalDrag)
    val latestOnVEnd by rememberUpdatedState(onVerticalEnd)
    val latestOnVCancel by rememberUpdatedState(onVerticalCancel)

    pointerInput(key, enableHorizontal, enableVertical, edgeExclusionDp, directionLockRatio) {
        if (!enableHorizontal && !enableVertical) return@pointerInput
        val edgePx = edgeExclusionDp.toPx()
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            if (isWithinSystemEdgeExclusion(down.position.x, size.width.toFloat(), edgePx)) {
                return@awaitEachGesture
            }

            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            var lockedAxis: DragAxis? = null
            var totalDx = 0f
            var totalDy = 0f
            var trackingPointerId = down.id

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change: PointerInputChange = event.changes.firstOrNull { it.id == trackingPointerId }
                    ?: event.changes.firstOrNull { it.pressed }
                    ?: break
                trackingPointerId = change.id

                // If a child composable (seek bar, slider, nested scrollable) already claimed it, yield cleanly.
                if (change.isConsumed) {
                    when (lockedAxis) {
                        DragAxis.HORIZONTAL -> latestOnHCancel()
                        DragAxis.VERTICAL -> latestOnVCancel()
                        null -> Unit
                    }
                    return@awaitEachGesture
                }

                if (!change.pressed) {
                    val velocity = velocityTracker.calculateVelocity()
                    when (lockedAxis) {
                        DragAxis.HORIZONTAL -> latestOnHEnd(velocity.x)
                        DragAxis.VERTICAL -> latestOnVEnd(velocity.y)
                        null -> Unit
                    }
                    return@awaitEachGesture
                }

                val delta = change.positionChange()
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                if (lockedAxis == null) {
                    totalDx += delta.x
                    totalDy += delta.y

                    val candidate = resolveDragAxis(
                        dx = totalDx,
                        dy = totalDy,
                        touchSlop = touchSlop,
                        enableHorizontal = enableHorizontal,
                        enableVertical = enableVertical,
                        lockRatio = directionLockRatio
                    )
                    if (candidate != null) {
                        lockedAxis = candidate
                        change.consume()
                        when (candidate) {
                            DragAxis.HORIZONTAL -> if (totalDx != 0f) latestOnHDrag(totalDx)
                            DragAxis.VERTICAL -> if (totalDy != 0f) latestOnVDrag(totalDy)
                        }
                    } else {
                        // Abort early if the user clearly moved in an unsupported axis
                        val absDx = abs(totalDx)
                        val absDy = abs(totalDy)
                        if (!enableHorizontal && absDx > touchSlop * 1.4f && absDx > absDy * 1.5f) {
                            return@awaitEachGesture
                        }
                        if (!enableVertical && absDy > touchSlop * 1.4f && absDy > absDx * 1.5f) {
                            return@awaitEachGesture
                        }
                    }
                } else {
                    change.consume()
                    when (lockedAxis) {
                        DragAxis.HORIZONTAL -> if (delta.x != 0f) latestOnHDrag(delta.x)
                        DragAxis.VERTICAL -> if (delta.y != 0f) latestOnVDrag(delta.y)
                    }
                }
            }

            when (lockedAxis) {
                DragAxis.HORIZONTAL -> latestOnHCancel()
                DragAxis.VERTICAL -> latestOnVCancel()
                null -> Unit
            }
        }
    }
}
