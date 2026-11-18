package com.novamusic.player.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmUpdatePushTest {

    @Test
    fun `notification channel and branding constants match requirements`() {
        assertEquals("nova_updates", AndroidUpdateNotifier.CHANNEL_ID)
        assertEquals("NOVA Updates", AndroidUpdateNotifier.CHANNEL_NAME)
        assertEquals("New Update Available!", AndroidUpdateNotifier.NOTIFICATION_TITLE)
        assertEquals("Update Now", AndroidUpdateNotifier.ACTION_UPDATE_NOW_LABEL)
        assertEquals("nova_updates", PushUpdatePayload.UPDATES_TOPIC)
    }

    @Test
    fun `parseFcmDataPayload parses valid update payload and sanitizes version and description`() {
        val data = mapOf(
            "type" to "nova_update",
            "versionName" to "v1.2.2",
            "versionCode" to "23",
            "shortDescription" to "  Faster   Liquid Glass playback\nwith improved Bluetooth routing.  ",
            "releaseTitle" to "New Update Available!",
            "apkUrl" to "https://github.com/kz4manoff/Nova-Music/releases/download/v1.2.2/NOVA-MUSIC-v1.2.2.apk",
            "sha256" to "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789",
            "mandatoryUpdate" to "false"
        )

        val payload = PushUpdatePayload.parseFcmDataPayload(
            data = data,
            messageId = "msg-123"
        )
        assertNotNull(payload)
        assertEquals("1.2.2", payload!!.versionName)
        assertEquals(23, payload.versionCode)
        assertEquals(
            "Faster Liquid Glass playback with improved Bluetooth routing.",
            payload.shortDescription
        )
        assertEquals("msg-123", payload.messageId)
        assertFalse(payload.mandatoryUpdate)
    }

    @Test
    fun `parseFcmDataPayload rejects unrelated push type or invalid versionCode`() {
        val wrongType = PushUpdatePayload.parseFcmDataPayload(
            mapOf("type" to "promo", "versionName" to "1.2.2", "versionCode" to "23")
        )
        assertNull(wrongType)

        val invalidCode = PushUpdatePayload.parseFcmDataPayload(
            mapOf("type" to "nova_update", "versionName" to "1.2.2", "versionCode" to "0")
        )
        assertNull(invalidCode)

        val missingVersion = PushUpdatePayload.parseFcmDataPayload(
            mapOf("type" to "nova_update", "versionCode" to "23")
        )
        assertNull(missingVersion)
    }

    @Test
    fun `evaluateIncomingPush suppresses already installed, duplicate messageId, notified, and dismissed versions`() {
        val payload23 = PushUpdatePayload.parseFcmDataPayload(
            data = mapOf(
                "type" to "nova_update",
                "versionName" to "1.2.2",
                "versionCode" to "23",
                "shortDescription" to "Bug fixes."
            ),
            messageId = "msg-23"
        )!!

        // Already installed (23 <= 23)
        val installedSuppress = PushUpdatePayload.evaluateIncomingPush(
            payload = payload23,
            installedVersionCode = 23,
            lastNotifiedVersionCode = 0,
            dismissedNotificationVersionCode = 0,
            lastFcmMessageId = null
        )
        assertTrue(installedSuppress is PushDedupeDecision.Suppress)

        // Duplicate FCM messageId
        val duplicateMsgSuppress = PushUpdatePayload.evaluateIncomingPush(
            payload = payload23,
            installedVersionCode = 22,
            lastNotifiedVersionCode = 0,
            dismissedNotificationVersionCode = 0,
            lastFcmMessageId = "msg-23"
        )
        assertTrue(duplicateMsgSuppress is PushDedupeDecision.Suppress)

        // Already notified for versionCode 23
        val notifiedSuppress = PushUpdatePayload.evaluateIncomingPush(
            payload = payload23,
            installedVersionCode = 22,
            lastNotifiedVersionCode = 23,
            dismissedNotificationVersionCode = 0,
            lastFcmMessageId = "msg-23-retry"
        )
        assertTrue(notifiedSuppress is PushDedupeDecision.Suppress)

        // User swiped/dismissed notification for versionCode 23
        val dismissedSuppress = PushUpdatePayload.evaluateIncomingPush(
            payload = payload23,
            installedVersionCode = 22,
            lastNotifiedVersionCode = 0,
            dismissedNotificationVersionCode = 23,
            lastFcmMessageId = "msg-23-retry"
        )
        assertTrue(dismissedSuppress is PushDedupeDecision.Suppress)

        // Fresh newer update passes cleanly
        val showDecision = PushUpdatePayload.evaluateIncomingPush(
            payload = payload23,
            installedVersionCode = 22,
            lastNotifiedVersionCode = 21,
            dismissedNotificationVersionCode = 20,
            lastFcmMessageId = "msg-21"
        )
        assertTrue(showDecision is PushDedupeDecision.ShowNotification)
        val config = (showDecision as PushDedupeDecision.ShowNotification).config
        assertEquals("1.2.2", config.latestVersion)
        assertEquals(23, config.versionCode)
        assertTrue(UpdateEvaluator.isTrustedApkUrl(config.apkUrl))
    }

    @Test
    fun `update notification tap intent flags handle closed background and open states`() {
        // Explicit pending intent extra (foreground/data push or Update Now button)
        val fromPendingIntent = PushUpdatePayload.parseUpdateNavigationFlags(
            action = PushUpdatePayload.ACTION_OPEN_UPDATE_DETAILS,
            hasOpenUpdatesExtra = true,
            typeExtra = null,
            remoteVersionCodeFromExtra = 23,
            remoteVersionCodeFromFcmData = null
        )
        assertTrue(fromPendingIntent.shouldOpenUpdates)
        assertEquals(23, fromPendingIntent.remoteVersionCode)

        // System-tray FCM tap when app was closed or backgrounded (click_action + string data extras)
        val fromSystemTrayTap = PushUpdatePayload.parseUpdateNavigationFlags(
            action = PushUpdatePayload.ACTION_OPEN_UPDATE_DETAILS,
            hasOpenUpdatesExtra = false,
            typeExtra = PushUpdatePayload.TYPE_NOVA_UPDATE,
            remoteVersionCodeFromExtra = 0,
            remoteVersionCodeFromFcmData = "24"
        )
        assertTrue(fromSystemTrayTap.shouldOpenUpdates)
        assertEquals(24, fromSystemTrayTap.remoteVersionCode)

        // FCM payload delivered onto launcher MAIN intent when click_action falls back to default launcher
        val fromLauncherWithFcmData = PushUpdatePayload.parseUpdateNavigationFlags(
            action = "android.intent.action.MAIN",
            hasOpenUpdatesExtra = false,
            typeExtra = PushUpdatePayload.TYPE_NOVA_UPDATE,
            remoteVersionCodeFromExtra = 0,
            remoteVersionCodeFromFcmData = "25"
        )
        assertTrue(fromLauncherWithFcmData.shouldOpenUpdates)
        assertEquals(25, fromLauncherWithFcmData.remoteVersionCode)

        // Normal launcher open must not route to Updates screen
        val normalLaunch = PushUpdatePayload.parseUpdateNavigationFlags(
            action = "android.intent.action.MAIN",
            hasOpenUpdatesExtra = false,
            typeExtra = null,
            remoteVersionCodeFromExtra = 0,
            remoteVersionCodeFromFcmData = null
        )
        assertFalse(normalLaunch.shouldOpenUpdates)
        assertEquals(0, normalLaunch.remoteVersionCode)
    }
}
