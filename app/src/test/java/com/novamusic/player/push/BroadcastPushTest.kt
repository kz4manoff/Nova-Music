package com.novamusic.player.push

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastPushTest {

    private class FakeRegistrationStore(
        var deviceId: String = "device-uuid-001",
        var syncedToken: String = "",
        var syncedUrl: String = "",
        var syncedVersionCode: Int = 0
    ) : BroadcastRegistrationStore {
        override suspend fun getOrCreateDeviceId(): String = deviceId
        override suspend fun getLastSyncedFcmToken(): String = syncedToken
        override suspend fun getLastSyncedEndpointUrl(): String = syncedUrl
        override suspend fun getLastSyncedAppVersionCode(): Int = syncedVersionCode

        override suspend fun recordSynced(
            token: String,
            endpointUrl: String,
            versionCode: Int,
            syncedAtMs: Long
        ) {
            syncedToken = token
            syncedUrl = endpointUrl
            syncedVersionCode = versionCode
        }
    }

    private class FakeBackendClient(
        var nextResult: BroadcastRegistrationResult = BroadcastRegistrationResult.Success(1_700_000_000_000L)
    ) : BroadcastBackendClient {
        val requests = mutableListOf<Triple<String, String, BroadcastRegistrationPayload>>()

        override suspend fun registerDevice(
            endpointUrl: String,
            clientKey: String,
            payload: BroadcastRegistrationPayload
        ): BroadcastRegistrationResult {
            requests.add(Triple(endpointUrl, clientKey, payload))
            return nextResult
        }
    }

    private class FakeRetryScheduler : BroadcastRetryScheduler {
        var scheduleCalls: Int = 0
        override fun scheduleRetry() {
            scheduleCalls++
        }
    }

    @Test
    fun `registerDeviceToken refuses blank or non-https endpoint and never simulates success`() = runBlocking {
        val store = FakeRegistrationStore()
        val client = FakeBackendClient()
        val retry = FakeRetryScheduler()

        val unconfiguredRegistrar = BroadcastDeviceRegistrar(
            store = store,
            backendClient = client,
            retryScheduler = retry,
            endpointProvider = { "" },
            clientKeyProvider = { "" },
            notificationsEnabledProvider = { true }
        )

        val emptyResult = unconfiguredRegistrar.registerDeviceToken("fcm-token-1")
        assertTrue(emptyResult is BroadcastRegistrationResult.NotConfigured)
        assertTrue(client.requests.isEmpty())
        assertEquals("", store.syncedToken)

        val httpRegistrar = BroadcastDeviceRegistrar(
            store = store,
            backendClient = client,
            retryScheduler = retry,
            endpointProvider = { "http://insecure.example.com/api/devices/register" },
            clientKeyProvider = { "" },
            notificationsEnabledProvider = { true }
        )
        val insecureResult = httpRegistrar.registerDeviceToken("fcm-token-1")
        assertTrue(insecureResult is BroadcastRegistrationResult.NotConfigured)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun `registerDeviceToken deduplicates same token and re-syncs cleanly when token rotates`() = runBlocking {
        val store = FakeRegistrationStore(deviceId = "install-abc")
        val client = FakeBackendClient()
        val retry = FakeRetryScheduler()
        val endpoint = "https://broadcast.example.com/api/devices/register"

        val registrar = BroadcastDeviceRegistrar(
            store = store,
            backendClient = client,
            retryScheduler = retry,
            endpointProvider = { endpoint },
            clientKeyProvider = { "public-client-key" },
            packageNameProvider = { "com.novamusic.player" },
            versionNameProvider = { "1.2.3" },
            versionCodeProvider = { 23 },
            sdkIntProvider = { 34 },
            deviceModelProvider = { "Pixel 8" },
            manufacturerProvider = { "Google" },
            localeProvider = { "en-US" },
            notificationsEnabledProvider = { true }
        )

        // Initial registration succeeds and persists synced state
        val first = registrar.registerDeviceToken("token-v1")
        assertTrue(first is BroadcastRegistrationResult.Success)
        assertEquals(1, client.requests.size)
        assertEquals("install-abc", client.requests[0].third.deviceId)
        assertEquals("token-v1", client.requests[0].third.fcmToken)
        assertEquals("com.novamusic.player", client.requests[0].third.packageName)
        assertEquals("token-v1", store.syncedToken)

        // Repeated call with unchanged token & version is skipped cleanly
        val repeat = registrar.registerDeviceToken("token-v1")
        assertTrue(repeat is BroadcastRegistrationResult.AlreadySynced)
        assertEquals(1, client.requests.size)

        // Token rotation updates same deviceId without creating duplicate device records
        val rotated = registrar.registerDeviceToken("token-v2", force = true)
        assertTrue(rotated is BroadcastRegistrationResult.Success)
        assertEquals(2, client.requests.size)
        assertEquals("install-abc", client.requests[1].third.deviceId)
        assertEquals("token-v2", client.requests[1].third.fcmToken)
        assertEquals("token-v2", store.syncedToken)
    }

    @Test
    fun `retryable registration failure schedules retry and does not mark token synced`() = runBlocking {
        val store = FakeRegistrationStore()
        val client = FakeBackendClient(
            nextResult = BroadcastRegistrationResult.RetryableFailure("Network offline")
        )
        val retry = FakeRetryScheduler()

        val registrar = BroadcastDeviceRegistrar(
            store = store,
            backendClient = client,
            retryScheduler = retry,
            endpointProvider = { "https://broadcast.example.com/api/devices/register" },
            clientKeyProvider = { "" },
            notificationsEnabledProvider = { true }
        )

        val result = registrar.registerDeviceToken("token-offline")
        assertTrue(result is BroadcastRegistrationResult.RetryableFailure)
        assertEquals(1, retry.scheduleCalls)
        assertEquals("", store.syncedToken)
    }

    @Test
    fun `broadcast notification payload parses title body image deepLink and rejects insecure images`() {
        assertEquals("nova_broadcast", NovaBroadcastNotificationHelper.CHANNEL_ID)
        assertEquals("NOVA Broadcast", NovaBroadcastNotificationHelper.CHANNEL_NAME)

        val payload = BroadcastNotificationParser.parseRemotePayload(
            data = mapOf(
                "title" to "NOVA Wrapped Is Here",
                "body" to "See your top tracks of the year.",
                "imageUrl" to "https://cdn.example.com/wrapped-banner.png",
                "deepLink" to "novamusic://wrapped",
                "broadcastId" to "bc-2026-01"
            ),
            messageId = "msg-900"
        )
        assertNotNull(payload)
        assertEquals("NOVA Wrapped Is Here", payload!!.title)
        assertEquals("See your top tracks of the year.", payload.body)
        assertEquals("https://cdn.example.com/wrapped-banner.png", payload.imageUrl)
        assertEquals("novamusic://wrapped", payload.deepLink)
        assertEquals("msg-900", payload.dedupeKey)

        // Insecure http:// image URL is discarded gracefully
        val insecureImgPayload = BroadcastNotificationParser.parseRemotePayload(
            data = mapOf(
                "title" to "Announcement",
                "body" to "Tap to open library.",
                "imageUrl" to "http://insecure.example.com/banner.png"
            )
        )
        assertNotNull(insecureImgPayload)
        assertNull(insecureImgPayload!!.imageUrl)
    }

    @Test
    fun `deep link resolver maps in-app routes novamusic URIs and external https links cleanly`() {
        val wrapped = BroadcastNotificationParser.resolveDeepLinkTarget("novamusic://wrapped")
        assertEquals(BroadcastDeepLinkTarget.InAppRoute("wrapped"), wrapped)

        val album = BroadcastNotificationParser.resolveDeepLinkTarget("novamusic://album/42")
        assertEquals(BroadcastDeepLinkTarget.InAppRoute("album/42"), album)

        val updates = BroadcastNotificationParser.resolveDeepLinkTarget("updates")
        assertEquals(BroadcastDeepLinkTarget.InAppRoute("settings/updates"), updates)

        val external = BroadcastNotificationParser.resolveDeepLinkTarget("https://nova-website-1tg.pages.dev/changelog")
        assertEquals(
            BroadcastDeepLinkTarget.ExternalHttps("https://nova-website-1tg.pages.dev/changelog"),
            external
        )
    }
}
