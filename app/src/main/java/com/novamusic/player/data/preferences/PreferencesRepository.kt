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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.AudioFocusBehavior
import com.novamusic.player.data.model.AudioQualityMode
import com.novamusic.player.data.model.NavBarAppearance
import com.novamusic.player.data.model.NotificationAppearance
import com.novamusic.player.data.model.PlaybackMode
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.RepeatModeOption
import com.novamusic.player.data.model.RescanInterval
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_preferences")

data class AppSettings(
    // General
    val languageTag: String = "",
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val dynamicColors: Boolean = true,
    val hapticFeedback: Boolean = true,
    val keepScreenOn: Boolean = false,
    // Appearance
    val accentColor: AccentColorOption = AccentColorOption.CRIMSON_PULSE,
    val artworkStyle: ArtworkStyle = ArtworkStyle.ROUNDED_CARD,
    val blurIntensity: Float = 1.0f,
    val playerAnimationStyle: PlayerAnimationStyle = PlayerAnimationStyle.FLUID,
    val navBarAppearance: NavBarAppearance = NavBarAppearance.FLOATING_GLASS,
    val interfaceAnimations: Boolean = true,
    // Playback
    val audioQuality: AudioQualityMode = AudioQualityMode.HIGH_RES,
    val gaplessPlayback: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val replayGainEnabled: Boolean = false,
    val resumePlaybackOnStart: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val audioFocusBehavior: AudioFocusBehavior = AudioFocusBehavior.PAUSE_ON_LOSS,
    // Library & Storage
    val minTrackDurationSec: Int = 10,
    val rescanInterval: RescanInterval = RescanInterval.ON_START,
    val excludedFolders: Set<String> = emptySet(),
    val highResArtwork: Boolean = true,
    val lastScanTimestampMs: Long = 0L,
    // Notifications
    val notificationsEnabled: Boolean = true,
    val lockScreenControls: Boolean = true,
    val showNotificationArtwork: Boolean = true,
    val notificationAppearance: NotificationAppearance = NotificationAppearance.ARTWORK_GLOW,
    // Library Sorting
    val sortOrder: SortOrder = SortOrder.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    // Headphones & Routing
    val pauseOnHeadphonesDisconnect: Boolean = true,
    val resumeOnHeadphonesConnect: Boolean = false,
    // Automatic App Updates & Push Notifications
    val autoCheckUpdates: Boolean = true,
    val wifiOnlyUpdateDownloads: Boolean = false,
    val lastUpdateCheckMs: Long = 0L,
    val dismissedUpdateVersionCode: Int = 0,
    val lastNotifiedUpdateVersionCode: Int = 0,
    val dismissedUpdateNotificationVersionCode: Int = 0,
    val fcmToken: String = "",
    val fcmSubscribedTopic: String = "",
    val lastFcmMessageId: String = "",
    val hasPromptedNotificationPermission: Boolean = false
)

