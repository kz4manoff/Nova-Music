package com.novamusic.player.wrapped

import android.os.SystemClock
import com.novamusic.player.data.model.Song
import com.novamusic.player.database.entities.ListeningEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class TrackedTrackSnapshot(
    val songId: Long,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val artworkUri: String,
    val durationMs: Long
) {
    companion object {
        fun fromSong(song: Song): TrackedTrackSnapshot {
            val key = if (song.id > 0L) {
                "song:${song.id}"
            } else if (song.uri.toString().isNotBlank()) {
                "uri:${song.uri}"
            } else {
                "meta:${song.title.trim().lowercase()}|${song.artist.trim().lowercase()}"
            }
            return TrackedTrackSnapshot(
                songId = song.id,
                trackKey = key,
                title = song.title.ifBlank { "Unknown Track" },
                artist = song.artist.ifBlank { "Unknown Artist" },
                album = song.album,
                albumId = song.albumId,
                genre = song.genre,
                artworkUri = song.artworkUri.toString(),
                durationMs = song.durationMs.coerceAtLeast(0L)
            )
        }
    }
}

/**
 * Stateful, session-safe Media3 listening tracker.
 *
 * Guarantees:
 * - Only active playing time (between play and pause/stop/buffer) is counted.
 * - A play counts only once per session after >= 30s (or >= 50% of shorter tracks).
 * - Seeking keeps the same sessionId and never creates duplicate play records.
 * - Pausing & resuming continues accumulating cleanly inside the same session.
 */
