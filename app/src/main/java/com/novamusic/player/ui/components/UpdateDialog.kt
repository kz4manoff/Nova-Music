package com.novamusic.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novamusic.player.BuildConfig
import com.novamusic.player.R
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.update.UpdateUiState
import com.novamusic.player.utils.FormatUtils
import kotlin.math.roundToInt

/**
 * Resolves whether the Liquid Glass update dialog should currently be presented to the user.
 */
fun UpdateUiState.shouldShowDialog(): Boolean {
    return when (this) {
        is UpdateUiState.Available -> dialogVisible
        is UpdateUiState.Downloading -> true
        is UpdateUiState.Verifying -> true
        is UpdateUiState.ReadyToInstall -> true
        is UpdateUiState.Error -> dialogVisible
        is UpdateUiState.UpToDate -> showConfirmationDialog
        UpdateUiState.Idle,
        is UpdateUiState.Checking -> false
    }
}

@Composable
fun NovaUpdateDialog(
    state: UpdateUiState,
    onUpdateNow: () -> Unit,
    onCancelDownload: () -> Unit,
    onRequestInstallPermission: () -> Unit,
    onRetryCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.shouldShowDialog()) return

    val isMandatory = when (state) {
        is UpdateUiState.Available -> state.isMandatory
        is UpdateUiState.Downloading -> state.isMandatory
        is UpdateUiState.Verifying -> state.isMandatory
        is UpdateUiState.ReadyToInstall -> state.isMandatory
        is UpdateUiState.Error -> state.isMandatory
        else -> false
    }

    val canDismiss = !isMandatory &&
        state !is UpdateUiState.Downloading &&
        state !is UpdateUiState.Verifying

    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false
        )
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(30.dp),
            accentTint = accent,
            highlighted = isMandatory,
            elevation = 24.dp,
            glassOpacity = 1.08f,
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // NOVA MUSIC Liquid Glass Branding Header
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .shadow(16.dp, CircleShape, ambientColor = accent, spotColor = accent)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.28f),
                                    accent.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.62f),
                                    accent.copy(alpha = 0.30f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nova_logo),
                        contentDescription = "NOVA MUSIC",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NOVA MUSIC",
                    style = NovaMonoTelemetry.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    ),
                    color = accent
                )

                Spacer(modifier = Modifier.height(6.dp))

                when (state) {
                    is UpdateUiState.Available -> {
                        UpdateBadgePill(
                            label = if (state.isMandatory) "REQUIRED UPDATE" else "UPDATE AVAILABLE",
                            accent = if (state.isMandatory) MaterialTheme.colorScheme.error else accent
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Version ${state.config.latestVersion} Ready",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Installed v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  →  v${state.config.latestVersion} (${state.config.versionCode})",
                            style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ReleaseNotesGlassBox(
                            releaseNotes = state.config.releaseNotes,
                            recessedBg = novaColors.recessedSurface,
                            borderColor = novaColors.glassBorderBottom
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SecurityVerificationNote()

                        Spacer(modifier = Modifier.height(18.dp))

                        LiquidGlassButton(
                            onClick = {
                                performHaptic()
                                onUpdateNow()
                            },
                            highlighted = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Update Now",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (!state.isMandatory) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LiquidGlassButton(
                                onClick = {
                                    performHaptic()
                                    onDismiss()
                                },
                                highlighted = false,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Later",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is UpdateUiState.Downloading -> {
                        UpdateBadgePill(
                            label = "DOWNLOADING UPDATE",
                            accent = accent
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "NOVA MUSIC v${state.config.latestVersion}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val progressText = buildString {
                            append(FormatUtils.formatFileSize(state.bytesDownloaded))
                            if (state.totalBytes > 0L) {
                                append(" / ")
                                append(FormatUtils.formatFileSize(state.totalBytes))
                            }
                            state.progressFraction?.let { frac ->
                                append("  •  ")
                                append("${(frac * 100f).roundToInt()}%")
                            }
                        }
                        Text(
                            text = progressText,
                            style = NovaMonoTelemetry.copy(fontSize = 12.sp),
                            color = accent,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val animatedProgress by animateFloatAsState(
                            targetValue = state.progressFraction ?: 0f,
                            animationSpec = tween(durationMillis = 180),
                            label = "nova_update_progress"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(novaColors.recessedSurface.copy(alpha = 0.85f))
                                .border(0.7.dp, novaColors.glassBorderBottom, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            if (state.progressFraction != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(accent.copy(alpha = 0.85f), accent)
                                                )
                                            )
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    color = accent,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Downloading securely over HTTPS from GitHub Releases...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (!state.isMandatory) {
                            Spacer(modifier = Modifier.height(18.dp))
                            LiquidGlassButton(
                                onClick = {
                                    performHaptic()
                                    onCancelDownload()
                                },
                                highlighted = false,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Cancel Download",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is UpdateUiState.Verifying -> {
                        UpdateBadgePill(
                            label = "VERIFYING PACKAGE",
                            accent = accent
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Verifying APK Integrity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Checking SHA-256 checksum and signing certificate compatibility with NOVA MUSIC...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        UpdateBadgePill(
                            label = if (state.requiresInstallPermission) "PERMISSION NEEDED" else "VERIFIED & READY",
                            accent = accent
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.requiresInstallPermission) {
                                "Allow App Installs"
                            } else {
                                "Ready to Install v${state.config.latestVersion}"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.requiresInstallPermission) {
                                "Android requires your permission to install NOVA MUSIC updates directly from this app. Your playlists, equalizer presets, and library settings stay intact."
                            } else {
                                "SHA-256 integrity and signing certificate matched this NOVA MUSIC installation. Confirm the Android system prompt to update in place while preserving all app data."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LiquidGlassButton(
                            onClick = {
                                performHaptic()
                                if (state.requiresInstallPermission) {
                                    onRequestInstallPermission()
                                } else {
                                    onUpdateNow()
                                }
                            },
                            highlighted = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (state.requiresInstallPermission) "Open Install Settings" else "Install Update",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        if (!state.isMandatory) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LiquidGlassButton(
                                onClick = {
                                    performHaptic()
                                    onDismiss()
                                },
                                highlighted = false,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Later",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is UpdateUiState.Error -> {
                        val errorAccent = MaterialTheme.colorScheme.error
                        UpdateBadgePill(
                            label = if (state.isMandatory) "UPDATE REQUIRED" else "UPDATE ERROR",
                            accent = errorAccent
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = errorAccent,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Update Could Not Complete",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(errorAccent.copy(alpha = 0.12f))
                                .border(0.7.dp, errorAccent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        if (state.canRetryDownload && state.config != null) {
                            LiquidGlassButton(
                                onClick = {
                                    performHaptic()
                                    onUpdateNow()
                                },
                                highlighted = true,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Retry Download",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            if (!state.isMandatory) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        } else if (state.isMandatory) {
                            LiquidGlassButton(
                                onClick = {
                                    performHaptic()
                                    onRetryCheck()
                                },
                                highlighted = true,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Retry Check",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (!state.isMandatory) {
                            LiquidGlassButton(
                                onClick = {
                                    performHaptic()
                                    onDismiss()
                                },
                                highlighted = false,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is UpdateUiState.UpToDate -> {
                        UpdateBadgePill(
                            label = "UP TO DATE",
                            accent = accent
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You're Up to Date",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "NOVA MUSIC v${state.installedVersionName} (Build ${state.installedVersionCode}) is the latest official release.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LiquidGlassButton(
                            onClick = {
                                performHaptic()
                                onDismiss()
                            },
                            highlighted = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    UpdateUiState.Idle,
                    is UpdateUiState.Checking -> Unit
                }
            }
        }
    }
}

@Composable
private fun UpdateBadgePill(
    label: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(0.6.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = NovaMonoTelemetry.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}

@Composable
private fun ReleaseNotesGlassBox(
    releaseNotes: String,
    recessedBg: Color,
    borderColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(recessedBg.copy(alpha = 0.82f))
            .border(0.65.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "RELEASE NOTES",
            style = NovaMonoTelemetry.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = releaseNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SecurityVerificationNote() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "SHA-256 & signing key verified • App data preserved",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
