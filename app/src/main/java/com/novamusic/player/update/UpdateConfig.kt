package com.novamusic.player.update

import com.novamusic.player.BuildConfig

/**
 * Central configuration for NOVA MUSIC's automatic update system.
 *
 * To change the remote update.json endpoint, update `NOVA_UPDATE_CONFIG_URL`
 * in `gradle.properties` (single central location).
 */
object UpdateConfig {
    val updateJsonUrl: String
        get() = BuildConfig.UPDATE_CONFIG_URL

    const val GITHUB_REPO_OWNER = "kz4manoff"
    const val GITHUB_REPO_NAME = "Nova-Music"
    const val ALLOWED_APK_HOST = "github.com"
    const val ALLOWED_APK_PATH_PREFIX = "/kz4manoff/Nova-Music/releases/download/"

    val TRUSTED_DOWNLOAD_REDIRECT_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "github-releases.githubusercontent.com"
    )

    const val CHECK_INTERVAL_MS: Long = 24L * 60L * 60L * 1000L // 24 hours
    const val CONNECT_TIMEOUT_MS: Int = 15_000
    const val READ_TIMEOUT_MS: Int = 30_000
    const val MAX_REDIRECTS: Int = 5
    const val MAX_MANIFEST_BYTES: Int = 512 * 1024 // 512 KB safety cap
    const val MAX_APK_BYTES: Long = 250L * 1024L * 1024L // 250 MB safety cap
    const val UPDATES_SUBDIR: String = "updates"
}
