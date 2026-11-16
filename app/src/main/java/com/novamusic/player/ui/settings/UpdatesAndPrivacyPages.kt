package com.novamusic.player.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.BuildConfig
import com.novamusic.player.R
import com.novamusic.player.permissions.PermissionHelper
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.update.UpdateConfig
import com.novamusic.player.update.UpdateUiState
import com.novamusic.player.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppUpdatesSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val context = LocalContext.current

    var canInstallUnknownApps by remember { mutableStateOf(viewModel.canRequestPackageInstalls()) }
    val installPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        canInstallUnknownApps = viewModel.canRequestPackageInstalls()
    }

    val lastCheckedLabel = remember(settings.lastUpdateCheckMs) {
        if (settings.lastUpdateCheckMs <= 0L) "Not checked yet"
        else SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(settings.lastUpdateCheckMs))
    }

    val statusTitle = when (val state = updateState) {
        is UpdateUiState.Checking -> "Checking GitHub Releases..."
        is UpdateUiState.Available -> "Version ${state.config.latestVersion} Available"
        is UpdateUiState.Downloading -> "Downloading v${state.config.latestVersion}..."
        is UpdateUiState.Verifying -> "Verifying APK Signature & SHA-256..."
        is UpdateUiState.ReadyToInstall -> "Ready to Install v${state.config.latestVersion}"
        is UpdateUiState.UpToDate -> "You're Up to Date"
        is UpdateUiState.Error -> "Update Check Failed"
        UpdateUiState.Idle -> "Check for Updates"
    }

    val statusSubtitle = when (val state = updateState) {
        is UpdateUiState.Available -> "Build ${state.config.versionCode} • Tap to view details & update"
        is UpdateUiState.UpToDate -> "v${state.installedVersionName} (Build ${state.installedVersionCode}) is the latest release"
        is UpdateUiState.Error -> "${state.message} • Tap to retry"
        else -> "Last checked: $lastCheckedLabel"
    }

    val trailingBadge = when (val state = updateState) {
        is UpdateUiState.Available -> "v${state.config.latestVersion}"
        is UpdateUiState.UpToDate -> "Latest"
        is UpdateUiState.ReadyToInstall -> "Install"
        else -> null
    }

    NovaSettingsSubpageScaffold(
        title = "App Updates",
        subtitle = "Installed v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "updates_version_hero") {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(novaColors.recessedSurface)
                                .border(0.7.dp, novaColors.glassBorderTop, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_nova_logo),
                                contentDescription = "NOVA MUSIC",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NOVA MUSIC",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                                style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                                color = accent
                            )
                            Text(
                                text = "Last checked: $lastCheckedLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item(key = "updates_actions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Update Controls", icon = Icons.Rounded.SystemUpdate)
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = statusTitle,
                        subtitle = statusSubtitle,
                        trailingText = trailingBadge,
                        trailingContent = if (updateState is UpdateUiState.Checking) {
                            { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent) }
                        } else null,
                        onClick = {
                            when (updateState) {
                                is UpdateUiState.Available,
                                is UpdateUiState.Downloading,
                                is UpdateUiState.ReadyToInstall -> viewModel.reopenUpdateDialog()
                                else -> viewModel.checkForUpdates()
                            }
                        }
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Automatic Update Checks",
                        subtitle = "Check once every 24 hours in the background for new releases",
                        checked = settings.autoCheckUpdates,
                        onCheckedChange = viewModel::setAutoCheckUpdates
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Security,
                        title = "Install Unknown Apps Permission",
                        subtitle = if (canInstallUnknownApps) {
                            "Authorized to install verified NOVA MUSIC updates in place"
                        } else {
                            "Required by Android to install downloaded APK updates"
                        },
                        trailingText = if (canInstallUnknownApps) "Granted" else "Allow",
                        onClick = { installPermLauncher.launch(viewModel.buildInstallPermissionIntent()) }
                    )
                }
            }
        }

        item(key = "updates_changelog") {
            val activeRemoteConfig = when (val state = updateState) {
                is UpdateUiState.Available -> state.config
                is UpdateUiState.Downloading -> state.config
                is UpdateUiState.Verifying -> state.config
                is UpdateUiState.ReadyToInstall -> state.config
                else -> null
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = if (activeRemoteConfig != null) "v${activeRemoteConfig.latestVersion} Release Notes" else "Release Channel & Security",
                    icon = Icons.Rounded.CheckCircle
                )
                NovaSettingsCard {
                    if (activeRemoteConfig != null) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = activeRemoteConfig.releaseNotes.ifBlank { "Performance, stability, and playback refinements." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        NovaSettingsDivider()
                    }
                    NovaInfoRow("Package ID", context.packageName)
                    NovaSettingsDivider()
                    NovaInfoRow(
                        "Manifest Endpoint",
                        UpdateConfig.updateJsonUrl.removePrefix("https://").removePrefix("http://")
                    )
                    NovaSettingsDivider()
                    NovaInfoRow("Verification", "HTTPS + SHA-256 + Key Match")
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val routingState by viewModel.audioRoutingState.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var historyClearedBanner by remember { mutableStateOf(false) }
    var cacheClearedBanner by remember { mutableStateOf(false) }

    val hasAudioPerm = PermissionHelper.hasAudioPermission(context)
    val btPermission = PermissionHelper.bluetoothConnectPermission()
    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshAudioDevices()
    }

    NovaSettingsSubpageScaffold(
        title = "Privacy",
        subtitle = "100% offline playback • Zero telemetry",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding
    ) {
        item(key = "privacy_guarantee") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Privacy Architecture", icon = Icons.Rounded.Security)
                NovaSettingsCard {
                    NovaInfoRow("Telemetry & Analytics", "None • Zero Tracking")
                    NovaSettingsDivider()
                    NovaInfoRow("Data Storage", "100% On-Device (Room & DataStore)")
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Policy,
                        title = "Privacy Policy",
                        subtitle = "Review how NOVA MUSIC handles local audio & permissions",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
        }

        item(key = "privacy_controls") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Visibility & Network", icon = Icons.Rounded.Lock)
                NovaSettingsCard {
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.Lock,
                        title = "Lock Screen Media Controls",
                        subtitle = "Display currently playing track details on the lock screen",
                        checked = settings.lockScreenControls,
                        onCheckedChange = viewModel::setLockScreenControls
                    )
                    NovaSettingsDivider()
                    NovaSwitchSettingRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Automatic Update Checks",
                        subtitle = "Turn off for strictly zero background network requests",
                        checked = settings.autoCheckUpdates,
                        onCheckedChange = viewModel::setAutoCheckUpdates
                    )
                }
            }
        }

        item(key = "privacy_permissions_and_data") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(title = "Permissions & Local Data", icon = Icons.Rounded.Folder)
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Folder,
                        title = "Audio Library Permission",
                        subtitle = "Used only to scan and play your local audio files",
                        trailingText = if (hasAudioPerm) "Granted" else "Denied",
                        onClick = { viewModel.openAppDetailsSettings(context) }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Bluetooth,
                        title = "Bluetooth Device Permission",
                        subtitle = "Used only to display connected headphone names & battery",
                        trailingText = if (routingState.hasBluetoothConnectPermission) "Granted" else "Allow",
                        onClick = {
                            if (!routingState.hasBluetoothConnectPermission && btPermission != null) {
                                btPermLauncher.launch(btPermission)
                            } else {
                                viewModel.openAppDetailsSettings(context)
                            }
                        }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.History,
                        title = "Clear Playback History",
                        subtitle = if (historyClearedBanner) "Recently played & counts cleared" else "Erase recently played tracks & play counts",
                        onClick = {
                            viewModel.clearHistory()
                            historyClearedBanner = true
                        }
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Cached Data",
                        subtitle = if (cacheClearedBanner) "Artwork and temporary cache cleared" else "Remove cached cover art & temp files",
                        trailingText = FormatUtils.formatFileSize(cacheSizeBytes),
                        onClick = {
                            viewModel.clearCache()
                            cacheClearedBanner = true
                        }
                    )
                }
            }
        }
    }

    if (showPrivacyDialog) {
        NovaPrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}
