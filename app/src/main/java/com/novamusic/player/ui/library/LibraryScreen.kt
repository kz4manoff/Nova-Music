package com.novamusic.player.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.Album
import com.novamusic.player.data.model.Artist
import com.novamusic.player.data.model.Genre
import com.novamusic.player.data.model.MusicFolder
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.ui.components.AddToPlaylistDialog
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassCard
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSearchBar
import com.novamusic.player.ui.components.NovaAlbumArtwork
import com.novamusic.player.ui.components.SongListItem
import com.novamusic.player.ui.components.SongOptionsBottomSheet
import com.novamusic.player.ui.components.TrackInfoDialog
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaGold
import com.novamusic.player.ui.theme.NovaMonoTelemetry

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    initialFocusSearch: Boolean = false,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenLyricsForSong: (Song) -> Unit,
    bottomContentPadding: Dp
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val selectedTrackInfo by viewModel.selectedTrackInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var showSearchBar by remember(initialFocusSearch) { mutableStateOf(initialFocusSearch) }
    var showSortMenu by remember { mutableStateOf(false) }
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    val safTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.openSafFolder(uri)
        }
    }

    val safFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openSafAudioFile(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Floating Liquid Glass Header
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Music Library",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${state.songs.size} tracks • ${state.albums.size} albums",
                        style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Normal),
                        color = accent
                    )
                }

                LiquidGlassIconButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search",
                    active = showSearchBar,
                    size = 38.dp,
                    iconSize = 19.dp,
                    onClick = { showSearchBar = !showSearchBar }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = "Sort",
                        size = 38.dp,
                        iconSize = 19.dp,
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = novaColors.elevatedSurface
                    ) {
                        SortOrder.entries.forEach { order ->
                            val selected = state.sortOrder == order
                            DropdownMenuItem(
                                text = {
                                    val arrow = if (selected) {
                                        if (state.sortDirection == SortDirection.ASCENDING) " ↑" else " ↓"
                                    } else ""
                                    Text(
                                        text = order.label + arrow,
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    val nextDir = if (selected) {
                                        if (state.sortDirection == SortDirection.ASCENDING) {
                                            SortDirection.DESCENDING
                                        } else {
                                            SortDirection.ASCENDING
                                        }
                                    } else {
                                        SortDirection.ASCENDING
                                    }
                                    viewModel.setSort(order, nextDir)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (state.isScanning) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent)
                    }
                } else {
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Library",
                        size = 38.dp,
                        iconSize = 19.dp,
                        onClick = viewModel::refreshLibrary
                    )
                }
            }
        }

        AnimatedVisibility(visible = showSearchBar || state.searchQuery.isNotEmpty()) {
            LiquidGlassSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                placeholder = "Search songs, artists, albums, folders...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Floating Glass Category Pill Rail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                LiquidGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    highlighted = isSelected,
                    elevation = if (isSelected) 10.dp else 4.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                    onClick = { selectedTab = tab }
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when (selectedTab) {
            LibraryTab.SONGS -> {
                SongsListContent(
                    songs = state.songs,
                    currentSongId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    favoriteIds = state.favoriteIds,
                    highResArtwork = state.highResArtwork,
                    bottomPadding = bottomContentPadding,
                    onPlayAll = { viewModel.playAll(state.songs, shuffle = false) },
                    onShuffleAll = { viewModel.playAll(state.songs, shuffle = true) },
                    onSongClick = { song -> viewModel.playSong(song, state.songs) },
                    onFavoriteClick = viewModel::toggleFavorite,
                    onMoreClick = { optionsSong = it }
                )
            }
            LibraryTab.ALBUMS -> {
                AlbumsGridContent(
                    albums = state.albums,
                    highResArtwork = state.highResArtwork,
                    bottomPadding = bottomContentPadding,
                    onAlbumClick = { onOpenAlbum(it.id) }
                )
            }
            LibraryTab.ARTISTS -> {
                ArtistsListContent(
                    artists = state.artists,
                    highResArtwork = state.highResArtwork,
                    bottomPadding = bottomContentPadding,
                    onArtistClick = { onOpenArtist(it.name) }
                )
            }
            LibraryTab.FOLDERS -> {
                FoldersManagerContent(
                    folders = state.folders,
                    safSongs = state.safPickedSongs,
                    safFolderName = state.safFolderName,
                    currentSongId = playerState.currentSong?.id,
                    isPlaying = playerState.isPlaying,
                    favoriteIds = state.favoriteIds,
                    bottomPadding = bottomContentPadding,
                    onFolderClick = { onOpenFolder(it.path) },
                    onToggleFavoriteFolder = viewModel::toggleFavoriteFolder,
                    onPickSafFolder = { safTreeLauncher.launch(null) },
                    onPickSafAudioFile = { safFileLauncher.launch(arrayOf("audio/*")) },
                    onClearSafFolder = viewModel::clearSafFolder,
                    onPlaySafSong = { song -> viewModel.playSong(song, state.safPickedSongs) },
                    onFavoriteSong = viewModel::toggleFavorite,
                    onMoreSong = { optionsSong = it }
                )
            }
            LibraryTab.GENRES -> {
                GenresListContent(
                    genres = state.genres,
                    bottomPadding = bottomContentPadding,
                    onGenreClick = { onOpenGenre(it.name) }
                )
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
            onViewInfo = { viewModel.loadTrackInfo(song) }
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

@Composable
private fun SongsListContent(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    highResArtwork: Boolean,
    bottomPadding: Dp,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onMoreClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 10.dp,
            top = 4.dp,
            bottom = bottomPadding + 20.dp
        )
    ) {
        if (songs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassButton(
                        onClick = onPlayAll,
                        highlighted = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Play (${songs.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    LiquidGlassButton(
                        onClick = onShuffleAll,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching tracks found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                isCurrentSong = song.id == currentSongId,
                isPlaying = isPlaying,
                isFavorite = song.id in favoriteIds,
                onClick = { onSongClick(song) },
                onFavoriteClick = { onFavoriteClick(song.id) },
                onMoreClick = { onMoreClick(song) },
                highResArtwork = highResArtwork
            )
        }
    }
}

@Composable
private fun AlbumsGridContent(
    albums: List<Album>,
    highResArtwork: Boolean,
    bottomPadding: Dp,
    onAlbumClick: (Album) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val columns = if (screenWidth >= 600) 4 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPadding + 20.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = 14.dp,
                contentPadding = PaddingValues(6.dp),
                onClick = { onAlbumClick(album) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NovaAlbumArtwork(
                        artworkUri = album.artworkUri,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 18.dp,
                        highRes = highResArtwork,
                        fallbackIcon = Icons.Rounded.Album
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.22f),
                                        Color.Black.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                    if (album.year > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(9.dp)
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
                            .padding(11.dp)
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
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
private fun ArtistsListContent(
    artists: List<Artist>,
    highResArtwork: Boolean,
    bottomPadding: Dp,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(artists, key = { "${it.id}_${it.name}" }) { artist ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = 8.dp,
                contentPadding = PaddingValues(12.dp),
                onClick = { onArtistClick(artist) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.4.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color.White.copy(alpha = 0.35f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        NovaAlbumArtwork(
                            artworkUri = artist.artworkUri,
                            contentDescription = artist.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            cornerRadius = 28.dp,
                            highRes = highResArtwork,
                            fallbackIcon = Icons.Rounded.Person
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${artist.albumCount} albums • ${artist.songCount} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersManagerContent(
    folders: List<MusicFolder>,
    safSongs: List<Song>,
    safFolderName: String?,
    currentSongId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    bottomPadding: Dp,
    onFolderClick: (MusicFolder) -> Unit,
    onToggleFavoriteFolder: (MusicFolder) -> Unit,
    onPickSafFolder: () -> Unit,
    onPickSafAudioFile: () -> Unit,
    onClearSafFolder: () -> Unit,
    onPlaySafSong: (Song) -> Unit,
    onFavoriteSong: (Long) -> Unit,
    onMoreSong: (Song) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassButton(
                    onClick = onPickSafFolder,
                    highlighted = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "SAF Folder",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }
                LiquidGlassButton(
                    onClick = onPickSafAudioFile,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Audio File", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }

        if (safSongs.isNotEmpty()) {
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    highlighted = true,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FolderSpecial, contentDescription = null, tint = accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${safFolderName ?: "Picked Folder"} (${safSongs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearSafFolder) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Close")
                        }
                    }
                }
            }
            items(safSongs, key = { "saf_${it.id}" }) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                    isPlaying = isPlaying,
                    isFavorite = song.id in favoriteIds,
                    onClick = { onPlaySafSong(song) },
                    onFavoriteClick = { onFavoriteSong(song.id) },
                    onMoreClick = { onMoreSong(song) }
                )
            }
        }

        items(folders, key = { it.path }) { folder ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                highlighted = folder.isFavorite,
                accentTint = if (folder.isFavorite) NovaGold else accent,
                elevation = 8.dp,
                contentPadding = PaddingValues(14.dp),
                onClick = { onFolderClick(folder) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = accent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folder.songCount} tracks • ${folder.path}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onToggleFavoriteFolder(folder) }) {
                        Icon(
                            imageVector = if (folder.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Favorite Folder",
                            tint = if (folder.isFavorite) NovaGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresListContent(
    genres: List<Genre>,
    bottomPadding: Dp,
    onGenreClick: (Genre) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(genres, key = { it.name }) { genre ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 8.dp,
                contentPadding = PaddingValues(16.dp),
                onClick = { onGenreClick(genre) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = accent
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${genre.songCount} tracks",
                        style = NovaMonoTelemetry,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    artworkUri: Uri? = null,
    songs: List<Song>,
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onOpenLyricsForSong: (Song) -> Unit,
    bottomContentPadding: Dp
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTrackInfo by viewModel.selectedTrackInfo.collectAsStateWithLifecycle()
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (artworkUri != null) {
            LiquidGlassBackdrop(
                artworkUri = artworkUri,
                isPlaying = playerState.isPlaying,
                intensity = 1.0f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(26.dp),
                elevation = 14.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        size = 38.dp,
                        iconSize = 19.dp,
                        onClick = onBack
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (artworkUri != null) {
                        NovaAlbumArtwork(
                            artworkUri = artworkUri,
                            contentDescription = title,
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp)),
                            cornerRadius = 16.dp,
                            highRes = state.highResArtwork
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$subtitle • ${songs.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            SongsListContent(
                songs = songs,
                currentSongId = playerState.currentSong?.id,
                isPlaying = playerState.isPlaying,
                favoriteIds = state.favoriteIds,
                highResArtwork = state.highResArtwork,
                bottomPadding = bottomContentPadding,
                onPlayAll = { viewModel.playAll(songs, shuffle = false) },
                onShuffleAll = { viewModel.playAll(songs, shuffle = true) },
                onSongClick = { song -> viewModel.playSong(song, songs) },
                onFavoriteClick = viewModel::toggleFavorite,
                onMoreClick = { optionsSong = it }
            )
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
            onViewInfo = { viewModel.loadTrackInfo(song) }
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
