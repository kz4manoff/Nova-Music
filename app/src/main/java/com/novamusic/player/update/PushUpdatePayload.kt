package com.novamusic.player.update

import android.content.Intent

data class UpdatePushPayload(
    val messageId: String?,
    val versionName: String,
    val versionCode: Int,
    val shortDescription: String,
    val releaseTitle: String,
    val apkUrl: String?,
    val sha256: String?,
    val mandatoryUpdate: Boolean
)

sealed interface PushDedupeDecision {
    data class ShowNotification(
        val payload: UpdatePushPayload,
        val config: RemoteUpdateConfig
    ) : PushDedupeDecision

    data class Suppress(val reason: String) : PushDedupeDecision
}

data class UpdateNotificationIntentData(
    val shouldOpenUpdates: Boolean,
    val remoteVersionCode: Int
)

object PushUpdatePayload {
    const val UPDATES_TOPIC = "nova_updates"
    const val TYPE_NOVA_UPDATE = "nova_update"
    const val ACTION_OPEN_UPDATE_DETAILS = "com.novamusic.player.ACTION_OPEN_UPDATE_DETAILS"

    const val KEY_TYPE = "type"
    const val KEY_VERSION_NAME = "versionName"
    const val KEY_VERSION_CODE = "versionCode"
    const val KEY_SHORT_DESCRIPTION = "shortDescription"
    const val KEY_RELEASE_TITLE = "releaseTitle"
    const val KEY_APK_URL = "apkUrl"
    const val KEY_SHA256 = "sha256"
    const val KEY_MANDATORY_UPDATE = "mandatoryUpdate"
    const val KEY_MESSAGE_ID = "messageId"

    const val EXTRA_REMOTE_VERSION_CODE = "extra_remote_version_code"

    const val DEFAULT_TITLE = "New Update Available!"
    const val DEFAULT_DESCRIPTION = "Performance improvements and playback refinements."
    private const val MAX_SHORT_DESCRIPTION_CHARS = 140

    fun parseFcmDataPayload(
        data: Map<String, String>,
        messageId: String? = null,
        notificationTitle: String? = null,
        notificationBody: String? = null
    ): UpdatePushPayload? {
        val typeValue = data[KEY_TYPE]?.trim().orEmpty()
        if (typeValue.isNotEmpty() && typeValue != TYPE_NOVA_UPDATE) {
            return null
        }

        val rawVersionName = data[KEY_VERSION_NAME]
            ?.trim()
            ?.removePrefix("v")
            ?.removePrefix("V")
            .orEmpty()
        val versionCode = data[KEY_VERSION_CODE]?.trim()?.toIntOrNull() ?: 0

        if (rawVersionName.isBlank() || versionCode <= 0) {
            return null
        }

        val rawDesc = data[KEY_SHORT_DESCRIPTION]
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: data["releaseNotes"]?.trim()?.takeIf { it.isNotBlank() }
            ?: data["body"]?.trim()?.takeIf { it.isNotBlank() }
            ?: notificationBody?.trim()?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DESCRIPTION

        val shortDescription = sanitizeShortDescription(rawDesc)
        val releaseTitle = data[KEY_RELEASE_TITLE]?.trim()?.takeIf { it.isNotBlank() }
            ?: notificationTitle?.trim()?.takeIf { it.isNotBlank() }
            ?: DEFAULT_TITLE
        val apkUrl = data[KEY_APK_URL]?.trim()?.takeIf { it.isNotBlank() }
        val sha256 = data[KEY_SHA256]?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val mandatory = data[KEY_MANDATORY_UPDATE]?.trim()?.toBooleanStrictOrNull() ?: false
        val resolvedMessageId = messageId?.trim()?.takeIf { it.isNotBlank() }
            ?: data[KEY_MESSAGE_ID]?.trim()?.takeIf { it.isNotBlank() }

        return UpdatePushPayload(
            messageId = resolvedMessageId,
            versionName = rawVersionName,
            versionCode = versionCode,
            shortDescription = shortDescription,
            releaseTitle = releaseTitle,
            apkUrl = apkUrl,
            sha256 = sha256,
            mandatoryUpdate = mandatory
        )
    }

