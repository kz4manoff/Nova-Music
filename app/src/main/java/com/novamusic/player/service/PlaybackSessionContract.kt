package com.novamusic.player.service

/**
 * Shared Media3 SessionCommand actions and Bundle keys synchronizing playback mode
 * across MusicService, Android media notifications / lock screen, and AudioPlayerManager.
 */
object PlaybackSessionContract {
    const val ACTION_CYCLE_PLAYBACK_MODE = "com.novamusic.player.action.CYCLE_PLAYBACK_MODE"
    const val ACTION_SET_PLAYBACK_MODE = "com.novamusic.player.action.SET_PLAYBACK_MODE"

    const val EXTRA_PLAYBACK_MODE = "extra_playback_mode"
    const val EXTRA_RESTORE_ONLY = "extra_restore_only"
    const val EXTRA_ORIGINAL_QUEUE_IDS = "extra_original_queue_ids"
}
