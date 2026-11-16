package com.novamusic.player.update

data class RemoteUpdateConfig(
    val latestVersion: String,
    val versionCode: Int,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String,
    val mandatoryUpdate: Boolean,
    val minimumSupportedVersion: Int
)

sealed interface UpdateEvaluationResult {
    data class NoUpdate(
        val installedVersionCode: Int,
        val remoteVersionCode: Int
    ) : UpdateEvaluationResult

    data class UpdateAvailable(
        val config: RemoteUpdateConfig,
        val isMandatory: Boolean
    ) : UpdateEvaluationResult

    data class InvalidConfiguration(
        val reason: String
    ) : UpdateEvaluationResult
}

sealed interface ApkVerificationResult {
    data class Valid(
        val filePath: String,
        val sha256Hex: String,
        val archiveVersionCode: Long
    ) : ApkVerificationResult

    data class Invalid(
        val reason: String
    ) : ApkVerificationResult
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    data class Checking(
        val manual: Boolean
    ) : UpdateUiState

    data class UpToDate(
        val installedVersionName: String,
        val installedVersionCode: Int,
        val checkedAtMs: Long,
        val showConfirmationDialog: Boolean
    ) : UpdateUiState

    data class Available(
        val config: RemoteUpdateConfig,
        val isMandatory: Boolean,
        val dialogVisible: Boolean
    ) : UpdateUiState

    data class Downloading(
        val config: RemoteUpdateConfig,
        val isMandatory: Boolean,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressFraction: Float?
    ) : UpdateUiState

    data class Verifying(
        val config: RemoteUpdateConfig,
        val isMandatory: Boolean
    ) : UpdateUiState

    data class ReadyToInstall(
        val config: RemoteUpdateConfig,
        val isMandatory: Boolean,
        val apkFilePath: String,
        val requiresInstallPermission: Boolean
    ) : UpdateUiState

    data class Error(
        val message: String,
        val canRetryDownload: Boolean,
        val config: RemoteUpdateConfig? = null,
        val isMandatory: Boolean = false,
        val dialogVisible: Boolean = true
    ) : UpdateUiState
}
