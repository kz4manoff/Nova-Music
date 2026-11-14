package com.novamusic.player.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.Genre
import com.novamusic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicScanner(private val context: Context) {

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus"
        )

        private val SUPPORTED_MIME_PREFIXES = setOf(
            "audio/mpeg",
            "audio/mp3",
            "audio/flac",
            "audio/x-flac",
            "audio/wav",
            "audio/x-wav",
            "audio/aac",
            "audio/mp4",
            "audio/m4a",
            "audio/x-m4a",
            "audio/ogg",
            "audio/opus",
            "application/ogg"
        )

        private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }

    suspend fun scanDeviceMusic(minDurationSec: Int = 10): Pair<List<Song>, List<Genre>> =
        withContext(Dispatchers.IO) {
            val genreMapBySongId = loadGenresBySongId()
            val songs = mutableListOf<Song>()
            val minDurationMs = minDurationSec * 1000L

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ARTIST_ID)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.DATA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Audio.Media.RELATIVE_PATH)
                }
                add(MediaStore.Audio.Media.MIME_TYPE)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATE_ADDED)
                add(MediaStore.Audio.Media.TRACK)
                add(MediaStore.Audio.Media.YEAR)
                add(MediaStore.Audio.Media.DISPLAY_NAME)
                add(MediaStore.Audio.Media.IS_MUSIC)
            }.toTypedArray()

            val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(minDurationMs.toString())
            val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            runCatching {
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    } else -1
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val isMusicCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val path = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                        val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty().trim('/') else ""
                        val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol).orEmpty() else ""
                        val mimeType = cursor.getString(mimeCol).orEmpty().lowercase()
                        val isMusic = if (isMusicCol >= 0) cursor.getInt(isMusicCol) != 0 else true

                        val ext = path.substringAfterLast('.', displayName.substringAfterLast('.', "")).lowercase()
                        val isSupportedFormat = ext in SUPPORTED_EXTENSIONS ||
                            SUPPORTED_MIME_PREFIXES.any { mimeType.startsWith(it) }

                        if (!isSupportedFormat) continue
                        if (!isMusic && ext !in setOf("flac", "opus", "ogg", "wav")) continue

                        val duration = cursor.getLong(durationCol)
                        if (duration < minDurationMs) continue

                        val lowerPath = (path.ifBlank { relPath }).lowercase()
                        if (lowerPath.contains("/ringtones/") ||
                            lowerPath.contains("/notifications/") ||
                            lowerPath.contains("/alarms/") ||
                            (lowerPath.contains("whatsapp audio") && duration < 30_000L)
                        ) {
                            continue
                        }

                        val title = cursor.getString(titleCol).orEmpty()
                            .ifBlank { displayName.substringBeforeLast('.') }
                            .ifBlank { "Unknown Track" }
                        val artist = cursor.getString(artistCol).orEmpty()
                            .takeUnless { it == "<unknown>" }
                            ?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
                        val artistId = cursor.getLong(artistIdCol)
                        val album = cursor.getString(albumCol).orEmpty()
                            .ifBlank { "Unknown Album" }
                        val albumId = cursor.getLong(albumIdCol)
                        val size = cursor.getLong(sizeCol)
                        val dateAdded = cursor.getLong(dateCol)
                        val trackNumber = cursor.getInt(trackCol)
                        val year = cursor.getInt(yearCol)

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val artworkUri = ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId)

                        val parentFile = if (path.isNotBlank()) File(path).parentFile else null
                        val folderPath = parentFile?.absolutePath
                            ?: (if (relPath.isNotBlank()) "/$relPath" else "/Music")
                        val folderName = parentFile?.name?.ifBlank { "Internal Storage" }
                            ?: relPath.substringAfterLast('/', missingDelimiterValue = "Music").ifBlank { "Music" }

                        val genreName = genreMapBySongId[id] ?: "Unknown"

                        songs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                artistId = artistId,
                                album = album,
                                albumId = albumId,
                                durationMs = duration,
                                uri = contentUri,
                                artworkUri = artworkUri,
                                path = path.ifBlank { contentUri.toString() },
                                folderPath = folderPath,
                                folderName = folderName,
                                mimeType = mimeType,
                                sizeBytes = size,
                                dateAddedSec = dateAdded,
                                trackNumber = trackNumber,
                                year = year,
                                genre = genreName
                            )
                        )
                    }
                }
            }

            val genres = songs
                .groupBy { it.genre }
                .entries
                .sortedBy { it.key.lowercase() }
                .mapIndexed { idx, entry ->
                    Genre(
                        id = idx.toLong() + 1L,
                        name = entry.key,
                        songCount = entry.value.size
                    )
                }

            songs to genres
        }

    private fun loadGenresBySongId(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        runCatching {
            val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
            )
            context.contentResolver.query(genresUri, projection, null, null, null)?.use { genreCursor ->
                val idCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(idCol)
                    val genreName = genreCursor.getString(nameCol)?.trim().orEmpty()
                    if (genreName.isBlank()) continue

                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    runCatching {
                        context.contentResolver.query(
                            membersUri,
                            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                            null,
                            null,
                            null
                        )?.use { memberCursor ->
                            val audioIdCol = memberCursor.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID)
                            if (audioIdCol >= 0) {
                                while (memberCursor.moveToNext()) {
                                    val audioId = memberCursor.getLong(audioIdCol)
                                    map.putIfAbsent(audioId, genreName)
                                }
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    suspend fun getAudioFileInfo(song: Song): AudioFileInfo = withContext(Dispatchers.IO) {
        var bitrateKbps: Int? = null
        var sampleRateHz: Int? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri)
            bitrateKbps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()
                ?.let { it / 1000 }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { retriever.release() }
        }

        AudioFileInfo(
            fileName = File(song.path).name.ifBlank { song.title },
            filePath = song.path,
            format = song.fileExtension.ifBlank { "AUDIO" },
            mimeType = song.mimeType.ifBlank { "audio/*" },
            sizeBytes = song.sizeBytes,
            durationMs = song.durationMs,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            dateAddedSec = song.dateAddedSec
        )
    }

    suspend fun resolveSafUriToSong(uri: Uri): Song? = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var durationMs = 0L

            try {
                retriever.setDataSource(context, uri)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } finally {
                runCatching { retriever.release() }
            }

            var displayName = "Audio Track"
            var sizeBytes = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }

            val mime = context.contentResolver.getType(uri).orEmpty()
            val stableId = -kotlin.math.abs(uri.toString().hashCode().toLong()).coerceAtLeast(1L)

            Song(
                id = stableId,
                title = title?.ifBlank { null } ?: displayName.substringBeforeLast('.'),
                artist = artist?.ifBlank { null } ?: "Unknown Artist",
                artistId = -1L,
                album = album?.ifBlank { null } ?: "Local File",
                albumId = -1L,
                durationMs = durationMs,
                uri = uri,
                artworkUri = uri,
                path = displayName,
                folderPath = "SAF",
                folderName = "Picked Files",
                mimeType = mime,
                sizeBytes = sizeBytes,
                dateAddedSec = System.currentTimeMillis() / 1000L,
                trackNumber = 0,
                year = 0,
                genre = "Local"
            )
        }.getOrNull()
    }

    suspend fun scanSafTree(treeUri: Uri): List<Song> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val collected = mutableListOf<Song>()
        fun walk(dir: DocumentFile) {
            dir.listFiles().forEach { child ->
                if (child.isDirectory) {
                    walk(child)
                } else if (child.isFile) {
                    val name = child.name.orEmpty()
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val mime = child.type.orEmpty().lowercase()
                    if (ext in SUPPORTED_EXTENSIONS || SUPPORTED_MIME_PREFIXES.any { mime.startsWith(it) }) {
                        val id = -kotlin.math.abs(child.uri.toString().hashCode().toLong()).coerceAtLeast(1L)
                        collected.add(
                            Song(
                                id = id,
                                title = name.substringBeforeLast('.').ifBlank { "Audio File" },
                                artist = "Local Folder",
                                artistId = -1L,
                                album = root.name ?: "SAF Folder",
                                albumId = -1L,
                                durationMs = 0L,
                                uri = child.uri,
                                artworkUri = child.uri,
                                path = name,
                                folderPath = treeUri.toString(),
                                folderName = root.name ?: "Folder",
                                mimeType = mime,
                                sizeBytes = child.length(),
                                dateAddedSec = child.lastModified() / 1000L,
                                trackNumber = 0,
                                year = 0,
                                genre = "Local"
                            )
                        )
                    }
                }
            }
        }
        walk(root)
        collected.sortedBy { it.title.lowercase() }
    }
}
