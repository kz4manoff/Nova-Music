package com.novamusic.player.push

import android.content.Intent
import com.novamusic.player.navigation.NovaDestinations
import java.net.URI

data class BroadcastNotificationPayload(
    val dedupeKey: String?,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val deepLink: String?
)

sealed interface BroadcastDeepLinkTarget {
    data class InAppRoute(val route: String) : BroadcastDeepLinkTarget
    data class ExternalHttps(val url: String) : BroadcastDeepLinkTarget
    data object OpenAppHome : BroadcastDeepLinkTarget
}

object BroadcastNotificationParser {
    const val ACTION_OPEN_BROADCAST = "com.novamusic.player.ACTION_OPEN_BROADCAST"
    const val EXTRA_BROADCAST_DEEP_LINK = "extra_broadcast_deep_link"

    private val DEEP_LINK_KEYS = listOf(
        "deepLink",
        "deep_link",
        "deeplink",
        "route",
        "screen",
        "link",
        "url"
    )

    private val IMAGE_KEYS = listOf(
        "imageUrl",
        "image_url",
        "image",
        "picture",
        "bannerUrl",
        "banner_url"
    )

    private val TITLE_KEYS = listOf("title", "notificationTitle", "heading")
    private val BODY_KEYS = listOf("body", "message", "text", "description", "shortDescription")
    private val DEDUPE_KEYS = listOf("notificationId", "notification_id", "broadcastId", "broadcast_id", "id")

    fun parseRemotePayload(
        data: Map<String, String>,
        messageId: String? = null,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        notificationImageUrl: String? = null
    ): BroadcastNotificationPayload? {
        val title = TITLE_KEYS.firstNotNullOfOrNull { key ->
            data[key]?.trim()?.takeIf { it.isNotBlank() }
        } ?: notificationTitle?.trim()?.takeIf { it.isNotBlank() }
        val body = BODY_KEYS.firstNotNullOfOrNull { key ->
            data[key]?.trim()?.takeIf { it.isNotBlank() }
        } ?: notificationBody?.trim()?.takeIf { it.isNotBlank() }

        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            return null
        }

        val resolvedTitle = title ?: "NOVA MUSIC"
        val resolvedBody = body ?: ""

        val rawImage = IMAGE_KEYS.firstNotNullOfOrNull { key ->
            data[key]?.trim()?.takeIf { it.isNotBlank() }
        } ?: notificationImageUrl?.trim()?.takeIf { it.isNotBlank() }
        val safeImageUrl = rawImage?.takeIf { isValidHttpsUrl(it) }

        val rawDeepLink = extractDeepLinkFromMap(data)
        val dedupeKey = messageId?.trim()?.takeIf { it.isNotBlank() }
            ?: DEDUPE_KEYS.firstNotNullOfOrNull { key -> data[key]?.trim()?.takeIf { it.isNotBlank() } }

        return BroadcastNotificationPayload(
            dedupeKey = dedupeKey,
            title = resolvedTitle,
            body = resolvedBody,
            imageUrl = safeImageUrl,
            deepLink = rawDeepLink
        )
    }

    fun extractDeepLinkFromMap(data: Map<String, String>): String? {
        return DEEP_LINK_KEYS.firstNotNullOfOrNull { key ->
            data[key]?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    fun resolveDeepLinkTarget(rawDeepLink: String?): BroadcastDeepLinkTarget? {
        val trimmed = rawDeepLink?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val lower = trimmed.lowercase()

        if (lower.startsWith("https://")) {
            return if (isValidHttpsUrl(trimmed)) {
                BroadcastDeepLinkTarget.ExternalHttps(trimmed)
            } else {
                null
            }
        }

        val normalizedPath = when {
            lower.startsWith("novamusic://") -> {
                val uri = runCatching { URI(trimmed) }.getOrNull() ?: return BroadcastDeepLinkTarget.OpenAppHome
                val host = uri.host?.trim().orEmpty()
                val path = uri.path?.trim('/').orEmpty()
                when {
                    host.equals("open", ignoreCase = true) && path.isNotBlank() -> path
                    host.isNotBlank() && path.isNotBlank() -> "$host/$path"
                    host.isNotBlank() -> host
                    path.isNotBlank() -> path
                    else -> ""
                }
            }
            else -> trimmed.trim('/')
        }

        val route = when (normalizedPath.lowercase()) {
            "", "home" -> return BroadcastDeepLinkTarget.OpenAppHome
            "library" -> NovaDestinations.LIBRARY
            "search" -> NovaDestinations.SEARCH
            "playlists" -> NovaDestinations.PLAYLISTS
            "favorites" -> NovaDestinations.FAVORITES_DETAIL
            "wrapped", "nova-wrapped", "nova_wrapped" -> NovaDestinations.WRAPPED
            "equalizer", "eq" -> NovaDestinations.EQUALIZER
            "settings" -> NovaDestinations.SETTINGS
            "updates", "update", "settings/updates" -> NovaDestinations.SETTINGS_UPDATES
            "notifications", "settings/notifications" -> NovaDestinations.SETTINGS_NOTIFICATIONS
            "appearance", "settings/appearance" -> NovaDestinations.SETTINGS_APPEARANCE
            "playback", "settings/playback" -> NovaDestinations.SETTINGS_PLAYBACK
            "privacy", "settings/privacy" -> NovaDestinations.SETTINGS_PRIVACY
            "about" -> NovaDestinations.ABOUT
            else -> {
                val segments = normalizedPath.split('/')
                when {
                    segments.size == 2 && segments[0].equals("album", ignoreCase = true) -> {
                        segments[1].toLongOrNull()?.let { NovaDestinations.albumDetail(it) }
                    }
                    segments.size == 2 && segments[0].equals("playlist", ignoreCase = true) -> {
                        segments[1].toLongOrNull()?.let { NovaDestinations.playlistDetail(it) }
                    }
                    segments.size == 2 && segments[0].equals("artist", ignoreCase = true) -> {
                        NovaDestinations.artistDetail(segments[1])
                    }
                    segments.size == 2 && segments[0].equals("genre", ignoreCase = true) -> {
                        NovaDestinations.genreDetail(segments[1])
                    }
                    else -> null
                }
            }
        }

        return if (route != null) {
            BroadcastDeepLinkTarget.InAppRoute(route)
        } else {
            BroadcastDeepLinkTarget.OpenAppHome
        }
    }

    fun extractDeepLinkFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        intent.getStringExtra(EXTRA_BROADCAST_DEEP_LINK)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        intent.dataString?.trim()?.takeIf { it.startsWith("novamusic://", ignoreCase = true) }?.let { return it }
        for (key in DEEP_LINK_KEYS) {
            val value = intent.getStringExtra(key)?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    fun clearConsumedDeepLinkExtras(intent: Intent?) {
        if (intent == null) return
        intent.removeExtra(EXTRA_BROADCAST_DEEP_LINK)
        for (key in DEEP_LINK_KEYS) {
            intent.removeExtra(key)
        }
        if (intent.action == ACTION_OPEN_BROADCAST) {
            intent.action = null
        }
        if (intent.data?.scheme.equals("novamusic", ignoreCase = true)) {
            intent.data = null
        }
    }

    fun isValidHttpsUrl(url: String): Boolean {
        val parsed = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        return parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.isNullOrBlank()
    }
}
