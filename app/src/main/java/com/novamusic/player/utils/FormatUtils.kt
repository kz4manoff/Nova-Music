package com.novamusic.player.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0:00"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatFrequency(freqHz: Int): String {
        return if (freqHz >= 1000) {
            val kHz = freqHz / 1000f
            if (kHz % 1f == 0f) {
                "${kHz.toInt()}k"
            } else {
                String.format(Locale.US, "%.1fk", kHz)
            }
        } else {
            "$freqHz"
        }
    }

    fun formatBitrate(kbps: Int?): String {
        return if (kbps != null && kbps > 0) "$kbps kbps" else "Unknown"
    }

    fun formatSampleRate(hz: Int?): String {
        return if (hz != null && hz > 0) {
            String.format(Locale.US, "%.1f kHz", hz / 1000.0)
        } else {
            "Unknown"
        }
    }

    fun formatTimerCountdown(remainingMs: Long): String {
        if (remainingMs <= 0L) return "00:00"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
