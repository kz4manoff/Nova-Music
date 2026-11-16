package com.novamusic.player.update

import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

/**
 * Pure Kotlin evaluation, security validation, and JSON parsing for NOVA MUSIC updates.
 * Intentionally avoids Android framework types so all security & version logic is 100% JVM unit-testable.
 */
object UpdateEvaluator {

    private val SHA256_REGEX = Regex("^[0-9a-fA-F]{64}$")
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK\x03\x04"

    fun shouldAutoCheck(
        nowMs: Long,
        lastCheckMs: Long,
        intervalMs: Long = UpdateConfig.CHECK_INTERVAL_MS
    ): Boolean {
        if (lastCheckMs <= 0L) return true
        if (nowMs < lastCheckMs) return true
        return (nowMs - lastCheckMs) >= intervalMs
    }

    fun isTrustedUpdateConfigUrl(url: String): Boolean {
        val uri = parseHttpsUri(url) ?: return false
        return uri.host?.isNotBlank() == true
    }

    fun isTrustedApkUrl(url: String): Boolean {
        val uri = parseHttpsUri(url) ?: return false
        val host = uri.host?.lowercase(Locale.US) ?: return false
        if (host != UpdateConfig.ALLOWED_APK_HOST) return false
        if (uri.userInfo != null) return false

        val path = uri.rawPath ?: return false
        if (path.contains("..")) return false
        if (!path.startsWith(UpdateConfig.ALLOWED_APK_PATH_PREFIX)) return false
        if (!path.lowercase(Locale.US).endsWith(".apk")) return false
        return true
    }

    fun isTrustedRedirectUrl(url: String): Boolean {
        val uri = parseHttpsUri(url) ?: return false
        val host = uri.host?.lowercase(Locale.US) ?: return false
        if (uri.userInfo != null) return false
        return host in UpdateConfig.TRUSTED_DOWNLOAD_REDIRECT_HOSTS
    }

    fun isValidSha256(sha256: String): Boolean {
        return SHA256_REGEX.matches(sha256.trim())
    }

