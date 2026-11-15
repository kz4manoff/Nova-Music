package com.novamusic.player.navigation

import android.net.Uri

object NovaDestinations {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LIBRARY_SEARCH = "library?search=true"
    const val SEARCH = "search"
    const val PLAYLISTS = "playlists"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
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
