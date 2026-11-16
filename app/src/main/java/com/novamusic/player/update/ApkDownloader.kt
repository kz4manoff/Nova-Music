package com.novamusic.player.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

interface UpdateNetworkClient {
    fun isNetworkAvailable(): Boolean
    suspend fun fetchManifestJson(url: String): String
    suspend fun downloadApk(
        url: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): File
}

class AndroidUpdateNetworkClient(
    private val context: Context
) : UpdateNetworkClient {

    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override suspend fun fetchManifestJson(url: String): String = withContext(Dispatchers.IO) {
        if (!UpdateEvaluator.isTrustedUpdateConfigUrl(url)) {
            throw SecurityException("Update manifest URL must use HTTPS.")
        }
        val conn = openSecureConnection(url, allowRedirects = false)
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException("Update server returned HTTP $code")
            }
            val contentType = conn.contentType?.lowercase().orEmpty()
            if (contentType.contains("text/html")) {
                throw IOException("Update manifest not found at $url (received HTML page instead of JSON).")
            }
            val contentLength = conn.contentLengthLong
            if (contentLength > UpdateConfig.MAX_MANIFEST_BYTES) {
                throw IOException("Update manifest exceeds maximum allowed size.")
            }
            conn.inputStream.buffered().use { input ->
                val buffer = ByteArray(8 * 1024)
                val out = java.io.ByteArrayOutputStream()
                var total = 0
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > UpdateConfig.MAX_MANIFEST_BYTES) {
                        throw IOException("Update manifest exceeded size limit.")
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun downloadApk(
        url: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!UpdateEvaluator.isTrustedApkUrl(url)) {
            throw SecurityException("Blocked untrusted APK download URL.")
        }

        val parentDir = destinationFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        // Remove stale APKs / partial files before starting a fresh download
        parentDir?.listFiles()?.forEach { child ->
            if (child.name.endsWith(".apk") || child.name.endsWith(".part")) {
                child.delete()
            }
        }

        val partFile = File(destinationFile.parentFile, "${destinationFile.name}.part")
        var currentUrl = url
        var redirects = 0
        var activeConnection: HttpURLConnection? = null

        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                val conn = openSecureConnection(currentUrl, allowRedirects = false)
                val status = conn.responseCode
                if (status in listOf(
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        307,
                        308
                    )
                ) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        throw IOException("Redirect response missing Location header.")
                    }
                    redirects++
                    if (redirects > UpdateConfig.MAX_REDIRECTS) {
                        throw IOException("Too many redirects while downloading update.")
                    }
                    val resolved = URI(currentUrl).resolve(location).toString()
                    if (!UpdateEvaluator.isTrustedRedirectUrl(resolved)) {
                        throw SecurityException("Blocked redirect to untrusted host.")
                    }
                    currentUrl = resolved
                } else {
                    activeConnection = conn
                    break
                }
            }

            val conn = activeConnection ?: throw IOException("Failed to open download connection.")
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw IOException("Update APK not found on GitHub Releases (HTTP 404).")
            }
            if (code !in 200..299) {
                throw IOException("Download failed with server status HTTP $code.")
            }

            val totalBytes = conn.contentLengthLong
            if (totalBytes > UpdateConfig.MAX_APK_BYTES) {
                throw IOException("Update APK exceeds maximum allowed package size.")
            }

            var downloaded = 0L
            var lastReported = 0L
            onProgress(0L, totalBytes)

            conn.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded > UpdateConfig.MAX_APK_BYTES) {
                            throw IOException("Downloaded file exceeded maximum allowed APK size.")
                        }
                        if (downloaded - lastReported >= 64 * 1024 || downloaded == totalBytes) {
                            lastReported = downloaded
                            onProgress(downloaded, totalBytes)
                        }
                    }
                    output.fd.sync()
                }
            }

            if (downloaded <= 0L) {
                throw IOException("Downloaded APK file is empty.")
            }
            if (totalBytes > 0L && downloaded != totalBytes) {
                throw IOException("Download interrupted ($downloaded of $totalBytes bytes received).")
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            if (!partFile.renameTo(destinationFile)) {
                partFile.copyTo(destinationFile, overwrite = true)
                partFile.delete()
            }
            onProgress(downloaded, if (totalBytes > 0L) totalBytes else downloaded)
            destinationFile
        } catch (ce: CancellationException) {
            partFile.delete()
            throw ce
        } catch (t: Throwable) {
            partFile.delete()
            throw t
        } finally {
            activeConnection?.disconnect()
        }
    }

    private fun openSecureConnection(url: String, allowRedirects: Boolean): HttpURLConnection {
        val conn = (URL(url).openConnection() as? HttpURLConnection)
            ?: throw IOException("Invalid HTTP connection for update.")
        conn.instanceFollowRedirects = allowRedirects
        conn.connectTimeout = UpdateConfig.CONNECT_TIMEOUT_MS
        conn.readTimeout = UpdateConfig.READ_TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json, application/vnd.android.package-archive, */*")
        conn.setRequestProperty("User-Agent", "NOVA-MUSIC-Android-Updater")
        return conn
    }
}
