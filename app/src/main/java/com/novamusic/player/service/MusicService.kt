package com.novamusic.player.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.novamusic.player.MainActivity
import com.novamusic.player.NovaMusicApplication
import com.novamusic.player.R
import com.novamusic.player.data.model.AudioFocusBehavior
import com.novamusic.player.data.model.AudioOutputType
import com.novamusic.player.data.model.AudioQualityMode
import com.novamusic.player.data.model.NotificationAppearance
import com.novamusic.player.data.preferences.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    @Volatile
    private var currentSettings: AppSettings = AppSettings()

    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
                currentSettings.pauseOnHeadphonesDisconnect &&
                ::exoPlayer.isInitialized &&
                exoPlayer.playWhenReady
            ) {
                exoPlayer.pause()
            }
        }
    }
    private var noisyReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        val container = (application as NovaMusicApplication).container

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        ContextCompat.registerReceiver(
            this,
            audioBecomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_EXPORTED
        )
        noisyReceiverRegistered = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    container.equalizerManager.attachToSession(audioSessionId)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    persistCurrentPosition(container)
                }
            }
        })

        if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId > 0) {
            container.equalizerManager.attachToSession(exoPlayer.audioSessionId)
        }

        container.audioOutputManager.start()
        serviceScope.launch {
            container.audioOutputManager.preferredAudioDeviceInfo.collectLatest { preferredDevice ->
                exoPlayer.setPreferredAudioDevice(preferredDevice)
            }
        }
        serviceScope.launch {
            container.audioOutputManager.externalDeviceConnectedEvents.collect { device ->
                val isHeadphoneDevice = device.type == AudioOutputType.WIRED_HEADPHONES ||
                    device.type == AudioOutputType.BLUETOOTH_HEADPHONES ||
                    device.type == AudioOutputType.USB_AUDIO
                if (isHeadphoneDevice &&
                    currentSettings.resumeOnHeadphonesConnect &&
                    ::exoPlayer.isInitialized &&
                    exoPlayer.mediaItemCount > 0 &&
                    !exoPlayer.isPlaying
                ) {
                    if (exoPlayer.playbackState == Player.STATE_IDLE) {
                        exoPlayer.prepare()
                    }
                    exoPlayer.play()
                }
            }
        }

        serviceScope.launch {
            container.preferencesRepository.appSettings.collectLatest { settings ->
                currentSettings = settings
                exoPlayer.pauseAtEndOfMediaItems = !settings.gaplessPlayback
                val handleFocus = settings.audioFocusBehavior != AudioFocusBehavior.IGNORE_TRANSIENT
                exoPlayer.setAudioAttributes(audioAttributes, handleFocus)
                exoPlayer.setWakeMode(
                    when (settings.audioQuality) {
                        AudioQualityMode.LOSSLESS_DIRECT -> C.WAKE_MODE_LOCAL
                        AudioQualityMode.HIGH_RES -> C.WAKE_MODE_LOCAL
                        AudioQualityMode.BALANCED -> C.WAKE_MODE_NONE
                    }
                )
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaItemsWithStartPosition> {
                val future = SettableFuture.create<MediaItemsWithStartPosition>()
                serviceScope.launch {
                    try {
                        if (container.musicRepository.allSongs.value.isEmpty()) {
                            container.musicRepository.refreshLibrary()
                        }
                        val allSongs = container.musicRepository.allSongs.value
                        val byId = allSongs.associateBy { it.id }
                        val saved = container.preferencesRepository.getPlaybackPersistenceState()
                        val queueSongs = saved.queueSongIds.mapNotNull { byId[it] }
                            .ifEmpty { listOfNotNull(byId[saved.lastSongId]) }

                        if (queueSongs.isEmpty()) {
                            future.setException(IllegalStateException("No saved playback queue"))
                            return@launch
                        }

                        container.audioPlayerManager.registerSongs(queueSongs)
                        val startIdx = queueSongs.indexOfFirst { it.id == saved.lastSongId }.coerceAtLeast(0)
                        val includeArt = currentSettings.showNotificationArtwork
                        val mediaItems = queueSongs.map { song ->
                            val metaBuilder = MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .setAlbumTitle(song.album)
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            if (includeArt) {
                                metaBuilder.setArtworkUri(song.artworkUri)
                            }
                            MediaItem.Builder()
                                .setMediaId(song.id.toString())
                                .setUri(song.uri)
                                .setMediaMetadata(metaBuilder.build())
                                .build()
                        }
                        future.set(
                            MediaItemsWithStartPosition(
                                mediaItems,
                                startIdx,
                                saved.lastPositionMs.coerceAtLeast(0L)
                            )
                        )
                    } catch (t: Throwable) {
                        future.setException(t)
                    }
                }
                return future
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        val defaultProvider = DefaultMediaNotificationProvider.Builder(this)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_nova_notification)
            }

        val customProvider = object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val base = defaultProvider.createNotification(
                    mediaSession,
                    customLayout,
                    actionFactory,
                    onNotificationChangedCallback
                )
                val s = currentSettings
                val notification = base.notification
                notification.visibility = if (s.notificationsEnabled && s.lockScreenControls) {
                    Notification.VISIBILITY_PUBLIC
                } else {
                    Notification.VISIBILITY_SECRET
                }
                if (!s.notificationsEnabled || s.notificationAppearance == NotificationAppearance.COMPACT) {
                    notification.priority = Notification.PRIORITY_MIN
                }
                return base
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean = defaultProvider.handleCustomCommand(session, action, extras)
        }
        setMediaNotificationProvider(customProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val container = (application as NovaMusicApplication).container
        persistCurrentPosition(container)
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val container = (application as NovaMusicApplication).container
        persistCurrentPosition(container)
        if (noisyReceiverRegistered) {
            runCatching { unregisterReceiver(audioBecomingNoisyReceiver) }
            noisyReceiverRegistered = false
        }
        serviceScope.cancel()
        container.equalizerManager.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun persistCurrentPosition(container: com.novamusic.player.AppContainer) {
        if (!::exoPlayer.isInitialized) return
        val songId = exoPlayer.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val queueIds = (0 until exoPlayer.mediaItemCount).mapNotNull {
            exoPlayer.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        val shuffle = exoPlayer.shuffleModeEnabled
        val repeat = when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_ALL -> com.novamusic.player.data.model.RepeatModeOption.ALL
            Player.REPEAT_MODE_ONE -> com.novamusic.player.data.model.RepeatModeOption.ONE
            else -> com.novamusic.player.data.model.RepeatModeOption.OFF
        }
        serviceScope.launch {
            container.preferencesRepository.savePlaybackState(
                songId = songId,
                positionMs = pos,
                queueIds = queueIds,
                shuffleEnabled = shuffle,
                repeatMode = repeat
            )
        }
    }
}
