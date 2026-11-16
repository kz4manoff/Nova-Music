package com.novamusic.player.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.Album
import com.novamusic.player.data.model.Artist
import com.novamusic.player.data.model.Genre
import com.novamusic.player.data.model.MusicFolder
import com.novamusic.player.data.model.Song
import com.novamusic.player.data.model.SortDirection
import com.novamusic.player.data.model.SortOrder
import com.novamusic.player.ui.components.AddToPlaylistDialog
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSearchBar
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.SongListItem
import com.novamusic.player.ui.components.SongOptionsBottomSheet
import com.novamusic.player.ui.components.TrackInfoDialog
import com.novamusic.player.ui.components.shareAudioTrack
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaGold
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import kotlin.math.abs

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
        // Floating iOS Liquid Glass Top Bar
        LiquidGlassTopBar(
            title = "Library",
            subtitle = "${state.songs.size} tracks • ${state.albums.size} albums",
            trailingActions = {
                LiquidGlassIconButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search",
                    active = showSearchBar,
                    size = 38.dp,
                    iconSize = 19.dp,
                    onClick = { showSearchBar = !showSearchBar }
                )

                Box {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = "Sort",
                        size = 38.dp,
                        iconSize = 19.dp,
                        onClick = { showSortMenu = true }
                    )
                    val menuBorderColor = if (novaColors.isLight) Color(0x220F172A) else Color.White.copy(alpha = 0.18f)
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        offset = DpOffset(0.dp, 6.dp),
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            clippingEnabled = true
                        ),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = novaColors.elevatedSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 18.dp,
                        border = BorderStroke(0.8.dp, menuBorderColor),
                        modifier = Modifier
                            .widthIn(min = 188.dp, max = 240.dp)
                            .heightIn(max = 320.dp)
                    ) {
                        SortOrder.entries.forEach { order ->
                            val selected = state.sortOrder == order
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = if (state.sortDirection == SortDirection.ASCENDING) {
                                                Icons.Rounded.ArrowUpward
                                            } else {
                                                Icons.Rounded.ArrowDownward
                                            },
                                            contentDescription = if (state.sortDirection == SortDirection.ASCENDING) {
                                                "Ascending"
                                            } else {
                                                "Descending"
                                            },
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = if (selected) {
                                    Modifier
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accent.copy(alpha = if (novaColors.isLight) 0.11f else 0.16f))
                                } else {
                                    Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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

                if (state.isScanning) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = accent)
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
        )

        AnimatedVisibility(visible = showSearchBar || state.searchQuery.isNotEmpty()) {
            LiquidGlassSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                placeholder = "Filter library...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Spring-Sliding iOS Optical Glass Segment Control
        LibrarySlidingSegmentBar(
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val reducedMotion = LocalReducedMotion.current
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                } else {
                    (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(220))) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.99f, animationSpec = tween(150)))
                }
            },
            label = "library_tab_content"
        ) { activeTab ->
            when (activeTab) {
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

@Composable
private fun LibrarySlidingSegmentBar(
    selectedTab: LibraryTab,
    onSelectTab: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = LibraryTab.entries
    val selectedIdx = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val palette = LocalArtworkPalette.current
    val novaColors = LocalNovaColors.current
    val isLight = novaColors.isLight
    val accent = MaterialTheme.colorScheme.primary
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 10.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(4.dp)
        ) {
            val segWidth = maxWidth / tabs.size
            val segWidthPx = with(density) { segWidth.toPx() }.coerceAtLeast(1f)
            val targetX = segWidthPx * selectedIdx
            val pillX by animateFloatAsState(
                targetValue = targetX,
                animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.74f, stiffness = 380f),
                label = "lib_seg_slide_px"
            )
            val travel = if (reducedMotion) 0f else (abs(targetX - pillX) / segWidthPx).coerceIn(0f, 1.3f)

            Box(
                modifier = Modifier
                    .width(segWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = pillX
                        scaleX = 1f + travel * 0.12f
                        scaleY = 1f - travel * 0.04f
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLight) {
                                listOf(Color.White.copy(alpha = 0.95f), lerp(Color.White, palette.primary, 0.14f))
                            } else {
                                listOf(lerp(Color.White, palette.glow, 0.2f).copy(alpha = 0.24f), palette.primary.copy(alpha = 0.10f))
                            }
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = if (isLight) 0.9f else 0.36f),
                        shape = RoundedCornerShape(18.dp)
                    )
            )

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    val tint by animateColorAsState(
                        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = if (reducedMotion) snap() else tween(200),
                        label = "lib_seg_tint_${tab.name}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectTab(tab) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
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
            bottom = bottomPadding + 24.dp
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
                        shape = RoundedCornerShape(18.dp),
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
                        shape = RoundedCornerShape(18.dp),
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
            top = 8.dp,
            bottom = bottomPadding + 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                ) {
                    LiquidGlassArtwork(
                        artworkUri = album.artworkUri,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 20.dp,
                        highRes = highResArtwork,
                        fallbackIcon = Icons.Rounded.Album
                    )
                    if (album.year > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(9.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
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
            bottom = bottomPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(artists, key = { "${it.id}_${it.name}" }) { artist ->
            LiquidGlassSurface(
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
                    LiquidGlassArtwork(
                        artworkUri = artist.artworkUri,
                        contentDescription = artist.name,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        shadowElevation = 12.dp,
                        highRes = highResArtwork,
                        fallbackIcon = Icons.Rounded.Person
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
            bottom = bottomPadding + 24.dp
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
                LiquidGlassSurface(
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
            LiquidGlassSurface(
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
                            .size(46.dp)
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
            bottom = bottomPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(genres, key = { it.name }) { genre ->
            LiquidGlassSurface(
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
    val context = LocalContext.current
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (artworkUri != null) {
            LiquidGlassBackdrop(
                artworkUri = artworkUri,
                isPlaying = playerState.isPlaying,
                intensity = 1.1f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LiquidGlassTopBar(
                title = title,
                subtitle = "$subtitle • ${songs.size} tracks",
                leadingAction = {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        size = 40.dp,
                        iconSize = 20.dp,
                        onClick = onBack
                    )
                }
            )

            if (artworkUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassArtwork(
                        artworkUri = artworkUri,
                        contentDescription = title,
                        modifier = Modifier.size(96.dp),
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 22.dp,
                        highRes = state.highResArtwork
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
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
            onViewInfo = { viewModel.loadTrackInfo(song) },
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
