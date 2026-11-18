package com.novamusic.player

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.novamusic.player.data.model.RescanInterval
import com.novamusic.player.data.preferences.AppSettings
import com.novamusic.player.navigation.NovaMainAppShell
import com.novamusic.player.permissions.PermissionHelper
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.NovaUpdateDialog
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.ui.theme.NovaMusicTheme
import com.novamusic.player.ui.theme.rememberArtworkPalette
import com.novamusic.player.update.PushUpdatePayload
import com.novamusic.player.update.UpdateWorkScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_PLAYER = "extra_open_player"
        const val EXTRA_OPEN_UPDATES = "extra_open_updates"
    }

    private var hasAudioPermissionState by mutableStateOf(false)
    private var openNowPlayingTrigger by mutableIntStateOf(0)
    private var openUpdatesTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as NovaMusicApplication).container
        hasAudioPermissionState = PermissionHelper.hasAudioPermission(this)
        handleIncomingUpdateAndPlayerIntent(intent, container)

        container.audioPlayerManager.connect()
        container.updateManager.startPeriodicChecks()
        container.fcmUpdatePushManager.ensureRegisteredAsync()
        lifecycleScope.launch {
            container.updateManager.clearStaleNotificationIfUpdated()
        }

        if (hasAudioPermissionState) {
            initializeLibraryAndRestore(container)
        }
        handleIncomingAudioIntent(intent, container)

        setContent {
            val settings by container.preferencesRepository.appSettings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val playerState by container.audioPlayerManager.playerState
                .collectAsStateWithLifecycle()
            val updateState by container.updateManager.uiState
                .collectAsStateWithLifecycle()

            LaunchedEffect(settings.autoCheckUpdates) {
                UpdateWorkScheduler.syncSchedule(applicationContext, settings.autoCheckUpdates)
            }

            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val baseAccent = Color(settings.accentColor.hex)
            val dynamicPalette = rememberArtworkPalette(
                artworkUri = if (settings.dynamicColors) playerState.currentSong?.artworkUri else null,
                fallbackAccent = baseAccent
            )
            val resolvedDynamicAccent = if (settings.dynamicColors && playerState.currentSong != null) {
                dynamicPalette.primary
            } else {
                null
            }

            NovaMusicTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor,
                dynamicAccentColor = resolvedDynamicAccent
            ) {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    val granted = PermissionHelper.hasAudioPermission(context)
                    hasAudioPermissionState = granted
                    if (granted) {
                        initializeLibraryAndRestore(container)
                    }
                    if (PermissionHelper.hasNotificationPermission(context)) {
                        container.fcmUpdatePushManager.ensureRegisteredAsync()
                    }
                    container.audioOutputManager.refreshPermissionsAndDevices()
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted || PermissionHelper.hasNotificationPermission(context)) {
                        container.fcmUpdatePushManager.ensureRegisteredAsync()
                    }
                }

                LaunchedEffect(hasAudioPermissionState, settings.hasPromptedNotificationPermission) {
                    if (hasAudioPermissionState &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !PermissionHelper.hasNotificationPermission(context) &&
                        !settings.hasPromptedNotificationPermission
                    ) {
                        container.preferencesRepository.setHasPromptedNotificationPermission(true)
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val installPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    container.updateManager.onInstallPermissionReturned()
                }

                if (hasAudioPermissionState) {
                    NovaMainAppShell(
                        container = container,
                        openNowPlayingTrigger = openNowPlayingTrigger,
                        openUpdatesTrigger = openUpdatesTrigger
                    )
                } else {
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(PermissionHelper.initialPermissionsToRequest())
                    }
                    PermissionGateScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(PermissionHelper.initialPermissionsToRequest())
                        },
                        onOpenSystemSettings = {
                            val settingsIntent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                            startActivity(settingsIntent)
                        }
                    )
                }

                NovaUpdateDialog(
                    state = updateState,
                    onUpdateNow = { container.updateManager.startDownload() },
                    onCancelDownload = { container.updateManager.cancelDownload() },
                    onRequestInstallPermission = {
                        installPermissionLauncher.launch(
                            container.updateManager.buildInstallPermissionIntent()
                        )
                    },
                    onRetryCheck = { container.updateManager.checkForUpdatesManual() },
                    onDismiss = { container.updateManager.dismissDialog() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as NovaMusicApplication).container
        container.audioOutputManager.refreshPermissionsAndDevices()
        container.updateManager.onInstallPermissionReturned()
        lifecycleScope.launch {
            container.updateManager.checkIfIntervalElapsed()
        }
        val grantedNow = PermissionHelper.hasAudioPermission(this)
        if (grantedNow && !hasAudioPermissionState) {
            hasAudioPermissionState = true
            initializeLibraryAndRestore(container)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val container = (application as NovaMusicApplication).container
        handleIncomingUpdateAndPlayerIntent(intent, container)
        handleIncomingAudioIntent(intent, container)
    }

    private fun handleIncomingUpdateAndPlayerIntent(intent: Intent?, container: AppContainer) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            openNowPlayingTrigger++
            intent.removeExtra(EXTRA_OPEN_PLAYER)
        }
        val updateNavData = PushUpdatePayload.extractUpdateIntentData(
            intent = intent,
            openUpdatesExtraKey = EXTRA_OPEN_UPDATES
        )
        if (updateNavData.shouldOpenUpdates) {
            openUpdatesTrigger++
            container.updateManager.onUpdateNotificationOpened(updateNavData.remoteVersionCode)
            PushUpdatePayload.clearConsumedUpdateExtras(intent, EXTRA_OPEN_UPDATES)
        }
    }

    private fun initializeLibraryAndRestore(container: AppContainer) {
        lifecycleScope.launch {
            val settings = container.preferencesRepository.appSettings.first()
            val now = System.currentTimeMillis()
            val shouldScan = when {
                container.musicRepository.allSongs.value.isEmpty() -> true
                settings.rescanInterval == RescanInterval.ON_START -> true
                settings.rescanInterval == RescanInterval.DAILY ->
                    (now - settings.lastScanTimestampMs) >= 86_400_000L
                settings.rescanInterval == RescanInterval.MANUAL -> false
                else -> true
            }
            if (shouldScan) {
                container.musicRepository.refreshLibrary()
            }
            val songs = container.musicRepository.allSongs.value
            container.audioPlayerManager.restoreLastSessionIfNeeded(songs)
        }
    }

    private fun handleIncomingAudioIntent(intent: Intent?, container: AppContainer) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val audioUri = intent.data ?: return
            lifecycleScope.launch {
                val song = container.musicRepository.resolveSafUriToSong(audioUri)
                if (song != null) {
                    container.audioPlayerManager.playSong(song, listOf(song))
                    openNowPlayingTrigger++
                }
            }
        }
    }
}

@Composable
private fun PermissionGateScreen(
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackdrop(
            artworkUri = null,
            isPlaying = false,
            intensity = 1.05f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(32.dp),
                elevation = 24.dp,
                contentPadding = PaddingValues(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .shadow(18.dp, CircleShape, ambientColor = accent, spotColor = accent)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.28f),
                                        accent.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        accent.copy(alpha = 0.28f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nova_logo),
                            contentDescription = "NOVA MUSIC",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NOVA MUSIC",
                        style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.permission_audio_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(id = R.string.permission_audio_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    LiquidGlassButton(
                        onClick = onRequestPermission,
                        highlighted = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.grant_permission),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LiquidGlassButton(
                        onClick = onOpenSystemSettings,
                        highlighted = false,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Open App Settings",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
