package com.novamusic.player.update

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.preferences.PreferencesRepository
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
        if (!isFirebaseInitialized()) {
            Log.i(
                TAG,
                "Firebase runtime config not present (app/google-services.json not bundled yet); skipping FCM registration."
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
            !token.isNullOrBlank() && subscribed
        }.getOrElse { error ->
            Log.w(TAG, "FCM registration failed: ${error.message}")
            false
        }
    }

    suspend fun onNewToken(token: String) = registrationLock.withLock {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        if (token.isNotBlank()) {
            preferencesRepository.saveFcmToken(token)
        }
        if (!isFirebaseInitialized()) return@withLock

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

    suspend fun handleRemoteMessage(remoteMessage: RemoteMessage): PushDedupeDecision? {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        val payload = PushUpdatePayload.parseFcmDataPayload(
            data = remoteMessage.data,
            messageId = remoteMessage.messageId,
            notificationTitle = remoteMessage.notification?.title,
            notificationBody = remoteMessage.notification?.body
        ) ?: return null

        return messageLock.withLock {
            val settings = preferencesRepository.appSettings.first()
            val decision = PushUpdatePayload.evaluateIncomingPush(
                payload = payload,
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
