package com.novamusic.player.update

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class UpdateSystemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakePreferencesStore(
        var lastCheckMs: Long = 0L,
        var dismissedVersionCode: Int = 0,
        var autoCheckEnabled: Boolean = true,
        var wifiOnlyEnabled: Boolean = false
    ) : UpdatePreferencesStore {
        override suspend fun getLastUpdateCheckMs(): Long = lastCheckMs
        override suspend fun setLastUpdateCheckMs(timestampMs: Long) {
            lastCheckMs = timestampMs
        }

        override suspend fun getDismissedUpdateVersionCode(): Int = dismissedVersionCode
        override suspend fun setDismissedUpdateVersionCode(versionCode: Int) {
            dismissedVersionCode = versionCode
        }

        override suspend fun isAutoCheckUpdatesEnabled(): Boolean = autoCheckEnabled
        override suspend fun isWifiOnlyDownloadEnabled(): Boolean = wifiOnlyEnabled
    }

    private class FakeClock(var currentMs: Long = 1_700_000_000_000L) : UpdateClock {
        override fun nowMs(): Long = currentMs
    }

    private class FakeNetworkClient(
        var networkAvailable: Boolean = true,
        var unmeteredAvailable: Boolean = true,
        var manifestJsonResponse: Result<String> = Result.success("{}"),
        var downloadBehavior: (url: String, dest: File, onProgress: (Long, Long) -> Unit) -> File =
            { _, dest, onProgress ->
                dest.parentFile?.mkdirs()
                val payload = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00)
                dest.writeBytes(payload)
                onProgress(payload.size.toLong(), payload.size.toLong())
                dest
            }
    ) : UpdateNetworkClient {
        override fun isNetworkAvailable(): Boolean = networkAvailable
        override fun isUnmeteredNetworkAvailable(): Boolean = networkAvailable && unmeteredAvailable

        override suspend fun fetchManifestJson(url: String): String =
            manifestJsonResponse.getOrThrow()

        override suspend fun downloadApk(
            url: String,
            destinationFile: File,
            onProgress: (Long, Long) -> Unit
        ): File = downloadBehavior(url, destinationFile, onProgress)
    }

    private class FakeApkVerifier(
        var result: ApkVerificationResult = ApkVerificationResult.Valid(
            filePath = "/tmp/NOVA-MUSIC.apk",
            sha256Hex = SAMPLE_SHA256,
            archiveVersionCode = 3L
        )
    ) : ApkVerifier {
        var verifyCalls: Int = 0

        override suspend fun verifyDownloadedApk(
            apkFile: File,
            expectedConfig: RemoteUpdateConfig,
            installedVersionCode: Int
        ): ApkVerificationResult {
            verifyCalls++
            return result
        }
    }

    private class FakeApkInstaller(
        var canInstall: Boolean = true,
        var launchSucceeds: Boolean = true
    ) : ApkInstaller {
        val launchedFiles = mutableListOf<File>()

        override fun canRequestPackageInstalls(): Boolean = canInstall

        override fun createUnknownSourcesPermissionIntent(): Intent = Intent()

        override fun launchSystemInstaller(apkFile: File): Boolean {
            if (!launchSucceeds) return false
            launchedFiles.add(apkFile)
            return true
        }
    }

    private fun createManager(
        prefs: FakePreferencesStore = FakePreferencesStore(),
        network: FakeNetworkClient = FakeNetworkClient(),
        verifier: FakeApkVerifier = FakeApkVerifier(),
        installer: FakeApkInstaller = FakeApkInstaller(),
        clock: FakeClock = FakeClock(),
        installedVersionCode: Int = 2,
        installedVersionName: String = "1.0.1"
    ): UpdateManager {
        return UpdateManager(
            preferencesStore = prefs,
            updatesDir = tempFolder.newFolder("updates-${System.nanoTime()}"),
            networkClient = network,
            apkVerifier = verifier,
            apkInstaller = installer,
            installedVersionCode = installedVersionCode,
            installedVersionName = installedVersionName,
            updateJsonUrl = "https://novamusic.example.com/update.json",
            clock = clock,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
    }

    private fun validManifestJson(
        version: String = "1.0.2",
        versionCode: Int = 3,
        apkUrl: String = OFFICIAL_APK_URL,
        sha256: String = SAMPLE_SHA256,
        mandatory: Boolean = false,
        minSupported: Int = 1
    ): String {
        return """
            {
              "latestVersion": "$version",
              "versionCode": $versionCode,
              "apkUrl": "$apkUrl",
              "sha256": "$sha256",
              "releaseNotes": "Performance improvements and bug fixes.",
              "mandatoryUpdate": $mandatory,
              "minimumSupportedVersion": $minSupported
            }
        """.trimIndent()
    }

    // 1. No update available (equal or older remote versionCode)
    @Test
    fun `no update available when remote versionCode is equal or older than installed`() = runBlocking {
        val network = FakeNetworkClient(
            manifestJsonResponse = Result.success(validManifestJson(version = "1.0.1", versionCode = 2))
        )
        val manager = createManager(network = network, installedVersionCode = 2)

        manager.checkForUpdatesNow(manual = true)
        val equalState = manager.uiState.value as UpdateUiState.UpToDate
        assertEquals("1.0.1", equalState.installedVersionName)
        assertTrue(equalState.showConfirmationDialog)

        // Older remote version (downgrade protection)
        network.manifestJsonResponse = Result.success(validManifestJson(version = "1.0.0", versionCode = 1))
        manager.checkForUpdatesNow(manual = false)
        val olderState = manager.uiState.value as UpdateUiState.UpToDate
        assertFalse(olderState.showConfirmationDialog)
    }

    // 2. Optional update available & dismissible with Later persistence
    @Test
    fun `optional update is shown and dismissDialog remembers versionCode without losing state`() = runBlocking {
        val prefs = FakePreferencesStore()
        val network = FakeNetworkClient(
            manifestJsonResponse = Result.success(validManifestJson(versionCode = 3, mandatory = false))
        )
        val manager = createManager(prefs = prefs, network = network, installedVersionCode = 2)

        manager.checkForUpdatesNow(manual = false)
        val available = manager.uiState.value as UpdateUiState.Available
        assertFalse(available.isMandatory)
        assertTrue(available.dialogVisible)

        manager.dismissDialog()
        val afterDismiss = manager.uiState.value as UpdateUiState.Available
        assertFalse(afterDismiss.dialogVisible)
        assertEquals(3, prefs.dismissedVersionCode)

        // Subsequent background 24h auto-check stays quiet for dismissed optional version
        manager.checkForUpdatesNow(manual = false)
        val rechecked = manager.uiState.value as UpdateUiState.Available
        assertFalse(rechecked.dialogVisible)
    }

    // 3. Mandatory update via flag or minimumSupportedVersion
    @Test
    fun `mandatory update activates when mandatoryUpdate is true or below minimumSupportedVersion and cannot be dismissed`() = runBlocking {
        val network = FakeNetworkClient(
            manifestJsonResponse = Result.success(
                validManifestJson(versionCode = 3, mandatory = false, minSupported = 3)
            )
        )
        val manager = createManager(network = network, installedVersionCode = 2)

        manager.checkForUpdatesNow(manual = false)
        val available = manager.uiState.value as UpdateUiState.Available
        assertTrue(available.isMandatory)
        assertTrue(available.dialogVisible)

        // Later / dismiss has no effect on mandatory updates
        manager.dismissDialog()
        assertTrue((manager.uiState.value as UpdateUiState.Available).dialogVisible)
    }

    // 4. Successful APK download + SHA-256 verification
    @Test
    fun `successful download tracks progress and verifies SHA-256 before ready to install`() = runBlocking {
        val apkBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + "nova-apk-payload".toByteArray()
        val expectedSha = UpdateEvaluator.computeSha256Hex(apkBytes)
        assertTrue(UpdateEvaluator.isZipMagicHeader(apkBytes))
        assertTrue(UpdateEvaluator.isValidSha256(expectedSha))

        val config = RemoteUpdateConfig(
            latestVersion = "1.0.2",
            versionCode = 3,
            apkUrl = OFFICIAL_APK_URL,
            sha256 = expectedSha,
            releaseNotes = "Audio fixes",
            mandatoryUpdate = false,
            minimumSupportedVersion = 1
        )
        val network = FakeNetworkClient(
            downloadBehavior = { _, dest, onProgress ->
                dest.parentFile?.mkdirs()
                dest.writeBytes(apkBytes)
                onProgress(apkBytes.size.toLong(), apkBytes.size.toLong())
                dest
            }
        )
        val verifier = FakeApkVerifier(
            result = ApkVerificationResult.Valid(
                filePath = "ok",
                sha256Hex = expectedSha,
                archiveVersionCode = 3L
            )
        )
        val installer = FakeApkInstaller(canInstall = true, launchSucceeds = true)
        val manager = createManager(network = network, verifier = verifier, installer = installer)

        manager.downloadAndVerifyNow(config, isMandatory = false)
        assertEquals(1, verifier.verifyCalls)
        val ready = manager.uiState.value as UpdateUiState.ReadyToInstall
        assertFalse(ready.requiresInstallPermission)
        assertEquals(1, installer.launchedFiles.size)
    }

    // 5. Interrupted download recovers cleanly with retryable Error state
    @Test
    fun `interrupted download emits retryable error state without crashing`() = runBlocking {
        val config = RemoteUpdateConfig(
            latestVersion = "1.0.2",
            versionCode = 3,
            apkUrl = OFFICIAL_APK_URL,
            sha256 = SAMPLE_SHA256,
            releaseNotes = "Fixes",
            mandatoryUpdate = false,
            minimumSupportedVersion = 1
        )
        val network = FakeNetworkClient(
            downloadBehavior = { _, dest, onProgress ->
                dest.parentFile?.mkdirs()
                onProgress(2048L, 10_000L)
                throw IOException("Connection reset by peer")
            }
        )
        val verifier = FakeApkVerifier()
        val manager = createManager(network = network, verifier = verifier)

        manager.downloadAndVerifyNow(config, isMandatory = false)
        assertEquals(0, verifier.verifyCalls)
        val error = manager.uiState.value as UpdateUiState.Error
        assertTrue(error.canRetryDownload)
        assertTrue(error.message.contains("Connection reset"))
        assertEquals(config, error.config)
    }

    // 6. Invalid APK, invalid JSON, missing APK (404), and untrusted URLs rejected
    @Test
    fun `invalid JSON untrusted download URL and invalid APK checksum are safely rejected`() = runBlocking {
        // Invalid JSON
        val badJson = UpdateEvaluator.parseUpdateConfigJson("{ invalid json")
        assertTrue(badJson.isFailure)

        // Untrusted non-GitHub / non-HTTPS URLs rejected
        assertFalse(UpdateEvaluator.isTrustedApkUrl("http://github.com/kz4manoff/Nova-Music/releases/download/v1.0.2/NOVA-MUSIC.apk"))
        assertFalse(UpdateEvaluator.isTrustedApkUrl("https://evil.example.com/NOVA-MUSIC.apk"))
        assertFalse(UpdateEvaluator.isTrustedApkUrl("https://github.com/otheruser/otherrepo/releases/download/v1.0.2/NOVA-MUSIC.apk"))
        assertTrue(UpdateEvaluator.isTrustedApkUrl(OFFICIAL_APK_URL))
        assertTrue(UpdateEvaluator.isTrustedRedirectUrl("https://objects.githubusercontent.com/some-release-asset"))
        assertFalse(UpdateEvaluator.isTrustedRedirectUrl("https://untrusted-cdn.example.com/payload.apk"))

        // Non-ZIP corrupt APK bytes rejected by magic header & SHA-256 check
        assertFalse(UpdateEvaluator.isZipMagicHeader("<html>404 Not Found</html>".toByteArray()))

        // UpdateEvaluator rejects untrusted url inside JSON manifest
        val manager = createManager(
            network = FakeNetworkClient(
                manifestJsonResponse = Result.success(
                    validManifestJson(apkUrl = "https://evil.example.com/NOVA-MUSIC.apk")
                )
            )
        )
        manager.checkForUpdatesNow(manual = true)
        assertTrue(manager.uiState.value is UpdateUiState.Error)
    }

    // 7. Signature mismatch rejected
    @Test
    fun `signature mismatch fails verification and never launches system installer`() = runBlocking {
        assertFalse(
            UpdateEvaluator.signaturesMatch(
                installedCertSha256s = setOf("a".repeat(64)),
                archiveCertSha256s = setOf("b".repeat(64))
            )
        )
        assertTrue(
            UpdateEvaluator.signaturesMatch(
                installedCertSha256s = setOf("A".repeat(64)),
                archiveCertSha256s = setOf("a".repeat(64))
            )
        )

        val config = RemoteUpdateConfig(
            latestVersion = "1.0.2",
            versionCode = 3,
            apkUrl = OFFICIAL_APK_URL,
            sha256 = SAMPLE_SHA256,
            releaseNotes = "Fixes",
            mandatoryUpdate = false,
            minimumSupportedVersion = 1
        )
        val verifier = FakeApkVerifier(
            result = ApkVerificationResult.Invalid("Security check failed: APK signing certificate does not match.")
        )
        val installer = FakeApkInstaller(canInstall = true)
        val manager = createManager(verifier = verifier, installer = installer)

        manager.downloadAndVerifyNow(config, isMandatory = false)
        val error = manager.uiState.value as UpdateUiState.Error
        assertFalse(error.canRetryDownload)
        assertTrue(error.message.contains("signing certificate", ignoreCase = true))
        assertTrue(installer.launchedFiles.isEmpty())
    }

    // 8. No internet connection / server error never crashes
    @Test
    fun `offline and server error states never crash and differentiate manual vs automatic checks`() = runBlocking {
        val network = FakeNetworkClient(networkAvailable = false)
        val manager = createManager(network = network)

        // Automatic check when offline stays quietly Idle
        manager.checkForUpdatesNow(manual = false)
        assertEquals(UpdateUiState.Idle, manager.uiState.value)

        // Manual check when offline shows user-friendly Error
        manager.checkForUpdatesNow(manual = true)
        val offlineError = manager.uiState.value as UpdateUiState.Error
        assertTrue(offlineError.message.contains("No internet connection"))

        // Server 500 error during auto check stays quietly Idle
        network.networkAvailable = true
        network.manifestJsonResponse = Result.failure(IOException("Update server returned HTTP 500"))
        manager.checkForUpdatesNow(manual = false)
        assertEquals(UpdateUiState.Idle, manager.uiState.value)
    }

    // 9. Install permission + official Android installer flow
    @Test
    fun `install flow requests unknown sources permission when missing and launches installer once granted`() = runBlocking {
        val config = RemoteUpdateConfig(
            latestVersion = "1.0.2",
            versionCode = 3,
            apkUrl = OFFICIAL_APK_URL,
            sha256 = SAMPLE_SHA256,
            releaseNotes = "Fixes",
            mandatoryUpdate = false,
            minimumSupportedVersion = 1
        )
        val installer = FakeApkInstaller(canInstall = false, launchSucceeds = true)
        val manager = createManager(installer = installer)

        manager.downloadAndVerifyNow(config, isMandatory = false)
        val waitingPermission = manager.uiState.value as UpdateUiState.ReadyToInstall
        assertTrue(waitingPermission.requiresInstallPermission)
        assertTrue(installer.launchedFiles.isEmpty())

        // User enables "Allow from this source" in Android Settings and returns
        installer.canInstall = true
        manager.onInstallPermissionReturned()
        val ready = manager.uiState.value as UpdateUiState.ReadyToInstall
        assertFalse(ready.requiresInstallPermission)
        assertEquals(1, installer.launchedFiles.size)
    }

    // 10. 24-hour check interval & existing app data preservation
    @Test
    fun `periodic check respects 24 hour interval and preserves existing preferences`() = runBlocking {
        val now = 1_700_000_000_000L
        assertTrue(UpdateEvaluator.shouldAutoCheck(nowMs = now, lastCheckMs = 0L))
        assertFalse(UpdateEvaluator.shouldAutoCheck(nowMs = now, lastCheckMs = now - 86_399_999L))
        assertTrue(UpdateEvaluator.shouldAutoCheck(nowMs = now, lastCheckMs = now - 86_400_000L))

        val prefs = FakePreferencesStore(lastCheckMs = now - 1_000L, dismissedVersionCode = 99)
        val clock = FakeClock(currentMs = now)
        val network = FakeNetworkClient(
            manifestJsonResponse = Result.success(validManifestJson(versionCode = 3))
        )
        val manager = createManager(prefs = prefs, network = network, clock = clock)

        // Within 24h window -> skipped, nothing touched
        manager.checkIfIntervalElapsed()
        assertEquals(UpdateUiState.Idle, manager.uiState.value)
        assertEquals(99, prefs.dismissedVersionCode)

        // Advance clock by 24h -> runs check and updates timestamp while preserving dismissedVersionCode for unrelated build
        clock.currentMs = now + UpdateConfig.CHECK_INTERVAL_MS
        manager.checkIfIntervalElapsed()
        assertTrue(manager.uiState.value is UpdateUiState.Available)
        assertEquals(clock.currentMs, prefs.lastCheckMs)
        assertEquals(99, prefs.dismissedVersionCode)
    }

    // 11. Compatibility with Nova-Website live update.json schema (versionName + mandatory + versionCode=0 placeholder)
    @Test
    fun `website schema with versionName mandatory and versionCode zero placeholder parses and evaluates safely`() = runBlocking {
        // Exact placeholder currently hosted on Cloudflare Pages
        val placeholderJson = """
            {
              "versionName": "0.0.0-placeholder",
              "versionCode": 0,
              "apkUrl": "REPLACE_WITH_GITHUB_RELEASE_APK_URL",
              "releaseNotes": "Placeholder update manifest.",
              "mandatory": false
            }
        """.trimIndent()
        val network = FakeNetworkClient(manifestJsonResponse = Result.success(placeholderJson))
        val manager = createManager(network = network, installedVersionCode = 2)

        manager.checkForUpdatesNow(manual = true)
        val upToDate = manager.uiState.value as UpdateUiState.UpToDate
        assertEquals(2, upToDate.installedVersionCode)

        // Real update using versionName + mandatory=true
        val websiteUpdateJson = """
            {
              "versionName": "v1.0.2",
              "versionCode": 3,
              "apkUrl": "$OFFICIAL_APK_URL",
              "sha256": "$SAMPLE_SHA256",
              "releaseNotes": "Audio engine polish.",
              "mandatory": true
            }
        """.trimIndent()
        network.manifestJsonResponse = Result.success(websiteUpdateJson)
        manager.checkForUpdatesNow(manual = true)
        val available = manager.uiState.value as UpdateUiState.Available
        assertEquals("1.0.2", available.config.latestVersion)
        assertEquals(3, available.config.versionCode)
        assertTrue(available.isMandatory)
    }

    // 12. Missing required fields always fail explicitly instead of being silently ignored
    @Test
    fun `missing required manifest fields fail with explicit field errors`() = runBlocking {
        val missingVersion = """{"versionCode": 3, "apkUrl": "$OFFICIAL_APK_URL"}"""
        val err1 = UpdateEvaluator.parseUpdateConfigJson(missingVersion).exceptionOrNull()
        assertTrue(err1?.message?.contains("latestVersion") == true)

        val missingCode = """{"versionName": "1.0.2", "apkUrl": "$OFFICIAL_APK_URL"}"""
        val err2 = UpdateEvaluator.parseUpdateConfigJson(missingCode).exceptionOrNull()
        assertTrue(err2?.message?.contains("versionCode") == true)

        val missingApkUrl = """{"versionName": "1.0.2", "versionCode": 3}"""
        val err3 = UpdateEvaluator.parseUpdateConfigJson(missingApkUrl).exceptionOrNull()
        assertTrue(err3?.message?.contains("apkUrl") == true)
    }

    // 13. Installed version 1.0.3 (versionCode 4) must not offer update when manifest is 1.0.3 (4) or older
    @Test
    fun `installed version 1_0_3 reports up to date when remote versionCode is 4 or lower`() = runBlocking {
        val network = FakeNetworkClient(
            manifestJsonResponse = Result.success(
                validManifestJson(
                    version = "1.0.3",
                    versionCode = 4,
                    apkUrl = "https://github.com/kz4manoff/Nova-Music/releases/download/v1.0.3/NOVA-MUSIC-v1.0.3.apk"
                )
            )
        )
        val manager = createManager(
            network = network,
            installedVersionCode = 4,
            installedVersionName = "1.0.3"
        )

        manager.checkForUpdatesNow(manual = true)
        val upToDate = manager.uiState.value as UpdateUiState.UpToDate
        assertEquals(4, upToDate.installedVersionCode)
        assertEquals("1.0.3", upToDate.installedVersionName)
        assertTrue(upToDate.showConfirmationDialog)

        network.manifestJsonResponse = Result.success(validManifestJson(version = "1.0.2", versionCode = 3))
        manager.checkForUpdatesNow(manual = false)
        val olderRemote = manager.uiState.value as UpdateUiState.UpToDate
        assertEquals(4, olderRemote.installedVersionCode)
        assertEquals("1.0.3", olderRemote.installedVersionName)
        assertFalse(olderRemote.showConfirmationDialog)
    }

    // 14. Auto-check disabled and Wi-Fi-only download setting enforcement
    @Test
    fun `autoCheckUpdates disabled skips periodic check and wifiOnly blocks metered download`() = runBlocking {
        val prefs = FakePreferencesStore(
            lastCheckMs = 0L,
            autoCheckEnabled = false,
            wifiOnlyEnabled = true
        )
        val network = FakeNetworkClient(
            networkAvailable = true,
            unmeteredAvailable = false,
            manifestJsonResponse = Result.success(validManifestJson(version = "1.0.4", versionCode = 5))
        )
        val manager = createManager(
            prefs = prefs,
            network = network,
            installedVersionCode = 4,
            installedVersionName = "1.0.3"
        )

        // Periodic background check skipped when autoCheckEnabled is false
        manager.checkIfIntervalElapsed()
        assertEquals(UpdateUiState.Idle, manager.uiState.value)

        // Manual check still works
        manager.checkForUpdatesNow(manual = true)
        val available = manager.uiState.value as UpdateUiState.Available
        assertEquals(5, available.config.versionCode)

        // Wi-Fi only download refuses metered cellular network cleanly
        manager.downloadAndVerifyNow(available.config, isMandatory = false)
        val wifiError = manager.uiState.value as UpdateUiState.Error
        assertTrue(wifiError.canRetryDownload)
        assertTrue(wifiError.message.contains("Wi-Fi", ignoreCase = true))
    }

    // 15. Upgrade path 1.0.3 (4) -> 1.0.9 (10) and up-to-date check when 1.0.9 (10) is installed
    @Test
    fun `upgrade from 1_0_3 to 1_0_9 offers update and installed 1_0_9 reports up to date`() = runBlocking {
        val manifest109 = validManifestJson(
            version = "1.0.9",
            versionCode = 10,
            apkUrl = "https://github.com/kz4manoff/Nova-Music/releases/download/v1.0.9/NOVA-MUSIC-v1.0.9.apk"
        )
        assertTrue(
            UpdateEvaluator.isTrustedApkUrl(
                "https://github.com/kz4manoff/Nova-Music/releases/download/v1.0.9/NOVA-MUSIC-v1.0.9.apk"
            )
        )

        // Installed 1.0.3 (versionCode 4) detects 1.0.9 (versionCode 10)
        val legacyManager = createManager(
            network = FakeNetworkClient(manifestJsonResponse = Result.success(manifest109)),
            installedVersionCode = 4,
            installedVersionName = "1.0.3"
        )
        legacyManager.checkForUpdatesNow(manual = true)
        val available = legacyManager.uiState.value as UpdateUiState.Available
        assertEquals("1.0.9", available.config.latestVersion)
        assertEquals(10, available.config.versionCode)
        assertFalse(available.isMandatory)

        // Installed 1.0.9 (versionCode 10) reports UpToDate against 1.0.9 (10) and 1.0.3 (4)
        val network = FakeNetworkClient(manifestJsonResponse = Result.success(manifest109))
        val currentManager = createManager(
            network = network,
            installedVersionCode = 10,
            installedVersionName = "1.0.9"
        )
        currentManager.checkForUpdatesNow(manual = true)
        val upToDate = currentManager.uiState.value as UpdateUiState.UpToDate
        assertEquals(10, upToDate.installedVersionCode)
        assertEquals("1.0.9", upToDate.installedVersionName)
        assertTrue(upToDate.showConfirmationDialog)

        network.manifestJsonResponse = Result.success(
            validManifestJson(
                version = "1.0.3",
                versionCode = 4,
                apkUrl = "https://github.com/kz4manoff/Nova-Music/releases/download/v1.0.3/NOVA-MUSIC-v1.0.3.apk"
            )
        )
        currentManager.checkForUpdatesNow(manual = false)
        val olderRemote = currentManager.uiState.value as UpdateUiState.UpToDate
        assertEquals(10, olderRemote.installedVersionCode)
        assertEquals("1.0.9", olderRemote.installedVersionName)
        assertFalse(olderRemote.showConfirmationDialog)
    }

    private companion object {
        const val OFFICIAL_APK_URL =
            "https://github.com/kz4manoff/Nova-Music/releases/download/v1.0.2/NOVA-MUSIC.apk"
        const val SAMPLE_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
}