data class PlaybackPersistenceState(
    val lastSongId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val queueSongIds: List<Long> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playbackMode: PlaybackMode = PlaybackMode.NORMAL,
    val originalQueueSongIds: List<Long> = emptyList()
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
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        val ARTWORK_STYLE = stringPreferencesKey("artwork_style")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val PLAYER_ANIM_STYLE = stringPreferencesKey("player_anim_style")
        val NAV_BAR_APPEARANCE = stringPreferencesKey("nav_bar_appearance")
        val INTERFACE_ANIMATIONS = booleanPreferencesKey("interface_animations")

        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val REPLAY_GAIN = booleanPreferencesKey("replay_gain")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
        val AUDIO_FOCUS_BEHAVIOR = stringPreferencesKey("audio_focus_behavior")

        val MIN_DURATION_SEC = intPreferencesKey("min_duration_sec")
        val RESCAN_INTERVAL = stringPreferencesKey("rescan_interval")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val HIGH_RES_ARTWORK = booleanPreferencesKey("high_res_artwork")
        val LAST_SCAN_TIMESTAMP_MS = longPreferencesKey("last_scan_timestamp_ms")

        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LOCK_SCREEN_CONTROLS = booleanPreferencesKey("lock_screen_controls")
        val SHOW_NOTIFICATION_ARTWORK = booleanPreferencesKey("show_notification_artwork")
        val NOTIFICATION_APPEARANCE = stringPreferencesKey("notification_appearance")

        val SORT_ORDER = stringPreferencesKey("sort_order")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")

        val LAST_SONG_ID = longPreferencesKey("last_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val SMART_ORIGINAL_QUEUE_IDS = stringPreferencesKey("smart_original_queue_ids")

        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_LOUDNESS = intPreferencesKey("eq_loudness")
        val EQ_PREAMP_DB = floatPreferencesKey("eq_preamp_db")

        val PAUSE_ON_HEADPHONES_DISCONNECT = booleanPreferencesKey("pause_on_headphones_disconnect")
        val RESUME_ON_HEADPHONES_CONNECT = booleanPreferencesKey("resume_on_headphones_connect")

        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val WIFI_ONLY_UPDATE_DOWNLOADS = booleanPreferencesKey("wifi_only_update_downloads")
        val LAST_UPDATE_CHECK_MS = longPreferencesKey("last_update_check_ms")
        val DISMISSED_UPDATE_VERSION_CODE = intPreferencesKey("dismissed_update_version_code")
        val LAST_NOTIFIED_UPDATE_VERSION_CODE = intPreferencesKey("last_notified_update_version_code")
        val DISMISSED_UPDATE_NOTIFICATION_VERSION_CODE = intPreferencesKey("dismissed_update_notification_version_code")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val FCM_SUBSCRIBED_TOPIC = stringPreferencesKey("fcm_subscribed_topic")
        val LAST_FCM_MESSAGE_ID = stringPreferencesKey("last_fcm_message_id")
        val HAS_PROMPTED_NOTIFICATION_PERMISSION = booleanPreferencesKey("has_prompted_notification_permission")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            languageTag = prefs[Keys.LANGUAGE_TAG] ?: "",
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.AMOLED,
            dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            accentColor = AccentColorOption.fromStorageKey(prefs[Keys.ACCENT_COLOR]),
            artworkStyle = prefs[Keys.ARTWORK_STYLE]?.let { runCatching { ArtworkStyle.valueOf(it) }.getOrNull() }
                ?: ArtworkStyle.ROUNDED_CARD,
            blurIntensity = (prefs[Keys.BLUR_INTENSITY] ?: 1.0f).coerceIn(0.25f, 1.5f),
            playerAnimationStyle = prefs[Keys.PLAYER_ANIM_STYLE]?.let {
                runCatching { PlayerAnimationStyle.valueOf(it) }.getOrNull()
            } ?: PlayerAnimationStyle.FLUID,
            navBarAppearance = prefs[Keys.NAV_BAR_APPEARANCE]?.let {
                runCatching { NavBarAppearance.valueOf(it) }.getOrNull()
            } ?: NavBarAppearance.FLOATING_GLASS,
            interfaceAnimations = prefs[Keys.INTERFACE_ANIMATIONS] ?: true,
            audioQuality = prefs[Keys.AUDIO_QUALITY]?.let {
                runCatching { AudioQualityMode.valueOf(it) }.getOrNull()
            } ?: AudioQualityMode.HIGH_RES,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            crossfadeSeconds = (prefs[Keys.CROSSFADE_SECONDS] ?: 0).coerceIn(0, 10),
            replayGainEnabled = prefs[Keys.REPLAY_GAIN] ?: false,
            resumePlaybackOnStart = prefs[Keys.RESUME_PLAYBACK] ?: true,
            autoplayEnabled = prefs[Keys.AUTOPLAY_ENABLED] ?: true,
            audioFocusBehavior = prefs[Keys.AUDIO_FOCUS_BEHAVIOR]?.let {
                runCatching { AudioFocusBehavior.valueOf(it) }.getOrNull()
            } ?: AudioFocusBehavior.PAUSE_ON_LOSS,
            minTrackDurationSec = prefs[Keys.MIN_DURATION_SEC] ?: 10,
            rescanInterval = prefs[Keys.RESCAN_INTERVAL]?.let {
                runCatching { RescanInterval.valueOf(it) }.getOrNull()
            } ?: RescanInterval.ON_START,
            excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            highResArtwork = prefs[Keys.HIGH_RES_ARTWORK] ?: true,
            lastScanTimestampMs = prefs[Keys.LAST_SCAN_TIMESTAMP_MS] ?: 0L,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            lockScreenControls = prefs[Keys.LOCK_SCREEN_CONTROLS] ?: true,
            showNotificationArtwork = prefs[Keys.SHOW_NOTIFICATION_ARTWORK] ?: true,
            notificationAppearance = prefs[Keys.NOTIFICATION_APPEARANCE]?.let {
                runCatching { NotificationAppearance.valueOf(it) }.getOrNull()
            } ?: NotificationAppearance.ARTWORK_GLOW,
            sortOrder = prefs[Keys.SORT_ORDER]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.NAME,
            sortDirection = prefs[Keys.SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING,
            pauseOnHeadphonesDisconnect = prefs[Keys.PAUSE_ON_HEADPHONES_DISCONNECT] ?: true,
            resumeOnHeadphonesConnect = prefs[Keys.RESUME_ON_HEADPHONES_CONNECT] ?: false,
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            wifiOnlyUpdateDownloads = prefs[Keys.WIFI_ONLY_UPDATE_DOWNLOADS] ?: false,
            lastUpdateCheckMs = prefs[Keys.LAST_UPDATE_CHECK_MS] ?: 0L,
            dismissedUpdateVersionCode = prefs[Keys.DISMISSED_UPDATE_VERSION_CODE] ?: 0,
            lastNotifiedUpdateVersionCode = prefs[Keys.LAST_NOTIFIED_UPDATE_VERSION_CODE] ?: 0,
            dismissedUpdateNotificationVersionCode = prefs[Keys.DISMISSED_UPDATE_NOTIFICATION_VERSION_CODE] ?: 0,
            fcmToken = prefs[Keys.FCM_TOKEN] ?: "",
            fcmSubscribedTopic = prefs[Keys.FCM_SUBSCRIBED_TOPIC] ?: "",
            lastFcmMessageId = prefs[Keys.LAST_FCM_MESSAGE_ID] ?: "",
            hasPromptedNotificationPermission = prefs[Keys.HAS_PROMPTED_NOTIFICATION_PERMISSION] ?: false
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
        val originalQueueIds = prefs[Keys.SMART_ORIGINAL_QUEUE_IDS]
            .orEmpty()
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        val shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false
        val repeat = prefs[Keys.REPEAT_MODE]?.let {
            runCatching { RepeatModeOption.valueOf(it) }.getOrNull()
        } ?: RepeatModeOption.OFF
        val playbackMode = PlaybackMode.fromStorageKey(prefs[Keys.PLAYBACK_MODE])
            ?: PlaybackMode.fromLegacy(shuffleEnabled = shuffleEnabled, repeatMode = repeat)

        return PlaybackPersistenceState(
            lastSongId = prefs[Keys.LAST_SONG_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            queueSongIds = queueIds,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeat,
            playbackMode = playbackMode,
            originalQueueSongIds = originalQueueIds
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

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setArtworkStyle(style: ArtworkStyle) {
        context.dataStore.edit { it[Keys.ARTWORK_STYLE] = style.name }
    }

    suspend fun setBlurIntensity(intensity: Float) {
        context.dataStore.edit { it[Keys.BLUR_INTENSITY] = intensity.coerceIn(0.25f, 1.5f) }
    }

    suspend fun setPlayerAnimationStyle(style: PlayerAnimationStyle) {
        context.dataStore.edit { it[Keys.PLAYER_ANIM_STYLE] = style.name }
    }

    suspend fun setNavBarAppearance(appearance: NavBarAppearance) {
        context.dataStore.edit { it[Keys.NAV_BAR_APPEARANCE] = appearance.name }
    }

    suspend fun setInterfaceAnimations(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INTERFACE_ANIMATIONS] = enabled }
    }

    suspend fun setAudioQuality(quality: AudioQualityMode) {
        context.dataStore.edit { it[Keys.AUDIO_QUALITY] = quality.name }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 10) }
    }

    suspend fun setReplayGainEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REPLAY_GAIN] = enabled }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_ENABLED] = enabled }
    }

    suspend fun setAudioFocusBehavior(behavior: AudioFocusBehavior) {
        context.dataStore.edit { it[Keys.AUDIO_FOCUS_BEHAVIOR] = behavior.name }
    }

    suspend fun setMinTrackDurationSec(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_DURATION_SEC] = seconds }
    }

    suspend fun setRescanInterval(interval: RescanInterval) {
        context.dataStore.edit { it[Keys.RESCAN_INTERVAL] = interval.name }
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        context.dataStore.edit { it[Keys.EXCLUDED_FOLDERS] = folders }
    }

    suspend fun toggleExcludedFolder(folderPath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_FOLDERS].orEmpty().toMutableSet()
            if (!current.add(folderPath)) {
                current.remove(folderPath)
            }
            prefs[Keys.EXCLUDED_FOLDERS] = current
        }
    }

    suspend fun setHighResArtwork(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_RES_ARTWORK] = enabled }
    }

    suspend fun setLastScanTimestampMs(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_SCAN_TIMESTAMP_MS] = timestampMs }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLockScreenControls(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_SCREEN_CONTROLS] = enabled }
    }

    suspend fun setShowNotificationArtwork(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NOTIFICATION_ARTWORK] = enabled }
    }

    suspend fun setNotificationAppearance(appearance: NotificationAppearance) {
        context.dataStore.edit { it[Keys.NOTIFICATION_APPEARANCE] = appearance.name }
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
        playbackMode: PlaybackMode,
        originalQueueIds: List<Long>? = null
    ) {
        val shuffleEnabled = playbackMode == PlaybackMode.SHUFFLE || playbackMode == PlaybackMode.SMART_SHUFFLE
        val repeatMode = when (playbackMode) {
            PlaybackMode.REPEAT_ALL -> RepeatModeOption.ALL
            PlaybackMode.REPEAT_ONE -> RepeatModeOption.ONE
            else -> RepeatModeOption.OFF
        }
        context.dataStore.edit {
            it[Keys.LAST_SONG_ID] = songId
            it[Keys.LAST_POSITION_MS] = positionMs
            it[Keys.LAST_QUEUE_IDS] = queueIds.joinToString(",")
            it[Keys.SHUFFLE_ENABLED] = shuffleEnabled
            it[Keys.REPEAT_MODE] = repeatMode.name
            it[Keys.PLAYBACK_MODE] = playbackMode.name
            if (originalQueueIds != null) {
                if (originalQueueIds.isEmpty()) {
                    it.remove(Keys.SMART_ORIGINAL_QUEUE_IDS)
                } else {
                    it[Keys.SMART_ORIGINAL_QUEUE_IDS] = originalQueueIds.joinToString(",")
                }
            }
        }
    }

    suspend fun savePlaybackMode(
        playbackMode: PlaybackMode,
        originalQueueIds: List<Long>? = null
    ) {
        val shuffleEnabled = playbackMode == PlaybackMode.SHUFFLE || playbackMode == PlaybackMode.SMART_SHUFFLE
        val repeatMode = when (playbackMode) {
            PlaybackMode.REPEAT_ALL -> RepeatModeOption.ALL
            PlaybackMode.REPEAT_ONE -> RepeatModeOption.ONE
            else -> RepeatModeOption.OFF
        }
        context.dataStore.edit {
            it[Keys.PLAYBACK_MODE] = playbackMode.name
            it[Keys.SHUFFLE_ENABLED] = shuffleEnabled
            it[Keys.REPEAT_MODE] = repeatMode.name
            if (originalQueueIds != null) {
                if (originalQueueIds.isEmpty()) {
                    it.remove(Keys.SMART_ORIGINAL_QUEUE_IDS)
                } else {
                    it[Keys.SMART_ORIGINAL_QUEUE_IDS] = originalQueueIds.joinToString(",")
                }
            }
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

    suspend fun setPauseOnHeadphonesDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_ON_HEADPHONES_DISCONNECT] = enabled }
    }

    suspend fun setResumeOnHeadphonesConnect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_ON_HEADPHONES_CONNECT] = enabled }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setWifiOnlyUpdateDownloads(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY_UPDATE_DOWNLOADS] = enabled }
    }

    suspend fun setLastUpdateCheckMs(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_UPDATE_CHECK_MS] = timestampMs }
    }

    suspend fun setDismissedUpdateVersionCode(versionCode: Int) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE_VERSION_CODE] = versionCode }
    }

    suspend fun setLastNotifiedUpdateVersionCode(versionCode: Int) {
        context.dataStore.edit { it[Keys.LAST_NOTIFIED_UPDATE_VERSION_CODE] = versionCode }
    }

    suspend fun setDismissedUpdateNotificationVersionCode(versionCode: Int) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE_NOTIFICATION_VERSION_CODE] = versionCode }
    }

    suspend fun clearStaleUpdateNotificationStateIfInstalled(installedVersionCode: Int) {
        context.dataStore.edit { prefs ->
            val notifiedCode = prefs[Keys.LAST_NOTIFIED_UPDATE_VERSION_CODE] ?: 0
            if (notifiedCode > 0 && installedVersionCode >= notifiedCode) {
                prefs[Keys.LAST_NOTIFIED_UPDATE_VERSION_CODE] = 0
            }
            val dismissedNotifCode = prefs[Keys.DISMISSED_UPDATE_NOTIFICATION_VERSION_CODE] ?: 0
            if (dismissedNotifCode > 0 && installedVersionCode >= dismissedNotifCode) {
                prefs[Keys.DISMISSED_UPDATE_NOTIFICATION_VERSION_CODE] = 0
            }
            val dismissedDialogCode = prefs[Keys.DISMISSED_UPDATE_VERSION_CODE] ?: 0
            if (dismissedDialogCode > 0 && installedVersionCode >= dismissedDialogCode) {
                prefs[Keys.DISMISSED_UPDATE_VERSION_CODE] = 0
            }
        }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { it[Keys.FCM_TOKEN] = token }
    }

    suspend fun saveFcmSubscribedTopic(topic: String) {
        context.dataStore.edit { it[Keys.FCM_SUBSCRIBED_TOPIC] = topic }
    }

    suspend fun recordUpdateNotificationPosted(versionCode: Int, messageId: String?) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.LAST_NOTIFIED_UPDATE_VERSION_CODE] ?: 0
            if (versionCode > current) {
                prefs[Keys.LAST_NOTIFIED_UPDATE_VERSION_CODE] = versionCode
            }
            if (!messageId.isNullOrBlank()) {
                prefs[Keys.LAST_FCM_MESSAGE_ID] = messageId.trim()
            }
        }
    }

    suspend fun setHasPromptedNotificationPermission(prompted: Boolean) {
        context.dataStore.edit { it[Keys.HAS_PROMPTED_NOTIFICATION_PERMISSION] = prompted }
    }
}
