package com.novamusic.player.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.NotificationAppearance
import com.novamusic.player.data.model.OutputSwitchOutcome
import com.novamusic.player.permissions.PermissionHelper
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.audioOutputIcon

@Composable
fun HeadphonesSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val routingState by viewModel.audioRoutingState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val btPermission = PermissionHelper.bluetoothConnectPermission()
    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshAudioDevices()
    }

    val externalDevices = routingState.devices.filter { it.type.isExternal }
    val activeDevice = routingState.activeDevice

    NovaSettingsSubpageScaffold(
        title = "Headphones",
        subtitle = "Connected audio hardware & plug behavior",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding,
        trailingActions = {
            LiquidGlassIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh audio devices",
                size = 40.dp,
                iconSize = 19.dp,
                onClick = { viewModel.refreshAudioDevices() }
            )
        }
    ) {
        item(key = "headphones_connected") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Connected Headphones & Devices",
                    icon = Icons.Rounded.Headphones,
                    trailingBadge = if (externalDevices.isEmpty()) "None" else "${externalDevices.size} External"
                )
                NovaSettingsCard {
                    if (!routingState.hasBluetoothConnectPermission && btPermission != null) {
                        NovaActionSettingRow(
                            icon = Icons.Rounded.Bluetooth,
                            title = "Allow Bluetooth Device Detection",
                            subtitle = "Grant nearby Bluetooth access to read headphone names & battery",
                            trailingText = "Allow",
                            onClick = { btPermLauncher.launch(btPermission) }
                        )
                        NovaSettingsDivider()
                    }

                    if (externalDevices.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No headphones detected",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "Active route: ${activeDevice?.name ?: "Phone Speaker"}. Connect wired, USB-C, or Bluetooth headphones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        externalDevices.forEachIndexed { idx, device ->
                            val statusText = buildString {
                                append(device.type.label)
                                if (device.isActive) append(" • Active Output")
                                else append(" • Connected")
                            }
                            val badgeText = buildString {
                                device.batteryPercent?.let { append("$it%") }
                                if (device.isActive) {
                                    if (isNotEmpty()) append(" • ")
                                    append("Active")
                                }
                            }.ifBlank { if (device.isDirectSwitchSupported) "Switch" else "System" }

                            NovaActionSettingRow(
                                icon = audioOutputIcon(device.type),
                                title = device.name,
                                subtitle = statusText,
                                trailingText = badgeText,
                                onClick = {
                                    val outcome = viewModel.selectAudioOutput(device.id)
                                    if (outcome == OutputSwitchOutcome.SYSTEM_SELECTOR_RECOMMENDED) {
                                        viewModel.openSystemSoundOutputPanel(context)
                                    }
                                }
                            )
                            if (idx < externalDevices.lastIndex) {
                                NovaSettingsDivider()
                            }
                        }
                    }

                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.VolumeUp,
                        title = "Audio Output Switcher",
                        subtitle = "Active: ${activeDevice?.name ?: "Phone Speaker"}",
                        trailingText = "${routingState.devices.size} total",
                        onClick = onOpenAudioOutput
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Bluetooth,
                        title = "Bluetooth System Settings",
                        subtitle = "Pair or manage wireless headphones in Android Settings",
                        onClick = { viewModel.openBluetoothSettings(context) }
                    )
                }
            }
        }

        item(key = "headphones_behavior") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Connection Behavior", icon = Icons.Rounded.PlayArrow)
                NovaSettingsCard {
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Headphones,
                        title = "Pause When Headphones Disconnect",
                        subtitle = "Immediately pause playback when wired or Bluetooth headphones unplug",
                        checked = settings.pauseOnHeadphonesDisconnect,
                        onCheckedChange = viewModel::setPauseOnHeadphonesDisconnect
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Resume When Headphones Connect",
                        subtitle = "Automatically resume queued playback when headphones connect",
                        checked = settings.resumeOnHeadphonesConnect,
                        onCheckedChange = viewModel::setResumeOnHeadphonesConnect
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenFcmDiagnostics: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val fcmDiag by viewModel.fcmDiagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        viewModel.refreshFcmDiagnostics()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshFcmDiagnostics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshFcmDiagnostics()
        if (granted) {
            viewModel.syncFcmDeviceRegistration(force = true)
        } else {
            viewModel.openAppNotificationSettings(context)
        }
    }

    NovaSettingsSubpageScaffold(
        title = "Notifications",
        subtitle = "Media shade controls, push broadcast sync & permissions",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "notification_controls") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Media Notification", icon = Icons.Rounded.Notifications)
                NovaSettingsCard {
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Playback Notifications",
                        subtitle = "Display media transport controls in the notification shade",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Lock,
                        title = "Lock Screen Controls",
                        subtitle = "Show playback controls and track info on device lock screen",
                        checked = settings.lockScreenControls,
                        enabled = settings.notificationsEnabled,
                        onCheckedChange = viewModel::setLockScreenControls
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Image,
                        title = "Show Album Artwork",
                        subtitle = "Publish active track cover art in system media controls",
                        checked = settings.showNotificationArtwork,
                        enabled = settings.notificationsEnabled,
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

        item(key = "notification_system") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Android System Permissions", icon = Icons.Rounded.Notifications)
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = "System Notification Settings",
                        subtitle = if (fcmDiag.notificationPermissionGranted) {
                            "Notifications permitted by Android • Manage channels"
                        } else {
                            "Blocked or permission not granted • Tap to allow"
                        },
                        trailingText = if (fcmDiag.notificationPermissionGranted) "Allowed" else "Allow",
                        onClick = {
                            if (!fcmDiag.postNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.openAppNotificationSettings(context)
                            }
                        }
                    )
                }
            }
        }

        item(key = "notification_broadcast_fcm") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "NOVA Broadcast & Push Status",
                    icon = Icons.Rounded.Sync,
                    trailingBadge = if (fcmDiag.isRegisteredConfirmed) "Registered" else fcmDiag.registrationStatus
                )
                NovaSettingsCard {
                    NovaInfoRow(
                        label = "FCM Initialization",
                        value = if (fcmDiag.firebaseInitialized) "Initialized" else "Not Initialized"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Notification Permission",
                        value = if (fcmDiag.notificationPermissionGranted) "Granted" else "Denied"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Token Registration",
                        value = fcmDiag.registrationStatus
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Last Successful Sync",
                        value = formatDiagnosticTimestamp(fcmDiag.lastSuccessMs, "Never")
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.BugReport,
                        title = "NOVA Notifications Diagnostics",
                        subtitle = "Inspect Firebase status, FCM token, backend connection & retry registration",
                        trailingText = if (fcmDiag.lastBackendHttpCode > 0) "HTTP ${fcmDiag.lastBackendHttpCode}" else "Open",
                        onClick = onOpenFcmDiagnostics
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
