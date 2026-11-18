package com.novamusic.player.audio

import com.novamusic.player.data.model.Song
import com.novamusic.player.database.entities.PlaybackHistoryEntity
import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure Kotlin intelligent queue-ordering engine for Nova Music Smart Shuffle.
 *
 * Priorities:
 * 1. Anchors the currently playing song so switching modes never interrupts playback.
 * 2. Boosts tracks related to the active song via real local metadata (artist, genre, album, folder, era)
 *    while ignoring scanner placeholder labels ("Unknown Artist", "Unknown", "Local", etc.).
 * 3. Incorporates local listening history and favorites while applying strong recency cooldowns so
 *    recently played or overplayed songs are not repeated back-to-back.
 * 4. Spreads tracks across different artists to avoid clustering the same artist consecutively when
 *    alternatives exist, falling back cleanly when metadata or history is sparse.
 */
object SmartShuffleEngine {

    private val PLACEHOLDER_ARTISTS = setOf(
        "", "unknown", "unknown artist", "<unknown>", "local folder", "various artists"
    )
    private val PLACEHOLDER_ALBUMS = setOf(
        "", "unknown", "unknown album", "<unknown>", "local file"
    )
    private val PLACEHOLDER_GENRES = setOf(
        "", "unknown", "<unknown>", "local"
    )

    fun buildSmartQueue(
        currentSong: Song,
        queue: List<Song>,
        historyMap: Map<Long, PlaybackHistoryEntity> = emptyMap(),
        favoriteIds: Set<Long> = emptySet(),
        recentSessionSongIds: List<Long> = emptyList(),
        nowMs: Long = System.currentTimeMillis(),
        random: Random = Random.Default
    ): List<Song> {
        if (queue.isEmpty()) return listOf(currentSong)
        val anchorIndex = queue.indexOfFirst { it.id == currentSong.id }
        val workingQueue = if (anchorIndex >= 0) queue else listOf(currentSong) + queue
        val effectiveAnchor = anchorIndex.coerceAtLeast(0)
        val indices = buildSmartOrderIndices(
            currentIndex = effectiveAnchor,
            queue = workingQueue,
            historyMap = historyMap,
            favoriteIds = favoriteIds,
            recentSessionSongIds = recentSessionSongIds,
            nowMs = nowMs,
            random = random
        )
        return indices.map { workingQueue[it] }
    }

    /**
     * Returns an ordered list of queue indices starting with [currentIndex] at index 0,
     * followed by the intelligently ordered remaining queue indices.
     */
    fun buildSmartOrderIndices(
        currentIndex: Int,
        queue: List<Song>,
        historyMap: Map<Long, PlaybackHistoryEntity> = emptyMap(),
        favoriteIds: Set<Long> = emptySet(),
        recentSessionSongIds: List<Long> = emptyList(),
        nowMs: Long = System.currentTimeMillis(),
        random: Random = Random.Default
    ): List<Int> {
        if (queue.isEmpty()) return emptyList()
        val safeAnchor = currentIndex.coerceIn(0, queue.lastIndex)
        if (queue.size == 1) return listOf(safeAnchor)

        val anchorSong = queue[safeAnchor]
        val remainingIndices = queue.indices.filter { it != safeAnchor }.toMutableList()
        if (remainingIndices.isEmpty()) return listOf(safeAnchor)

        // Precompute static base affinity + freshness scores for every candidate index.
        val baseScores = HashMap<Int, Float>(remainingIndices.size)
        for (idx in remainingIndices) {
            val candidate = queue[idx]
            baseScores[idx] = computeBaseScore(
                anchor = anchorSong,
                candidate = candidate,
                historyMap = historyMap,
                favoriteIds = favoriteIds,
                recentSessionSongIds = recentSessionSongIds,
                nowMs = nowMs,
                random = random
            )
        }

        val ordered = ArrayList<Int>(queue.size)
        ordered.add(safeAnchor)

        var previousArtist = normalizedArtist(anchorSong)
        var twoBackArtist: String? = null
        var previousGenre = normalizedGenre(anchorSong)
        val seenSongIds = HashSet<Long>(queue.size).apply { add(anchorSong.id) }

        while (remainingIndices.isNotEmpty()) {
            var bestPos = 0
            var bestScore = Float.NEGATIVE_INFINITY

            for (pos in remainingIndices.indices) {
                val idx = remainingIndices[pos]
                val candidate = queue[idx]
                var stepScore = baseScores[idx] ?: 0f

                val candidateArtist = normalizedArtist(candidate)
                if (candidateArtist != null) {
                    if (candidateArtist == previousArtist) {
                        stepScore -= 4.5f
                    } else if (candidateArtist == twoBackArtist) {
                        stepScore -= 2.1f
                    }
                }

                val candidateGenre = normalizedGenre(candidate)
                if (candidateGenre != null && candidateGenre == previousGenre) {
                    stepScore += 0.85f
                }

                // Push duplicate queue entries (same songId) toward the tail so unique songs play first.
                if (candidate.id in seenSongIds) {
                    stepScore -= 10.0f
                }

                if (stepScore > bestScore) {
                    bestScore = stepScore
                    bestPos = pos
                }
            }

            val chosenIdx = remainingIndices.removeAt(bestPos)
            val chosenSong = queue[chosenIdx]
            ordered.add(chosenIdx)
            seenSongIds.add(chosenSong.id)

            twoBackArtist = previousArtist
            previousArtist = normalizedArtist(chosenSong)
            previousGenre = normalizedGenre(chosenSong) ?: previousGenre
        }

        return ordered
    }

