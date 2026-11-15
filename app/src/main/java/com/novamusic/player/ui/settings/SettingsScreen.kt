package com.novamusic.player.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.AudioFocusBehavior
import com.novamusic.player.data.model.AudioQualityMode
import com.novamusic.player.data.model.NavBarAppearance
import com.novamusic.player.data.model.NotificationAppearance
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.RescanInterval
import com.novamusic.player.data.model.ThemeMode
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.SleepTimerDialog
import com.novamusic.player.ui.components.rememberNovaHaptic
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils
import java.util.Locale
import kotlin.math.roundToInt

private data class SettingsSearchItem(
    val id: String,
    val section: String,
    val title: String,
    val keywords: String,
    val content: @Composable () -> Unit
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val availableFolders by viewModel.availableFolders.collectAsStateWithLifecycle()
    val totalSongsCount by viewModel.totalSongsCount.collectAsStateWithLifecycle()
    val totalLibraryBytes by viewModel.totalLibraryBytes.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var eqBandsExpanded by remember { mutableStateOf(false) }
    var cacheClearedBanner by remember { mutableStateOf(false) }
    var historyClearedBanner by remember { mutableStateOf(false) }

    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val density = LocalDensity.current

    // Lightweight GPU entrance animation
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(260, easing = FastOutSlowInEasing),
        label = "settings_entrance_alpha"
    )
    val entranceOffsetPx by animateFloatAsState(
        targetValue = if (entered) 0f else with(density) { 14.dp.toPx() },
        animationSpec = if (reducedMotion) snap() else tween(280, easing = FastOutSlowInEasing),
        label = "settings_entrance_y"
    )

    val languages = remember {
        listOf(
            "" to "System Default",
            "en" to "English",
            "fr" to "Français",
            "es" to "Español",
            "de" to "Deutsch",
            "ar" to "العربية"
        )
    }
    val currentLanguageLabel = languages.firstOrNull { it.first == settings.languageTag }?.second ?: "System Default"

    // Reusable setting blocks shared seamlessly between full dashboard & search results
    val themeRow: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DarkMode,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            NovaSegmentedGlassBar(
                options = listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.DARK to "Dark",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.AMOLED to "AMOLED"
                ),
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode
            )
        }
    }

    val searchIndex = remember(
        settings,
        eqState,
        isScanning,
        playerState.sleepTimer,
        totalSongsCount,
        totalLibraryBytes,
        cacheSizeBytes,
        cacheClearedBanner,
        historyClearedBanner,
        currentLanguageLabel
    ) {
        buildList {
            // GENERAL
            add(SettingsSearchItem("lang", "General", "Language", "locale english french spanish arabic") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.Language,
                    title = "Language",
                    subtitle = "App display language",
                    trailingText = currentLanguageLabel,
                    onClick = { showLanguageDialog = true }
                )
            })
            add(SettingsSearchItem("theme", "General", "Theme", "dark light system amoled black appearance", themeRow))
            add(SettingsSearchItem("dynamic_colors", "General", "Dynamic Colors", "artwork pigment palette material") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Dynamic Colors",
                    subtitle = "Adapt controls and glow to active album artwork",
                    checked = settings.dynamicColors,
                    onCheckedChange = viewModel::setDynamicColors
                )
            })
            add(SettingsSearchItem("haptic", "General", "Haptic Feedback", "vibration touch tactile") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Subtle tactile pulse on controls and navigation",
                    checked = settings.hapticFeedback,
                    onCheckedChange = viewModel::setHapticFeedback
                )
            })
            add(SettingsSearchItem("keep_screen", "General", "Keep Screen On", "awake display timeout screen") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Visibility,
                    title = "Keep Screen On",
                    subtitle = "Prevent display sleep while NOVA MUSIC is open",
                    checked = settings.keepScreenOn,
                    onCheckedChange = viewModel::setKeepScreenOn
                )
            })

            // APPEARANCE
            add(SettingsSearchItem("accent", "Appearance", "Accent Color", "orange cyan crimson emerald violet theme") {
                NovaAccentColorPicker(
                    selected = settings.accentColor,
                    onSelect = viewModel::setAccentColor
                )
            })
            add(SettingsSearchItem("artwork_style", "Appearance", "Album Artwork Style", "vinyl rounded glow studio cover") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.Album,
                    title = "Album Artwork Style",
                    subtitle = "Now Playing presentation",
                    selectedLabel = settings.artworkStyle.label,
                    options = ArtworkStyle.entries.map { it to it.label },
                    selectedValue = settings.artworkStyle,
                    onSelect = viewModel::setArtworkStyle
                )
            })
            add(SettingsSearchItem("bg_blur", "Appearance", "Background Blur", "glass depth frosted intensity backdrop") {
                NovaGlassSliderRow(
                    icon = Icons.Rounded.BlurOn,
                    title = "Background Blur",
                    subtitle = "Live artwork backdrop diffusion",
                    valueLabel = "${(settings.blurIntensity * 100).roundToInt()}%",
                    value = settings.blurIntensity,
                    valueRange = 0.25f..1.50f,
                    onValueChange = viewModel::setBlurIntensity
                )
            })
            add(SettingsSearchItem("player_anim", "Appearance", "Player Animation", "spring morph fluid balanced minimal") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.Animation,
                    title = "Player Animation",
                    subtitle = settings.playerAnimationStyle.description,
                    selectedLabel = settings.playerAnimationStyle.label,
                    options = PlayerAnimationStyle.entries.map { it to it.label },
                    selectedValue = settings.playerAnimationStyle,
                    onSelect = viewModel::setPlayerAnimationStyle
                )
            })
            add(SettingsSearchItem("nav_bar", "Appearance", "Navigation Bar Style", "floating glass compact minimal pill tabs") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.Tune,
                    title = "Navigation Bar Style",
                    subtitle = settings.navBarAppearance.description,
                    selectedLabel = settings.navBarAppearance.label,
                    options = NavBarAppearance.entries.map { it to it.label },
                    selectedValue = settings.navBarAppearance,
                    onSelect = viewModel::setNavBarAppearance
                )
            })
            add(SettingsSearchItem("anim_effects", "Appearance", "Animation Effects", "motion transitions reduce") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Animation,
                    title = "Animation Effects",
                    subtitle = "Fluid transitions, glass highlights & breathing motion",
                    checked = settings.interfaceAnimations,
                    onCheckedChange = viewModel::setInterfaceAnimations
                )
            })

            // PLAYBACK
            add(SettingsSearchItem("audio_quality", "Playback", "Audio Quality", "lossless direct high-res balanced pcm") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.HighQuality,
                    title = "Audio Quality",
                    subtitle = settings.audioQuality.description,
                    selectedLabel = settings.audioQuality.label,
                    options = AudioQualityMode.entries.map { it to it.label },
                    selectedValue = settings.audioQuality,
                    onSelect = viewModel::setAudioQuality
                )
            })
            add(SettingsSearchItem("gapless", "Playback", "Gapless Playback", "seamless transition album") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions without silence between tracks",
                    checked = settings.gaplessPlayback,
                    onCheckedChange = viewModel::setGaplessPlayback
                )
            })
            add(SettingsSearchItem("crossfade", "Playback", "Crossfade", "fade transition overlap seconds") {
                NovaGlassSliderRow(
                    icon = Icons.Rounded.Tune,
                    title = "Crossfade",
                    subtitle = "Smooth volume ramp between tracks",
                    valueLabel = if (settings.crossfadeSeconds == 0) "Off" else "${settings.crossfadeSeconds}s",
                    value = settings.crossfadeSeconds.toFloat(),
                    valueRange = 0f..10f,
                    steps = 9,
                    onValueChange = { viewModel.setCrossfadeSeconds(it.roundToInt()) }
                )
            })
            add(SettingsSearchItem("replaygain", "Playback", "ReplayGain", "volume loudness normalize headroom") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "ReplayGain",
                    subtitle = "Normalize loudness headroom across tracks",
                    checked = settings.replayGainEnabled,
                    onCheckedChange = viewModel::setReplayGainEnabled
                )
            })
            add(SettingsSearchItem("resume", "Playback", "Resume Playback", "restore queue position restart") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.PlayArrow,
                    title = "Resume Playback",
                    subtitle = "Remember last played track, queue and position",
                    checked = settings.resumePlaybackOnStart,
                    onCheckedChange = viewModel::setResumePlayback
                )
            })
            add(SettingsSearchItem("autoplay", "Playback", "Autoplay", "continue queue similar next") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Autoplay",
                    subtitle = "Continue with library tracks when the queue ends",
                    checked = settings.autoplayEnabled,
                    onCheckedChange = viewModel::setAutoplayEnabled
                )
            })
            add(SettingsSearchItem("audio_focus", "Playback", "Audio Focus", "calls duck interrupt notifications") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.Hearing,
                    title = "Audio Focus",
                    subtitle = settings.audioFocusBehavior.description,
                    selectedLabel = settings.audioFocusBehavior.label,
                    options = AudioFocusBehavior.entries.map { it to it.label },
                    selectedValue = settings.audioFocusBehavior,
                    onSelect = viewModel::setAudioFocusBehavior
                )
            })
            add(SettingsSearchItem("sleep_timer", "Playback", "Sleep Timer", "bedtime countdown stop playback") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.Bedtime,
                    title = "Sleep Timer",
                    subtitle = "Automatically pause playback",
                    trailingText = if (playerState.sleepTimer.isActive) {
                        if (playerState.sleepTimer.stopAfterCurrentTrack) "End of track"
                        else FormatUtils.formatTimerCountdown(playerState.sleepTimer.remainingMs)
                    } else "Off",
                    onClick = { showSleepTimerDialog = true }
                )
            })

            // SOUND & EQUALIZER
            add(SettingsSearchItem("eq_enable", "Sound & Equalizer", "Enable Equalizer", "dsp audio bands 10-band") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Equalizer,
                    title = "Enable Equalizer",
                    subtitle = if (eqState.isEnabled) "Active • ${eqState.activePresetName}" else "Bypassed • Direct signal",
                    checked = eqState.isEnabled,
                    onCheckedChange = viewModel::setEqEnabled
                )
            })
            add(SettingsSearchItem("bass_boost", "Sound & Equalizer", "Bass Boost", "low frequency sub punch") {
                NovaGlassSliderRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Bass Boost",
                    subtitle = if (eqState.bassBoostSupported) "Low-frequency weight" else "Not supported on this device",
                    valueLabel = if (eqState.bassBoostSupported) "${eqState.bassBoostStrength / 10}%" else "N/A",
                    value = eqState.bassBoostStrength.toFloat(),
                    valueRange = 0f..1000f,
                    enabled = eqState.bassBoostSupported,
                    onValueChange = { viewModel.setBassBoost(it.roundToInt()) }
                )
            })
            add(SettingsSearchItem("virtualizer", "Sound & Equalizer", "Virtualizer", "spatial surround soundstage") {
                NovaGlassSliderRow(
                    icon = Icons.Rounded.SurroundSound,
                    title = "Virtualizer",
                    subtitle = if (eqState.virtualizerSupported) "Stereo soundstage width" else "Not supported on this device",
                    valueLabel = if (eqState.virtualizerSupported) "${eqState.virtualizerStrength / 10}%" else "N/A",
                    value = eqState.virtualizerStrength.toFloat(),
                    valueRange = 0f..1000f,
                    enabled = eqState.virtualizerSupported,
                    onValueChange = { viewModel.setVirtualizer(it.roundToInt()) }
                )
            })
            if (eqState.loudnessSupported) {
                add(SettingsSearchItem("loudness", "Sound & Equalizer", "Loudness Enhancement", "gain boost preamp") {
                    NovaGlassSliderRow(
                        icon = Icons.Rounded.VolumeUp,
                        title = "Loudness Enhancement",
                        subtitle = "Dynamic output gain",
                        valueLabel = String.format(Locale.US, "+%.1f dB", eqState.loudnessGainMillibels / 100f),
                        value = eqState.loudnessGainMillibels.toFloat(),
                        valueRange = 0f..1500f,
                        onValueChange = { viewModel.setLoudnessGain(it.roundToInt()) }
                    )
                })
            }

            // LIBRARY & STORAGE
            add(SettingsSearchItem("scan", "Library & Storage", "Scan Music Library", "refresh tracks mp3 flac wav") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.Refresh,
                    title = "Scan Music Library",
                    subtitle = if (isScanning) "Scanning audio storage..." else "MP3, FLAC, WAV, AAC, M4A, OGG, OPUS",
                    trailingContent = if (isScanning) {
                        { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent) }
                    } else null,
                    onClick = viewModel::rescanLibrary
                )
            })
            add(SettingsSearchItem("auto_rescan", "Library & Storage", "Automatic Rescan", "interval daily launch manual") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.Schedule,
                    title = "Automatic Rescan",
                    subtitle = "Schedule background library check",
                    selectedLabel = settings.rescanInterval.label,
                    options = RescanInterval.entries.map { it to it.label },
                    selectedValue = settings.rescanInterval,
                    onSelect = viewModel::setRescanInterval
                )
            })
            add(SettingsSearchItem("excluded_folders", "Library & Storage", "Excluded Folders", "hide directory filter") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.FolderOff,
                    title = "Excluded Folders",
                    subtitle = if (settings.excludedFolders.isEmpty()) "All music folders included" else "${settings.excludedFolders.size} folder(s) hidden",
                    trailingText = if (settings.excludedFolders.isEmpty()) "None" else "${settings.excludedFolders.size}",
                    onClick = { showExcludedFoldersDialog = true }
                )
            })
            add(SettingsSearchItem("min_duration", "Library & Storage", "Minimum Track Duration", "short clips ringtones filter") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.Schedule,
                    title = "Minimum Track Duration",
                    subtitle = "Ignore short notification or voice clips",
                    trailingText = if (settings.minTrackDurationSec == 0) "All" else "${settings.minTrackDurationSec}s",
                    onClick = { showMinDurationDialog = true }
                )
            })
            add(SettingsSearchItem("artwork_cache", "Library & Storage", "Artwork Cache", "high resolution album covers") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Image,
                    title = "Artwork Cache",
                    subtitle = "Load and cache full-resolution album artwork",
                    checked = settings.highResArtwork,
                    onCheckedChange = viewModel::setHighResArtwork
                )
            })
            add(SettingsSearchItem("clear_cache", "Library & Storage", "Clear Cache", "free space temporary coil") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Clear Cache",
                    subtitle = if (cacheClearedBanner) "Artwork and temporary cache cleared" else "Free cached images & temporary files",
                    trailingText = FormatUtils.formatFileSize(cacheSizeBytes),
                    onClick = {
                        viewModel.clearCache()
                        cacheClearedBanner = true
                    }
                )
            })
            add(SettingsSearchItem("clear_history", "Library & Storage", "Clear Playback History", "reset recent most played") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Clear Playback History",
                    subtitle = if (historyClearedBanner) "Recently played & counts reset" else "Reset recently played & play counts",
                    onClick = {
                        viewModel.clearHistory()
                        historyClearedBanner = true
                    }
                )
            })

            // NOTIFICATIONS
            add(SettingsSearchItem("notif_enable", "Notifications", "Playback Notifications", "media bar controls") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Playback Notifications",
                    subtitle = "Show media transport controls in notification shade",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
            })
            add(SettingsSearchItem("lock_controls", "Notifications", "Lock Screen Controls", "lockscreen metadata visibility") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Lock,
                    title = "Lock Screen Controls",
                    subtitle = "Display playback card on lock screen",
                    checked = settings.lockScreenControls,
                    onCheckedChange = viewModel::setLockScreenControls
                )
            })
            add(SettingsSearchItem("notif_art", "Notifications", "Show Album Artwork", "cover media notification") {
                NovaSwitchSettingRow(
                    icon = Icons.Rounded.Image,
                    title = "Show Album Artwork",
                    subtitle = "Include track cover art in system notification",
                    checked = settings.showNotificationArtwork,
                    onCheckedChange = viewModel::setShowNotificationArtwork
                )
            })
            add(SettingsSearchItem("notif_style", "Notifications", "Notification Appearance", "rich compact card") {
                NovaDropdownSettingRow(
                    icon = Icons.Rounded.Palette,
                    title = "Notification Appearance",
                    subtitle = settings.notificationAppearance.description,
                    selectedLabel = settings.notificationAppearance.label,
                    options = NotificationAppearance.entries.map { it to it.label },
                    selectedValue = settings.notificationAppearance,
                    onSelect = viewModel::setNotificationAppearance
                )
            })

            // ABOUT NOVA
            add(SettingsSearchItem("privacy", "About NOVA", "Privacy Policy", "offline tracking data permissions") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.Policy,
                    title = "Privacy Policy",
                    subtitle = "100% offline • Zero telemetry or tracking",
                    onClick = { showPrivacyDialog = true }
                )
            })
            add(SettingsSearchItem("licenses", "About NOVA", "Open Source Licenses", "androidx media3 compose coil room") {
                NovaActionSettingRow(
                    icon = Icons.Rounded.Code,
                    title = "Open Source Licenses",
                    subtitle = "Third-party libraries & notices",
                    onClick = { showLicensesDialog = true }
                )
            })
        }
    }

    val normalizedQuery = searchQuery.trim()
    val filteredSearchResults = remember(normalizedQuery, searchIndex) {
        if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            searchIndex.filter { item ->
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                    item.section.contains(normalizedQuery, ignoreCase = true) ||
                    item.keywords.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entranceAlpha
                translationY = entranceOffsetPx
            },
        contentPadding = PaddingValues(bottom = bottomContentPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: Large Title "Settings" + Subtitle "Make NOVA yours."
        item(key = "header") {
            LiquidGlassTopBar(
                title = "Settings",
                subtitle = "Make NOVA yours.",
                leadingAction = if (onBack != null) {
                    {
                        LiquidGlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            size = 40.dp,
                            iconSize = 20.dp,
                            onClick = onBack
                        )
                    }
                } else null
            )
        }

        // Elegant App Identity Card + Small Search Field
        item(key = "profile_and_search") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovaGlassProfileCard(
                    songCount = totalSongsCount,
                    librarySizeLabel = FormatUtils.formatFileSize(totalLibraryBytes),
                    eqEnabled = eqState.isEnabled,
                    activePreset = eqState.activePresetName
                )
                NovaGlassSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
        }

        if (normalizedQuery.isNotBlank()) {
            // LIVE SEARCH RESULTS
            if (filteredSearchResults.isEmpty()) {
                item(key = "search_empty") {
                    NovaSettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No settings match \"$normalizedQuery\"",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Try \"theme\", \"crossfade\", \"equalizer\", or \"cache\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val grouped = filteredSearchResults.groupBy { it.section }
                grouped.forEach { (sectionName, itemsInSection) ->
                    item(key = "search_sec_$sectionName") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NovaSectionHeader(
                                title = sectionName,
                                icon = Icons.Rounded.Search,
                                trailingBadge = "${itemsInSection.size} match${if (itemsInSection.size == 1) "" else "es"}"
                            )
                            NovaSettingsCard {
                                itemsInSection.forEachIndexed { idx, entry ->
                                    entry.content()
                                    if (idx < itemsInSection.lastIndex) {
                                        NovaSettingsDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // 1. GENERAL
            // =========================================================================
            item(key = "sec_general") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(title = "General", icon = Icons.Rounded.Tune)
                    NovaSettingsCard {
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Language,
                            title = "Language",
                            subtitle = "App display language",
                            trailingText = currentLanguageLabel,
                            onClick = { showLanguageDialog = true }
                        )
                        NovaSettingsDivider()
                        themeRow()
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Dynamic Colors",
                            subtitle = "Adapt controls and glow to active album artwork",
                            checked = settings.dynamicColors,
                            onCheckedChange = viewModel::setDynamicColors
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Vibration,
                            title = "Haptic Feedback",
                            subtitle = "Subtle tactile pulse on controls and navigation",
                            checked = settings.hapticFeedback,
                            onCheckedChange = viewModel::setHapticFeedback
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Visibility,
                            title = "Keep Screen On",
                            subtitle = "Prevent display sleep while NOVA MUSIC is open",
                            checked = settings.keepScreenOn,
                            onCheckedChange = viewModel::setKeepScreenOn
                        )
                    }
                }
            }

            // =========================================================================
            // 2. APPEARANCE
            // =========================================================================
            item(key = "sec_appearance") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(title = "Appearance", icon = Icons.Rounded.Palette)
                    NovaSettingsCard {
                        NovaAccentColorPicker(
                            selected = settings.accentColor,
                            onSelect = viewModel::setAccentColor
                        )
                        NovaSettingsDivider()
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.Album,
                            title = "Album Artwork Style",
                            subtitle = "Now Playing artwork frame",
                            selectedLabel = settings.artworkStyle.label,
                            options = ArtworkStyle.entries.map { it to it.label },
                            selectedValue = settings.artworkStyle,
                            onSelect = viewModel::setArtworkStyle
                        )
                        NovaSettingsDivider()
                        NovaGlassSliderRow(
                            icon = Icons.Rounded.BlurOn,
                            title = "Background Blur",
                            subtitle = "Live artwork backdrop diffusion",
                            valueLabel = "${(settings.blurIntensity * 100).roundToInt()}%",
                            value = settings.blurIntensity,
                            valueRange = 0.25f..1.50f,
                            onValueChange = viewModel::setBlurIntensity
                        )
                        NovaSettingsDivider()
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.Animation,
                            title = "Player Animation",
                            subtitle = settings.playerAnimationStyle.description,
                            selectedLabel = settings.playerAnimationStyle.label,
                            options = PlayerAnimationStyle.entries.map { it to it.label },
                            selectedValue = settings.playerAnimationStyle,
                            onSelect = viewModel::setPlayerAnimationStyle
                        )
                        NovaSettingsDivider()
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.Tune,
                            title = "Navigation Bar Style",
                            subtitle = settings.navBarAppearance.description,
                            selectedLabel = settings.navBarAppearance.label,
                            options = NavBarAppearance.entries.map { it to it.label },
                            selectedValue = settings.navBarAppearance,
                            onSelect = viewModel::setNavBarAppearance
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Animation,
                            title = "Animation Effects",
                            subtitle = "Fluid transitions, glass highlights & breathing motion",
                            checked = settings.interfaceAnimations,
                            onCheckedChange = viewModel::setInterfaceAnimations
                        )
                    }
                }
            }

            // =========================================================================
            // 3. PLAYBACK
            // =========================================================================
            item(key = "sec_playback") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(title = "Playback", icon = Icons.Rounded.PlayArrow)
                    NovaSettingsCard {
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.HighQuality,
                            title = "Audio Quality",
                            subtitle = settings.audioQuality.description,
                            selectedLabel = settings.audioQuality.label,
                            options = AudioQualityMode.entries.map { it to it.label },
                            selectedValue = settings.audioQuality,
                            onSelect = viewModel::setAudioQuality
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Gapless Playback",
                            subtitle = "Seamless transitions without silence between tracks",
                            checked = settings.gaplessPlayback,
                            onCheckedChange = viewModel::setGaplessPlayback
                        )
                        NovaSettingsDivider()
                        NovaGlassSliderRow(
                            icon = Icons.Rounded.Tune,
                            title = "Crossfade",
                            subtitle = "Smooth volume ramp between consecutive tracks",
                            valueLabel = if (settings.crossfadeSeconds == 0) "Off" else "${settings.crossfadeSeconds}s",
                            value = settings.crossfadeSeconds.toFloat(),
                            valueRange = 0f..10f,
                            steps = 9,
                            onValueChange = { viewModel.setCrossfadeSeconds(it.roundToInt()) }
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.VolumeUp,
                            title = "ReplayGain",
                            subtitle = "Normalize loudness headroom across tracks",
                            checked = settings.replayGainEnabled,
                            onCheckedChange = viewModel::setReplayGainEnabled
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.PlayArrow,
                            title = "Resume Playback",
                            subtitle = "Restore last played track, queue & timestamp on start",
                            checked = settings.resumePlaybackOnStart,
                            onCheckedChange = viewModel::setResumePlayback
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Autoplay",
                            subtitle = "Continue with library tracks when the queue ends",
                            checked = settings.autoplayEnabled,
                            onCheckedChange = viewModel::setAutoplayEnabled
                        )
                        NovaSettingsDivider()
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.Hearing,
                            title = "Audio Focus",
                            subtitle = settings.audioFocusBehavior.description,
                            selectedLabel = settings.audioFocusBehavior.label,
                            options = AudioFocusBehavior.entries.map { it to it.label },
                            selectedValue = settings.audioFocusBehavior,
                            onSelect = viewModel::setAudioFocusBehavior
                        )
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Bedtime,
                            title = "Sleep Timer",
                            subtitle = "Automatically pause playback",
                            trailingText = if (playerState.sleepTimer.isActive) {
                                if (playerState.sleepTimer.stopAfterCurrentTrack) "End of track"
                                else FormatUtils.formatTimerCountdown(playerState.sleepTimer.remainingMs)
                            } else "Off",
                            onClick = { showSleepTimerDialog = true }
                        )
                    }
                }
            }

            // =========================================================================
            // 4. SOUND & EQUALIZER
            // =========================================================================
            item(key = "sec_sound_eq") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(
                        title = "Sound & Equalizer",
                        icon = Icons.Rounded.Equalizer,
                        trailingBadge = if (eqState.isEnabled) eqState.activePresetName else "Bypassed"
                    )
                    NovaSettingsCard {
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Equalizer,
                            title = "Enable Equalizer",
                            subtitle = if (eqState.isSupported) eqState.engineDescription else "Hardware EQ unavailable on this output",
                            checked = eqState.isEnabled,
                            onCheckedChange = viewModel::setEqEnabled
                        )
                        NovaSettingsDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        NovaInlineEqCurve(eqState = eqState, accent = accent)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Equalizer Presets",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = eqState.activePresetName,
                                style = NovaMonoTelemetry.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = accent
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.builtInPresets.forEach { presetName ->
                                val selected = eqState.activePresetName == presetName
                                LiquidGlassSurface(
                                    shape = RoundedCornerShape(14.dp),
                                    highlighted = selected,
                                    elevation = if (selected) 8.dp else 2.dp,
                                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                                    onClick = {
                                        performHaptic()
                                        viewModel.applyBuiltInPreset(presetName)
                                    }
                                ) {
                                    Text(
                                        text = presetName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            customPresets.forEach { custom ->
                                val selected = eqState.activePresetName == custom.name
                                LiquidGlassSurface(
                                    shape = RoundedCornerShape(14.dp),
                                    highlighted = selected,
                                    elevation = if (selected) 8.dp else 2.dp,
                                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                                    onClick = {
                                        performHaptic()
                                        viewModel.applyCustomPreset(custom)
                                    }
                                ) {
                                    Text(
                                        text = "${custom.name} ★",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassSurface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                highlighted = eqBandsExpanded,
                                elevation = 3.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                onClick = {
                                    performHaptic()
                                    eqBandsExpanded = !eqBandsExpanded
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (eqBandsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (eqBandsExpanded) "Hide ${eqState.bands.size}-Band Faders" else "Customize ${eqState.bands.size}-Band Faders",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            LiquidGlassSurface(
                                shape = RoundedCornerShape(14.dp),
                                elevation = 3.dp,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                                onClick = {
                                    performHaptic()
                                    viewModel.resetEqualizer()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.RestartAlt,
                                        contentDescription = "Reset Equalizer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = eqBandsExpanded,
                            enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (eqState.preampSupported) {
                                    NovaGlassSliderRow(
                                        icon = Icons.Rounded.VolumeUp,
                                        title = "Preamp Gain",
                                        valueLabel = String.format(Locale.US, "%+.1f dB", eqState.preampGainDb),
                                        value = eqState.preampGainDb,
                                        valueRange = -12f..12f,
                                        onValueChange = viewModel::setPreampGain
                                    )
                                }
                                eqState.bands.forEach { band ->
                                    val minF = eqState.minLevelMillibels.toFloat()
                                    val maxF = eqState.maxLevelMillibels.toFloat()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = band.label,
                                            style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(50.dp)
                                        )
                                        Slider(
                                            value = band.levelMillibels.toFloat().coerceIn(minF, maxF),
                                            onValueChange = { viewModel.setBandLevel(band.index, it.roundToInt()) },
                                            valueRange = minF..maxF,
                                            enabled = eqState.isEnabled,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = accent,
                                                inactiveTrackColor = novaColors.recessedSurface
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%+.1f dB", band.levelMillibels / 100f),
                                            style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                                            color = accent,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(54.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        NovaSettingsDivider()
                        NovaGlassSliderRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Bass Boost",
                            subtitle = if (eqState.bassBoostSupported) "Low-frequency weight" else "Not supported on this device",
                            valueLabel = if (eqState.bassBoostSupported) "${eqState.bassBoostStrength / 10}%" else "Unsupported",
                            value = eqState.bassBoostStrength.toFloat(),
                            valueRange = 0f..1000f,
                            enabled = eqState.bassBoostSupported,
                            onValueChange = { viewModel.setBassBoost(it.roundToInt()) }
                        )

                        NovaSettingsDivider()
                        NovaGlassSliderRow(
                            icon = Icons.Rounded.SurroundSound,
                            title = "Virtualizer",
                            subtitle = if (eqState.virtualizerSupported) "Stereo soundstage width" else "Not supported on this device",
                            valueLabel = if (eqState.virtualizerSupported) "${eqState.virtualizerStrength / 10}%" else "Unsupported",
                            value = eqState.virtualizerStrength.toFloat(),
                            valueRange = 0f..1000f,
                            enabled = eqState.virtualizerSupported,
                            onValueChange = { viewModel.setVirtualizer(it.roundToInt()) }
                        )

                        if (eqState.loudnessSupported) {
                            NovaSettingsDivider()
                            NovaGlassSliderRow(
                                icon = Icons.Rounded.VolumeUp,
                                title = "Loudness Enhancement",
                                subtitle = "Dynamic output gain boost",
                                valueLabel = String.format(Locale.US, "+%.1f dB", eqState.loudnessGainMillibels / 100f),
                                value = eqState.loudnessGainMillibels.toFloat(),
                                valueRange = 0f..1500f,
                                onValueChange = { viewModel.setLoudnessGain(it.roundToInt()) }
                            )
                        }

                        NovaSettingsDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(novaColors.recessedSurface.copy(alpha = 0.72f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Audio Effects Status",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = eqState.engineDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (eqState.audioSessionId > 0) "SES #${eqState.audioSessionId}" else "IDLE",
                                style = NovaMonoTelemetry.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = if (eqState.isEnabled && eqState.audioSessionId > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // =========================================================================
            // 5. LIBRARY & STORAGE
            // =========================================================================
            item(key = "sec_library") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(
                        title = "Library & Storage",
                        icon = Icons.Rounded.LibraryMusic,
                        trailingBadge = "$totalSongsCount tracks"
                    )
                    NovaSettingsCard {
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Refresh,
                            title = "Scan Music Library",
                            subtitle = if (isScanning) "Scanning audio storage..." else "MP3, FLAC, WAV, AAC, M4A, OGG, OPUS",
                            trailingContent = if (isScanning) {
                                { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent) }
                            } else null,
                            onClick = viewModel::rescanLibrary
                        )
                        NovaSettingsDivider()
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.Schedule,
                            title = "Automatic Rescan",
                            subtitle = "Schedule library discovery",
                            selectedLabel = settings.rescanInterval.label,
                            options = RescanInterval.entries.map { it to it.label },
                            selectedValue = settings.rescanInterval,
                            onSelect = viewModel::setRescanInterval
                        )
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.FolderOff,
                            title = "Excluded Folders",
                            subtitle = if (settings.excludedFolders.isEmpty()) "All music folders included" else "${settings.excludedFolders.size} folder(s) hidden",
                            trailingText = if (settings.excludedFolders.isEmpty()) "None" else "${settings.excludedFolders.size}",
                            onClick = { showExcludedFoldersDialog = true }
                        )
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Schedule,
                            title = "Minimum Track Duration",
                            subtitle = "Ignore short notification or voice clips",
                            trailingText = if (settings.minTrackDurationSec == 0) "All" else "${settings.minTrackDurationSec}s",
                            onClick = { showMinDurationDialog = true }
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Image,
                            title = "Artwork Cache",
                            subtitle = "Load and cache full-resolution album artwork",
                            checked = settings.highResArtwork,
                            onCheckedChange = viewModel::setHighResArtwork
                        )
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = "Clear Cache",
                            subtitle = if (cacheClearedBanner) "Artwork and temporary cache cleared" else "Free cached artwork & temporary files",
                            trailingText = FormatUtils.formatFileSize(cacheSizeBytes),
                            onClick = {
                                viewModel.clearCache()
                                cacheClearedBanner = true
                            }
                        )
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = "Clear Playback History",
                            subtitle = if (historyClearedBanner) "Recently played & counts reset" else "Reset recently played & play counts",
                            onClick = {
                                viewModel.clearHistory()
                                historyClearedBanner = true
                            }
                        )
                        NovaSettingsDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Storage Information",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        NovaInfoRow("Indexed Tracks", "$totalSongsCount songs")
                        NovaInfoRow("Library Size", FormatUtils.formatFileSize(totalLibraryBytes))
                        NovaInfoRow("App & Artwork Cache", FormatUtils.formatFileSize(cacheSizeBytes))
                        NovaInfoRow("Local Database", "100% Offline • Room + DataStore")
                    }
                }
            }

            // =========================================================================
            // 6. NOTIFICATIONS
            // =========================================================================
            item(key = "sec_notifications") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(title = "Notifications", icon = Icons.Rounded.Notifications)
                    NovaSettingsCard {
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Notifications,
                            title = "Playback Notifications",
                            subtitle = "Display media transport controls in notification shade",
                            checked = settings.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Lock,
                            title = "Lock Screen Controls",
                            subtitle = "Display playback card & controls on lock screen",
                            checked = settings.lockScreenControls,
                            onCheckedChange = viewModel::setLockScreenControls
                        )
                        NovaSettingsDivider()
                        NovaSwitchSettingRow(
                            icon = Icons.Rounded.Image,
                            title = "Show Album Artwork",
                            subtitle = "Publish cover artwork to system media notification",
                            checked = settings.showNotificationArtwork,
                            onCheckedChange = viewModel::setShowNotificationArtwork
                        )
                        NovaSettingsDivider()
                        NovaDropdownSettingRow(
                            icon = Icons.Rounded.Palette,
                            title = "Notification Appearance",
                            subtitle = settings.notificationAppearance.description,
                            selectedLabel = settings.notificationAppearance.label,
                            options = NotificationAppearance.entries.map { it to it.label },
                            selectedValue = settings.notificationAppearance,
                            onSelect = viewModel::setNotificationAppearance
                        )
                    }
                }
            }

            // =========================================================================
            // 7. ABOUT NOVA
            // =========================================================================
            item(key = "sec_about") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaSectionHeader(title = "About NOVA", icon = Icons.Rounded.Info)
                    NovaSettingsCard {
                        NovaInfoRow("App Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        NovaSettingsDivider()
                        NovaInfoRow("Developer", "kz4man, Mohamed")
                        NovaSettingsDivider()
                        NovaInfoRow("Audio Engine", "Media3 ExoPlayer + MediaSession")
                        NovaSettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Created & Developed by kz4man, Mohamed",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "© 2026 kz4man, Mohamed. All Rights Reserved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Policy,
                            title = "Privacy Policy",
                            subtitle = "100% offline • Zero telemetry or tracking",
                            onClick = { showPrivacyDialog = true }
                        )
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Code,
                            title = "Open Source Licenses",
                            subtitle = "AndroidX, Media3, Compose, Coil, Room",
                            onClick = { showLicensesDialog = true }
                        )
                    }
                }
            }
        }

        // Subtle Copyright Signature Footer
        item(key = "footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Created & Developed by kz4man, Mohamed",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "© 2026 kz4man, Mohamed. All Rights Reserved.",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.25.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // =========================================================================
    // MODERN LIQUID GLASS SELECTION & INFO DIALOGS
    // =========================================================================
    if (showLanguageDialog) {
        NovaGlassSelectionDialog(
            title = "Language",
            subtitle = "Choose display language for NOVA MUSIC",
            onDismiss = { showLanguageDialog = false }
        ) {
            Column {
                languages.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                performHaptic()
                                viewModel.setLanguage(tag)
                                showLanguageDialog = false
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.languageTag == tag,
                            onClick = {
                                performHaptic()
                                viewModel.setLanguage(tag)
                                showLanguageDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showMinDurationDialog) {
        val options = listOf(0, 5, 10, 30, 60)
        NovaGlassSelectionDialog(
            title = "Minimum Track Duration",
            subtitle = "Exclude short sound clips when scanning",
            onDismiss = { showMinDurationDialog = false }
        ) {
            Column {
                options.forEach { sec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                performHaptic()
                                viewModel.setMinTrackDurationSec(sec)
                                showMinDurationDialog = false
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.minTrackDurationSec == sec,
                            onClick = {
                                performHaptic()
                                viewModel.setMinTrackDurationSec(sec)
                                showMinDurationDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sec == 0) "Show all audio files" else "At least $sec seconds",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    if (showExcludedFoldersDialog) {
        NovaGlassSelectionDialog(
            title = "Excluded Folders",
            subtitle = "Checked folders will be hidden from your music library",
            onDismiss = { showExcludedFoldersDialog = false },
            dismissText = if (settings.excludedFolders.isNotEmpty()) "Include All" else null,
            onDismissAction = if (settings.excludedFolders.isNotEmpty()) viewModel::clearExcludedFolders else null,
            confirmText = "Done"
        ) {
            if (availableFolders.isEmpty()) {
                Text(
                    text = "No folders discovered yet. Scan your music library first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(availableFolders, key = { it.path }) { folder ->
                        val isExcluded = folder.path in settings.excludedFolders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    performHaptic()
                                    viewModel.toggleExcludedFolder(folder.path)
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isExcluded,
                                onCheckedChange = {
                                    performHaptic()
                                    viewModel.toggleExcludedFolder(folder.path)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (isExcluded) MaterialTheme.colorScheme.error else accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${folder.songCount} tracks • ${folder.path}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerState = playerState.sleepTimer,
            onStartTimer = viewModel::startSleepTimer,
            onStopAfterCurrent = viewModel::setStopAfterCurrentTrack,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showPrivacyDialog) {
        NovaGlassSelectionDialog(
            title = "Privacy Policy",
            subtitle = "NOVA MUSIC Local Data Commitment",
            onDismiss = { showPrivacyDialog = false },
            confirmText = "OK"
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "NOVA MUSIC runs 100% locally on your device.\n\n" +
                        "• Zero network analytics, telemetry, ads, or user tracking.\n" +
                        "• Audio permissions (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE) are used exclusively to index and play local music files.\n" +
                        "• Playlists, favorites, EQ profiles, and settings remain stored privately on-device using Android Room and DataStore Preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showLicensesDialog) {
        NovaGlassSelectionDialog(
            title = "Open Source Licenses",
            subtitle = "Libraries powering NOVA MUSIC",
            onDismiss = { showLicensesDialog = false }
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "Jetpack Compose & Material3" to "Android Open Source Project • Apache 2.0",
                    "AndroidX Media3 (ExoPlayer & MediaSession)" to "The Android Open Source Project • Apache 2.0",
                    "Room Persistence & DataStore" to "AndroidX Jetpack • Apache 2.0",
                    "Coil Image Loading (coil-compose)" to "Coil Contributors • Apache 2.0",
                    "Kotlin Coroutines" to "JetBrains s.r.o. • Apache 2.0"
                ).forEach { (lib, detail) ->
                    Column {
                        Text(lib, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
