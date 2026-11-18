package com.novamusic.player.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.preferences.AppSettings
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates Home Screen Widget updates across MusicService, AudioPlayerManager,
 * AppSettings, and AppWidgetProvider lifecycle callbacks.
 *
 * Skips all work immediately if zero widgets are placed on the user's home screen,
 * and uses lightweight partial RemoteViews updates for 1s playback progress ticks.
 */
class NovaWidgetCoordinator(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val updateMutex = Mutex()

    @Volatile
    private var currentSettings: AppSettings = AppSettings()

    @Volatile
    private var latestPlayerState: PlayerUiState = PlayerUiState()

    @Volatile
    private var lastFullFingerprint: RenderFingerprint? = null

    @Volatile
    private var lastPartialTickElapsedMs: Long = 0L

    private var pendingJob: Job? = null

    private data class RenderFingerprint(
        val songId: Long,
        val artworkUri: String,
        val isPlaying: Boolean,
        val dynamicColors: Boolean,
        val accentHex: Long,
        val isNightMode: Boolean
    )

    init {
        scope.launch {
            var initialized = false
            preferencesRepository.appSettings.collect { settings ->
                val visualChanged = initialized && (
                    currentSettings.dynamicColors != settings.dynamicColors ||
                        currentSettings.accentColor != settings.accentColor
                    )
                currentSettings = settings
                initialized = true
                if (visualChanged && hasActiveWidgets()) {
                    scheduleRender(forceFull = true)
                }
            }
        }
    }

    fun hasActiveWidgets(): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val mediumIds = manager.getAppWidgetIds(
            ComponentName(context, NovaMusicMediumWidgetProvider::class.java)
        )
        if (mediumIds.isNotEmpty()) return true
        val largeIds = manager.getAppWidgetIds(
            ComponentName(context, NovaMusicLargeWidgetProvider::class.java)
        )
        return largeIds.isNotEmpty()
    }

    fun onPlaybackStateChanged(
        state: PlayerUiState,
        forceFullUpdate: Boolean = false
    ) {
        latestPlayerState = state
        if (!hasActiveWidgets()) return

        val fingerprint = buildFingerprint(state, currentSettings)
        val needsFullRender = forceFullUpdate || lastFullFingerprint != fingerprint

        if (needsFullRender) {
            scheduleRender(forceFull = true)
        } else {
            val now = SystemClock.elapsedRealtime()
            if (now - lastPartialTickElapsedMs >= 850L) {
                lastPartialTickElapsedMs = now
                pushProgressOnly(state.positionMs, state.durationMs)
            }
        }
    }

    fun onServicePlaybackSnapshot(
        songId: Long,
        title: String?,
        artist: String?,
        album: String?,
        artworkUri: Uri?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        forceFullUpdate: Boolean = false
    ) {
        if (!hasActiveWidgets()) return

        val existingSong = latestPlayerState.currentSong?.takeIf { it.id == songId }
            ?: if (songId > 0L) musicRepository.allSongs.value.firstOrNull { it.id == songId } else null

        val effectiveSong = existingSong ?: if (songId > 0L || !title.isNullOrBlank()) {
            com.novamusic.player.data.model.Song(
                id = songId,
                title = title.orEmpty().ifBlank { "Unknown Track" },
                artist = artist.orEmpty().ifBlank { "Unknown Artist" },
                artistId = -1L,
                album = album.orEmpty(),
                albumId = -1L,
                durationMs = durationMs.coerceAtLeast(0L),
                uri = Uri.EMPTY,
                artworkUri = artworkUri ?: Uri.EMPTY,
                path = "",
                folderPath = "",
                folderName = "",
                mimeType = "",
                sizeBytes = 0L,
                dateAddedSec = 0L,
                trackNumber = 0,
                year = 0
            )
        } else {
            null
        }

        val updatedState = latestPlayerState.copy(
            currentSong = effectiveSong,
            isPlaying = isPlaying,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(effectiveSong?.durationMs ?: 0L)
        )
        onPlaybackStateChanged(updatedState, forceFullUpdate = forceFullUpdate)
    }

    fun onProgressTick(positionMs: Long, durationMs: Long) {
        if (!hasActiveWidgets()) return
        val updated = latestPlayerState.copy(
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(latestPlayerState.durationMs)
        )
        latestPlayerState = updated

        val now = SystemClock.elapsedRealtime()
        if (now - lastPartialTickElapsedMs >= 850L) {
            lastPartialTickElapsedMs = now
            pushProgressOnly(updated.positionMs, updated.durationMs)
        }
    }

    fun refreshAll(forceFullUpdate: Boolean = true) {
        if (!hasActiveWidgets()) return
        scheduleRender(forceFull = forceFullUpdate)
    }

    fun updateProviderWidgets(
        providerClass: Class<*>,
        appWidgetIds: IntArray,
        defaultSize: NovaWidgetSize
    ) {
        if (appWidgetIds.isEmpty()) return
        scope.launch {
            updateMutex.withLock {
                val manager = AppWidgetManager.getInstance(context)
                val settings = runCatching { preferencesRepository.appSettings.first() }
                    .getOrDefault(currentSettings)
                currentSettings = settings
                val widgetState = resolveEffectiveWidgetState(settings)
                lastFullFingerprint = buildFingerprintFromWidgetState(widgetState)

                for (widgetId in appWidgetIds) {
                    val size = NovaWidgetRenderer.resolveWidgetSizeForId(
                        appWidgetManager = manager,
                        widgetId = widgetId,
                        defaultSize = defaultSize
                    )
                    val views = NovaWidgetRenderer.buildRemoteViews(
                        context = context,
                        providerClass = providerClass,
                        widgetSize = size,
                        state = widgetState
                    )
                    manager.updateAppWidget(widgetId, views)
                }
            }
        }
    }

    private fun scheduleRender(forceFull: Boolean) {
        if (!forceFull) return
        pendingJob?.cancel()
        pendingJob = scope.launch {
            updateMutex.withLock {
                if (!hasActiveWidgets()) return@withLock
                val manager = AppWidgetManager.getInstance(context)
                val settings = currentSettings
                val widgetState = resolveEffectiveWidgetState(settings)
                lastFullFingerprint = buildFingerprintFromWidgetState(widgetState)

                renderProviderIds(
                    manager = manager,
                    providerClass = NovaMusicMediumWidgetProvider::class.java,
                    defaultSize = NovaWidgetSize.MEDIUM,
                    state = widgetState
                )
                renderProviderIds(
                    manager = manager,
                    providerClass = NovaMusicLargeWidgetProvider::class.java,
                    defaultSize = NovaWidgetSize.LARGE,
                    state = widgetState
                )
            }
        }
    }

    private suspend fun renderProviderIds(
        manager: AppWidgetManager,
        providerClass: Class<*>,
        defaultSize: NovaWidgetSize,
        state: NovaWidgetState
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        if (ids.isEmpty()) return
        for (widgetId in ids) {
            val size = NovaWidgetRenderer.resolveWidgetSizeForId(
                appWidgetManager = manager,
                widgetId = widgetId,
                defaultSize = defaultSize
            )
            val views = NovaWidgetRenderer.buildRemoteViews(
                context = context,
                providerClass = providerClass,
                widgetSize = size,
                state = state
            )
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun pushProgressOnly(positionMs: Long, durationMs: Long) {
        val manager = AppWidgetManager.getInstance(context)
        pushProgressForProvider(
            manager = manager,
            providerClass = NovaMusicMediumWidgetProvider::class.java,
            defaultSize = NovaWidgetSize.MEDIUM,
            positionMs = positionMs,
            durationMs = durationMs
        )
        pushProgressForProvider(
            manager = manager,
            providerClass = NovaMusicLargeWidgetProvider::class.java,
            defaultSize = NovaWidgetSize.LARGE,
            positionMs = positionMs,
            durationMs = durationMs
        )
    }

    private fun pushProgressForProvider(
        manager: AppWidgetManager,
        providerClass: Class<*>,
        defaultSize: NovaWidgetSize,
        positionMs: Long,
        durationMs: Long
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        if (ids.isEmpty()) return
        for (widgetId in ids) {
            val size = NovaWidgetRenderer.resolveWidgetSizeForId(
                appWidgetManager = manager,
                widgetId = widgetId,
                defaultSize = defaultSize
            )
            val partial = NovaWidgetRenderer.buildProgressPartialRemoteViews(
                context = context,
                widgetSize = size,
                positionMs = positionMs,
                durationMs = durationMs
            )
            manager.partiallyUpdateAppWidget(widgetId, partial)
        }
    }

    private suspend fun resolveEffectiveWidgetState(settings: AppSettings): NovaWidgetState {
        val live = latestPlayerState
        val liveSong = live.currentSong
        if (liveSong != null) {
            return NovaWidgetState(
                songId = liveSong.id,
                title = liveSong.title,
                artist = liveSong.artist,
                album = liveSong.album,
                artworkUri = liveSong.artworkUri.takeIf { it != Uri.EMPTY },
                isPlaying = live.isPlaying,
                positionMs = live.positionMs,
                durationMs = live.durationMs.takeIf { it > 0L } ?: liveSong.durationMs,
                dynamicColorsEnabled = settings.dynamicColors,
                accentHex = settings.accentColor.hex
            )
        }

        // Cold-start / idle fallback: display last played track from persistence if available
        val saved = runCatching { preferencesRepository.getPlaybackPersistenceState() }.getOrNull()
        if (saved != null && saved.lastSongId > 0L) {
            if (musicRepository.allSongs.value.isEmpty()) {
                runCatching { musicRepository.refreshLibrary() }
            }
            val persistedSong = musicRepository.allSongs.value.firstOrNull { it.id == saved.lastSongId }
            if (persistedSong != null) {
                return NovaWidgetState(
                    songId = persistedSong.id,
                    title = persistedSong.title,
                    artist = persistedSong.artist,
                    album = persistedSong.album,
                    artworkUri = persistedSong.artworkUri.takeIf { it != Uri.EMPTY },
                    isPlaying = false,
                    positionMs = saved.lastPositionMs.coerceAtLeast(0L),
                    durationMs = persistedSong.durationMs,
                    dynamicColorsEnabled = settings.dynamicColors,
                    accentHex = settings.accentColor.hex
                )
            }
        }

        return NovaWidgetState(
            dynamicColorsEnabled = settings.dynamicColors,
            accentHex = settings.accentColor.hex
        )
    }

    private fun buildFingerprint(state: PlayerUiState, settings: AppSettings): RenderFingerprint {
        val song = state.currentSong
        return RenderFingerprint(
            songId = song?.id ?: -1L,
            artworkUri = song?.artworkUri?.toString().orEmpty(),
            isPlaying = state.isPlaying,
            dynamicColors = settings.dynamicColors,
            accentHex = settings.accentColor.hex,
            isNightMode = NovaWidgetBitmapRenderer.isSystemNightMode(context)
        )
    }

    private fun buildFingerprintFromWidgetState(state: NovaWidgetState): RenderFingerprint {
        return RenderFingerprint(
            songId = state.songId,
            artworkUri = state.artworkUri?.toString().orEmpty(),
            isPlaying = state.isPlaying,
            dynamicColors = state.dynamicColorsEnabled,
            accentHex = state.accentHex,
            isNightMode = NovaWidgetBitmapRenderer.isSystemNightMode(context)
        )
    }
}
