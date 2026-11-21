package com.novamusic.player.navigation

import android.net.Uri

object NovaDestinations {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LIBRARY_SEARCH = "library?search=true"
    const val SEARCH = "search"
    const val PLAYLISTS = "playlists"
    const val WRAPPED = "wrapped"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_PLAYBACK = "settings/playback"
    const val SETTINGS_EQUALIZER = "settings/equalizer"
    const val SETTINGS_HEADPHONES = "settings/headphones"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_FCM_DIAGNOSTICS = "settings/fcm-diagnostics"
    const val SETTINGS_LIBRARY = "settings/library"
    const val SETTINGS_DOWNLOADS = "settings/downloads"
    const val SETTINGS_STORAGE = "settings/storage"
    const val SETTINGS_UPDATES = "settings/updates"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_ABOUT = "settings/about"
    const val ABOUT = "about"

    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist?name={artistName}"
    const val FOLDER_DETAIL = "folder?path={folderPath}"
    const val GENRE_DETAIL = "genre?name={genreName}"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val FAVORITES_DETAIL = "favorites"

    fun albumDetail(albumId: Long) = "album/$albumId"
    fun artistDetail(artistName: String) = "artist?name=${Uri.encode(artistName)}"
    fun folderDetail(folderPath: String) = "folder?path=${Uri.encode(folderPath)}"
    fun genreDetail(genreName: String) = "genre?name=${Uri.encode(genreName)}"
    fun playlistDetail(playlistId: Long) = "playlist/$playlistId"
}
