package com.novamusic.player

import android.app.Application
import android.content.Context
import com.novamusic.player.audio.AudioPlayerManager
import com.novamusic.player.audio.EqualizerManager
import com.novamusic.player.data.preferences.PreferencesRepository
import com.novamusic.player.data.repository.MusicRepository
import com.novamusic.player.data.scanner.MusicScanner
import com.novamusic.player.database.NovaDatabase

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
    val equalizerManager: EqualizerManager = EqualizerManager(preferencesRepository)
    val audioOutputManager: AudioOutputManager = AudioOutputManager(appContext)
    val audioPlayerManager: AudioPlayerManager = AudioPlayerManager(
        context = appContext,
        repository = musicRepository,
        preferences = preferencesRepository
    )
}

class NovaMusicApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.audioOutputManager.start()
    }
}
