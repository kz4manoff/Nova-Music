package com.novamusic.player.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

interface ApkInstaller {
    fun canRequestPackageInstalls(): Boolean
    fun createUnknownSourcesPermissionIntent(): Intent
    fun launchSystemInstaller(apkFile: File): Boolean
}

class AndroidApkInstaller(
    private val context: Context
) : ApkInstaller {

    override fun canRequestPackageInstalls(): Boolean {
        return runCatching {
            context.packageManager.canRequestPackageInstalls()
        }.getOrDefault(false)
    }

    override fun createUnknownSourcesPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    override fun launchSystemInstaller(apkFile: File): Boolean {
        if (!apkFile.exists() || !apkFile.isFile) return false
        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        }.getOrDefault(false)
    }
}
