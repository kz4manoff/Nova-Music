package com.novamusic.player.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.novamusic.player.MainActivity
import com.novamusic.player.NovaMusicApplication
import com.novamusic.player.R
import com.novamusic.player.permissions.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface UpdateNotifier {
    fun canPostNotifications(): Boolean = true
    fun showUpdateAvailableNotification(config: RemoteUpdateConfig): Boolean = false
    fun cancelUpdateNotification() {}
}

class AndroidUpdateNotifier(
    context: Context
) : UpdateNotifier {

    private val appContext = context.applicationContext

    override fun canPostNotifications(): Boolean {
        return PermissionHelper.hasNotificationPermission(appContext)
    }

    override fun showUpdateAvailableNotification(config: RemoteUpdateConfig): Boolean {
        if (!canPostNotifications()) {
            return false
        }

        ensureNotificationChannel()

        val openUpdatesIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_UPDATES, true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_OPEN_UPDATES,
            openUpdatesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(appContext, UpdateNotificationDismissReceiver::class.java).apply {
            action = ACTION_DISMISS_UPDATE_NOTIFICATION
            putExtra(EXTRA_DISMISSED_VERSION_CODE, config.versionCode)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_DISMISS_UPDATE,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = runCatching {
            BitmapFactory.decodeResource(appContext.resources, R.drawable.ic_nova_logo)
        }.getOrNull()

        val versionLabel = "Version ${config.latestVersion}"
        val summaryText = "A new version of NOVA MUSIC is ready to install."
        val expandedText = buildString {
            append(summaryText)
            append("\n")
            append(versionLabel)
            if (config.releaseNotes.isNotBlank()) {
                append(" — ")
                append(config.releaseNotes.trim())
            }
        }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setContentTitle("NOVA MUSIC Update Available")
            .setContentText("$summaryText ($versionLabel)")
            .setSubText(versionLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(
                0,
                "View Update",
                openPendingIntent
            )

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        return try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun cancelUpdateNotification() {
        runCatching {
            NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "nova_app_updates"
        const val CHANNEL_NAME = "App Updates"
        const val CHANNEL_DESCRIPTION = "Alerts when a new verified release of NOVA MUSIC is ready to install."
        const val NOTIFICATION_ID = 4096
        const val REQUEST_CODE_OPEN_UPDATES = 4097
        const val REQUEST_CODE_DISMISS_UPDATE = 4098
        const val ACTION_DISMISS_UPDATE_NOTIFICATION = "com.novamusic.player.action.DISMISS_UPDATE_NOTIFICATION"
        const val EXTRA_DISMISSED_VERSION_CODE = "extra_dismissed_version_code"
    }
}

class UpdateNotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != AndroidUpdateNotifier.ACTION_DISMISS_UPDATE_NOTIFICATION) {
            return
        }
        val dismissedVersionCode = intent.getIntExtra(
            AndroidUpdateNotifier.EXTRA_DISMISSED_VERSION_CODE,
            0
        )
        if (dismissedVersionCode <= 0) return

        val app = context.applicationContext as? NovaMusicApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.preferencesRepository.setDismissedUpdateNotificationVersionCode(dismissedVersionCode)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
