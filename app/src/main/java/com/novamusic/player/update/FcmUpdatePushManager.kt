package com.novamusic.player.update

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.preferences.AppSettings
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.permissions.PermissionHelper
import com.novamusic.player.push.BroadcastDeviceRegistrar
import com.novamusic.player.push.BroadcastNotificationParser
import com.novamusic.player.push.BroadcastRegistrationResult
import com.novamusic.player.push.NovaBroadcastNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class FcmDiagnosticsSnapshot(
    val firebaseInitialized: Boolean = false,
    val firebaseStatusDetail: String = "Checking...",
    val firebaseProjectId: String = "",
    val postNotificationsGranted: Boolean = false,
    val systemNotificationsEnabled: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val fcmTokenRaw: String = "",
    val fcmTokenPreview: String = "None",
    val fcmTokenStatus: String = "Pending",
    val fcmTokenError: String = "",
    val topicSubscribed: String = "",
    val backendEndpointUrl: String = "",
    val deviceId: String = "",
    val registrationStatus: String = "NOT_ATTEMPTED",
    val isRegisteredConfirmed: Boolean = false,
    val lastBackendHttpCode: Int = 0,
    val lastBackendResponse: String = "",
    val lastAttemptMs: Long = 0L,
    val lastSuccessMs: Long = 0L,
    val isSyncing: Boolean = false,
    val recentLogs: List<String> = emptyList()
)

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
    private val logLock = Any()
    private val logBuffer = ArrayDeque<String>(MAX_DIAGNOSTIC_LOGS)

    private val _diagnosticsState = MutableStateFlow(FcmDiagnosticsSnapshot())
    val diagnosticsState: StateFlow<FcmDiagnosticsSnapshot> = _diagnosticsState.asStateFlow()

    @Volatile
    private var lastTokenErrorMessage: String = ""

    @Volatile
    private var isSyncingNow: Boolean = false

    init {
        scope.launch {
            preferencesRepository.appSettings.collect { settings ->
                publishDiagnostics(settings)
            }
        }
    }

    fun ensureRegisteredAsync() {
        scope.launch {
            ensureRegistered()
        }
    }

    fun syncDeviceRegistrationAsync(force: Boolean = true) {
        scope.launch {
            syncDeviceRegistration(force = force)
        }
    }

    fun refreshDiagnosticsAsync() {
        scope.launch {
            refreshDiagnostics()
        }
    }

    suspend fun refreshDiagnostics() {
        val settings = preferencesRepository.appSettings.first()
        publishDiagnostics(settings)
    }

    suspend fun ensureRegistered(): Boolean = registrationLock.withLock {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        isSyncingNow = true
        publishCurrentDiagnostics()
        try {
            val firebaseApp = resolveFirebaseApp()
            if (firebaseApp == null) {
                val reason = "FirebaseApp not initialized (app/google-services.json missing from APK build)."
                appendLog("W", reason)
                preferencesRepository.recordBackendRegistrationAttempt(
                    statusLabel = "FIREBASE_NOT_INITIALIZED",
                    httpCode = 0,
                    responsePreview = reason,
                    attemptAtMs = System.currentTimeMillis()
                )
                return false
            }
            appendLog(
                "I",
                "Firebase initialized: project=${firebaseApp.options.projectId ?: "unknown"} appId=${firebaseApp.options.applicationId.take(18)}..."
            )

            val runtimeGrant = PermissionHelper.hasPostNotificationsRuntimeGrant(appContext)
            val systemEnabled = PermissionHelper.areSystemNotificationsEnabled(appContext)
            if (!runtimeGrant || !systemEnabled) {
                val reason = "Notifications disabled (runtimeGrant=$runtimeGrant, systemEnabled=$systemEnabled)."
                appendLog("I", reason)
                preferencesRepository.recordBackendRegistrationAttempt(
                    statusLabel = "PERMISSION_DENIED",
                    httpCode = 0,
                    responsePreview = reason,
                    attemptAtMs = System.currentTimeMillis()
                )
                return false
            }

            return runCatching {
                val messaging = FirebaseMessaging.getInstance()
                messaging.isAutoInitEnabled = true

                appendLog("I", "Requesting real FCM token via FirebaseMessaging.getInstance().token...")
                val (token, tokenError) = messaging.token.awaitTaskOutcome()
                if (tokenError != null) {
                    val detail = tokenError.message ?: tokenError.javaClass.simpleName
                    lastTokenErrorMessage = detail
                    appendLog("E", "FCM token retrieval failed: $detail")
                    preferencesRepository.recordBackendRegistrationAttempt(
                        statusLabel = "TOKEN_FAILED",
                        httpCode = 0,
                        responsePreview = "FirebaseMessaging.token failed: $detail",
                        attemptAtMs = System.currentTimeMillis()
                    )
                    return false
                }

                if (token.isNullOrBlank()) {
                    lastTokenErrorMessage = "FirebaseMessaging.getInstance().token returned empty."
                    appendLog("W", lastTokenErrorMessage)
                    preferencesRepository.recordBackendRegistrationAttempt(
                        statusLabel = "TOKEN_UNAVAILABLE",
                        httpCode = 0,
                        responsePreview = lastTokenErrorMessage,
                        attemptAtMs = System.currentTimeMillis()
                    )
                    return false
                }

                lastTokenErrorMessage = ""
                appendLog(
                    "I",
                    "Retrieved real FCM token (${token.length} chars): ${BroadcastDeviceRegistrar.maskToken(token)}"
                )
                preferencesRepository.saveFcmToken(token)

                val (subscribed, topicError) = messaging.subscribeToTopic(PushUpdatePayload.UPDATES_TOPIC)
                    .awaitCompletionOutcome()
                if (subscribed) {
                    preferencesRepository.saveFcmSubscribedTopic(PushUpdatePayload.UPDATES_TOPIC)
                    appendLog("I", "Subscribed to FCM topic '${PushUpdatePayload.UPDATES_TOPIC}'.")
                } else {
                    appendLog(
                        "W",
                        "Topic subscribe '${PushUpdatePayload.UPDATES_TOPIC}' failed: ${topicError?.message ?: "unknown"}"
                    )
                }

                val regResult = broadcastRegistrar.registerDeviceToken(token, force = false)
                logRegistrationOutcome(regResult)
                regResult is BroadcastRegistrationResult.Success ||
                    regResult is BroadcastRegistrationResult.AlreadySynced
            }.getOrElse { error ->
                val detail = error.message ?: error.javaClass.simpleName
                lastTokenErrorMessage = detail
                appendLog("E", "FCM registration failed: $detail")
                false
            }
        } finally {
            isSyncingNow = false
            publishCurrentDiagnostics()
        }
    }

    suspend fun syncDeviceRegistration(force: Boolean = false): BroadcastRegistrationResult = registrationLock.withLock {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        isSyncingNow = true
        publishCurrentDiagnostics()
        try {
            val runtimeGrant = PermissionHelper.hasPostNotificationsRuntimeGrant(appContext)
            val systemEnabled = PermissionHelper.areSystemNotificationsEnabled(appContext)
            if (!runtimeGrant || !systemEnabled) {
                val reason = "Notification permission is not granted (runtimeGrant=$runtimeGrant, systemEnabled=$systemEnabled)."
                appendLog("W", reason)
                preferencesRepository.recordBackendRegistrationAttempt(
                    statusLabel = "PERMISSION_DENIED",
                    httpCode = 0,
                    responsePreview = reason,
                    attemptAtMs = System.currentTimeMillis()
                )
                return BroadcastRegistrationResult.NotificationPermissionDenied(reason)
            }

            val firebaseApp = resolveFirebaseApp()
            if (firebaseApp == null) {
                val reason = "Firebase is not initialized (app/google-services.json missing from APK)."
                appendLog("E", reason)
                preferencesRepository.recordBackendRegistrationAttempt(
                    statusLabel = "FIREBASE_NOT_INITIALIZED",
                    httpCode = 0,
                    responsePreview = reason,
                    attemptAtMs = System.currentTimeMillis()
                )
                return BroadcastRegistrationResult.NotConfigured(reason)
            }

            val cachedToken = preferencesRepository.appSettings.first().fcmToken
            val resolvedToken = if (force || cachedToken.isBlank()) {
                appendLog("I", "Fetching fresh FCM token from FirebaseMessaging.getInstance().token...")
                val messaging = FirebaseMessaging.getInstance().apply { isAutoInitEnabled = true }
                val (freshToken, tokenError) = messaging.token.awaitTaskOutcome()
                if (!freshToken.isNullOrBlank()) {
                    lastTokenErrorMessage = ""
                    appendLog(
                        "I",
                        "Fresh FCM token ready (${freshToken.length} chars): ${BroadcastDeviceRegistrar.maskToken(freshToken)}"
                    )
                    preferencesRepository.saveFcmToken(freshToken)
                    freshToken
                } else {
                    val errDetail = tokenError?.message ?: tokenError?.javaClass?.simpleName ?: "empty token"
                    lastTokenErrorMessage = errDetail
                    appendLog("W", "Fresh FCM token fetch failed ($errDetail); cached=${cachedToken.isNotBlank()}.")
                    cachedToken
                }
            } else {
                cachedToken
            }

            return if (resolvedToken.isNotBlank()) {
                val regResult = broadcastRegistrar.registerDeviceToken(resolvedToken, force = force)
                logRegistrationOutcome(regResult)
                regResult
            } else {
                val reason = if (lastTokenErrorMessage.isNotBlank()) {
                    "FCM token unavailable: $lastTokenErrorMessage"
                } else {
                    "FCM token is not available yet (ensure Google Play services and google-services.json are configured)."
                }
                preferencesRepository.recordBackendRegistrationAttempt(
                    statusLabel = "TOKEN_FAILED",
                    httpCode = 0,
                    responsePreview = reason,
                    attemptAtMs = System.currentTimeMillis()
                )
                BroadcastRegistrationResult.NotConfigured(reason)
            }
        } finally {
            isSyncingNow = false
            publishCurrentDiagnostics()
        }
    }

    suspend fun onNewToken(token: String) = registrationLock.withLock {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        isSyncingNow = true
        publishCurrentDiagnostics()
        try {
            if (token.isNotBlank()) {
                lastTokenErrorMessage = ""
                appendLog(
                    "I",
                    "onNewToken received (${token.length} chars): ${BroadcastDeviceRegistrar.maskToken(token)}"
                )
                preferencesRepository.saveFcmToken(token)
            } else {
                appendLog("W", "onNewToken invoked with blank token.")
            }

            if (!PermissionHelper.hasNotificationPermission(appContext)) {
                val reason = "FCM token rotated while notification permission denied; deferred backend sync."
                appendLog("I", reason)
                preferencesRepository.recordBackendRegistrationAttempt(
                    statusLabel = "PERMISSION_DENIED",
                    httpCode = 0,
                    responsePreview = reason,
                    attemptAtMs = System.currentTimeMillis()
                )
                return@withLock
            }

            if (resolveFirebaseApp() != null) {
                runCatching {
                    val (subscribed, topicErr) = FirebaseMessaging.getInstance()
                        .subscribeToTopic(PushUpdatePayload.UPDATES_TOPIC)
                        .awaitCompletionOutcome()
                    if (subscribed) {
                        preferencesRepository.saveFcmSubscribedTopic(PushUpdatePayload.UPDATES_TOPIC)
                        appendLog("I", "Re-subscribed rotated token to '${PushUpdatePayload.UPDATES_TOPIC}'.")
                    } else {
                        appendLog("W", "Re-subscribe failed: ${topicErr?.message ?: "unknown"}")
                    }
                }.onFailure { error ->
                    appendLog("W", "Failed to re-subscribe rotated FCM token: ${error.message}")
                }
            }

            if (token.isNotBlank()) {
                val result = broadcastRegistrar.registerDeviceToken(token, force = true)
                logRegistrationOutcome(result)
            }
        } finally {
            isSyncingNow = false
            publishCurrentDiagnostics()
        }
    }

    suspend fun handleRemoteMessage(remoteMessage: RemoteMessage): PushDedupeDecision? {
        AndroidUpdateNotifier.ensureNotificationChannel(appContext)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(appContext)
        appendLog(
            "I",
            "onMessageReceived id=${remoteMessage.messageId ?: "none"} keys=${remoteMessage.data.keys.joinToString(",")}"
        )

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

    private fun logRegistrationOutcome(result: BroadcastRegistrationResult) {
        when (result) {
            is BroadcastRegistrationResult.Success -> {
                appendLog(
                    "I",
                    "Backend confirmed HTTP ${result.httpStatus}: ${result.responseBody.ifBlank { "{\"registered\":true}" }}"
                )
            }
            is BroadcastRegistrationResult.AlreadySynced -> {
                appendLog("D", "Device token & endpoint already confirmed synced in D1.")
            }
            is BroadcastRegistrationResult.NotificationPermissionDenied -> {
                appendLog("W", "Deferred (permission denied): ${result.reason}")
            }
            is BroadcastRegistrationResult.NotConfigured -> {
                appendLog("W", "Not configured: ${result.reason}")
            }
            is BroadcastRegistrationResult.RetryableFailure -> {
                appendLog("W", "Retryable failure (WorkManager scheduled): ${result.reason}")
            }
            is BroadcastRegistrationResult.Rejected -> {
                appendLog("E", "Backend rejected (HTTP ${result.statusCode}): ${result.reason}")
            }
        }
    }

    private suspend fun publishCurrentDiagnostics() {
        val settings = preferencesRepository.appSettings.first()
        publishDiagnostics(settings)
    }

    private suspend fun publishDiagnostics(settings: AppSettings) {
        val firebaseApp = resolveFirebaseApp()
        val firebaseInitialized = firebaseApp != null
        val projectId = firebaseApp?.options?.projectId.orEmpty()
        val appId = firebaseApp?.options?.applicationId.orEmpty()
        val firebaseDetail = if (firebaseInitialized) {
            buildString {
                append("Initialized")
                if (projectId.isNotBlank()) append(" • project=$projectId")
                if (appId.isNotBlank()) append(" • appId=${appId.take(18)}...")
            }
        } else {
            "Not initialized (app/google-services.json not bundled in APK)"
        }

        val postGrant = PermissionHelper.hasPostNotificationsRuntimeGrant(appContext)
        val systemEnabled = PermissionHelper.areSystemNotificationsEnabled(appContext)
        val notifGranted = postGrant && systemEnabled

        val endpointUrl = broadcastRegistrar.currentEndpointUrl()
        val deviceId = settings.deviceInstallationId.ifBlank {
            runCatching { broadcastRegistrar.getDeviceInstallationId() }.getOrDefault("")
        }
        val rawToken = settings.fcmToken
        val tokenPreview = if (rawToken.isNotBlank()) {
            "${BroadcastDeviceRegistrar.maskToken(rawToken)} (${rawToken.length} chars)"
        } else {
            "None"
        }
        val tokenStatus = when {
            rawToken.isNotBlank() -> "Retrieved"
            lastTokenErrorMessage.isNotBlank() -> "Failed"
            !firebaseInitialized -> "Blocked (Firebase uninitialized)"
            else -> "Pending"
        }

        val confirmed = rawToken.isNotBlank() &&
            settings.lastSyncedFcmToken == rawToken &&
            endpointUrl.isNotBlank() &&
            settings.lastSyncedBroadcastUrl == endpointUrl &&
            settings.lastBackendSyncMs > 0L

        val statusLabel = when {
            confirmed && settings.lastBroadcastRegStatus.isBlank() -> "REGISTERED"
            settings.lastBroadcastRegStatus.isNotBlank() -> settings.lastBroadcastRegStatus
            confirmed -> "REGISTERED"
            !firebaseInitialized -> "FIREBASE_NOT_INITIALIZED"
            !notifGranted -> "PERMISSION_DENIED"
            endpointUrl.isBlank() -> "NOT_CONFIGURED"
            else -> "NOT_ATTEMPTED"
        }

        val snapshotLogs = synchronized(logLock) { logBuffer.toList() }

        _diagnosticsState.value = FcmDiagnosticsSnapshot(
            firebaseInitialized = firebaseInitialized,
            firebaseStatusDetail = firebaseDetail,
            firebaseProjectId = projectId,
            postNotificationsGranted = postGrant,
            systemNotificationsEnabled = systemEnabled,
            notificationPermissionGranted = notifGranted,
            fcmTokenRaw = rawToken,
            fcmTokenPreview = tokenPreview,
            fcmTokenStatus = tokenStatus,
            fcmTokenError = lastTokenErrorMessage,
            topicSubscribed = settings.fcmSubscribedTopic,
            backendEndpointUrl = endpointUrl,
            deviceId = deviceId,
            registrationStatus = statusLabel,
            isRegisteredConfirmed = confirmed,
            lastBackendHttpCode = settings.lastBroadcastRegHttpCode,
            lastBackendResponse = settings.lastBroadcastRegResponse,
            lastAttemptMs = settings.lastBroadcastRegAttemptMs,
            lastSuccessMs = if (confirmed) settings.lastBackendSyncMs else 0L,
            isSyncing = isSyncingNow,
            recentLogs = snapshotLogs
        )
    }

    private fun resolveFirebaseApp(): FirebaseApp? {
        return runCatching {
            FirebaseApp.getApps(appContext).firstOrNull() ?: FirebaseApp.initializeApp(appContext)
        }.getOrNull()
    }

    private fun appendLog(level: String, message: String) {
        when (level) {
            "E" -> Log.e(TAG, message)
            "W" -> Log.w(TAG, message)
            "D" -> Log.d(TAG, message)
            else -> Log.i(TAG, message)
        }
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "$stamp [$level] $message"
        synchronized(logLock) {
            if (logBuffer.size >= MAX_DIAGNOSTIC_LOGS) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast(line)
        }
    }

    private suspend fun <T> Task<T>.awaitTaskOutcome(): Pair<T?, Throwable?> =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (!cont.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    cont.resume(task.result to null)
                } else {
                    cont.resume(null to (task.exception ?: IllegalStateException("Firebase Task failed")))
                }
            }
        }

    private suspend fun Task<Void>.awaitCompletionOutcome(): Pair<Boolean, Throwable?> =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (!cont.isActive) return@addOnCompleteListener
                cont.resume(task.isSuccessful to task.exception)
            }
        }

    companion object {
        private const val TAG = "NovaFcmPush"
        private const val MAX_DIAGNOSTIC_LOGS = 40
    }
}
