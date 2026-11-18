package com.novamusic.player.widget

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class NovaWidgetSize {
    MEDIUM,
    LARGE
}

@Immutable
data class NovaWidgetState(
    val songId: Long = -1L,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val dynamicColorsEnabled: Boolean = true,
    val accentHex: Long = 0xFFFF1744L
) {
    val hasTrack: Boolean
        get() = songId > 0L || !title.isNullOrBlank()
}

object NovaWidgetContract {
    const val ACTION_PLAY_PAUSE = "com.novamusic.player.widget.ACTION_PLAY_PAUSE"
    const val ACTION_NEXT = "com.novamusic.player.widget.ACTION_NEXT"
    const val ACTION_PREVIOUS = "com.novamusic.player.widget.ACTION_PREVIOUS"
    const val ACTION_REFRESH = "com.novamusic.player.widget.ACTION_REFRESH"

    const val PROGRESS_MAX = 1000
    const val LARGE_HEIGHT_THRESHOLD_DP = 165
}

/**
 * Pure-Kotlin presentation and sizing rules for NOVA MUSIC Home Screen Widgets.
 * Kept framework-independent so every layout decision is directly unit-testable.
 */
object NovaWidgetLogic {

    fun selectWidgetSize(
        defaultSize: NovaWidgetSize,
        minHeightDp: Int,
        maxHeightDp: Int
    ): NovaWidgetSize {
        val effectiveHeight = when {
            maxHeightDp > 0 -> maxHeightDp
            minHeightDp > 0 -> minHeightDp
            else -> return defaultSize
        }
        return if (effectiveHeight >= NovaWidgetContract.LARGE_HEIGHT_THRESHOLD_DP) {
            NovaWidgetSize.LARGE
        } else {
            NovaWidgetSize.MEDIUM
        }
    }

    fun computeProgress(
        positionMs: Long,
        durationMs: Long,
        maxProgress: Int = NovaWidgetContract.PROGRESS_MAX
    ): Int {
        if (durationMs <= 0L || positionMs <= 0L || maxProgress <= 0) return 0
        val ratio = (positionMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0)
        return (ratio * maxProgress).toInt().coerceIn(0, maxProgress)
    }

    fun resolveTitle(title: String?, emptyFallback: String): String {
        val cleaned = title?.trim().orEmpty()
        return cleaned.ifBlank { emptyFallback }
    }

    fun resolveArtist(
        artist: String?,
        hasTrack: Boolean,
        unknownArtistFallback: String = "Unknown Artist",
        emptyStateSubtitle: String
    ): String {
        if (!hasTrack) return emptyStateSubtitle
        val cleaned = artist?.trim().orEmpty()
        return cleaned.ifBlank { unknownArtistFallback }
    }
}
