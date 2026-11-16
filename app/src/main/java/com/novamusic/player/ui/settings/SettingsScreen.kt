package com.novamusic.player.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.BuildConfig
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.update.UpdateUiState
import com.novamusic.player.utils.FormatUtils

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
    bottomContentPadding: Dp,
    onOpenAppearance: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenAudioEqualizer: () -> Unit,
    onOpenHeadphones: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLibraryManagement: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAppUpdates: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()
    val audioRoutingState by viewModel.audioRoutingState.collectAsStateWithLifecycle()
    val totalSongsCount by viewModel.totalSongsCount.collectAsStateWithLifecycle()
    val totalLibraryBytes by viewModel.totalLibraryBytes.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val updateDownloadsSizeBytes by viewModel.updateDownloadsSizeBytes.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
        viewModel.refreshStorageStats()
    }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(240, easing = FastOutSlowInEasing),
        label = "settings_hub_alpha"
    )
    val entranceOffsetPx by animateFloatAsState(
        targetValue = if (entered) 0f else with(density) { 12.dp.toPx() },
        animationSpec = if (reducedMotion) snap() else tween(260, easing = FastOutSlowInEasing),
        label = "settings_hub_y"
    )

    val externalDevicesCount = remember(audioRoutingState.devices) {
        audioRoutingState.devices.count { it.type.isExternal }
    }
    val headphonesBadge = when {
        externalDevicesCount > 0 -> audioRoutingState.activeDevice?.type?.shortLabel ?: "$externalDevicesCount Connected"
        else -> "Speaker"
    }
    val updatesBadge = when (val state = updateState) {
        is UpdateUiState.Available -> "v${state.config.latestVersion}"
        is UpdateUiState.Downloading -> "Downloading"
        is UpdateUiState.ReadyToInstall -> "Ready"
        is UpdateUiState.UpToDate -> "Latest"
        else -> "v${BuildConfig.VERSION_NAME}"
    }
    val totalCacheAndDownloads = cacheSizeBytes + updateDownloadsSizeBytes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entranceAlpha
                translationY = entranceOffsetPx
            },
        contentPadding = PaddingValues(bottom = bottomContentPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_header") {
            LiquidGlassTopBar(
                title = "Settings",
                subtitle = "Customize NOVA MUSIC to your listening style.",
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

        item(key = "profile_card") {
            NovaGlassProfileCard(
                songCount = totalSongsCount,
                librarySizeLabel = FormatUtils.formatFileSize(totalLibraryBytes),
                eqEnabled = eqState.isEnabled,
                activePreset = eqState.activePresetName,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onOpenAbout
            )
        }

        // Group 1: Look & Audio Experience
        item(key = "group_experience") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Experience & Audio", icon = Icons.Rounded.Tune)
                NovaSettingsCard {
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Palette,
                        title = "Appearance",
                        description = "Theme, accent color, dynamic colors, animations & player look",
                        statusBadge = settings.themeMode.label,
                        onClick = onOpenAppearance
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Playback",
                        description = "Gapless playback, crossfade, ReplayGain, resume & audio focus",
                        statusBadge = if (settings.crossfadeSeconds > 0) "${settings.crossfadeSeconds}s" else settings.audioQuality.label.substringBefore(' '),
                        onClick = onOpenPlayback
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Equalizer,
                        title = "Audio & Equalizer",
                        description = "10-band equalizer, presets, bass boost, virtualizer & effects",
                        statusBadge = if (eqState.isEnabled) eqState.activePresetName else "Off",
                        highlightIcon = eqState.isEnabled,
                        onClick = onOpenAudioEqualizer
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Headphones,
                        title = "Headphones",
                        description = "Connected headphones, disconnect pause & connect auto-resume",
                        statusBadge = headphonesBadge,
                        highlightIcon = externalDevicesCount > 0,
                        onClick = onOpenHeadphones
                    )
                }
            }
        }

        // Group 2: Media, Notifications & Data
        item(key = "group_content") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Media & Device", icon = Icons.Rounded.LibraryMusic)
                NovaSettingsCard {
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        description = "Media notification controls, artwork, lock screen & card style",
                        statusBadge = if (settings.notificationsEnabled) "On" else "Off",
                        onClick = onOpenNotifications
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "Library & Music Management",
                        description = "Folder filters, rescan schedule, min track length & sorting",
                        statusBadge = "$totalSongsCount tracks",
                        onClick = onOpenLibraryManagement
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Download,
                        title = "Downloads",
                        description = "Release APK packages, Wi-Fi download rule & downloaded files",
                        statusBadge = if (updateDownloadsSizeBytes > 0L) FormatUtils.formatFileSize(updateDownloadsSizeBytes) else "Empty",
                        onClick = onOpenDownloads
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Storage,
                        title = "Storage",
                        description = "Library footprint, artwork cache size & playback history reset",
                        statusBadge = FormatUtils.formatFileSize(totalCacheAndDownloads),
                        onClick = onOpenStorage
                    )
                }
            }
        }

        // Group 3: Updates, Privacy & About
        item(key = "group_system") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "System & Information", icon = Icons.Rounded.Info)
                NovaSettingsCard {
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "App Updates",
                        description = "Check releases, auto-update schedule, changelog & installer status",
                        statusBadge = updatesBadge,
                        highlightIcon = updateState is UpdateUiState.Available || updateState is UpdateUiState.ReadyToInstall,
                        onClick = onOpenAppUpdates
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Policy,
                        title = "Privacy",
                        description = "Offline guarantee, app permissions, lock screen & data cleanup",
                        statusBadge = "Offline",
                        onClick = onOpenPrivacy
                    )
                    NovaSettingsDivider()
                    NovaSettingsCategoryRow(
                        icon = Icons.Rounded.Info,
                        title = "About NOVA MUSIC",
                        description = "Version info, developer credits, audio engine & open source licenses",
                        statusBadge = "v${BuildConfig.VERSION_NAME}",
                        onClick = onOpenAbout
                    )
                }
            }
        }

        item(key = "settings_footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
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
}
