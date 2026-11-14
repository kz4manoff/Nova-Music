package com.novamusic.player.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.R
import com.novamusic.player.data.model.Album
import com.novamusic.player.data.model.Artist
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.components.AnimatedEqualizerBars
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassCard
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSearchBar
import com.novamusic.player.ui.components.NovaAlbumArtwork
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isEqualizerActive: Boolean = false,
    onOpenSearch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    bottomContentPadding: Dp
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Floating Liquid Glass Top Navigation
        item {
            HomeTopHeader(
                isEqualizerActive = isEqualizerActive,
                onOpenEqualizer = onOpenEqualizer,
                onOpenSettings = onOpenSettings
            )
        }

        // Frosted Glass Search Bar
        item {
            LiquidGlassSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search songs, albums, artists, genres...",
                readOnlyOnClick = onOpenSearch,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Floating Liquid Glass Hero Deck
        item {
            HomeHeroDeck(
                totalSongs = state.allSongsCount,
                albumsCount = state.albums.size,
                artistsCount = state.artists.size,
                playlistsCount = state.playlists.size,
                currentSong = playerState.currentSong,
                isPlaying = playerState.isPlaying,
                isScanning = state.isScanning,
                highResArtwork = state.highResArtwork,
                onShuffleAll = viewModel::shuffleAll,
                onRefresh = viewModel::refreshLibrary,
                onBrowseLibrary = onOpenLibrary,
                onOpenNowPlaying = onOpenNowPlaying
            )
        }

        if (state.allSongsCount == 0 && !state.isScanning) {
            item {
                EmptyLibraryHomeCard(onRescan = viewModel::refreshLibrary)
            }
        }

        // Large Featured Albums Carousel
        if (state.albums.isNotEmpty()) {
            item {
                HorizontalAlbumsSection(
                    albums = state.albums,
                    highResArtwork = state.highResArtwork,
                    onAlbumClick = { album -> onOpenAlbum(album.id) }
                )
            }
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item {
                HorizontalSongsSection(
                    title = "Recently Played",
                    subtitle = "${state.recentlyPlayed.size} tracks",
                    songs = state.recentlyPlayed,
                    currentSongId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    highResArtwork = state.highResArtwork,
                    showRank = false,
                    onSongClick = { song -> viewModel.playSong(song, state.recentlyPlayed) }
                )
            }
        }

        if (state.recentlyAdded.isNotEmpty()) {
            item {
                HorizontalSongsSection(
                    title = "Recently Added",
                    subtitle = "${state.recentlyAdded.size} tracks",
                    songs = state.recentlyAdded,
                    currentSongId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    highResArtwork = state.highResArtwork,
                    showRank = false,
                    onSongClick = { song -> viewModel.playSong(song, state.recentlyAdded) }
                )
            }
        }

        if (state.mostPlayed.isNotEmpty()) {
            item {
                HorizontalSongsSection(
                    title = "Most Played",
                    subtitle = "Top rotation",
                    songs = state.mostPlayed,
                    currentSongId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    highResArtwork = state.highResArtwork,
                    showRank = true,
                    onSongClick = { song -> viewModel.playSong(song, state.mostPlayed) }
                )
            }
        }

        if (state.favoriteSongs.isNotEmpty()) {
            item {
                HorizontalSongsSection(
                    title = "Favorite Tracks",
                    subtitle = "${state.favoriteSongs.size} loved",
                    songs = state.favoriteSongs,
                    currentSongId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    highResArtwork = state.highResArtwork,
                    showRank = false,
                    onSongClick = { song -> viewModel.playSong(song, state.favoriteSongs) }
                )
            }
        }

        if (state.artists.isNotEmpty()) {
            item {
                HorizontalArtistsSection(
                    artists = state.artists,
                    highResArtwork = state.highResArtwork,
                    onArtistClick = { artist -> onOpenArtist(artist.name) }
                )
            }
        }

        item {
            QuickPlaylistsSection(
                playlists = state.playlists,
                highResArtwork = state.highResArtwork,
                onPlaylistClick = { playlist -> onOpenPlaylist(playlist.id) },
                onPlayPlaylist = viewModel::playPlaylist,
                onCreateClick = { showCreatePlaylistDialog = true }
            )
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistPromptDialog(
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }
}

@Composable
private fun HomeTopHeader(
    isEqualizerActive: Boolean,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 12.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nova_logo),
                    contentDescription = "NOVA MUSIC",
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)) {
                            append("NOVA ")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Light, color = accent)) {
                            append("MUSIC")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "LIQUID GLASS AUDIO",
                    style = NovaMonoTelemetry.copy(fontSize = 9.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LiquidGlassIconButton(
                icon = Icons.Rounded.Equalizer,
                contentDescription = "Equalizer",
                active = isEqualizerActive,
                showDotWhenActive = true,
                tint = accent,
                size = 38.dp,
                iconSize = 19.dp,
                onClick = onOpenEqualizer
            )

            Spacer(modifier = Modifier.width(8.dp))

            LiquidGlassIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                size = 38.dp,
                iconSize = 19.dp,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun HomeHeroDeck(
    totalSongs: Int,
    albumsCount: Int,
    artistsCount: Int,
    playlistsCount: Int,
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean,
    highResArtwork: Boolean,
    onShuffleAll: () -> Unit,
    onRefresh: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        highlighted = currentSong != null,
        elevation = 18.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (totalSongs > 0) "$totalSongs Tracks Ready" else "Local Audio Engine",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "MP3 • FLAC • WAV • AAC • M4A • OGG • OPUS",
                        style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(novaColors.recessedSurface)
                        .border(0.9.dp, novaColors.glassBorderTop.copy(alpha = 0.6f), CircleShape)
                        .clickable(enabled = !isScanning, onClick = onRefresh),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Rescan Library",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (currentSong != null) {
                Spacer(modifier = Modifier.height(14.dp))
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    highlighted = true,
                    elevation = 6.dp,
                    onClick = onOpenNowPlaying,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NovaAlbumArtwork(
                            artworkUri = currentSong.artworkUri,
                            contentDescription = currentSong.album,
                            modifier = Modifier.size(46.dp),
                            cornerRadius = 12.dp,
                            highRes = highResArtwork
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "NOW PLAYING" else "PAUSED SESSION",
                                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                color = accent
                            )
                            Text(
                                text = "${currentSong.title} — ${currentSong.artist}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AnimatedEqualizerBars(isPlaying = isPlaying, color = accent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(novaColors.recessedSurface)
                    .border(0.8.dp, novaColors.borderSubtle.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCell("ALBUMS", albumsCount.toString())
                StatCell("ARTISTS", artistsCount.toString())
                StatCell("PLAYLISTS", playlistsCount.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidGlassButton(
                    onClick = onShuffleAll,
                    enabled = totalSongs > 0,
                    highlighted = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shuffle All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                LiquidGlassButton(
                    onClick = onBrowseLibrary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explore Library", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = NovaMonoTelemetry.fontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = NovaMonoTelemetry.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLibraryHomeCard(onRescan: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No offline tracks found yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add audio files to your device or open Library → Folders to browse directories with Storage Access Framework.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            LiquidGlassButton(
                onClick = onRescan,
                highlighted = true,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Scan Device Music",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun HorizontalSongsSection(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    highResArtwork: Boolean,
    showRank: Boolean,
    onSongClick: (Song) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column {
        SectionHeader(title = title, subtitle = subtitle)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(songs, key = { _, s -> "${title}_${s.id}" }) { index, song ->
                val isCurrent = song.id == currentSongId
                LiquidGlassCard(
                    modifier = Modifier.width(156.dp),
                    shape = RoundedCornerShape(22.dp),
                    highlighted = isCurrent,
                    elevation = if (isCurrent) 14.dp else 8.dp,
                    contentPadding = PaddingValues(10.dp),
                    onClick = { onSongClick(song) }
                ) {
                    Column {
                        Box {
                            NovaAlbumArtwork(
                                artworkUri = song.artworkUri,
                                contentDescription = song.album,
                                modifier = Modifier.size(136.dp),
                                cornerRadius = 16.dp,
                                highRes = highResArtwork
                            )
                            if (showRank) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(7.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.72f))
                                        .border(0.7.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold),
                                        color = accent
                                    )
                                }
                            } else if (song.fileExtension in setOf("FLAC", "WAV", "OPUS")) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(7.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .border(0.7.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(7.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = song.fileExtension,
                                        style = NovaMonoTelemetry.copy(fontSize = 9.sp),
                                        color = accent
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(7.dp)
                                    .size(34.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) Color.Black.copy(alpha = 0.85f) else accent)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrent) {
                                    AnimatedEqualizerBars(isPlaying = isPlaying, color = accent)
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • ${FormatUtils.formatDuration(song.durationMs)}",
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

@Composable
private fun HorizontalAlbumsSection(
    albums: List<Album>,
    highResArtwork: Boolean,
    onAlbumClick: (Album) -> Unit
) {
    Column {
        SectionHeader(title = "Featured Albums", subtitle = "${albums.size} collections")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                LiquidGlassCard(
                    modifier = Modifier.size(width = 184.dp, height = 196.dp),
                    shape = RoundedCornerShape(26.dp),
                    elevation = 16.dp,
                    contentPadding = PaddingValues(6.dp),
                    onClick = { onAlbumClick(album) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NovaAlbumArtwork(
                            artworkUri = album.artworkUri,
                            contentDescription = album.name,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 20.dp,
                            highRes = highResArtwork,
                            fallbackIcon = Icons.Rounded.Album
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.18f),
                                            Color.Black.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        if (album.year > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(0.7.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = album.year.toString(),
                                    style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${album.songCount} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalArtistsSection(
    artists: List<Artist>,
    highResArtwork: Boolean,
    onArtistClick: (Artist) -> Unit
) {
    Column {
        SectionHeader(title = "Artists", subtitle = "${artists.size} artists")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists, key = { "${it.id}_${it.name}" }) { artist ->
                LiquidGlassCard(
                    modifier = Modifier.width(114.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 8.dp,
                    contentPadding = PaddingValues(10.dp),
                    onClick = { onArtistClick(artist) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.6.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color.White.copy(alpha = 0.35f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                        ) {
                            NovaAlbumArtwork(
                                artworkUri = artist.artworkUri,
                                contentDescription = artist.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                cornerRadius = 42.dp,
                                highRes = highResArtwork,
                                fallbackIcon = Icons.Rounded.Person
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${artist.songCount} tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPlaylistsSection(
    playlists: List<PlaylistWithSongs>,
    highResArtwork: Boolean,
    onPlaylistClick: (PlaylistWithSongs) -> Unit,
    onPlayPlaylist: (PlaylistWithSongs) -> Unit,
    onCreateClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${playlists.size} saved playlists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LiquidGlassButton(
                onClick = onCreateClick,
                highlighted = false,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", style = MaterialTheme.typography.labelLarge, color = accent)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists, key = { it.id }) { playlist ->
                LiquidGlassCard(
                    modifier = Modifier.width(190.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 10.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = { onPlaylistClick(playlist) }
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NovaAlbumArtwork(
                                artworkUri = playlist.songs.firstOrNull()?.artworkUri,
                                contentDescription = playlist.name,
                                modifier = Modifier.size(54.dp),
                                cornerRadius = 14.dp,
                                highRes = highResArtwork,
                                fallbackIcon = Icons.Rounded.QueueMusic
                            )
                            if (playlist.songs.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .shadow(8.dp, CircleShape, ambientColor = accent, spotColor = accent)
                                        .clip(CircleShape)
                                        .background(accent)
                                        .border(0.9.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { onPlayPlaylist(playlist) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = "Play Playlist",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlist.songs.size} tracks",
                            style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreatePlaylistPromptDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalNovaColors.current.elevatedSurface,
        title = { Text("New Playlist", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
