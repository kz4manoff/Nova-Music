package com.novamusic.player.update

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.permissions.PermissionHelper
import com.novamusic.player.push.BroadcastDeviceRegistrar
import com.novamusic.player.push.BroadcastNotificationParser
import com.novamusic.player.push.BroadcastRegistrationResult
import com.novamusic.player.push.NovaBroadcastNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class FcmUpdatePushManager(
    context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val notifier: UpdateNotifier = AndroidUpdateNotifier(context.applicationContext),
    private val broadcastNotificationHelper: NovaBroadcastNotificationHelper =
        NovaBroadcastNotificationHelper(context.applicationContext),
    private val broadcastRegistrar: BroadcastDeviceRegistrar =
        BroadcastDeviceRegistrar(context.applicationContext, preferencesRepository),
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageLock = Mutex()
    private val registrationLock = Mutex()

    fun ensureRegisteredAsync() {
        scope.launch {
            ensureRegistered()
        }
    }

    suspend fun ensureRegistered(): Boolean = registrationLock.withLock {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        if (!isFirebaseInitialized()) {
            Log.i(
                TAG,
                "Firebase runtime config not present (app/google-services.json not bundled yet); skipping FCM registration."
            )
            return false
        }

        if (!PermissionHelper.hasNotificationPermission(appContext)) {
            Log.i(
                TAG,
                "Notification permission not granted yet; deferring automatic FCM backend registration."
            )
            return false
        }

        return runCatching {
            val messaging = FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled = true

            val token = messaging.token.awaitResultOrNull()
            if (!token.isNullOrBlank()) {
                preferencesRepository.saveFcmToken(token)
            }

            val subscribed = messaging.subscribeToTopic(PushUpdatePayload.UPDATES_TOPIC)
                .awaitCompletionSuccess()
            if (subscribed) {
                preferencesRepository.saveFcmSubscribedTopic(PushUpdatePayload.UPDATES_TOPIC)
            }

            if (!token.isNullOrBlank()) {
                broadcastRegistrar.registerDeviceToken(token, force = false)
            }

            !token.isNullOrBlank() && subscribed
        }.getOrElse { error ->
            Log.w(TAG, "FCM registration failed: ${error.message}")
            false
        }
    }

    suspend fun syncDeviceRegistration(force: Boolean = false): BroadcastRegistrationResult {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        if (!PermissionHelper.hasNotificationPermission(appContext)) {
            return BroadcastRegistrationResult.NotificationPermissionDenied(
                "Notification permission is not granted."
            )
        }
        val cachedToken = preferencesRepository.appSettings.first().fcmToken
        val resolvedToken = if (cachedToken.isNotBlank()) {
            cachedToken
        } else if (isFirebaseInitialized()) {
            runCatching { FirebaseMessaging.getInstance().token.awaitResultOrNull() }.getOrNull().orEmpty()
                .also { fetched ->
                    if (fetched.isNotBlank()) {
                        preferencesRepository.saveFcmToken(fetched)
                    }
                }
        } else {
            ""
        }

        return if (resolvedToken.isNotBlank()) {
            broadcastRegistrar.registerDeviceToken(resolvedToken, force = force)
        } else {
            BroadcastRegistrationResult.NotConfigured(
                "FCM token is not available yet (ensure Google Play services and google-services.json are configured)."
            )
        }
    }

    suspend fun onNewToken(token: String) = registrationLock.withLock {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        if (token.isNotBlank()) {
            preferencesRepository.saveFcmToken(token)
        }
        if (!PermissionHelper.hasNotificationPermission(appContext)) {
            Log.i(TAG, "FCM token rotated while notification permission denied; deferred backend sync.")
            return@withLock
        }
        if (isFirebaseInitialized()) {
            runCatching {
                val subscribed = FirebaseMessaging.getInstance()
                    .subscribeToTopic(PushUpdatePayload.UPDATES_TOPIC)
                    .awaitCompletionSuccess()
                if (subscribed) {
                    preferencesRepository.saveFcmSubscribedTopic(PushUpdatePayload.UPDATES_TOPIC)
                }
            }.onFailure { error ->
                Log.w(TAG, "Failed to re-subscribe rotated FCM token: ${error.message}")
            }
        }
        if (token.isNotBlank()) {
            broadcastRegistrar.registerDeviceToken(token, force = true)
        }
    }

    suspend fun handleRemoteMessage(remoteMessage: RemoteMessage): PushDedupeDecision? {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)

        val updatePayload = PushUpdatePayload.parseFcmDataPayload(
            data = remoteMessage.data,
            messageId = remoteMessage.messageId,
            notificationTitle = remoteMessage.notification?.title,
            notificationBody = remoteMessage.notification?.body
        )

        if (updatePayload != null) {
            return messageLock.withLock {
                val settings = preferencesRepository.appSettings.first()
                val decision = PushUpdatePayload.evaluateIncomingPush(
                    payload = updatePayload,
                    installedVersionCode = installedVersionCode,
                    lastNotifiedVersionCode = settings.lastNotifiedUpdateVersionCode,
                    dismissedNotificationVersionCode = settings.dismissedUpdateNotificationVersionCode,
                    lastFcmMessageId = settings.lastFcmMessageId
                )

                when (decision) {
                    is PushDedupeDecision.Suppress -> {
                        Log.d(TAG, "Suppressed duplicate/stale update push: ${decision.reason}")
                    }
                    is PushDedupeDecision.ShowNotification -> {
                        if (notifier.canPostNotifications()) {
                            val posted = notifier.showUpdateAvailableNotification(decision.config)
                            if (posted) {
                                preferencesRepository.recordUpdateNotificationPosted(
                                    versionCode = decision.config.versionCode,
                                    messageId = decision.payload.messageId
                                )
                            }
                        } else {
                            Log.i(
                                TAG,
                                "Update push v${decision.payload.versionName} received, but POST_NOTIFICATIONS is denied."
                            )
                        }
                    }
                }
                decision
            }
        }

        val broadcastPayload = BroadcastNotificationParser.parseRemotePayload(
            data = remoteMessage.data,
            messageId = remoteMessage.messageId,
            notificationTitle = remoteMessage.notification?.title,
            notificationBody = remoteMessage.notification?.body,
            notificationImageUrl = remoteMessage.notification?.imageUrl?.toString()
        ) ?: return null

        messageLock.withLock {
            val lastBroadcastId = preferencesRepository.appSettings.first().lastBroadcastMessageId
            if (!broadcastPayload.dedupeKey.isNullOrBlank() && broadcastPayload.dedupeKey == lastBroadcastId) {
                Log.d(TAG, "Suppressed duplicate broadcast notification ${broadcastPayload.dedupeKey}")
                return null
            }
            val posted = broadcastNotificationHelper.showBroadcastNotification(broadcastPayload)
            if (posted && !broadcastPayload.dedupeKey.isNullOrBlank()) {
                preferencesRepository.saveLastBroadcastMessageId(broadcastPayload.dedupeKey)
            }
        }
        return null
    }

    private fun isFirebaseInitialized(): Boolean {
        return runCatching {
            FirebaseApp.getApps(appContext).isNotEmpty() ||
                FirebaseApp.initializeApp(appContext) != null
        }.getOrDefault(false)
    }

    private suspend fun <T> Task<T>.awaitResultOrNull(): T? = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (!cont.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resume(null)
            }
        }
    }

    private suspend fun Task<Void>.awaitCompletionSuccess(): Boolean = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (!cont.isActive) return@addOnCompleteListener
            cont.resume(task.isSuccessful)
        }
    }

    companion object {
        private const val TAG = "NovaFcmPush"
    }
}
