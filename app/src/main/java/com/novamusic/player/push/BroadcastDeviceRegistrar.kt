package com.novamusic.player.push

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.novamusic.player.BuildConfig
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.permissions.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BroadcastRegistrationPayload(
    val deviceId: String,
    val fcmToken: String,
    val platform: String = "android",
    val packageName: String,
    val appVersionName: String,
    val appVersionCode: Int,
    val sdkInt: Int,
    val deviceModel: String,
    val manufacturer: String,
    val notificationsEnabled: Boolean,
    val locale: String
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("deviceId", deviceId)
            put("fcmToken", fcmToken)
            put("platform", platform)
            put("packageName", packageName)
            put("appVersionName", appVersionName)
            put("appVersionCode", appVersionCode)
            put("sdkInt", sdkInt)
            put("deviceModel", deviceModel)
            put("manufacturer", manufacturer)
            put("notificationsEnabled", notificationsEnabled)
            put("locale", locale)
        }.toString()
    }
}

sealed interface BroadcastRegistrationResult {
    data class Success(
        val syncedAtMs: Long,
        val httpStatus: Int = 200,
        val responseBody: String = ""
    ) : BroadcastRegistrationResult
    data object AlreadySynced : BroadcastRegistrationResult
    data class NotificationPermissionDenied(val reason: String) : BroadcastRegistrationResult
    data class NotConfigured(val reason: String) : BroadcastRegistrationResult
    data class RetryableFailure(
        val reason: String,
        val httpStatus: Int = 0,
        val responseBody: String = ""
    ) : BroadcastRegistrationResult
    data class Rejected(
        val statusCode: Int,
        val reason: String,
        val responseBody: String = ""
    ) : BroadcastRegistrationResult
}

interface BroadcastBackendClient {
    suspend fun registerDevice(
        endpointUrl: String,
        clientKey: String,
        payload: BroadcastRegistrationPayload
    ): BroadcastRegistrationResult
}

interface BroadcastRegistrationStore {
    suspend fun getOrCreateDeviceId(): String
    suspend fun getLastSyncedFcmToken(): String
    suspend fun getLastSyncedEndpointUrl(): String
    suspend fun getLastSyncedAppVersionCode(): Int
    suspend fun recordSynced(token: String, endpointUrl: String, versionCode: Int, syncedAtMs: Long)
    suspend fun recordAttempt(
        statusLabel: String,
        httpCode: Int,
        responsePreview: String,
        attemptAtMs: Long
    ) {}
}

interface BroadcastRetryScheduler {
    fun scheduleRetry()
}

class WorkManagerBroadcastRetryScheduler(
    context: Context
) : BroadcastRetryScheduler {
    private val appContext = context.applicationContext

    override fun scheduleRetry() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BroadcastRegistrationWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "nova_broadcast_device_registration_retry"
    }
}

class RepositoryBroadcastRegistrationStore(
    private val preferences: PreferencesRepository
) : BroadcastRegistrationStore {
    override suspend fun getOrCreateDeviceId(): String =
        preferences.getOrCreateDeviceInstallationId()

    override suspend fun getLastSyncedFcmToken(): String =
        preferences.appSettings.first().lastSyncedFcmToken

    override suspend fun getLastSyncedEndpointUrl(): String =
        preferences.appSettings.first().lastSyncedBroadcastUrl

    override suspend fun getLastSyncedAppVersionCode(): Int =
        preferences.appSettings.first().lastSyncedAppVersionCode

    override suspend fun recordSynced(
        token: String,
        endpointUrl: String,
        versionCode: Int,
        syncedAtMs: Long
    ) {
        preferences.recordBackendDeviceSynced(
            token = token,
            endpointUrl = endpointUrl,
            versionCode = versionCode,
            syncedAtMs = syncedAtMs
        )
    }

    override suspend fun recordAttempt(
        statusLabel: String,
        httpCode: Int,
        responsePreview: String,
        attemptAtMs: Long
    ) {
        preferences.recordBackendRegistrationAttempt(
            statusLabel = statusLabel,
            httpCode = httpCode,
            responsePreview = responsePreview,
            attemptAtMs = attemptAtMs
        )
    }
}

