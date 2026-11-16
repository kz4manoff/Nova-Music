package com.novamusic.player.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.AudioFocusBehavior
import com.novamusic.player.data.model.AudioQualityMode
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.SleepTimerDialog
import com.novamusic.player.ui.components.audioOutputIcon
import com.novamusic.player.ui.components.rememberNovaHaptic
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlaybackSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    NovaSettingsSubpageScaffold(
        title = "Playback",
        subtitle = "Transitions, ReplayGain, audio focus & queue continuity",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "playback_transitions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Transitions & Loudness", icon = Icons.Rounded.GraphicEq)
                NovaSettingsCard {
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Gapless Playback",
                        subtitle = "Seamless transitions without silence between album tracks",
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
                        subtitle = "Normalize loudness headroom cleanly across tracks",
                        checked = settings.replayGainEnabled,
                        onCheckedChange = viewModel::setReplayGainEnabled
                    )
                }
            }
        }

        item(key = "playback_engine") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Session & Audio Focus", icon = Icons.Rounded.PlayArrow)
                NovaSettingsCard {
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.HighQuality,
                        title = "Audio Quality Mode",
                        subtitle = settings.audioQuality.description,
                        selectedLabel = settings.audioQuality.label,
                        options = AudioQualityMode.entries.map { it to it.label },
                        selectedValue = settings.audioQuality,
                        onSelect = viewModel::setAudioQuality
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Resume Playback",
                        subtitle = "Restore last played track, queue and timestamp on launch",
                        checked = settings.resumePlaybackOnStart,
                        onCheckedChange = viewModel::setResumePlayback
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Autoplay",
                        subtitle = "Continue playing library tracks automatically after queue ends",
                        checked = settings.autoplayEnabled,
                        onCheckedChange = viewModel::setAutoplayEnabled
                    )
                    NovaSettingsDivider()
                    NovaDropdownSettingRow(
                        icon = Icons.Rounded.Hearing,
                        title = "Audio Focus Behavior",
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
                        subtitle = "Automatically pause playback after countdown or current track",
                        trailingText = if (playerState.sleepTimer.isActive) {
                            if (playerState.sleepTimer.stopAfterCurrentTrack) "End of track"
                            else FormatUtils.formatTimerCountdown(playerState.sleepTimer.remainingMs)
                        } else "Off",
                        onClick = { showSleepTimerDialog = true }
                    )
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
}

@Composable
fun AudioEqualizerSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenFullEqualizer: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    bottomContentPadding: Dp
) {
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val audioRoutingState by viewModel.audioRoutingState.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()
    var eqBandsExpanded by remember { mutableStateOf(true) }

    NovaSettingsSubpageScaffold(
        title = "Audio & Equalizer",
        subtitle = if (eqState.isEnabled) "Active • ${eqState.activePresetName}" else "Direct audio signal",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "eq_master_curve") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Equalizer",
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
                                    text = if (eqBandsExpanded) "Hide ${eqState.bands.size}-Band Faders" else "Show ${eqState.bands.size}-Band Faders",
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
                }
            }
        }

        item(key = "dsp_effects") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Bass Boost, Virtualizer & Effects", icon = Icons.Rounded.SurroundSound)
                NovaSettingsCard {
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
                    val activeOutput = audioRoutingState.activeDevice
                    NovaActionSettingRow(
                        icon = activeOutput?.let { audioOutputIcon(it.type) } ?: Icons.Rounded.VolumeUp,
                        title = "Active Output Device",
                        subtitle = activeOutput?.type?.label ?: "Automatic System Route",
                        trailingText = activeOutput?.name ?: "Speaker",
                        onClick = onOpenAudioOutput
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Equalizer,
                        title = "Open Full Equalizer Studio",
                        subtitle = "Manage custom saved presets & dedicated console",
                        onClick = onOpenFullEqualizer
                    )
                }
            }
        }

        item(key = "dsp_status") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                NovaSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Effects Engine",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = eqState.engineDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (eqState.audioSessionId > 0) "SES #${eqState.audioSessionId}" else "IDLE",
                            style = NovaMonoTelemetry.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (eqState.isEnabled && eqState.audioSessionId > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
