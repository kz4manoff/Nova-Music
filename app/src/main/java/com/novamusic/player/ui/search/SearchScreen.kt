package com.novamusic.player.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.components.AddToPlaylistDialog
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassSearchBar
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.components.SongListItem
import com.novamusic.player.ui.components.SongOptionsBottomSheet
import com.novamusic.player.ui.components.TrackInfoDialog
import com.novamusic.player.ui.components.shareAudioTrack
import com.novamusic.player.ui.library.LibraryViewModel
import com.novamusic.player.ui.theme.LocalArtworkPalette

@Composable
fun SearchScreen(
    viewModel: LibraryViewModel,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenLyricsForSong: (Song) -> Unit,
    bottomContentPadding: Dp
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val selectedTrackInfo by viewModel.selectedTrackInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = LocalArtworkPalette.current
    val accent = MaterialTheme.colorScheme.primary

    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var filterLosslessOnly by remember { mutableStateOf(false) }

    val displayedSongs = remember(state.songs, filterLosslessOnly) {
        if (filterLosslessOnly) {
            state.songs.filter { it.fileExtension in setOf("FLAC", "WAV", "ALAC", "OPUS") }
        } else {
            state.songs
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LiquidGlassTopBar(
            title = "Search",
            subtitle = if (state.searchQuery.isBlank()) {
                "${state.songs.size} offline tracks indexed"
            } else {
                "${displayedSongs.size} matches"
            }
        )

        LiquidGlassSearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Songs, albums, artists, genres...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomContentPadding + 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Filter Chips Row
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            highlighted = filterLosslessOnly,
                            elevation = if (filterLosslessOnly) 12.dp else 6.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                            onClick = { filterLosslessOnly = !filterLosslessOnly }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.HighQuality,
                                    contentDescription = null,
                                    tint = if (filterLosslessOnly) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hi-Res / Lossless",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (filterLosslessOnly) accent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(state.genres.take(8), key = { it.name }) { genre ->
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            elevation = 6.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                            onClick = { onOpenGenre(genre.name) }
                        ) {
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (state.albums.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.albums.take(15), key = { it.id }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(142.dp)
                                        .clickable { onOpenAlbum(album.id) }
                                ) {
                                    LiquidGlassArtwork(
                                        artworkUri = album.artworkUri,
                                        contentDescription = album.name,
                                        modifier = Modifier.size(142.dp),
                                        shape = RoundedCornerShape(22.dp),
                                        shadowElevation = 18.dp,
                                        fallbackIcon = Icons.Rounded.Album
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = album.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.artists.isNotEmpty() && state.searchQuery.isNotBlank()) {
                item {
                    Column {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.artists.take(12), key = { "${it.id}_${it.name}" }) { artist ->
                                Column(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clickable { onOpenArtist(artist.name) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LiquidGlassArtwork(
                                        artworkUri = artist.artworkUri,
                                        contentDescription = artist.name,
                                        modifier = Modifier.size(84.dp),
                                        shape = CircleShape,
                                        shadowElevation = 14.dp,
                                        fallbackIcon = Icons.Rounded.Person
                                    )
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (state.searchQuery.isBlank() && !filterLosslessOnly) "All Tracks" else "Tracks",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (displayedSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = palette.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No matching tracks found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedSongs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isCurrentSong = song.id == playerState.currentSong?.id,
                        isPlaying = playerState.isPlaying,
                        isFavorite = song.id in state.favoriteIds,
                        onClick = { viewModel.playSong(song, displayedSongs) },
                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                        onMoreClick = { optionsSong = song },
                        highResArtwork = state.highResArtwork,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    optionsSong?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            isFavorite = song.id in state.favoriteIds,
            onDismiss = { optionsSong = null },
            onPlayNext = { viewModel.playNext(song) },
            onAddToQueue = { viewModel.addToQueue(song) },
            onAddToPlaylist = { addToPlaylistSong = song },
            onToggleFavorite = { viewModel.toggleFavorite(song.id) },
            onViewLyrics = { onOpenLyricsForSong(song) },
            onViewInfo = { viewModel.loadTrackInfo(song) },
            onGoToAlbum = { onOpenAlbum(song.albumId) },
            onGoToArtist = { onOpenArtist(song.artist) },
            onShare = { shareAudioTrack(context, song) }
        )
    }

    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(
            playlists = state.playlists,
            onSelectPlaylist = { pid -> viewModel.addSongToPlaylist(pid, song.id) },
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAddSong(name, song.id) },
            onDismiss = { addToPlaylistSong = null }
        )
    }

    selectedTrackInfo?.let { (song, info) ->
        TrackInfoDialog(
            song = song,
            fileInfo = info,
            onDismiss = viewModel::clearTrackInfo
        )
    }
}
