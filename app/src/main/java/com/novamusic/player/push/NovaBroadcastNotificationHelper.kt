package com.novamusic.player.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.novamusic.player.MainActivity
import com.novamusic.player.R
import com.novamusic.player.permissions.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class NovaBroadcastNotificationHelper(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun showBroadcastNotification(payload: BroadcastNotificationPayload): Boolean {
        if (!PermissionHelper.hasNotificationPermission(appContext)) {
            return false
        }

        ensureNotificationChannel(appContext)

        val notificationId = computeStableNotificationId(payload.dedupeKey ?: payload.title)
        val tapIntent = Intent(appContext, MainActivity::class.java).apply {
            action = BroadcastNotificationParser.ACTION_OPEN_BROADCAST
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!payload.deepLink.isNullOrBlank()) {
                putExtra(BroadcastNotificationParser.EXTRA_BROADCAST_DEEP_LINK, payload.deepLink)
            }
        }
        val tapPendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = runCatching {
            BitmapFactory.decodeResource(appContext.resources, R.drawable.ic_nova_logo)
        }.getOrNull()

        val optionalPictureBitmap = payload.imageUrl?.let { fetchImageBitmapSafely(it) }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setColor(ContextCompat.getColor(appContext, R.color.nova_red))
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setSubText("NOVA MUSIC")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tapPendingIntent)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        if (optionalPictureBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(optionalPictureBitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setBigContentTitle(payload.title)
                    .setSummaryText(payload.body)
            )
        } else if (payload.body.isNotBlank()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(payload.title)
                    .bigText(payload.body)
            )
        }

        return try {
            NotificationManagerCompat.from(appContext).notify(
                payload.dedupeKey ?: "nova_broadcast",
                notificationId,
                builder.build()
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private suspend fun fetchImageBitmapSafely(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!BroadcastNotificationParser.isValidHttpsUrl(imageUrl)) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(imageUrl).openConnection() as? HttpURLConnection) ?: return@withContext null
            conn.instanceFollowRedirects = true
            conn.connectTimeout = IMAGE_CONNECT_TIMEOUT_MS
            conn.readTimeout = IMAGE_READ_TIMEOUT_MS
            conn.requestMethod = "GET"
            if (conn.responseCode !in 200..299) return@withContext null

            val contentLength = conn.contentLengthLong
            if (contentLength > MAX_IMAGE_BYTES) return@withContext null

            val rawBytes = conn.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                val out = ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) return@withContext null
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun computeStableNotificationId(seed: String): Int {
        val hash = seed.hashCode() and 0x0FFF_FFFF
        return BASE_NOTIFICATION_ID + (hash % 10_000)
    }

    companion object {
        const val CHANNEL_ID = "nova_broadcast"
        const val CHANNEL_NAME = "NOVA Broadcast"
        const val CHANNEL_DESCRIPTION = "Announcements, highlights, and updates from NOVA BROADCAST."
        private const val BASE_NOTIFICATION_ID = 5000
        private const val IMAGE_CONNECT_TIMEOUT_MS = 6_000
        private const val IMAGE_READ_TIMEOUT_MS = 8_000
        private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024 // 5 MB safety limit

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.applicationContext.getSystemService(NotificationManager::class.java) ?: return
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
