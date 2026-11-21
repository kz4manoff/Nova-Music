package com.novamusic.player.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.BuildConfig
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.ui.theme.NovaRedPrimary
import com.novamusic.player.update.FcmDiagnosticsSnapshot
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FcmDiagnosticsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val diag by viewModel.fcmDiagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary

    var tokenCopied by remember { mutableStateOf(false) }
    var reportCopied by remember { mutableStateOf(false) }

    LaunchedEffect(tokenCopied) {
        if (tokenCopied) {
            delay(2_200L)
            tokenCopied = false
        }
    }

    LaunchedEffect(reportCopied) {
        if (reportCopied) {
            delay(2_200L)
            reportCopied = false
        }
    }

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

    val statusColor = if (diag.isRegisteredConfirmed) accent else NovaRedPrimary

    NovaSettingsSubpageScaffold(
        title = "FCM & Broadcast Diagnostics",
        subtitle = "Firebase initialization, token sync & NOVA_DB registration",
        onBack = onBack,
        bottomContentPadding = bottomContentPadding,
        trailingActions = {
            LiquidGlassIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Re-sync FCM device registration",
                size = 40.dp,
                iconSize = 19.dp,
                onClick = { viewModel.syncFcmDeviceRegistration(force = true) }
            )
        }
    ) {
        // Summary & Manual Sync Hero Card
        item(key = "fcm_diag_summary") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(statusColor.copy(alpha = 0.16f))
                                .border(0.8.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (diag.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = accent
                                )
                            } else {
                                Icon(
                                    imageVector = if (diag.isRegisteredConfirmed) {
                                        Icons.Rounded.CheckCircle
                                    } else {
                                        Icons.Rounded.ErrorOutline
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    diag.isSyncing -> "Syncing with NOVA BROADCAST..."
                                    diag.isRegisteredConfirmed -> "Device Registered in NOVA_DB"
                                    else -> "Device Not Registered Yet"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Status: ${diag.registrationStatus} • v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                                color = statusColor
                            )
                        }
                    }

                    NovaSettingsDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    LiquidGlassButton(
                        onClick = { viewModel.syncFcmDeviceRegistration(force = true) },
                        enabled = !diag.isSyncing,
                        highlighted = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (diag.isSyncing) "Registering Device..." else "Register / Re-Sync Device Now",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // 1. Firebase Initialization
        item(key = "fcm_diag_firebase") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Firebase Initialization",
                    icon = Icons.Rounded.BugReport,
                    trailingBadge = if (diag.firebaseInitialized) "Ready" else "Missing Config"
                )
                NovaSettingsCard {
                    NovaInfoRow(
                        label = "Firebase Init",
                        value = if (diag.firebaseInitialized) "Initialized" else "Not Initialized"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Firebase Project",
                        value = diag.firebaseProjectId.ifBlank { "Unavailable" }
                    )
                    NovaSettingsDivider()
                    Text(
                        text = diag.firebaseStatusDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diag.firebaseInitialized) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            NovaRedPrimary
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // 2. Notification Permission
        item(key = "fcm_diag_permission") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Notification Permission",
                    icon = Icons.Rounded.Notifications,
                    trailingBadge = if (diag.notificationPermissionGranted) "Granted" else "Action Needed"
                )
                NovaSettingsCard {
                    NovaInfoRow(
                        label = "POST_NOTIFICATIONS",
                        value = if (diag.postNotificationsGranted) "Granted" else "Denied"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "System Notifications",
                        value = if (diag.systemNotificationsEnabled) "Allowed" else "Blocked"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Broadcast Eligible",
                        value = if (diag.notificationPermissionGranted) "Yes" else "No"
                    )
                    NovaSettingsDivider()
                    NovaActionSettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = if (!diag.postNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            "Grant Notification Permission"
                        } else {
                            "Open Android Notification Settings"
                        },
                        subtitle = if (diag.notificationPermissionGranted) {
                            "Notification permission enabled for broadcast alerts"
                        } else {
                            "Required on Android 13+ before NOVA BROADCAST marks this device active"
                        },
                        trailingText = if (diag.notificationPermissionGranted) "Manage" else "Allow",
                        onClick = {
                            if (!diag.postNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.openAppNotificationSettings(context)
                            }
                        }
                    )
                }
            }
        }

        // 3. FCM Token Retrieval
        item(key = "fcm_diag_token") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "FCM Token Retrieval",
                    icon = Icons.Rounded.Key,
                    trailingBadge = diag.fcmTokenStatus
                )
                NovaSettingsCard {
                    NovaInfoRow("Token Status", diag.fcmTokenStatus)
                    NovaSettingsDivider()
                    NovaInfoRow("Token Preview", diag.fcmTokenPreview)
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Update Topic",
                        value = diag.topicSubscribed.ifBlank { "Not Subscribed" }
                    )
                    if (diag.fcmTokenError.isNotBlank()) {
                        NovaSettingsDivider()
                        SelectionContainer {
                            Text(
                                text = "Token Error: ${diag.fcmTokenError}",
                                style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                                color = NovaRedPrimary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    if (diag.fcmTokenRaw.isNotBlank()) {
                        NovaSettingsDivider()
                        NovaActionSettingRow(
                            icon = Icons.Rounded.ContentCopy,
                            title = if (tokenCopied) "FCM Token Copied" else "Copy Full FCM Token",
                            subtitle = "Copies real FirebaseMessaging.getInstance().token to clipboard",
                            trailingText = if (tokenCopied) "Copied" else "Copy",
                            onClick = {
                                copyPlainText(context, "NOVA FCM Token", diag.fcmTokenRaw)
                                tokenCopied = true
                            }
                        )
                    }
                }
            }
        }

        // 4. Backend Registration & Response
        item(key = "fcm_diag_backend") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Backend Registration & Response",
                    icon = Icons.Rounded.CloudDone,
                    trailingBadge = if (diag.lastBackendHttpCode > 0) "HTTP ${diag.lastBackendHttpCode}" else null
                )
                NovaSettingsCard {
                    NovaInfoRow(
                        label = "Device ID",
                        value = diag.deviceId.takeIf { it.isNotBlank() }?.take(14)?.plus("...") ?: "Pending"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "API Endpoint",
                        value = diag.backendEndpointUrl.removePrefix("https://").ifBlank { "Not Configured" }
                    )
                    NovaSettingsDivider()
                    NovaInfoRow("Registration Status", diag.registrationStatus)
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Last HTTP Code",
                        value = if (diag.lastBackendHttpCode > 0) "HTTP ${diag.lastBackendHttpCode}" else "None"
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Last Attempt",
                        value = formatDiagnosticTimestamp(diag.lastAttemptMs, "Never")
                    )
                    NovaSettingsDivider()
                    NovaInfoRow(
                        label = "Last Successful Sync",
                        value = formatDiagnosticTimestamp(diag.lastSuccessMs, "Never confirmed")
                    )
                    NovaSettingsDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last Backend Response",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(novaColors.recessedSurface.copy(alpha = 0.90f))
                            .border(0.65.dp, novaColors.glassBorderBottom, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = diag.lastBackendResponse.ifBlank { "No backend registration response recorded yet." },
                                style = NovaMonoTelemetry.copy(fontSize = 11.sp, lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // 5. Diagnostic Logs
        item(key = "fcm_diag_logs") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaSectionHeader(
                    title = "Session Diagnostic Logs",
                    icon = Icons.Rounded.Terminal,
                    trailingBadge = "${diag.recentLogs.size} events"
                )
                NovaSettingsCard {
                    NovaActionSettingRow(
                        icon = Icons.Rounded.ContentCopy,
                        title = if (reportCopied) "Diagnostic Report Copied" else "Copy Full Diagnostic Report",
                        subtitle = "Includes Firebase state, permission, endpoint, response & logs",
                        trailingText = if (reportCopied) "Copied" else "Copy",
                        onClick = {
                            copyPlainText(context, "NOVA FCM Diagnostics", buildDiagnosticReport(diag))
                            reportCopied = true
                        }
                    )
                    NovaSettingsDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A0A0A))
                            .border(0.65.dp, novaColors.glassBorderBottom, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = if (diag.recentLogs.isEmpty()) {
                                    "No session logs captured yet. Tap 'Register / Re-Sync Device Now' above to run live diagnostics."
                                } else {
                                    diag.recentLogs.asReversed().joinToString("\n")
                                },
                                style = NovaMonoTelemetry.copy(fontSize = 10.5.sp, lineHeight = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

internal fun formatDiagnosticTimestamp(timestampMs: Long, fallback: String): String {
    if (timestampMs <= 0L) return fallback
    return SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
}

private fun copyPlainText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun buildDiagnosticReport(diag: FcmDiagnosticsSnapshot): String = buildString {
    appendLine("NOVA MUSIC FCM & BROADCAST DIAGNOSTICS")
    appendLine("App: ${BuildConfig.APPLICATION_ID} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Firebase Initialized: ${diag.firebaseInitialized} (${diag.firebaseStatusDetail})")
    appendLine("POST_NOTIFICATIONS Granted: ${diag.postNotificationsGranted}")
    appendLine("System Notifications Enabled: ${diag.systemNotificationsEnabled}")
    appendLine("FCM Token Status: ${diag.fcmTokenStatus} (${diag.fcmTokenPreview})")
    if (diag.fcmTokenError.isNotBlank()) {
        appendLine("FCM Token Error: ${diag.fcmTokenError}")
    }
    appendLine("Topic Subscribed: ${diag.topicSubscribed.ifBlank { "none" }}")
    appendLine("Backend Endpoint: ${diag.backendEndpointUrl.ifBlank { "none" }}")
    appendLine("Device ID: ${diag.deviceId.ifBlank { "none" }}")
    appendLine("Registration Status: ${diag.registrationStatus} (confirmed=${diag.isRegisteredConfirmed})")
    appendLine("Last HTTP Status: ${diag.lastBackendHttpCode}")
    appendLine("Last Attempt: ${formatDiagnosticTimestamp(diag.lastAttemptMs, "Never")}")
    appendLine("Last Successful Sync: ${formatDiagnosticTimestamp(diag.lastSuccessMs, "Never")}")
    appendLine("Last Backend Response: ${diag.lastBackendResponse.ifBlank { "none" }}")
    appendLine("--- Logs ---")
    if (diag.recentLogs.isEmpty()) {
        appendLine("(none)")
    } else {
        diag.recentLogs.forEach { appendLine(it) }
    }
}
