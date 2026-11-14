package com.novamusic.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.ThemeMode
import com.novamusic.player.ui.components.LiquidGlassCard
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.SleepTimerDialog
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
    onOpenEqualizer: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val dividerColor = novaColors.borderSubtle.copy(alpha = 0.65f)

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = bottomContentPadding + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Liquid Glass Top Header
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = 14.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        LiquidGlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            size = 40.dp,
                            iconSize = 20.dp,
                            onClick = onBack
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${settings.themeMode.label.uppercase()} LIQUID GLASS • ${settings.accentColor.label.uppercase()}",
                            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                            color = accent
                        )
                    }
                }
            }
        }

        // Appearance: Theme Mode & Accent Color
        item {
            LiquidGlassSectionCard(title = "Appearance & Theme", icon = Icons.Rounded.Palette) {
                Text(
                    text = "Liquid Glass Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = settings.themeMode == mode
                        LiquidGlassCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            highlighted = selected,
                            elevation = if (selected) 12.dp else 4.dp,
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
                            onClick = { viewModel.setThemeMode(mode) }
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Accent Refraction Tint",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccentColorOption.entries.forEach { option ->
                        val isSelected = settings.accentColor == option
                        val swatchColor = Color(option.hex)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) swatchColor.copy(alpha = 0.22f) else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) swatchColor else novaColors.glassBorderBottom,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setAccentColor(option) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(
                                        elevation = if (isSelected) 10.dp else 4.dp,
                                        shape = CircleShape,
                                        ambientColor = swatchColor,
                                        spotColor = swatchColor
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                swatchColor.copy(alpha = 0.88f),
                                                swatchColor
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = option.label,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = dividerColor)
                SettingsActionRow(
                    icon = Icons.Rounded.Language,
                    title = "Language",
                    subtitle = languages.firstOrNull { it.first == settings.languageTag }?.second ?: "System Default",
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        // Audio & Playback Settings
        item {
            LiquidGlassSectionCard(title = "Audio & Playback", icon = Icons.Rounded.Equalizer) {
                SettingsSwitchRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between consecutive tracks",
                    checked = settings.gaplessPlayback,
                    onCheckedChange = viewModel::setGaplessPlayback
                )
                HorizontalDivider(color = dividerColor)
                SettingsSwitchRow(
                    title = "Playback Resume",
                    subtitle = "Remember last played track, queue, and position on restart",
                    checked = settings.resumePlaybackOnStart,
                    onCheckedChange = viewModel::setResumePlayback
                )
                HorizontalDivider(color = dividerColor)
                SettingsActionRow(
                    icon = Icons.Rounded.Equalizer,
                    title = "Equalizer & DSP",
                    subtitle = "10-Band EQ, Bass Boost, Virtualizer, Loudness, Preamp",
                    onClick = onOpenEqualizer
                )
                HorizontalDivider(color = dividerColor)
                SettingsActionRow(
                    icon = Icons.Rounded.Bedtime,
                    title = "Sleep Timer",
                    subtitle = if (playerState.sleepTimer.isActive) {
                        if (playerState.sleepTimer.stopAfterCurrentTrack) {
                            "Stopping after current track"
                        } else {
                            "${FormatUtils.formatTimerCountdown(playerState.sleepTimer.remainingMs)} remaining"
                        }
                    } else {
                        "Off"
                    },
                    onClick = { showSleepTimerDialog = true }
                )
            }
        }

        // Album Artwork Settings
        item {
            LiquidGlassSectionCard(title = "Album Artwork", icon = Icons.Rounded.Album) {
                Text(
                    text = "Now Playing Artwork Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtworkStyle.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { style ->
                                val selected = settings.artworkStyle == style
                                LiquidGlassCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    highlighted = selected,
                                    elevation = if (selected) 10.dp else 4.dp,
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                                    onClick = { viewModel.setArtworkStyle(style) }
                                ) {
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                SettingsSwitchRow(
                    title = "High-Resolution Artwork",
                    subtitle = "Load full-resolution album covers",
                    checked = settings.highResArtwork,
                    onCheckedChange = viewModel::setHighResArtwork
                )
            }
        }

        // Library Scanning Settings
        item {
            LiquidGlassSectionCard(title = "Library Scanning", icon = Icons.Rounded.LibraryMusic) {
                SettingsActionRow(
                    icon = Icons.Rounded.Refresh,
                    title = "Rescan Music Library",
                    subtitle = if (isScanning) "Scanning device storage..." else "Scan MP3, FLAC, WAV, AAC, M4A, OGG, OPUS",
                    trailing = if (isScanning) {
                        { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent) }
                    } else null,
                    onClick = viewModel::rescanLibrary
                )
                HorizontalDivider(color = dividerColor)
                SettingsActionRow(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Minimum Track Duration",
                    subtitle = "Ignore clips shorter than ${settings.minTrackDurationSec}s",
                    onClick = { showMinDurationDialog = true }
                )
                HorizontalDivider(color = dividerColor)
                SettingsActionRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Clear Playback History",
                    subtitle = "Reset recently played and most played counts",
                    onClick = viewModel::clearHistory
                )
            }
        }

        // App Information
        item {
            LiquidGlassSectionCard(title = "App Information", icon = Icons.Rounded.Info) {
                InfoLine("Application", "NOVA MUSIC")
                HorizontalDivider(color = dividerColor.copy(alpha = 0.5f))
                InfoLine("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                HorizontalDivider(color = dividerColor.copy(alpha = 0.5f))
                InfoLine("Design System", "Liquid Glass Multi-Layer")
                HorizontalDivider(color = dividerColor.copy(alpha = 0.5f))
                InfoLine("Audio Engine", "Media3 ExoPlayer + MediaSession")
                HorizontalDivider(color = dividerColor.copy(alpha = 0.5f))
                InfoLine("DSP Engine", "Android AudioEffect / Dynamics")
                HorizontalDivider(color = dividerColor.copy(alpha = 0.5f))
                InfoLine("Storage", "100% Offline • Room & DataStore")
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = LocalNovaColors.current.elevatedSurface,
            title = { Text("Select Language") },
            text = {
                Column {
                    languages.forEach { (tag, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(tag)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.languageTag == tag,
                                onClick = {
                                    viewModel.setLanguage(tag)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    if (showMinDurationDialog) {
        val options = listOf(0, 5, 10, 30, 60)
        AlertDialog(
            onDismissRequest = { showMinDurationDialog = false },
            containerColor = LocalNovaColors.current.elevatedSurface,
            title = { Text("Minimum Track Duration") },
            text = {
                Column {
                    options.forEach { sec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setMinTrackDurationSec(sec)
                                    showMinDurationDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.minTrackDurationSec == sec,
                                onClick = {
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
            },
            confirmButton = {
                TextButton(onClick = { showMinDurationDialog = false }) { Text("Close") }
            }
        )
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
}

@Composable
private fun LiquidGlassSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = 14.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(0.8.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title.uppercase(),
                    style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp),
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val novaColors = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = novaColors.recessedSurface,
                uncheckedBorderColor = novaColors.glassBorderBottom
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val novaColors = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(novaColors.recessedSurface.copy(alpha = 0.85f))
                .border(0.8.dp, novaColors.glassBorderBottom, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = NovaMonoTelemetry.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