    /**
     * Picks a single intelligent next track from [librarySongs] when autoplay triggers at queue end.
     */
    fun pickNextAutoplayTrack(
        currentSong: Song?,
        librarySongs: List<Song>,
        historyMap: Map<Long, PlaybackHistoryEntity> = emptyMap(),
        favoriteIds: Set<Long> = emptySet(),
        recentSessionSongIds: List<Long> = emptyList(),
        nowMs: Long = System.currentTimeMillis(),
        random: Random = Random.Default
    ): Song? {
        val candidates = if (currentSong != null) {
            librarySongs.filter { it.id != currentSong.id }
        } else {
            librarySongs
        }
        if (candidates.isEmpty()) return null
        if (currentSong == null) return candidates.random(random)

        return candidates.maxByOrNull { candidate ->
            computeBaseScore(
                anchor = currentSong,
                candidate = candidate,
                historyMap = historyMap,
                favoriteIds = favoriteIds,
                recentSessionSongIds = recentSessionSongIds,
                nowMs = nowMs,
                random = random
            )
        }
    }

    /**
     * Maps current queue indices back into [originalOrderIds] order when leaving Smart Shuffle,
     * preserving any newly queued items cleanly and returning `(orderedIndices, newAnchorPosition)`.
     */
    fun restoreSequentialOrderIndices(
        currentIndex: Int,
        currentQueueIds: List<Long>,
        originalOrderIds: List<Long>
    ): Pair<List<Int>, Int> {
        if (currentQueueIds.isEmpty()) return emptyList<Int>() to -1
        val safeCurrent = currentIndex.coerceIn(0, currentQueueIds.lastIndex)
        if (originalOrderIds.isEmpty()) return currentQueueIds.indices.toList() to safeCurrent

        val availableById = HashMap<Long, ArrayDeque<Int>>()
        currentQueueIds.forEachIndexed { index, id ->
            availableById.getOrPut(id) { ArrayDeque() }.addLast(index)
        }

        val currentSongId = currentQueueIds[safeCurrent]
        // Reserve the currently playing index so it keeps its exact place if its ID appears multiple times.
        availableById[currentSongId]?.remove(safeCurrent)
        var placedCurrent = false

        val restored = ArrayList<Int>(currentQueueIds.size)
        for (targetId in originalOrderIds) {
            if (!placedCurrent && targetId == currentSongId) {
                restored.add(safeCurrent)
                placedCurrent = true
                continue
            }
            val bucket = availableById[targetId]
            val idx = bucket?.removeFirstOrNull()
            if (idx != null) {
                restored.add(idx)
            }
        }

        if (!placedCurrent) {
            restored.add(0, safeCurrent)
        }

        // Append any extra tracks added to queue while Smart Shuffle was active.
        val alreadyIncluded = restored.toHashSet()
        for (idx in currentQueueIds.indices) {
            if (idx !in alreadyIncluded) {
                restored.add(idx)
            }
        }

        val newAnchorPos = restored.indexOf(safeCurrent).coerceAtLeast(0)
        return restored to newAnchorPos
    }