    fun isZipMagicHeader(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < ZIP_MAGIC.size) return false
        for (i in ZIP_MAGIC.indices) {
            if (headerBytes[i] != ZIP_MAGIC[i]) return false
        }
        return true
    }

    fun computeSha256Hex(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeSha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun signaturesMatch(
        installedCertSha256s: Set<String>,
        archiveCertSha256s: Set<String>
    ): Boolean {
        if (installedCertSha256s.isEmpty() || archiveCertSha256s.isEmpty()) return false
        val normalizedInstalled = installedCertSha256s.map { it.lowercase(Locale.US) }.toSet()
        val normalizedArchive = archiveCertSha256s.map { it.lowercase(Locale.US) }.toSet()
        return normalizedInstalled.intersect(normalizedArchive).isNotEmpty()
    }

    fun evaluateUpdate(
        installedVersionCode: Int,
        config: RemoteUpdateConfig
    ): UpdateEvaluationResult {
        if (config.versionCode <= installedVersionCode) {
            return UpdateEvaluationResult.NoUpdate(
                installedVersionCode = installedVersionCode,
                remoteVersionCode = config.versionCode
            )
        }
        if (config.latestVersion.isBlank()) {
            return UpdateEvaluationResult.InvalidConfiguration("Remote version name is missing.")
        }
        if (!isTrustedApkUrl(config.apkUrl)) {
            return UpdateEvaluationResult.InvalidConfiguration(
                "APK download URL is not from an official NOVA MUSIC GitHub Release over HTTPS."
            )
        }
        if (!isValidSha256(config.sha256)) {
            return UpdateEvaluationResult.InvalidConfiguration(
                "Update manifest is missing a valid SHA-256 integrity checksum."
            )
        }

        val mandatory = config.mandatoryUpdate || installedVersionCode < config.minimumSupportedVersion
        return UpdateEvaluationResult.UpdateAvailable(
            config = config.copy(sha256 = config.sha256.trim().lowercase(Locale.US)),
            isMandatory = mandatory
        )
    }

    fun parseUpdateConfigJson(rawJson: String): Result<RemoteUpdateConfig> {
        return runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("Empty update manifest.")
            }
            val parsed = JsonReader(trimmed).parseRootObject()

            val latestVersionRaw = when {
                parsed.containsKey("latestVersion") && parsed["latestVersion"] != null ->
                    parsed["latestVersion"] as? String
                        ?: throw IllegalArgumentException("Invalid 'latestVersion' field: expected string.")
                parsed.containsKey("versionName") && parsed["versionName"] != null ->
                    parsed["versionName"] as? String
                        ?: throw IllegalArgumentException("Invalid 'versionName' field: expected string.")
                else -> throw IllegalArgumentException("Missing required 'latestVersion' (or 'versionName') field.")
            }
            val latestVersion = latestVersionRaw.trim().removePrefix("v").removePrefix("V")
            if (latestVersion.isBlank()) {
                throw IllegalArgumentException("Missing required 'latestVersion' (or 'versionName') field.")
            }

            if (!parsed.containsKey("versionCode") || parsed["versionCode"] == null) {
                throw IllegalArgumentException("Missing required 'versionCode' field.")
            }
            val versionCodeNum = parsed["versionCode"] as? Number
                ?: throw IllegalArgumentException("Invalid 'versionCode' field: expected integer.")
            if (versionCodeNum is Double || versionCodeNum is Float) {
                throw IllegalArgumentException("Invalid 'versionCode' field: expected integer.")
            }
            val versionCode = versionCodeNum.toInt()
            if (versionCode < 0) {
                throw IllegalArgumentException("Invalid 'versionCode': $versionCode (must be 0 or greater).")
            }

            if (!parsed.containsKey("apkUrl") || parsed["apkUrl"] == null) {
                throw IllegalArgumentException("Missing required 'apkUrl' field.")
            }
            val apkUrlRaw = parsed["apkUrl"] as? String
                ?: throw IllegalArgumentException("Invalid 'apkUrl' field: expected string.")
            val apkUrl = apkUrlRaw.trim()
            if (apkUrl.isBlank()) {
                throw IllegalArgumentException("Missing required 'apkUrl' field.")
            }

            val sha256 = when (val rawSha = parsed["sha256"]) {
                null -> ""
                is String -> rawSha.trim()
                else -> throw IllegalArgumentException("Invalid 'sha256' field: expected string.")
            }

            val releaseNotes = when (val rawNotes = parsed["releaseNotes"]) {
                null -> "Bug fixes and performance improvements."
                is String -> rawNotes.takeIf { it.isNotBlank() } ?: "Bug fixes and performance improvements."
                else -> throw IllegalArgumentException("Invalid 'releaseNotes' field: expected string.")
            }

            val mandatoryUpdate = when {
                parsed.containsKey("mandatoryUpdate") && parsed["mandatoryUpdate"] != null ->
                    parsed["mandatoryUpdate"] as? Boolean
                        ?: throw IllegalArgumentException("Invalid 'mandatoryUpdate' field: expected boolean.")
                parsed.containsKey("mandatory") && parsed["mandatory"] != null ->
                    parsed["mandatory"] as? Boolean
                        ?: throw IllegalArgumentException("Invalid 'mandatory' field: expected boolean.")
                else -> false
            }

            val minimumSupportedVersion = when (val rawMin = parsed["minimumSupportedVersion"]) {
                null -> 1
                is Long -> rawMin.toInt()
                is Int -> rawMin
                is Number -> throw IllegalArgumentException("Invalid 'minimumSupportedVersion' field: expected integer.")
                else -> throw IllegalArgumentException("Invalid 'minimumSupportedVersion' field: expected integer.")
            }

            RemoteUpdateConfig(
                latestVersion = latestVersion,
                versionCode = versionCode,
                apkUrl = apkUrl,
                sha256 = sha256,
                releaseNotes = releaseNotes.trim(),
                mandatoryUpdate = mandatoryUpdate,
                minimumSupportedVersion = minimumSupportedVersion
            )
        }
    }

    private fun parseHttpsUri(raw: String): URI? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        return uri
    }

    /**
     * Minimal RFC-8259 compliant JSON parser with zero Android/third-party dependencies.
     */
    private class JsonReader(private val input: String) {
        private var pos = 0

        fun parseRootObject(): Map<String, Any?> {
            skipWhitespace()
            val obj = parseObject()
            skipWhitespace()
            if (pos != input.length) {
                throw IllegalArgumentException("Unexpected trailing JSON content at position $pos")
            }
            return obj
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) {
                throw IllegalArgumentException("Unexpected end of JSON")
            }
            return when (val ch = input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("Unexpected character '$ch' at position $pos")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val map = LinkedHashMap<String, Any?>()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at position $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            skipWhitespace()
            val list = ArrayList<Any?>()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at position $pos")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val ch = input[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= input.length) throw IllegalArgumentException("Unterminated escape")
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > input.length) {
                                    throw IllegalArgumentException("Invalid unicode escape")
                                }
                                val hex = input.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Unknown escape '\\$esc'")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated JSON string")
        }

        private fun parseBoolean(): Boolean {
            return if (input.startsWith("true", pos)) {
                pos += 4
                true
            } else if (input.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                throw IllegalArgumentException("Invalid boolean at position $pos")
            }
        }

        private fun parseNull(): Any? {
            if (input.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("Invalid null literal at position $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (input[pos] == '-') pos++
            var hasDigits = false
            while (pos < input.length && input[pos] in '0'..'9') {
                hasDigits = true
                pos++
            }
            if (!hasDigits) throw IllegalArgumentException("Invalid number at position $start")
            var isFloat = false
            if (pos < input.length && input[pos] == '.') {
                isFloat = true
                pos++
                while (pos < input.length && input[pos] in '0'..'9') pos++
            }
            if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                isFloat = true
                pos++
                if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                while (pos < input.length && input[pos] in '0'..'9') pos++
            }
            val raw = input.substring(start, pos)
            return if (isFloat) raw.toDouble() else raw.toLong()
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos] in charArrayOf(' ', '\t', '\r', '\n')) {
                pos++
            }
        }

        private fun peek(): Char {
            if (pos >= input.length) throw IllegalArgumentException("Unexpected end of JSON")
            return input[pos]
        }

        private fun expect(expected: Char) {
            if (pos >= input.length || input[pos] != expected) {
                throw IllegalArgumentException("Expected '$expected' at position $pos")
            }
            pos++
        }
    }
}