class HttpBroadcastBackendClient : BroadcastBackendClient {
    override suspend fun registerDevice(
        endpointUrl: String,
        clientKey: String,
        payload: BroadcastRegistrationPayload
    ): BroadcastRegistrationResult = withContext(Dispatchers.IO) {
        if (!BroadcastNotificationParser.isValidHttpsUrl(endpointUrl)) {
            return@withContext BroadcastRegistrationResult.NotConfigured(
                "NOVA BROADCAST endpoint must be a valid HTTPS URL."
            )
        }

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpointUrl).openConnection() as? HttpURLConnection)
                ?: return@withContext BroadcastRegistrationResult.RetryableFailure("Failed to open HTTPS connection.")
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "NOVA-MUSIC-Android/${payload.appVersionName}")
            conn.setRequestProperty("X-Nova-Device-Id", payload.deviceId)
            conn.setRequestProperty("X-Nova-Package", payload.packageName)

            val trimmedKey = clientKey.trim()
            if (trimmedKey.isNotEmpty()) {
                conn.setRequestProperty("X-Nova-App-Key", trimmedKey)
                conn.setRequestProperty("Authorization", "Bearer $trimmedKey")
                conn.setRequestProperty("X-Client-Key", trimmedKey)
            }

            val bodyBytes = payload.toJsonString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bodyBytes.size)
            conn.outputStream.use { out ->
                out.write(bodyBytes)
                out.flush()
            }

            val status = conn.responseCode
            when {
                status in 200..299 -> {
                    val responseText = runCatching {
                        conn.inputStream?.bufferedReader()?.use { it.readText() }
                    }.getOrNull().orEmpty().trim()
                    val json = runCatching {
                        if (responseText.isNotEmpty()) JSONObject(responseText) else null
                    }.getOrNull()

                    if (json != null && json.has("registered") && !json.optBoolean("registered", false)) {
                        val serverReason = json.optString("reason")
                            .ifBlank { json.optString("error") }
                            .ifBlank { "Backend returned registered=false" }
                        BroadcastRegistrationResult.Rejected(
                            statusCode = status,
                            reason = serverReason,
                            responseBody = responseText.take(400)
                        )
                    } else {
                        BroadcastRegistrationResult.Success(
                            syncedAtMs = System.currentTimeMillis(),
                            httpStatus = status,
                            responseBody = responseText.take(400)
                        )
                    }
                }
                status == 408 || status == 429 || status in 500..599 -> {
                    val errPreview = runCatching {
                        conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(300)
                    }.getOrNull().orEmpty()
                    BroadcastRegistrationResult.RetryableFailure(
                        reason = "Backend returned transient HTTP $status $errPreview".trim(),
                        httpStatus = status,
                        responseBody = errPreview
                    )
                }
                else -> {
                    val errPreview = runCatching {
                        conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(300)
                    }.getOrNull().orEmpty()
                    BroadcastRegistrationResult.Rejected(
                        statusCode = status,
                        reason = "Backend rejected registration (HTTP $status) $errPreview".trim(),
                        responseBody = errPreview
                    )
                }
            }
        } catch (io: IOException) {
            BroadcastRegistrationResult.RetryableFailure("Network I/O error: ${io.message ?: "offline"}")
        } catch (t: Throwable) {
            BroadcastRegistrationResult.RetryableFailure("Unexpected registration error: ${t.message ?: t.javaClass.simpleName}")
        } finally {
            conn?.disconnect()
        }
    }
}

