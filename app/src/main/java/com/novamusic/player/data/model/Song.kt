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
    AMOLED("AMOLED Black"),
    DARK("Dark Graphite"),
    LIGHT("Light")
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
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val playbackError: String? = null
)
