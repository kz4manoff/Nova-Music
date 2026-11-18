package com.novamusic.player.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val artworkUri: Uri,
    val path: String,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val trackNumber: Int,
    val year: Int,
    val genre: String = "Unknown"
) {
    val fileExtension: String
        get() = path.substringAfterLast('.', missingDelimiterValue = "")
            .ifBlank { mimeType.substringAfterLast('/') }
            .uppercase()
}

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artworkUri: Uri,
    val songCount: Int,
    val year: Int
)

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val artworkUri: Uri?
)

@Immutable
data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int
)

@Immutable
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
    val isFavorite: Boolean = false
)

@Immutable
data class AudioFileInfo(
    val fileName: String,
    val filePath: String,
    val format: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val dateAddedSec: Long
)

@Immutable
data class PlaylistWithSongs(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songs: List<Song>
)

@Immutable
data class LyricsLine(
    val timestampMs: Long?,
    val text: String
)

@Immutable
data class LyricsResult(
    val lines: List<LyricsLine>,
    val isSynchronized: Boolean,
    val source: LyricsSource
)

enum class LyricsSource {
    NONE,
    LRC_FILE,
    EMBEDDED,
    MANUAL
}

enum class SortOrder(val label: String) {
    NAME("Name"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Date Added")
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class ArtworkStyle(val label: String) {
    ROUNDED_CARD("Rounded Card"),
    ROTATING_VINYL("Rotating Vinyl"),
    IMMERSIVE_GLOW("Immersive Glow"),
    EDITORIAL_SQUARE("Studio Frame")
}

enum class ThemeMode(val label: String) {
    AMOLED("AMOLED"),
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class PlayerAnimationStyle(val label: String, val description: String) {
    FLUID("Fluid Spring", "Physics-driven overshoot and morphing transitions"),
    BALANCED("Balanced", "Crisp iOS-style easing with subtle bounce"),
    MINIMAL("Minimal", "Fast fades with reduced spatial travel")
}

enum class NavBarAppearance(val label: String, val description: String) {
    FLOATING_GLASS("Floating Glass", "Full pill dock with icons and labels"),
    COMPACT("Compact Dock", "Slimmer floating glass bar with labels"),
    MINIMAL_PILL("Minimal Pill", "Icon-only floating glass capsule")
}

enum class AudioQualityMode(val label: String, val description: String) {
    LOSSLESS_DIRECT("Bit-Perfect Direct", "Direct PCM path with audio offload when idle"),
    HIGH_RES("High-Res Studio", "Full precision DSP pipeline for EQ and spatial audio"),
    BALANCED("Battery Balanced", "Standard decoding optimized for endurance")
}

enum class AudioFocusBehavior(val label: String, val description: String) {
    PAUSE_ON_LOSS("Pause & Resume", "Pause on calls/navigation and auto-duck alerts"),
    DUCK("Lower Volume", "Duck volume during short notifications"),
    IGNORE_TRANSIENT("Keep Playing", "Continue playback uninterrupted over transient alerts")
}

enum class RescanInterval(val label: String) {
    ON_START("On App Launch"),
    DAILY("Once Daily"),
    MANUAL("Manual Only")
}

enum class NotificationAppearance(val label: String, val description: String) {
    ARTWORK_GLOW("Rich Artwork", "Large artwork badge with Prev, Play/Pause, Next"),
    MEDIA_CARD("Standard Card", "Clean system media card with transport actions"),
    COMPACT("Compact", "Minimal Play/Pause & Next controls only")
}

enum class AccentColorOption(val label: String, val hex: Long) {
    ELECTRIC_ORANGE("Electric Orange", 0xFFFF8A32),
    CRIMSON_PULSE("Crimson Pulse", 0xFFFF4D6D),
    CYAN_NEON("Cyan Neon", 0xFF00D1FF),
    EMERALD_MINT("Emerald Mint", 0xFF2CE59B),
    VIOLET_AURA("Violet Aura", 0xFF9D7BFF)
}

enum class RepeatModeOption {
    OFF,
    ALL,
    ONE
}

enum class PlaybackMode(
    val label: String,
    val shortLabel: String
) {
    SEQUENTIAL("Sequential", "Order"),
    SHUFFLE("Shuffle", "Shuffle"),
    REPEAT_ALL("Repeat All", "Repeat All"),
    REPEAT_ONE("Repeat One", "Repeat 1"),
    SMART_SHUFFLE("Smart Shuffle", "Smart");

    fun next(): PlaybackMode = when (this) {
        SEQUENTIAL -> SHUFFLE
        SHUFFLE -> REPEAT_ALL
        REPEAT_ALL -> REPEAT_ONE
        REPEAT_ONE -> SMART_SHUFFLE
        SMART_SHUFFLE -> SEQUENTIAL
    }

    val isActiveMode: Boolean
        get() = this != SEQUENTIAL

    companion object {
        fun fromLegacy(shuffleEnabled: Boolean, repeatMode: RepeatModeOption): PlaybackMode = when (repeatMode) {
            RepeatModeOption.ONE -> REPEAT_ONE
            RepeatModeOption.ALL -> REPEAT_ALL
            RepeatModeOption.OFF -> if (shuffleEnabled) SHUFFLE else SEQUENTIAL
        }
    }
}

@Immutable
data class EqualizerBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMillibels: Int
)

@Immutable
data class EqualizerState(
    val isEnabled: Boolean = false,
    val isSupported: Boolean = false,
    val isTenBandEngine: Boolean = false,
    val engineDescription: String = "Checking audio hardware...",
    val minLevelMillibels: Int = -1500,
    val maxLevelMillibels: Int = 1500,
    val bands: List<EqualizerBandInfo> = emptyList(),
    val activePresetName: String = "Normal",
    val bassBoostSupported: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessSupported: Boolean = false,
    val loudnessGainMillibels: Int = 0, // 0..1500
    val preampSupported: Boolean = false,
    val preampGainDb: Float = 0f, // -12f..+12f
    val audioSessionId: Int = 0
)

@Immutable
data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMs: Long = 0L,
    val stopAfterCurrentTrack: Boolean = false
)

@Immutable
data class PlayerUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val playbackError: String? = null
) {
    val shuffleEnabled: Boolean
        get() = playbackMode == PlaybackMode.SHUFFLE || playbackMode == PlaybackMode.SMART_SHUFFLE

    val repeatMode: RepeatModeOption
        get() = when (playbackMode) {
            PlaybackMode.REPEAT_ALL -> RepeatModeOption.ALL
            PlaybackMode.REPEAT_ONE -> RepeatModeOption.ONE
            else -> RepeatModeOption.OFF
        }
}
