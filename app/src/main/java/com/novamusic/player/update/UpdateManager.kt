package com.novamusic.player.update

import android.content.Context
import android.content.Intent
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.preferences.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

interface UpdateClock {
    fun nowMs(): Long = System.currentTimeMillis()
}

interface UpdatePreferencesStore {
    suspend fun getLastUpdateCheckMs(): Long
    suspend fun setLastUpdateCheckMs(timestampMs: Long)
    suspend fun getDismissedUpdateVersionCode(): Int
    suspend fun setDismissedUpdateVersionCode(versionCode: Int)
}

class RepositoryUpdatePreferencesStore(
    private val preferences: PreferencesRepository
) : UpdatePreferencesStore {
    override suspend fun getLastUpdateCheckMs(): Long =
        preferences.appSettings.first().lastUpdateCheckMs

    override suspend fun setLastUpdateCheckMs(timestampMs: Long) {
        preferences.setLastUpdateCheckMs(timestampMs)
    }

    override suspend fun getDismissedUpdateVersionCode(): Int =
        preferences.appSettings.first().dismissedUpdateVersionCode

    override suspend fun setDismissedUpdateVersionCode(versionCode: Int) {
        preferences.setDismissedUpdateVersionCode(versionCode)
    }
}

class UpdateManager(
    private val preferencesStore: UpdatePreferencesStore,
    private val updatesDir: File,
    private val networkClient: UpdateNetworkClient,
    private val apkVerifier: ApkVerifier,
    private val apkInstaller: ApkInstaller,
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    private val installedVersionName: String = BuildConfig.VERSION_NAME,
    private val updateJsonUrl: String = UpdateConfig.updateJsonUrl,
    private val clock: UpdateClock = object : UpdateClock {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    constructor(
        context: Context,
        preferences: PreferencesRepository
    ) : this(
        preferencesStore = RepositoryUpdatePreferencesStore(preferences),
        updatesDir = File(context.applicationContext.filesDir, UpdateConfig.UPDATES_SUBDIR),
        networkClient = AndroidUpdateNetworkClient(context.applicationContext),
        apkVerifier = AndroidApkVerifier(context.applicationContext),
        apkInstaller = AndroidApkInstaller(context.applicationContext)
    )

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var activeCheckJob: Job? = null
    private var activeDownloadJob: Job? = null
    private var periodicJob: Job? = null

    /**
     * Starts a 24-hour periodic scheduler while the process is active and performs an initial
     * check if 24 hours have elapsed since the previous check.
     */
    fun startPeriodicChecks() {
        if (periodicJob?.isActive == true) return
        periodicJob = scope.launch {
            checkIfIntervalElapsed()
            while (isActive) {
                delay(UpdateConfig.CHECK_INTERVAL_MS)
                checkIfIntervalElapsed()
            }
        }
    }

    suspend fun checkIfIntervalElapsed() {
        val currentState = _uiState.value
        if (currentState is UpdateUiState.Downloading ||
            currentState is UpdateUiState.Verifying ||
            currentState is UpdateUiState.ReadyToInstall
        ) {
            return
        }
        val lastCheckMs = preferencesStore.getLastUpdateCheckMs()
        val now = clock.nowMs()
        if (UpdateEvaluator.shouldAutoCheck(now, lastCheckMs)) {
            checkForUpdatesInternal(manual = false)
        }
    }

    fun checkForUpdatesManual() {
        val current = _uiState.value
        if (current is UpdateUiState.Downloading || current is UpdateUiState.Verifying) {
            return
        }
        activeCheckJob?.cancel()
        activeCheckJob = scope.launch {
            checkForUpdatesInternal(manual = true)
        }
    }

    suspend fun checkForUpdatesNow(manual: Boolean) {
        checkForUpdatesInternal(manual = manual)
    }

    private suspend fun checkForUpdatesInternal(manual: Boolean) {
        if (!networkClient.isNetworkAvailable()) {
            if (manual) {
                _uiState.value = UpdateUiState.Error(
                    message = "No internet connection. Connect to Wi-Fi or mobile data and try again.",
                    canRetryDownload = false,
                    dialogVisible = true
                )
            }
            return
        }

        _uiState.value = UpdateUiState.Checking(manual = manual)

        val rawJson = try {
            networkClient.fetchManifestJson(updateJsonUrl)
        } catch (ce: CancellationException) {
            throw ce
        } catch (se: SecurityException) {
            if (manual) {
                _uiState.value = UpdateUiState.Error(
                    message = se.message ?: "Insecure update manifest URL.",
                    canRetryDownload = false,
                    dialogVisible = true
                )
            } else {
                _uiState.value = UpdateUiState.Idle
            }
            return
        } catch (io: IOException) {
            if (manual) {
                _uiState.value = UpdateUiState.Error(
                    message = io.message ?: "Unable to reach the NOVA MUSIC update server.",
                    canRetryDownload = false,
                    dialogVisible = true
                )
            } else {
                _uiState.value = UpdateUiState.Idle
            }
            return
        } catch (t: Throwable) {
            if (manual) {
                _uiState.value = UpdateUiState.Error(
                    message = "Update check failed: ${t.message ?: "Unknown network error"}",
                    canRetryDownload = false,
                    dialogVisible = true
                )
            } else {
                _uiState.value = UpdateUiState.Idle
            }
            return
        }

        val parsedResult = UpdateEvaluator.parseUpdateConfigJson(rawJson)
        val config = parsedResult.getOrElse { err ->
            if (manual) {
                _uiState.value = UpdateUiState.Error(
                    message = "Invalid update manifest format: ${err.message ?: "malformed JSON"}.",
                    canRetryDownload = false,
                    dialogVisible = true
                )
            } else {
                _uiState.value = UpdateUiState.Idle
            }
            return
        }

        val now = clock.nowMs()
        preferencesStore.setLastUpdateCheckMs(now)

        when (val evaluation = UpdateEvaluator.evaluateUpdate(installedVersionCode, config)) {
            is UpdateEvaluationResult.NoUpdate -> {
                _uiState.value = UpdateUiState.UpToDate(
                    installedVersionName = installedVersionName,
                    installedVersionCode = installedVersionCode,
                    checkedAtMs = now,
                    showConfirmationDialog = manual
                )
            }

            is UpdateEvaluationResult.InvalidConfiguration -> {
                if (manual) {
                    _uiState.value = UpdateUiState.Error(
                        message = evaluation.reason,
                        canRetryDownload = false,
                        dialogVisible = true
                    )
                } else {
                    _uiState.value = UpdateUiState.Idle
                }
            }

            is UpdateEvaluationResult.UpdateAvailable -> {
                val dismissedVersion = preferencesStore.getDismissedUpdateVersionCode()
                val wasDismissed = !evaluation.isMandatory &&
                    dismissedVersion == evaluation.config.versionCode
                val showDialog = manual || evaluation.isMandatory || !wasDismissed
                _uiState.value = UpdateUiState.Available(
                    config = evaluation.config,
                    isMandatory = evaluation.isMandatory,
                    dialogVisible = showDialog
                )
            }
        }
    }

    fun startDownload() {
        val (config, isMandatory) = when (val state = _uiState.value) {
            is UpdateUiState.Available -> state.config to state.isMandatory
            is UpdateUiState.Error -> {
                val cfg = state.config ?: return
                cfg to state.isMandatory
            }
            is UpdateUiState.ReadyToInstall -> {
                triggerInstall(File(state.apkFilePath), state.config, state.isMandatory)
                return
            }
            else -> return
        }

        if (!networkClient.isNetworkAvailable()) {
            _uiState.value = UpdateUiState.Error(
                message = "No internet connection. Unable to download NOVA MUSIC v${config.latestVersion}.",
                canRetryDownload = true,
                config = config,
                isMandatory = isMandatory,
                dialogVisible = true
            )
            return
        }

        activeDownloadJob?.cancel()
        activeDownloadJob = scope.launch {
            executeDownloadAndVerify(config, isMandatory)
        }
    }

    suspend fun downloadAndVerifyNow(config: RemoteUpdateConfig, isMandatory: Boolean) {
        executeDownloadAndVerify(config, isMandatory)
    }

    private suspend fun executeDownloadAndVerify(config: RemoteUpdateConfig, isMandatory: Boolean) {
        val destinationFile = File(updatesDir, "NOVA-MUSIC-v${config.latestVersion}.apk")

        _uiState.value = UpdateUiState.Downloading(
            config = config,
            isMandatory = isMandatory,
            bytesDownloaded = 0L,
            totalBytes = -1L,
            progressFraction = null
        )

        val downloadedFile = try {
            networkClient.downloadApk(config.apkUrl, destinationFile) { downloaded, total ->
                val fraction = if (total > 0L) {
                    (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
                _uiState.value = UpdateUiState.Downloading(
                    config = config,
                    isMandatory = isMandatory,
                    bytesDownloaded = downloaded,
                    totalBytes = total,
                    progressFraction = fraction
                )
            }
        } catch (ce: CancellationException) {
            _uiState.value = UpdateUiState.Available(
                config = config,
                isMandatory = isMandatory,
                dialogVisible = isMandatory
            )
            throw ce
        } catch (t: Throwable) {
            _uiState.value = UpdateUiState.Error(
                message = t.message ?: "Download interrupted. Please check your connection and retry.",
                canRetryDownload = true,
                config = config,
                isMandatory = isMandatory,
                dialogVisible = true
            )
            return
        }

        _uiState.value = UpdateUiState.Verifying(config = config, isMandatory = isMandatory)

        when (val verification = apkVerifier.verifyDownloadedApk(downloadedFile, config, installedVersionCode)) {
            is ApkVerificationResult.Invalid -> {
                _uiState.value = UpdateUiState.Error(
                    message = verification.reason,
                    canRetryDownload = false,
                    config = config,
                    isMandatory = isMandatory,
                    dialogVisible = true
                )
            }
            is ApkVerificationResult.Valid -> {
                triggerInstall(downloadedFile, config, isMandatory)
            }
        }
    }

    private fun triggerInstall(apkFile: File, config: RemoteUpdateConfig, isMandatory: Boolean) {
        if (!apkInstaller.canRequestPackageInstalls()) {
            _uiState.value = UpdateUiState.ReadyToInstall(
                config = config,
                isMandatory = isMandatory,
                apkFilePath = apkFile.absolutePath,
                requiresInstallPermission = true
            )
            return
        }

        val launched = apkInstaller.launchSystemInstaller(apkFile)
        if (launched) {
            _uiState.value = UpdateUiState.ReadyToInstall(
                config = config,
                isMandatory = isMandatory,
                apkFilePath = apkFile.absolutePath,
                requiresInstallPermission = false
            )
        } else {
            _uiState.value = UpdateUiState.Error(
                message = "Unable to open Android's package installer for this APK.",
                canRetryDownload = true,
                config = config,
                isMandatory = isMandatory,
                dialogVisible = true
            )
        }
    }

    fun buildInstallPermissionIntent(): Intent {
        return apkInstaller.createUnknownSourcesPermissionIntent()
    }

    fun onInstallPermissionReturned() {
        val state = _uiState.value as? UpdateUiState.ReadyToInstall ?: return
        if (apkInstaller.canRequestPackageInstalls()) {
            triggerInstall(File(state.apkFilePath), state.config, state.isMandatory)
        }
    }

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
    }

    fun dismissDialog() {
        when (val state = _uiState.value) {
            is UpdateUiState.Available -> {
                if (!state.isMandatory) {
                    scope.launch {
                        preferencesStore.setDismissedUpdateVersionCode(state.config.versionCode)
                    }
                    _uiState.value = state.copy(dialogVisible = false)
                }
            }
            is UpdateUiState.UpToDate -> {
                _uiState.value = state.copy(showConfirmationDialog = false)
            }
            is UpdateUiState.ReadyToInstall -> {
                if (!state.isMandatory) {
                    _uiState.value = UpdateUiState.Available(
                        config = state.config,
                        isMandatory = false,
                        dialogVisible = false
                    )
                }
            }
            is UpdateUiState.Error -> {
                if (!state.isMandatory) {
                    _uiState.value = if (state.config != null) {
                        UpdateUiState.Available(
                            config = state.config,
                            isMandatory = false,
                            dialogVisible = false
                        )
                    } else {
                        UpdateUiState.Idle
                    }
                }
            }
            else -> Unit
        }
    }

    fun reopenAvailableDialog() {
        when (val state = _uiState.value) {
            is UpdateUiState.Available -> {
                _uiState.value = state.copy(dialogVisible = true)
            }
            is UpdateUiState.Error -> {
                _uiState.value = state.copy(dialogVisible = true)
            }
            else -> checkForUpdatesManual()
        }
    }
}
