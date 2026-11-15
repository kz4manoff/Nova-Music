package com.novamusic.player.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamusic.player.data.model.AudioDeviceEventBanner
import com.novamusic.player.data.model.AudioOutputDevice
import com.novamusic.player.data.model.AudioOutputType
import com.novamusic.player.data.model.AudioRoutingUiState
import com.novamusic.player.data.model.OutputSwitchOutcome
import com.novamusic.player.permissions.PermissionHelper
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry

fun audioOutputIcon(type: AudioOutputType): ImageVector = when (type) {
    AudioOutputType.PHONE_SPEAKER -> Icons.Rounded.PhoneAndroid
    AudioOutputType.EARPIECE -> Icons.Rounded.Hearing
    AudioOutputType.WIRED_HEADPHONES -> Icons.Rounded.Headphones
    AudioOutputType.USB_AUDIO -> Icons.Rounded.Usb
    AudioOutputType.BLUETOOTH_HEADPHONES -> Icons.Rounded.BluetoothAudio
    AudioOutputType.BLUETOOTH_SPEAKER -> Icons.Rounded.Speaker
}

/**
 * Floating iOS-style Liquid Glass popup notification for real-time audio device connect / disconnect events.
 */
@Composable
fun AudioDeviceEventPopup(
    banner: AudioDeviceEventBanner?,
    onDismiss: () -> Unit,
    onOpenSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val accent = MaterialTheme.colorScheme.primary
    val palette = LocalArtworkPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = banner != null,
            enter = if (reducedMotion) {
                fadeIn(tween(120))
            } else {
                slideInVertically(
                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 360f),
                    initialOffsetY = { -it }
                ) + scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)
                ) + fadeIn(tween(220))
            },
            exit = if (reducedMotion) {
                fadeOut(tween(100))
            } else {
                slideOutVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it }
                ) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)) + fadeOut(tween(180))
            }
        ) {
            banner?.let { event ->
                val isConnected = event.isConnectedEvent
                val tone = if (isConnected) accent else MaterialTheme.colorScheme.onSurfaceVariant

                LiquidGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    highlighted = isConnected,
                    accentTint = if (isConnected) palette.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(event.eventId) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -18f) onDismiss()
                            }
                        },
                    onClick = {
                        onDismiss()
                        onOpenSwitcher()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(
                                    elevation = if (isConnected) 12.dp else 4.dp,
                                    shape = CircleShape,
                                    ambientColor = tone.copy(alpha = 0.45f),
                                    spotColor = tone.copy(alpha = 0.55f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            tone.copy(alpha = if (isConnected) 0.30f else 0.16f),
                                            tone.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    width = 0.85.dp,
                                    color = tone.copy(alpha = 0.45f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = audioOutputIcon(event.deviceType),
                                contentDescription = event.deviceType.label,
                                tint = if (isConnected) accent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = event.deviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (event.batteryPercent != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accent.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${event.batteryPercent}%",
                                            style = NovaMonoTelemetry.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = accent
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isConnected) {
                                    "Connected Successfully • Ready to Listen"
                                } else {
                                    "${event.deviceType.label} Disconnected"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isConnected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isConnected) {
                            AnimatedEqualizerBars(isPlaying = true, color = accent)
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss notification",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact pill showing active audio output icon and device name (for Now Playing / Settings).
 */
@Composable
fun AudioOutputTriggerPill(
    activeDevice: AudioOutputDevice?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val icon = activeDevice?.let { audioOutputIcon(it.type) } ?: Icons.Rounded.VolumeUp
    val label = activeDevice?.name ?: "Phone Speaker"

    LiquidGlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        elevation = 10.dp,
        highlighted = activeDevice?.type?.isExternal == true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Audio Output",
                tint = if (activeDevice?.type?.isExternal == true) accent else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "output_pill_label"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isPlaying && activeDevice?.type?.isExternal == true) {
                Spacer(modifier = Modifier.width(6.dp))
                AnimatedEqualizerBars(isPlaying = true, color = accent)
            }
        }
    }
}

/**
 * Liquid Glass Audio Output Selector bottom sheet showing real connected devices,
 * active playback route, real battery telemetry when accessible, and legitimate Android routing controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputSwitcherSheet(
    routingState: AudioRoutingUiState,
    isPlaying: Boolean,
    onSelectDevice: (String?) -> OutputSwitchOutcome,
    onRefreshPermissions: () -> Unit,
    onOpenSystemOutput: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()
    val btPermission = PermissionHelper.bluetoothConnectPermission()
    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onRefreshPermissions()
    }

    LiquidGlassSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Audio Output",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = routingState.activeDevice?.let {
                            "Active: ${it.name} (${it.type.label})"
                        } ?: "Automatic System Route",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!routingState.isAutoRouteSelected) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        onClick = {
                            performHaptic()
                            onSelectDevice(null)
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = accent
                            )
                        }
                    }
                }
            }

            if (!routingState.hasBluetoothConnectPermission && btPermission != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LiquidGlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    highlighted = true,
                    accentTint = accent,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = { btPermLauncher.launch(btPermission) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Bluetooth Device Names",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Tap to display paired headphone names & speaker types",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            routingState.statusNotice?.takeIf { it.isNotBlank() }?.let { notice ->
                Spacer(modifier = Modifier.height(8.dp))
                LiquidGlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    glassOpacity = 0.72f,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (routingState.devices.isEmpty()) {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No audio output sinks reported by Android yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 330.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routingState.devices, key = { it.id }) { device ->
                        AudioOutputDeviceRow(
                            device = device,
                            isPlaying = isPlaying,
                            onClick = {
                                performHaptic()
                                val outcome = onSelectDevice(device.id)
                                if (outcome == OutputSwitchOutcome.SYSTEM_SELECTOR_RECOMMENDED) {
                                    onOpenSystemOutput()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Official System Panel & Bluetooth fallback actions for OS-level multi-BT switching
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassButton(
                    onClick = {
                        performHaptic()
                        onOpenSystemOutput()
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "System Output",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LiquidGlassButton(
                    onClick = {
                        performHaptic()
                        onOpenBluetoothSettings()
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = if (novaColors.isLight) MaterialTheme.colorScheme.onSurface else accent,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bluetooth",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioOutputDeviceRow(
    device: AudioOutputDevice,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (device.isActive) 1f else 0.99f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "output_row_scale_${device.id}"
    )

    LiquidGlassSurface(
        shape = RoundedCornerShape(20.dp),
        highlighted = device.isActive,
        accentTint = accent,
        elevation = if (device.isActive) 12.dp else 0.dp,
        glassOpacity = if (device.isActive) 1f else 0.65f,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isActive) accent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .border(
                        width = 0.75.dp,
                        color = if (device.isActive) accent.copy(alpha = 0.60f)
                        else Color.White.copy(alpha = 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = audioOutputIcon(device.type),
                    contentDescription = device.type.label,
                    tint = if (device.isActive) accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (device.isActive) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (device.isActive) accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (device.batteryPercent != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${device.batteryPercent}%",
                                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                color = if (device.isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(device.type.label)
                        append(" • ")
                        append(
                            when {
                                device.isActive -> "Active Output"
                                !device.isDirectSwitchSupported -> "Switch in System Output"
                                else -> "Connected"
                            }
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (device.isActive) {
                if (isPlaying) {
                    AnimatedEqualizerBars(isPlaying = true, color = accent)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected output",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
