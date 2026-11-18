package com.novamusic.player.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.BuildConfig
import com.novamusic.player.R
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.rememberNovaHaptic
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.ui.theme.NovaRedGlow
import com.novamusic.player.ui.theme.NovaRedPrimary
import com.novamusic.player.ui.theme.NovaRedSecondary
import com.novamusic.player.update.UpdateUiState
import com.novamusic.player.utils.FormatUtils
import kotlinx.coroutines.delay

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

    val navBarInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val resolvedBottomPadding = maxOf(bottomContentPadding, navBarInsetBottom) + 28.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entranceAlpha
                translationY = entranceOffsetPx
            },
        contentPadding = PaddingValues(bottom = resolvedBottomPadding),
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

        // Very bottom section: Contact Developer
        item(key = "contact_developer") {
            ContactDeveloperSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

private const val DEVELOPER_DISCORD_USERNAME = "k4zman_."
private const val DEVELOPER_INSTAGRAM_URL =
    "https://www.instagram.com/mohavmed.__?gwpx=NHFtc2h4cXRjcGV6"

@Composable
private fun ContactDeveloperSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()

    var discordCopied by remember { mutableStateOf(false) }

    LaunchedEffect(discordCopied) {
        if (discordCopied) {
            delay(2800L)
            discordCopied = false
        }
    }

    val copyDiscordUsername: () -> Unit = {
        performHaptic()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("Discord Username", DEVELOPER_DISCORD_USERNAME)
        )
        discordCopied = true
    }

    val openInstagramProfile: () -> Unit = {
        performHaptic()
        val uri = Uri.parse(DEVELOPER_INSTAGRAM_URL)
        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            runCatching { context.startActivity(browserIntent) }
        } catch (_: Throwable) {
            runCatching { context.startActivity(browserIntent) }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NovaSectionHeader(
            title = "CONTACT DEVELOPER",
            icon = Icons.Rounded.AlternateEmail
        )

        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            highlighted = true,
            accentTint = NovaRedPrimary,
            elevation = 14.dp,
            glassOpacity = 0.94f,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ContactDeveloperRow(
                    iconRes = R.drawable.ic_discord,
                    platformName = "Discord",
                    subtitle = DEVELOPER_DISCORD_USERNAME,
                    badgeColors = listOf(NovaRedPrimary, NovaRedSecondary),
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (discordCopied) {
                                        NovaRedPrimary.copy(alpha = 0.28f)
                                    } else {
                                        NovaRedPrimary.copy(alpha = 0.16f)
                                    }
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (discordCopied) {
                                        NovaRedPrimary.copy(alpha = 0.75f)
                                    } else {
                                        NovaRedGlow.copy(alpha = 0.45f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(onClick = copyDiscordUsername)
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            AnimatedContent(
                                targetState = discordCopied,
                                transitionSpec = {
                                    if (reducedMotion) {
                                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                    } else {
                                        fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                                    }
                                },
                                label = "discord_copy_label"
                            ) { copied ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        contentDescription = if (copied) "Copied" else "Copy Discord username",
                                        tint = if (copied) Color.White else NovaRedGlow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (copied) "Copied!" else "Copy",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    onClick = copyDiscordUsername
                )

                AnimatedVisibility(
                    visible = discordCopied,
                    enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, top = 2.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NovaRedPrimary.copy(alpha = 0.16f))
                            .border(0.65.dp, NovaRedPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = NovaRedGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copied username \"$DEVELOPER_DISCORD_USERNAME\" to clipboard",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                }

                NovaSettingsDivider()

                ContactDeveloperRow(
                    iconRes = R.drawable.ic_instagram,
                    platformName = "Instagram",
                    subtitle = "@mohavmed.__",
                    badgeColors = listOf(NovaRedGlow, NovaRedSecondary),
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NovaRedPrimary.copy(alpha = 0.16f))
                                .border(
                                    width = 0.75.dp,
                                    color = NovaRedGlow.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Open",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open Instagram profile",
                                    tint = NovaRedGlow,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    },
                    onClick = openInstagramProfile
                )
            }
        }
    }
}

@Composable
private fun ContactDeveloperRow(
    @DrawableRes iconRes: Int,
    platformName: String,
    subtitle: String,
    badgeColors: List<Color>,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.976f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "contact_row_scale_$platformName"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(colors = badgeColors))
                .border(0.75.dp, Color.White.copy(alpha = 0.36f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = platformName,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = platformName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = NovaMonoTelemetry.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        trailingContent()
    }
}
