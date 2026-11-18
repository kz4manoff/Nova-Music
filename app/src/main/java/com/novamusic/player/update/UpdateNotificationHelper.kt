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
import androidx.core.content.ContextCompat
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

        ensureNotificationChannel(appContext)

        val openUpdatesIntent = Intent(appContext, MainActivity::class.java).apply {
            action = PushUpdatePayload.ACTION_OPEN_UPDATE_DETAILS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_UPDATES, true)
            putExtra(PushUpdatePayload.EXTRA_REMOTE_VERSION_CODE, config.versionCode)
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_OPEN_UPDATES,
            openUpdatesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val updateNowIntent = Intent(appContext, MainActivity::class.java).apply {
            action = PushUpdatePayload.ACTION_OPEN_UPDATE_DETAILS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_UPDATES, true)
            putExtra(PushUpdatePayload.EXTRA_REMOTE_VERSION_CODE, config.versionCode)
        }
        val updateNowPendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_UPDATE_NOW,
            updateNowIntent,
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
        val shortDescription = PushUpdatePayload.sanitizeShortDescription(config.releaseNotes)
        val collapsedText = "$versionLabel — $shortDescription"
        val expandedText = "$versionLabel is ready to install.\n$shortDescription"

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setColor(ContextCompat.getColor(appContext, R.color.nova_red))
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(collapsedText)
            .setSubText("NOVA MUSIC • v${config.latestVersion}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(NOTIFICATION_TITLE)
                    .setSummaryText("NOVA MUSIC • $versionLabel")
                    .bigText(expandedText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(
                R.drawable.ic_nova_notification,
                ACTION_UPDATE_NOW_LABEL,
                updateNowPendingIntent
            )

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        return try {
            NotificationManagerCompat.from(appContext).notify(
                NOTIFICATION_TAG,
                NOTIFICATION_ID,
                builder.build()
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun cancelUpdateNotification() {
        runCatching {
            val mgr = NotificationManagerCompat.from(appContext)
            mgr.cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
            mgr.cancel(NOTIFICATION_ID)
        }
    }

    companion object {
        const val CHANNEL_ID = "nova_updates"
        const val CHANNEL_NAME = "NOVA Updates"
        const val CHANNEL_DESCRIPTION = "Alerts when a new NOVA MUSIC release is available to install."
        const val LEGACY_CHANNEL_ID = "nova_app_updates"
        const val NOTIFICATION_TITLE = "New Update Available!"
        const val ACTION_UPDATE_NOW_LABEL = "Update Now"
        const val NOTIFICATION_TAG = "nova_update_latest"
        const val NOTIFICATION_ID = 4096
        const val REQUEST_CODE_OPEN_UPDATES = 4097
        const val REQUEST_CODE_DISMISS_UPDATE = 4098
        const val REQUEST_CODE_UPDATE_NOW = 4099
        const val ACTION_DISMISS_UPDATE_NOTIFICATION = "com.novamusic.player.action.DISMISS_UPDATE_NOTIFICATION"
        const val EXTRA_DISMISSED_VERSION_CODE = "extra_dismissed_version_code"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.applicationContext.getSystemService(NotificationManager::class.java) ?: return
            runCatching {
                if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
                    manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
                }
            }
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) {
                if (existing.name?.toString() != CHANNEL_NAME) {
                    existing.name = CHANNEL_NAME
                    existing.description = CHANNEL_DESCRIPTION
                    manager.createNotificationChannel(existing)
                }
                return
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
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
