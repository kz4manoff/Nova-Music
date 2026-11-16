package com.novamusic.player.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

interface ApkVerifier {
    suspend fun verifyDownloadedApk(
        apkFile: File,
        expectedConfig: RemoteUpdateConfig,
        installedVersionCode: Int
    ): ApkVerificationResult
}

class AndroidApkVerifier(
    private val context: Context
) : ApkVerifier {

    override suspend fun verifyDownloadedApk(
        apkFile: File,
        expectedConfig: RemoteUpdateConfig,
        installedVersionCode: Int
    ): ApkVerificationResult = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() <= 0L) {
            return@withContext ApkVerificationResult.Invalid("Downloaded APK file is missing or empty.")
        }

        // 1. Validate ZIP/APK magic header
        val header = ByteArray(4)
        val headerRead = runCatching {
            FileInputStream(apkFile).use { it.read(header) }
        }.getOrDefault(-1)
        if (headerRead < 4 || !UpdateEvaluator.isZipMagicHeader(header)) {
            apkFile.delete()
            return@withContext ApkVerificationResult.Invalid("Downloaded file is not a valid Android APK archive.")
        }

        // 2. Verify SHA-256 integrity against update.json
        val actualSha256 = runCatching {
            FileInputStream(apkFile).use { UpdateEvaluator.computeSha256Hex(it) }
        }.getOrNull()
        if (actualSha256 == null || !actualSha256.equals(expectedConfig.sha256, ignoreCase = true)) {
            apkFile.delete()
            return@withContext ApkVerificationResult.Invalid(
                "APK integrity check failed (SHA-256 mismatch). The file has been discarded."
            )
        }

        // 3. Parse archive package metadata & signing certificates
        val pm = context.packageManager
        val archiveInfo = getPackageArchiveInfoCompat(pm, apkFile.absolutePath)
        if (archiveInfo == null) {
            apkFile.delete()
            return@withContext ApkVerificationResult.Invalid("Unable to parse downloaded APK package metadata.")
        }

        if (archiveInfo.packageName != context.packageName) {
            apkFile.delete()
            return@withContext ApkVerificationResult.Invalid(
                "Package ID mismatch (${archiveInfo.packageName} != ${context.packageName})."
            )
        }

        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersionCode <= installedVersionCode.toLong() ||
            archiveVersionCode < expectedConfig.versionCode.toLong()
        ) {
            apkFile.delete()
            return@withContext ApkVerificationResult.Invalid(
                "Downloaded APK version code ($archiveVersionCode) does not match expected update (${expectedConfig.versionCode})."
            )
        }

        // 4. Verify signing certificate matches currently installed application
        val installedCerts = extractCertificateSha256s(getInstalledPackageInfoCompat(pm, context.packageName))
        val archiveCerts = extractCertificateSha256s(archiveInfo)

        if (!UpdateEvaluator.signaturesMatch(installedCerts, archiveCerts)) {
            apkFile.delete()
            return@withContext ApkVerificationResult.Invalid(
                "Security check failed: APK signing certificate does not match this NOVA MUSIC installation."
            )
        }

        ApkVerificationResult.Valid(
            filePath = apkFile.absolutePath,
            sha256Hex = actualSha256,
            archiveVersionCode = archiveVersionCode
        )
    }

    @Suppress("DEPRECATION")
    private fun getPackageArchiveInfoCompat(pm: PackageManager, archivePath: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(archivePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getPackageArchiveInfo(archivePath, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfoCompat(pm: PackageManager, packageName: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(packageName, flags)
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun extractCertificateSha256s(packageInfo: PackageInfo?): Set<String> {
        if (packageInfo == null) return emptySet()
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            when {
                signingInfo == null -> emptyArray()
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures ?: emptyArray()
        }
        return signatures
            .map { sig -> UpdateEvaluator.computeSha256Hex(sig.toByteArray()) }
            .toSet()
    }
}
