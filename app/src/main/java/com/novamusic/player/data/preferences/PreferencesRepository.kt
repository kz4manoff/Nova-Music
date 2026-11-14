package com.novamusic.player.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.RepeatModeOption
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_preferences")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val accentColor: AccentColorOption = AccentColorOption.ELECTRIC_ORANGE,
    val languageTag: String = "",
    val artworkStyle: ArtworkStyle = ArtworkStyle.ROUNDED_CARD,
    val gaplessPlayback: Boolean = true,
    val resumePlaybackOnStart: Boolean = true,
    val minTrackDurationSec: Int = 10,
    val highResArtwork: Boolean = true,
    val sortOrder: SortOrder = SortOrder.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING
)

data class PlaybackPersistenceState(
    val lastSongId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val queueSongIds: List<Long> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF
)

data class EqualizerPersistenceState(
    val isEnabled: Boolean = false,
    val presetName: String = "Normal",
    val bandLevelsMillibels: List<Int> = emptyList(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMillibels: Int = 0,
    val preampGainDb: Float = 0f
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val ARTWORK_STYLE = stringPreferencesKey("artwork_style")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val MIN_DURATION_SEC = intPreferencesKey("min_duration_sec")
        val HIGH_RES_ARTWORK = booleanPreferencesKey("high_res_artwork")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")

        val LAST_SONG_ID = longPreferencesKey("last_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")

        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_LOUDNESS = intPreferencesKey("eq_loudness")
        val EQ_PREAMP_DB = floatPreferencesKey("eq_preamp_db")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.AMOLED,
            accentColor = prefs[Keys.ACCENT_COLOR]?.let { runCatching { AccentColorOption.valueOf(it) }.getOrNull() }
                ?: AccentColorOption.ELECTRIC_ORANGE,
            languageTag = prefs[Keys.LANGUAGE_TAG] ?: "",
            artworkStyle = prefs[Keys.ARTWORK_STYLE]?.let { runCatching { ArtworkStyle.valueOf(it) }.getOrNull() }
                ?: ArtworkStyle.ROUNDED_CARD,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            resumePlaybackOnStart = prefs[Keys.RESUME_PLAYBACK] ?: true,
            minTrackDurationSec = prefs[Keys.MIN_DURATION_SEC] ?: 10,
            highResArtwork = prefs[Keys.HIGH_RES_ARTWORK] ?: true,
            sortOrder = prefs[Keys.SORT_ORDER]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.NAME,
            sortDirection = prefs[Keys.SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING
        )
    }

    val equalizerState: Flow<EqualizerPersistenceState> = context.dataStore.data.map { prefs ->
        val bandsCsv = prefs[Keys.EQ_BANDS_CSV].orEmpty()
        val bands = if (bandsCsv.isBlank()) {
            emptyList()
        } else {
            bandsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        EqualizerPersistenceState(
            isEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            presetName = prefs[Keys.EQ_PRESET] ?: "Normal",
            bandLevelsMillibels = bands,
            bassBoostStrength = prefs[Keys.EQ_BASS_BOOST] ?: 0,
            virtualizerStrength = prefs[Keys.EQ_VIRTUALIZER] ?: 0,
            loudnessGainMillibels = prefs[Keys.EQ_LOUDNESS] ?: 0,
            preampGainDb = prefs[Keys.EQ_PREAMP_DB] ?: 0f
        )
    }

    suspend fun getPlaybackPersistenceState(): PlaybackPersistenceState {
        val prefs = context.dataStore.data.first()
        val queueIds = prefs[Keys.LAST_QUEUE_IDS]
            .orEmpty()
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        val repeat = prefs[Keys.REPEAT_MODE]?.let {
            runCatching { RepeatModeOption.valueOf(it) }.getOrNull()
        } ?: RepeatModeOption.OFF

        return PlaybackPersistenceState(
            lastSongId = prefs[Keys.LAST_SONG_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            queueSongIds = queueIds,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
            repeatMode = repeat
        )
    }

    suspend fun getEqualizerPersistenceStateOnce(): EqualizerPersistenceState {
        return equalizerState.first()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(accent: AccentColorOption) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = accent.name }
    }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }

    suspend fun setArtworkStyle(style: ArtworkStyle) {
        context.dataStore.edit { it[Keys.ARTWORK_STYLE] = style.name }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setMinTrackDurationSec(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_DURATION_SEC] = seconds }
    }

    suspend fun setHighResArtwork(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_RES_ARTWORK] = enabled }
    }

    suspend fun setSort(order: SortOrder, direction: SortDirection) {
        context.dataStore.edit {
            it[Keys.SORT_ORDER] = order.name
            it[Keys.SORT_DIRECTION] = direction.name
        }
    }

    suspend fun savePlaybackState(
        songId: Long,
        positionMs: Long,
        queueIds: List<Long>,
        shuffleEnabled: Boolean,
        repeatMode: RepeatModeOption
    ) {
        context.dataStore.edit {
            it[Keys.LAST_SONG_ID] = songId
            it[Keys.LAST_POSITION_MS] = positionMs
            it[Keys.LAST_QUEUE_IDS] = queueIds.joinToString(",")
            it[Keys.SHUFFLE_ENABLED] = shuffleEnabled
            it[Keys.REPEAT_MODE] = repeatMode.name
        }
    }

    suspend fun saveEqualizerState(state: EqualizerPersistenceState) {
        context.dataStore.edit {
            it[Keys.EQ_ENABLED] = state.isEnabled
            it[Keys.EQ_PRESET] = state.presetName
            it[Keys.EQ_BANDS_CSV] = state.bandLevelsMillibels.joinToString(",")
            it[Keys.EQ_BASS_BOOST] = state.bassBoostStrength
            it[Keys.EQ_VIRTUALIZER] = state.virtualizerStrength
            it[Keys.EQ_LOUDNESS] = state.loudnessGainMillibels
            it[Keys.EQ_PREAMP_DB] = state.preampGainDb
        }
    }
}