    fun evaluateIncomingPush(
        payload: UpdatePushPayload,
        installedVersionCode: Int,
        lastNotifiedVersionCode: Int,
        dismissedNotificationVersionCode: Int,
        lastFcmMessageId: String?
    ): PushDedupeDecision {
        if (payload.versionCode <= installedVersionCode) {
            return PushDedupeDecision.Suppress(
                "Installed version ($installedVersionCode) is already up to date with push version (${payload.versionCode})."
            )
        }
        if (!payload.messageId.isNullOrBlank() && payload.messageId == lastFcmMessageId?.trim()) {
            return PushDedupeDecision.Suppress(
                "Duplicate FCM messageId (${payload.messageId}) already handled."
            )
        }
        if (payload.versionCode <= lastNotifiedVersionCode) {
            return PushDedupeDecision.Suppress(
                "Update notification for versionCode ${payload.versionCode} was already shown (lastNotified=$lastNotifiedVersionCode)."
            )
        }
        if (payload.versionCode == dismissedNotificationVersionCode) {
            return PushDedupeDecision.Suppress(
                "User previously dismissed notification for versionCode ${payload.versionCode}."
            )
        }

        val fallbackApkUrl =
            "https://github.com/kz4manoff/Nova-Music/releases/download/v${payload.versionName}/NOVA-MUSIC-v${payload.versionName}.apk"
        val candidateApkUrl = payload.apkUrl?.takeIf { UpdateEvaluator.isTrustedApkUrl(it) }
            ?: fallbackApkUrl

        val config = RemoteUpdateConfig(
            latestVersion = payload.versionName,
            versionCode = payload.versionCode,
            apkUrl = candidateApkUrl,
            sha256 = payload.sha256.orEmpty(),
            releaseNotes = payload.shortDescription,
            mandatoryUpdate = payload.mandatoryUpdate,
            minimumSupportedVersion = if (payload.mandatoryUpdate) payload.versionCode else 1
        )

        return PushDedupeDecision.ShowNotification(
            payload = payload,
            config = config
        )
    }

    fun sanitizeShortDescription(raw: String): String {
        val collapsed = raw
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (collapsed.isBlank()) return DEFAULT_DESCRIPTION
        if (collapsed.length <= MAX_SHORT_DESCRIPTION_CHARS) return collapsed
        val clipped = collapsed.substring(0, MAX_SHORT_DESCRIPTION_CHARS - 1).trimEnd()
        val lastSpace = clipped.lastIndexOf(' ')
        return if (lastSpace > MAX_SHORT_DESCRIPTION_CHARS / 2) {
            clipped.substring(0, lastSpace) + "…"
        } else {
            "$clipped…"
        }
    }

    fun parseUpdateNavigationFlags(
        action: String?,
        hasOpenUpdatesExtra: Boolean,
        typeExtra: String?,
        remoteVersionCodeFromExtra: Int,
        remoteVersionCodeFromFcmData: String?
    ): UpdateNotificationIntentData {
        val isFcmUpdateType = typeExtra?.trim() == TYPE_NOVA_UPDATE
        val shouldOpenUpdates = hasOpenUpdatesExtra ||
            action == ACTION_OPEN_UPDATE_DETAILS ||
            isFcmUpdateType

        val versionCode = when {
            remoteVersionCodeFromExtra > 0 -> remoteVersionCodeFromExtra
            else -> remoteVersionCodeFromFcmData?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        }

        return UpdateNotificationIntentData(
            shouldOpenUpdates = shouldOpenUpdates,
            remoteVersionCode = versionCode
        )
    }

    fun extractUpdateIntentData(intent: Intent?, openUpdatesExtraKey: String): UpdateNotificationIntentData {
        if (intent == null) {
            return UpdateNotificationIntentData(shouldOpenUpdates = false, remoteVersionCode = 0)
        }
        return parseUpdateNavigationFlags(
            action = intent.action,
            hasOpenUpdatesExtra = intent.getBooleanExtra(openUpdatesExtraKey, false),
            typeExtra = intent.getStringExtra(KEY_TYPE),
            remoteVersionCodeFromExtra = intent.getIntExtra(EXTRA_REMOTE_VERSION_CODE, 0),
            remoteVersionCodeFromFcmData = intent.getStringExtra(KEY_VERSION_CODE)
        )
    }

    fun clearConsumedUpdateExtras(intent: Intent?, openUpdatesExtraKey: String) {
        if (intent == null) return
        intent.removeExtra(openUpdatesExtraKey)
        intent.removeExtra(EXTRA_REMOTE_VERSION_CODE)
        intent.removeExtra(KEY_TYPE)
        intent.removeExtra(KEY_VERSION_CODE)
        if (intent.action == ACTION_OPEN_UPDATE_DETAILS) {
            intent.action = null
        }
    }
}