    internal fun computeBaseScore(
        anchor: Song,
        candidate: Song,
        historyMap: Map<Long, PlaybackHistoryEntity>,
        favoriteIds: Set<Long>,
        recentSessionSongIds: List<Long>,
        nowMs: Long,
        random: Random
    ): Float {
        var score = 0f

        // 1. Metadata affinity with current song (ignoring scanner placeholder strings).
        val anchorArtist = normalizedArtist(anchor)
        val candidateArtist = normalizedArtist(candidate)
        val sameArtist = (anchor.artistId > 0L && anchor.artistId == candidate.artistId) ||
            (anchorArtist != null && anchorArtist == candidateArtist)
        if (sameArtist) {
            score += 3.4f
        }

        val anchorGenre = normalizedGenre(anchor)
        val candidateGenre = normalizedGenre(candidate)
        if (anchorGenre != null && anchorGenre == candidateGenre) {
            score += 3.0f
        }

        val anchorAlbum = normalizedAlbum(anchor)
        val candidateAlbum = normalizedAlbum(candidate)
        val sameAlbum = (anchor.albumId > 0L && anchor.albumId == candidate.albumId) ||
            (anchorAlbum != null && anchorAlbum == candidateAlbum)
        if (sameAlbum) {
            score += 2.2f
        }

        if (anchor.folderPath.isNotBlank() && anchor.folderPath == candidate.folderPath) {
            score += 1.0f
        }

        if (anchor.year > 1900 && candidate.year > 1900 && abs(anchor.year - candidate.year) <= 4) {
            score += 0.75f
        }

        // 2. Listener preference signals (favorites + moderate history familiarity).
        if (candidate.id in favoriteIds) {
            score += 1.6f
        }

        val history = historyMap[candidate.id]
        if (history != null) {
            score += history.playCount.coerceIn(0, 8) * 0.25f
            val ageMs = (nowMs - history.lastPlayedAt).coerceAtLeast(0L)
            score -= when {
                ageMs < 20 * 60_000L -> 5.5f
                ageMs < 2 * 60 * 60_000L -> 2.8f
                ageMs < 12 * 60 * 60_000L -> 1.0f
                else -> 0f
            }
        }

        // 3. Strong in-session recency cooldown to avoid repeating songs just heard.
        val recentIdx = recentSessionSongIds.lastIndexOf(candidate.id)
        if (recentIdx >= 0) {
            val stepsAgo = (recentSessionSongIds.lastIndex - recentIdx)
            val cooldown = when {
                stepsAgo <= 1 -> 8.5f
                stepsAgo <= 4 -> 6.0f
                stepsAgo <= 9 -> 3.5f
                else -> 1.5f
            }
            score -= cooldown
        }

        // 4. Gentle stochastic tie-breaker so Smart Shuffle feels organic across toggles.
        score += random.nextFloat() * 1.4f
        return score
    }

    private fun normalizedArtist(song: Song): String? {
        val raw = song.artist.trim().lowercase()
        return if (raw in PLACEHOLDER_ARTISTS) null else raw
    }

    private fun normalizedAlbum(song: Song): String? {
        val raw = song.album.trim().lowercase()
        return if (raw in PLACEHOLDER_ALBUMS) null else raw
    }

    private fun normalizedGenre(song: Song): String? {
        val raw = song.genre.trim().lowercase()
        return if (raw in PLACEHOLDER_GENRES) null else raw
    }
}