class BroadcastDeviceRegistrar(
    private val store: BroadcastRegistrationStore,
    private val backendClient: BroadcastBackendClient,
    private val retryScheduler: BroadcastRetryScheduler,
    private val endpointProvider: () -> String = { BuildConfig.BROADCAST_REGISTER_URL.trim() },
    private val clientKeyProvider: () -> String = { BuildConfig.BROADCAST_CLIENT_KEY.trim() },
    private val packageNameProvider: () -> String = { BuildConfig.APPLICATION_ID },
    private val versionNameProvider: () -> String = { BuildConfig.VERSION_NAME },
    private val versionCodeProvider: () -> Int = { BuildConfig.VERSION_CODE },
    private val sdkIntProvider: () -> Int = { Build.VERSION.SDK_INT },
    private val deviceModelProvider: () -> String = { Build.MODEL.orEmpty() },
    private val manufacturerProvider: () -> String = { Build.MANUFACTURER.orEmpty() },
    private val localeProvider: () -> String = { Locale.getDefault().toLanguageTag() },
    private val notificationsEnabledProvider: () -> Boolean
) {
    constructor(
        context: Context,
        preferencesRepository: PreferencesRepository
    ) : this(
        store = RepositoryBroadcastRegistrationStore(preferencesRepository),
        backendClient = HttpBroadcastBackendClient(),
        retryScheduler = WorkManagerBroadcastRetryScheduler(context.applicationContext),
        packageNameProvider = { context.applicationContext.packageName },
        notificationsEnabledProvider = {
            PermissionHelper.hasNotificationPermission(context.applicationContext)
        }
    )

    private val syncMutex = Mutex()

    fun currentEndpointUrl(): String = endpointProvider().trim()

    suspend fun getDeviceInstallationId(): String = store.getOrCreateDeviceId()

    suspend fun registerDeviceToken(
        fcmToken: String,
        force: Boolean = false
    ): BroadcastRegistrationResult = syncMutex.withLock {
        val nowMs = System.currentTimeMillis()
        if (!notificationsEnabledProvider()) {
            val reason = "POST_NOTIFICATIONS permission or app notifications must be enabled before registering device."
            Log.i(TAG, "Notification permission not granted yet; deferring NOVA BROADCAST device registration.")
            store.recordAttempt("PERMISSION_DENIED", 0, reason, nowMs)
            return BroadcastRegistrationResult.NotificationPermissionDenied(reason)
        }

        val cleanToken = fcmToken.trim()
        if (cleanToken.isBlank()) {
            val reason = "FCM token is unavailable."
            Log.w(TAG, "Skipping backend registration: $reason")
            store.recordAttempt("TOKEN_UNAVAILABLE", 0, reason, nowMs)
            return BroadcastRegistrationResult.NotConfigured(reason)
        }

        val endpointUrl = endpointProvider().trim()
        if (endpointUrl.isBlank()) {
            val reason = "NOVA BROADCAST device registration endpoint is not configured."
            Log.w(
                TAG,
                "NOVA BROADCAST endpoint is not configured (BuildConfig.BROADCAST_REGISTER_URL is empty); skipping backend sync."
            )
            store.recordAttempt("NOT_CONFIGURED", 0, reason, nowMs)
            return BroadcastRegistrationResult.NotConfigured(reason)
        }

        if (!BroadcastNotificationParser.isValidHttpsUrl(endpointUrl)) {
            val reason = "NOVA BROADCAST device registration endpoint must use HTTPS."
            Log.w(TAG, "Refusing non-HTTPS NOVA BROADCAST registration endpoint: $endpointUrl")
            store.recordAttempt("INVALID_ENDPOINT", 0, reason, nowMs)
            return BroadcastRegistrationResult.NotConfigured(reason)
        }

        val appVersionCode = versionCodeProvider()
        val lastSyncedToken = store.getLastSyncedFcmToken()
        val lastSyncedUrl = store.getLastSyncedEndpointUrl()
        val lastSyncedVersion = store.getLastSyncedAppVersionCode()

        if (!force &&
            lastSyncedToken == cleanToken &&
            lastSyncedUrl == endpointUrl &&
            lastSyncedVersion == appVersionCode
        ) {
            Log.d(
                TAG,
                "Device already confirmed synced with $endpointUrl (token=${maskToken(cleanToken)}, versionCode=$appVersionCode)."
            )
            return BroadcastRegistrationResult.AlreadySynced
        }

        val deviceId = store.getOrCreateDeviceId()
        val payload = BroadcastRegistrationPayload(
            deviceId = deviceId,
            fcmToken = cleanToken,
            packageName = packageNameProvider(),
            appVersionName = versionNameProvider(),
            appVersionCode = appVersionCode,
            sdkInt = sdkIntProvider(),
            deviceModel = deviceModelProvider(),
            manufacturer = manufacturerProvider(),
            notificationsEnabled = notificationsEnabledProvider(),
            locale = localeProvider()
        )

        Log.i(
            TAG,
            "POST $endpointUrl deviceId=${deviceId.take(8)}... token=${maskToken(cleanToken)} app=${payload.appVersionName}(${payload.appVersionCode}) sdk=${payload.sdkInt} notifications=${payload.notificationsEnabled}"
        )

        return when (val result = backendClient.registerDevice(endpointUrl, clientKeyProvider(), payload)) {
            is BroadcastRegistrationResult.Success -> {
                store.recordSynced(
                    token = cleanToken,
                    endpointUrl = endpointUrl,
                    versionCode = appVersionCode,
                    syncedAtMs = result.syncedAtMs
                )
                store.recordAttempt(
                    statusLabel = "REGISTERED",
                    httpCode = result.httpStatus,
                    responsePreview = result.responseBody.ifBlank { "{\"registered\":true}" },
                    attemptAtMs = result.syncedAtMs
                )
                Log.i(
                    TAG,
                    "Confirmed device registration HTTP ${result.httpStatus} at $endpointUrl (device=${deviceId.take(8)}..., body=${result.responseBody})"
                )
                result
            }
            is BroadcastRegistrationResult.RetryableFailure -> {
                store.recordAttempt(
                    statusLabel = "RETRY_QUEUED",
                    httpCode = result.httpStatus,
                    responsePreview = result.responseBody.ifBlank { result.reason },
                    attemptAtMs = System.currentTimeMillis()
                )
                Log.w(TAG, "NOVA BROADCAST device registration deferred for retry: ${result.reason}")
                retryScheduler.scheduleRetry()
                result
            }
            is BroadcastRegistrationResult.Rejected -> {
                store.recordAttempt(
                    statusLabel = "REJECTED",
                    httpCode = result.statusCode,
                    responsePreview = result.responseBody.ifBlank { result.reason },
                    attemptAtMs = System.currentTimeMillis()
                )
                Log.e(TAG, "NOVA BROADCAST rejected device registration (${result.statusCode}): ${result.reason}")
                result
            }
            is BroadcastRegistrationResult.AlreadySynced,
            is BroadcastRegistrationResult.NotificationPermissionDenied,
            is BroadcastRegistrationResult.NotConfigured -> result
        }
    }

    companion object {
        private const val TAG = "NovaBroadcastRegistrar"

        fun maskToken(token: String): String {
            val trimmed = token.trim()
            if (trimmed.length <= 14) return trimmed
            return "${trimmed.take(8)}...${trimmed.takeLast(6)}"
        }
    }
}
