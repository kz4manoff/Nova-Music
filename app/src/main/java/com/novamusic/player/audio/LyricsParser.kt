package com.novamusic.player.audio

import android.content.Context
import android.net.Uri
import com.novamusic.player.data.model.LyricsLine
import com.novamusic.player.data.model.LyricsResult
import com.novamusic.player.data.model.LyricsSource
import com.novamusic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object LyricsParser {

    private val LRC_TIME_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    suspend fun loadLyrics(
        context: Context,
        song: Song,
        manualLyrics: String?
    ): LyricsResult = withContext(Dispatchers.IO) {
        // 1. Check user-saved manual/imported lyrics in Room first if present
        if (!manualLyrics.isNullOrBlank()) {
            val parsed = parseRawText(manualLyrics)
            if (parsed.isNotEmpty()) {
                return@withContext LyricsResult(
                    lines = parsed,
                    isSynchronized = parsed.any { it.timestampMs != null },
                    source = LyricsSource.MANUAL
                )
            }
        }

        // 2. Check sidecar .lrc file in the same directory
        val sidecarLrc = findSidecarLrc(song.path)
        if (!sidecarLrc.isNullOrBlank()) {
            val parsed = parseRawText(sidecarLrc)
            if (parsed.isNotEmpty()) {
                return@withContext LyricsResult(
                    lines = parsed,
                    isSynchronized = parsed.any { it.timestampMs != null },
                    source = LyricsSource.LRC_FILE
                )
            }
        }

        // 3. Check embedded lyrics (ID3v2 USLT / Vorbis comment / MP4 ©lyr)
        val embedded = extractEmbeddedLyrics(context, song.uri)
        if (!embedded.isNullOrBlank()) {
            val parsed = parseRawText(embedded)
            if (parsed.isNotEmpty()) {
                return@withContext LyricsResult(
                    lines = parsed,
                    isSynchronized = parsed.any { it.timestampMs != null },
                    source = LyricsSource.EMBEDDED
                )
            }
        }

        LyricsResult(lines = emptyList(), isSynchronized = false, source = LyricsSource.NONE)
    }

    suspend fun readLrcFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }

    fun parseRawText(raw: String): List<LyricsLine> {
        val lines = raw.lines()
        val timedEntries = mutableListOf<LyricsLine>()
        var hasTimestamps = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val matches = LRC_TIME_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                hasTimestamps = true
                val lyricText = LRC_TIME_REGEX.replace(trimmed, "").trim()
                if (lyricText.isBlank()) continue
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]
                    val millis = when (fracStr.length) {
                        0 -> 0L
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        else -> fracStr.take(3).toLongOrNull() ?: 0L
                    }
                    val totalMs = minutes * 60_000L + seconds * 1_000L + millis
                    timedEntries.add(LyricsLine(timestampMs = totalMs, text = lyricText))
                }
            } else if (!trimmed.startsWith("[")) {
                timedEntries.add(LyricsLine(timestampMs = null, text = trimmed))
            }
        }

        return if (hasTimestamps) {
            timedEntries.filter { it.timestampMs != null }.sortedBy { it.timestampMs }
        } else {
            raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.map {
                LyricsLine(timestampMs = null, text = it)
            }
        }
    }

    fun findActiveLineIndex(lines: List<LyricsLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            val ts = lines[i].timestampMs ?: continue
            if (ts <= positionMs + 150L) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }

    private fun findSidecarLrc(audioPath: String): String? {
        if (audioPath.isBlank() || audioPath.startsWith("content://")) return null
        return runCatching {
            val audioFile = File(audioPath)
            val parent = audioFile.parentFile ?: return null
            val lrcFile = File(parent, audioFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                lrcFile.readText(StandardCharsets.UTF_8)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun extractEmbeddedLyrics(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val maxRead = 512 * 1024 // Read up to 512 KB header metadata
                val buffer = ByteArray(maxRead)
                val bytesRead = stream.read(buffer)
                if (bytesRead <= 10) return@use null
                val data = buffer.copyOf(bytesRead)

                // Check ID3v2 header ("ID3")
                if (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) {
                    parseId3v2Uslt(data)?.let { return@use it }
                }
                // Check Vorbis comment (FLAC/OGG/OPUS) for LYRICS= or UNSYNCEDLYRICS=
                parseVorbisCommentLyrics(data)?.let { return@use it }
                // Check MP4/M4A ©lyr atom
                parseMp4LyricsAtom(data)
            }
        }.getOrNull()
    }

    private fun parseId3v2Uslt(data: ByteArray): String? {
        if (data.size < 10) return null
        val majorVersion = data[3].toInt() and 0xFF
        val tagSize = ((data[6].toInt() and 0x7F) shl 21) or
            ((data[7].toInt() and 0x7F) shl 14) or
            ((data[8].toInt() and 0x7F) shl 7) or
            (data[9].toInt() and 0x7F)
        val limit = minOf(data.size, 10 + tagSize)
        var offset = 10

        while (offset + 10 <= limit) {
            val frameId = String(data, offset, 4, StandardCharsets.US_ASCII)
            if (frameId.all { it == '\u0000' }) break
            val frameSize = if (majorVersion >= 4) {
                ((data[offset + 4].toInt() and 0x7F) shl 21) or
                    ((data[offset + 5].toInt() and 0x7F) shl 14) or
                    ((data[offset + 6].toInt() and 0x7F) shl 7) or
                    (data[offset + 7].toInt() and 0x7F)
            } else {
                ((data[offset + 4].toInt() and 0xFF) shl 24) or
                    ((data[offset + 5].toInt() and 0xFF) shl 16) or
                    ((data[offset + 6].toInt() and 0xFF) shl 8) or
                    (data[offset + 7].toInt() and 0xFF)
            }
            if (frameSize <= 0 || offset + 10 + frameSize > limit) break
            if (frameId == "USLT") {
                val payloadStart = offset + 10
                val encoding = data[payloadStart].toInt() and 0xFF
                val charset: Charset = when (encoding) {
                    1 -> StandardCharsets.UTF_16
                    2 -> StandardCharsets.UTF_16BE
                    3 -> StandardCharsets.UTF_8
                    else -> StandardCharsets.ISO_8859_1
                }
                // Skip encoding (1 byte) + language (3 bytes) + content descriptor null terminator
                var textStart = payloadStart + 4
                if (encoding == 1 || encoding == 2) {
                    while (textStart + 1 < payloadStart + frameSize) {
                        if (data[textStart] == 0.toByte() && data[textStart + 1] == 0.toByte()) {
                            textStart += 2
                            break
                        }
                        textStart += 2
                    }
                } else {
                    while (textStart < payloadStart + frameSize) {
                        if (data[textStart] == 0.toByte()) {
                            textStart += 1
                            break
                        }
                        textStart += 1
                    }
                }
                val textLen = (payloadStart + frameSize) - textStart
                if (textLen > 0) {
                    val text = String(data, textStart, textLen, charset).trim('\u0000', ' ')
                    if (text.isNotBlank()) return text
                }
            }
            offset += 10 + frameSize
        }
        return null
    }

    private fun parseVorbisCommentLyrics(data: ByteArray): String? {
        val asUtf8 = String(data, StandardCharsets.UTF_8)
        val markers = listOf("LYRICS=", "UNSYNCEDLYRICS=", "lyrics=", "unsyncedlyrics=")
        for (marker in markers) {
            val idx = asUtf8.indexOf(marker)
            if (idx >= 4) {
                val contentStart = idx + marker.length
                val endIdx = asUtf8.indexOf('\u0000', contentStart)
                    .takeIf { it > contentStart }
                    ?: minOf(asUtf8.length, contentStart + 8192)
                val candidate = asUtf8.substring(contentStart, endIdx)
                    .takeWhile { it.code >= 9 || it == '\n' || it == '\r' }
                    .trim()
                if (candidate.length > 8) return candidate
            }
        }
        return null
    }

    private fun parseMp4LyricsAtom(data: ByteArray): String? {
        // Look for ©lyr atom (0xA9, 'l', 'y', 'r')
        for (i in 0 until data.size - 24) {
            if (data[i] == 0xA9.toByte() &&
                data[i + 1] == 'l'.code.toByte() &&
                data[i + 2] == 'y'.code.toByte() &&
                data[i + 3] == 'r'.code.toByte()
            ) {
                // Inside ©lyr -> data atom: [4B size]['d','a','t','a'][8B flags/reserved][utf8 text]
                if (i + 16 < data.size &&
                    data[i + 8] == 'd'.code.toByte() &&
                    data[i + 9] == 'a'.code.toByte() &&
                    data[i + 10] == 't'.code.toByte() &&
                    data[i + 11] == 'a'.code.toByte()
                ) {
                    val atomSize = ((data[i + 4].toInt() and 0xFF) shl 24) or
                        ((data[i + 5].toInt() and 0xFF) shl 16) or
                        ((data[i + 6].toInt() and 0xFF) shl 8) or
                        (data[i + 7].toInt() and 0xFF)
                    val textStart = i + 20
                    val textEnd = minOf(data.size, i + 4 + atomSize)
                    if (textEnd > textStart) {
                        val lyrics = String(data, textStart, textEnd - textStart, StandardCharsets.UTF_8).trim()
                        if (lyrics.isNotBlank()) return lyrics
                    }
                }
            }
        }
        return null
    }
}