class ListeningTracker(
    private val persistCallback: suspend (ListeningEventEntity) -> Unit,
    private val elapsedRealtimeProvider: () -> Long = { SystemClock.elapsedRealtime() },
    private val wallClockProvider: () -> Long = { System.currentTimeMillis() },
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionLock = Any()

    private data class ActiveSession(
        val sessionId: String,
        var track: TrackedTrackSnapshot,
        val startedAtMs: Long,
        val localYear: Int,
        val localMonth: Int,
        val localDayKey: Int,
        val hourOfDay: Int,
        var accumulatedMs: Long = 0L,
        var lastResumeElapsedMs: Long = -1L,
        var isCounting: Boolean = false,
        var countedPlay: Boolean = false,
        var completed: Boolean = false,
        var skipped: Boolean = false,
        var hasPersistedOnce: Boolean = false,
        var lastObservedTrackPosMs: Long = 0L
    )

    private var activeSession: ActiveSession? = null
    private var checkpointTickerJob: Job? = null

    fun onTrackChanged(
        newTrack: TrackedTrackSnapshot?,
        isCurrentlyPlaying: Boolean,
        wasAutoOrRepeatTransition: Boolean,
        outgoingPositionMs: Long
    ) {
        synchronized(sessionLock) {
            val outgoing = activeSession
            if (outgoing != null) {
                commitActiveDeltaLocked(outgoing)
                val dur = outgoing.track.durationMs
                val finishedByPosition = dur > 0L && outgoingPositionMs >= (dur * 0.88).toLong()
                val completed = wasAutoOrRepeatTransition || finishedByPosition || outgoing.completed
                outgoing.completed = completed
                outgoing.skipped = !completed && (outgoing.accumulatedMs >= 1_000L || outgoing.hasPersistedOnce)
                val finalizedEntity = toEntity(outgoing)
                if (outgoing.accumulatedMs >= 1_000L || outgoing.hasPersistedOnce) {
                    outgoing.hasPersistedOnce = true
                    persistAsync(finalizedEntity)
                }
            }

            activeSession = null
            stopCheckpointTicker()

            if (newTrack != null) {
                val session = createSession(newTrack)
                activeSession = session
                if (isCurrentlyPlaying) {
                    resumeCountingLocked(session)
                    startCheckpointTicker()
                }
            }
        }
    }

    fun onIsPlayingChanged(
        isPlaying: Boolean,
        currentTrack: TrackedTrackSnapshot?,
        currentPositionMs: Long
    ) {
        synchronized(sessionLock) {
            if (activeSession == null && currentTrack != null) {
                activeSession = createSession(currentTrack)
            }
            val session = activeSession ?: return
            maybeUpdateTrackDurationLocked(session, currentTrack?.durationMs ?: 0L)
            session.lastObservedTrackPosMs = currentPositionMs.coerceAtLeast(0L)

            if (isPlaying) {
                resumeCountingLocked(session)
                startCheckpointTicker()
            } else {
                pauseCountingLocked(session)
                stopCheckpointTicker()
                flushIfMeaningful(session)
            }
        }
    }

    fun onSeekDiscontinuity(newPositionMs: Long) {
        synchronized(sessionLock) {
            val session = activeSession ?: return
            commitActiveDeltaLocked(session)
            session.lastObservedTrackPosMs = newPositionMs.coerceAtLeast(0L)
            if (session.isCounting) {
                session.lastResumeElapsedMs = elapsedRealtimeProvider()
            }
            if (session.countedPlay || session.hasPersistedOnce) {
                flushIfMeaningful(session)
            }
        }
    }

    fun onPlaybackEnded(durationMs: Long) {
        synchronized(sessionLock) {
            val session = activeSession ?: return
            maybeUpdateTrackDurationLocked(session, durationMs)
            commitActiveDeltaLocked(session)
            session.isCounting = false
            session.lastResumeElapsedMs = -1L
            session.completed = true
            session.skipped = false
            stopCheckpointTicker()
            val effectiveThreshold = WrappedAnalyticsEngine.computePlayThresholdMs(
                if (durationMs > 0L) durationMs else session.track.durationMs
            )
            if (session.accumulatedMs >= effectiveThreshold) {
                session.countedPlay = true
            }
            if (session.accumulatedMs >= 1_000L || session.hasPersistedOnce) {
                session.hasPersistedOnce = true
                persistAsync(toEntity(session))
            }
        }
    }

    fun checkpointNow() {
        synchronized(sessionLock) {
            val session = activeSession ?: return
            commitActiveDeltaLocked(session)
            flushIfMeaningful(session)
        }
    }

    fun onServiceDestroyed() {
        synchronized(sessionLock) {
            val session = activeSession ?: return
            commitActiveDeltaLocked(session)
            session.isCounting = false
            session.lastResumeElapsedMs = -1L
            stopCheckpointTicker()
            if (session.accumulatedMs >= 1_500L || session.hasPersistedOnce) {
                session.hasPersistedOnce = true
                val entity = toEntity(session)
                persistAsync(entity)
            }
        }
    }

    /**
     * Exposed internally for deterministic JVM unit tests verifying session logic.
     */
    internal fun snapshotCurrentSessionForTest(): ListeningEventEntity? {
        synchronized(sessionLock) {
            val session = activeSession ?: return null
            commitActiveDeltaLocked(session)
            return toEntity(session)
        }
    }

    private fun createSession(track: TrackedTrackSnapshot): ActiveSession {
        val nowMs = wallClockProvider()
        val zdt = Instant.ofEpochMilli(nowMs).atZone(zoneId)
        val dayKey = zdt.year * 10_000 + zdt.monthValue * 100 + zdt.dayOfMonth
        return ActiveSession(
            sessionId = UUID.randomUUID().toString(),
            track = track,
            startedAtMs = nowMs,
            localYear = zdt.year,
            localMonth = zdt.monthValue,
            localDayKey = dayKey,
            hourOfDay = zdt.hour
        )
    }

    private fun resumeCountingLocked(session: ActiveSession) {
        if (!session.isCounting) {
            session.isCounting = true
            session.lastResumeElapsedMs = elapsedRealtimeProvider()
        }
    }

    private fun pauseCountingLocked(session: ActiveSession) {
        commitActiveDeltaLocked(session)
        session.isCounting = false
        session.lastResumeElapsedMs = -1L
    }

    private fun commitActiveDeltaLocked(session: ActiveSession) {
        val nowElapsed = elapsedRealtimeProvider()
        if (session.isCounting && session.lastResumeElapsedMs >= 0L) {
            val delta = (nowElapsed - session.lastResumeElapsedMs).coerceAtLeast(0L)
            // Guard against spurious huge jumps (> 2 hours without checkpoint)
            val safeDelta = delta.coerceAtMost(7_200_000L)
            session.accumulatedMs += safeDelta
            session.lastResumeElapsedMs = nowElapsed
        }
        val threshold = WrappedAnalyticsEngine.computePlayThresholdMs(session.track.durationMs)
        if (!session.countedPlay && session.accumulatedMs >= threshold) {
            session.countedPlay = true
        }
    }

    private fun flushIfMeaningful(session: ActiveSession) {
        if (session.accumulatedMs >= 2_000L || session.countedPlay || session.hasPersistedOnce) {
            session.hasPersistedOnce = true
            persistAsync(toEntity(session))
        }
    }

    private fun maybeUpdateTrackDurationLocked(session: ActiveSession, durationMs: Long) {
        if (session.track.durationMs <= 0L && durationMs > 0L) {
            session.track = session.track.copy(durationMs = durationMs)
        }
    }

    private fun startCheckpointTicker() {
        if (checkpointTickerJob?.isActive == true) return
        checkpointTickerJob = trackerScope.launch {
            while (isActive) {
                delay(15_000L)
                val shouldContinue = synchronized(sessionLock) {
                    val session = activeSession
                    if (session == null || !session.isCounting) {
                        false
                    } else {
                        commitActiveDeltaLocked(session)
                        flushIfMeaningful(session)
                        true
                    }
                }
                if (!shouldContinue) break
            }
        }
    }

    private fun stopCheckpointTicker() {
        checkpointTickerJob?.cancel()
        checkpointTickerJob = null
    }

    private fun toEntity(session: ActiveSession): ListeningEventEntity {
        val nowWall = wallClockProvider().coerceAtLeast(session.startedAtMs)
        return ListeningEventEntity(
            sessionId = session.sessionId,
            songId = session.track.songId,
            trackKey = session.track.trackKey,
            title = session.track.title,
            artist = session.track.artist,
            album = session.track.album,
            albumId = session.track.albumId,
            genre = session.track.genre,
            artworkUri = session.track.artworkUri,
            trackDurationMs = session.track.durationMs,
            listenedMs = session.accumulatedMs,
            startedAtMs = session.startedAtMs,
            endedAtMs = nowWall,
            localYear = session.localYear,
            localMonth = session.localMonth,
            localDayKey = session.localDayKey,
            hourOfDay = session.hourOfDay,
            countedPlay = session.countedPlay,
            completed = session.completed,
            skipped = session.skipped
        )
    }

    private fun persistAsync(entity: ListeningEventEntity) {
        trackerScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                runCatching { persistCallback(entity) }
            }
        }
    }
}
