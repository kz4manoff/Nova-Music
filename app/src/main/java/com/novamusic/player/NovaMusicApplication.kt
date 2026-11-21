package com.novamusic.player

import android.app.Application
import android.content.Context
import android.util.Log
import com.novamusic.player.audio.AudioOutputManager
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.audio.EqualizerManager
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import com.novamusic.player.data.scanner.MusicScanner
import com.novamusic.player.database.NovaDatabase
import com.novamusic.player.push.NovaBroadcastNotificationHelper
import com.novamusic.player.update.AndroidUpdateNotifier
import com.novamusic.player.update.FcmUpdatePushManager
import com.novamusic.player.update.UpdateManager
import com.novamusic.player.widget.NovaWidgetCoordinator
import com.novamusic.player.wrapped.ListeningTracker
import com.novamusic.player.wrapped.WrappedRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: NovaDatabase = NovaDatabase.getInstance(appContext)
    val preferencesRepository: PreferencesRepository = PreferencesRepository(appContext)
    val musicScanner: MusicScanner = MusicScanner(appContext)
    val musicRepository: MusicRepository = MusicRepository(
        scanner = musicScanner,
        database = database,
        preferences = preferencesRepository
    )
    val wrappedRepository: WrappedRepository = WrappedRepository(database.listeningDao())
    val listeningTracker: ListeningTracker = ListeningTracker(
        persistCallback = { event -> wrappedRepository.persistListeningSession(event) }
    )
    val equalizerManager: EqualizerManager = EqualizerManager(preferencesRepository)
    val audioOutputManager: AudioOutputManager = AudioOutputManager(appContext)
    val widgetCoordinator: NovaWidgetCoordinator = NovaWidgetCoordinator(
        context = appContext,
        preferencesRepository = preferencesRepository,
        musicRepository = musicRepository
    )
    val audioPlayerManager: AudioPlayerManager = AudioPlayerManager(
        context = appContext,
        repository = musicRepository,
        preferences = preferencesRepository
    ).apply {
        bindWidgetCoordinator(widgetCoordinator)
    }
    val updateManager: UpdateManager = UpdateManager(
        context = appContext,
        preferences = preferencesRepository
    )
    val fcmUpdatePushManager: FcmUpdatePushManager = FcmUpdatePushManager(
        context = appContext,
        preferencesRepository = preferencesRepository
    )
}

class NovaMusicApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidUpdateNotifier.ensureNotificationChannel(this)
        NovaBroadcastNotificationHelper.ensureNotificationChannel(this)
        container = AppContainer(this)
        container.audioOutputManager.start()
        Log.i("NovaMusicApp", "Application started; triggering automatic FCM registration sync.")
        container.fcmUpdatePushManager.ensureRegisteredAsync()
    }
}
