package com.novamusic.player.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.layout.ContentScale
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
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSearchBar
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.theme.LocalArtworkPalette
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
        contentPadding = PaddingValues(bottom = bottomContentPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Airy Floating iOS Header
        item {
            HomeFloatingHeader(
                isEqualizerActive = isEqualizerActive,
                isScanning = state.isScanning,
                onRefresh = viewModel::refreshLibrary,
                onOpenEqualizer = onOpenEqualizer,
                onOpenSettings = onOpenSettings
            )
        }

        // Frosted Glass Search Pill Launcher
        item {
            LiquidGlassSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search songs, albums, artists...",
                readOnlyOnClick = onOpenSearch,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Atmospheric Hero Deck
        item {
            HomeHeroStage(
                totalSongs = state.allSongsCount,
                albumsCount = state.albums.size,
                artistsCount = state.artists.size,
                playlistsCount = state.playlists.size,
                currentSong = playerState.currentSong,
                isPlaying = playerState.isPlaying,
                highResArtwork = state.highResArtwork,
                onShuffleAll = viewModel::shuffleAll,
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
                    title = "Heavy Rotation",
                    subtitle = "Most played",
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
private fun HomeFloatingHeader(
    isEqualizerActive: Boolean,
    isScanning: Boolean,
    onRefresh: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val palette = LocalArtworkPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidGlassSurface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            highlighted = true,
            accentTint = palette.primary,
            elevation = 12.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nova_logo),
                    contentDescription = "NOVA MUSIC",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)) {
                        append("Listen Now")
                    }
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp)
            )
            Text(
                text = "NOVA MUSIC • HI-RES AUDIO",
                style = NovaMonoTelemetry.copy(fontSize = 9.5.sp),
                color = accent
            )
        }

        if (isScanning) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent)
            }
        } else {
            LiquidGlassIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Rescan Library",
                size = 40.dp,
                iconSize = 20.dp,
                onClick = onRefresh
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        LiquidGlassIconButton(
            icon = Icons.Rounded.Equalizer,
            contentDescription = "Equalizer",
            active = isEqualizerActive,
            showDotWhenActive = true,
            tint = accent,
            size = 40.dp,
            iconSize = 20.dp,
            onClick = onOpenEqualizer
        )

        Spacer(modifier = Modifier.width(8.dp))

        LiquidGlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            size = 40.dp,
            iconSize = 20.dp,
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun HomeHeroStage(
    totalSongs: Int,
    albumsCount: Int,
    artistsCount: Int,
    playlistsCount: Int,
    currentSong: Song?,
    isPlaying: Boolean,
    highResArtwork: Boolean,
    onShuffleAll: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val palette = LocalArtworkPalette.current

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(30.dp),
        highlighted = currentSong != null,
        accentTint = palette.primary,
        elevation = 20.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        val reducedMotion = LocalReducedMotion.current
        Column {
            AnimatedContent(
                targetState = currentSong,
                transitionSpec = {
                    if (reducedMotion) fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    else fadeIn(tween(260, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(180))
                },
                contentKey = { it?.id ?: -1L },
                label = "home_hero_stage"
            ) { activeSong ->
                if (activeSong != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenNowPlaying),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassArtwork(
                            artworkUri = activeSong.artworkUri,
                            contentDescription = activeSong.album,
                            modifier = Modifier.size(66.dp),
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 18.dp,
                            highRes = highResArtwork
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "NOW PLAYING" else "PAUSED",
                                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                color = palette.glow
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeSong.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeSong.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AnimatedEqualizerBars(isPlaying = isPlaying, color = accent)
                    }
                } else {
                    Column {
                        Text(
                            text = if (totalSongs > 0) "$totalSongs Offline Tracks Ready" else "Offline Audio Library",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$albumsCount albums • $artistsCount artists • $playlistsCount playlists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidGlassButton(
                    onClick = onShuffleAll,
                    enabled = totalSongs > 0,
                    highlighted = true,
                    shape = RoundedCornerShape(18.dp),
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
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Library ($totalSongs)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryHomeCard(onRescan: () -> Unit) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(26.dp),
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
                text = "Add audio files to your device or open Library → Folders to pick folders directly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            LiquidGlassButton(
                onClick = onRescan,
                highlighted = true,
                shape = RoundedCornerShape(16.dp)
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(songs, key = { _, s -> "${title}_${s.id}" }) { index, song ->
                val isCurrent = song.id == currentSongId
                Column(
                    modifier = Modifier
                        .animateItem()
                        .width(148.dp)
                        .clickable { onSongClick(song) }
                ) {
                    Box {
                        LiquidGlassArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier.size(148.dp),
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = if (isCurrent) 22.dp else 14.dp,
                            highRes = highResArtwork
                        )
                        if (showRank) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.62f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .shadow(10.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) Color.Black.copy(alpha = 0.72f)
                                    else Color.White.copy(alpha = 0.24f)
                                )
                                .border(0.8.dp, Color.White.copy(alpha = 0.52f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                AnimatedEqualizerBars(isPlaying = isPlaying, color = accent)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isCurrent) accent else MaterialTheme.colorScheme.onBackground,
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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                Column(
                    modifier = Modifier
                        .width(178.dp)
                        .clickable { onAlbumClick(album) }
                ) {
                    Box(modifier = Modifier.size(178.dp)) {
                        LiquidGlassArtwork(
                            artworkUri = album.artworkUri,
                            contentDescription = album.name,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(26.dp),
                            shadowElevation = 22.dp,
                            highRes = highResArtwork,
                            fallbackIcon = Icons.Rounded.Album
                        )
                        if (album.year > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.56f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = album.year.toString(),
                                    style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(9.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist} • ${album.songCount} tracks",
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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists, key = { "${it.id}_${it.name}" }) { artist ->
                Column(
                    modifier = Modifier
                        .width(98.dp)
                        .clickable { onArtistClick(artist) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidGlassArtwork(
                        artworkUri = artist.artworkUri,
                        contentDescription = artist.name,
                        modifier = Modifier.size(90.dp),
                        shape = CircleShape,
                        shadowElevation = 16.dp,
                        highRes = highResArtwork,
                        fallbackIcon = Icons.Rounded.Person
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
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
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                shape = RoundedCornerShape(16.dp),
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
                LiquidGlassSurface(
                    modifier = Modifier.width(196.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 12.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = { onPlaylistClick(playlist) }
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LiquidGlassArtwork(
                                artworkUri = playlist.songs.firstOrNull()?.artworkUri,
                                contentDescription = playlist.name,
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(15.dp),
                                shadowElevation = 10.dp,
                                highRes = highResArtwork,
                                fallbackIcon = Icons.Rounded.QueueMusic
                            )
                            if (playlist.songs.isNotEmpty()) {
                                LiquidGlassIconButton(
                                    icon = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Playlist",
                                    active = true,
                                    size = 38.dp,
                                    iconSize = 20.dp,
                                    onClick = { onPlayPlaylist(playlist) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
