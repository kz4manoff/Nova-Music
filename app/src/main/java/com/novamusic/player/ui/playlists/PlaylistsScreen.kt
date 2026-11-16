package com.novamusic.player.ui.playlists

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.components.AnimatedEqualizerBars
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.ui.theme.NovaOrangeDeep
import com.novamusic.player.ui.theme.NovaOrangeGlow
import com.novamusic.player.utils.FormatUtils
import java.util.Locale

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFavorites: () -> Unit,
    bottomContentPadding: Dp
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()

    val accent = MaterialTheme.colorScheme.primary

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistWithSongs?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LiquidGlassTopBar(
            title = "Playlists",
            subtitle = "${playlists.size} collections • ${favorites.size} loved",
            trailingActions = {
                LiquidGlassButton(
                    onClick = { showCreateDialog = true },
                    highlighted = true,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "New",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomContentPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val favDuration = favorites.sumOf { it.durationMs }
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    highlighted = true,
                    elevation = 16.dp,
                    contentPadding = PaddingValues(16.dp),
                    onClick = onOpenFavorites
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(14.dp, RoundedCornerShape(18.dp), ambientColor = accent, spotColor = accent)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NovaOrangeGlow, accent, NovaOrangeDeep)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMART COLLECTION",
                                style = NovaMonoTelemetry.copy(fontSize = 9.5.sp),
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Favorite Tracks",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${favorites.size} tracks • ${FormatUtils.formatDuration(favDuration)}",
                                style = NovaMonoTelemetry,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (favorites.isNotEmpty()) {
                            LiquidGlassIconButton(
                                icon = Icons.Rounded.PlayArrow,
                                contentDescription = "Play Favorites",
                                active = true,
                                size = 42.dp,
                                iconSize = 22.dp,
                                onClick = { viewModel.playPlaylist(favorites) }
                            )
                        }
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(26.dp),
                        contentPadding = PaddingValues(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Custom Playlists Yet", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Create your own curated mixes and reorder tracks anytime.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                val totalDurationMs = playlist.songs.sumOf { it.durationMs }
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 10.dp,
                    contentPadding = PaddingValues(12.dp),
                    onClick = { onOpenPlaylist(playlist.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistMosaicArtwork(
                            songs = playlist.songs,
                            modifier = Modifier.size(66.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${playlist.songs.size} tracks • ${FormatUtils.formatDuration(totalDurationMs)}",
                                style = NovaMonoTelemetry,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (playlist.songs.isNotEmpty()) {
                            LiquidGlassIconButton(
                                icon = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                active = true,
                                size = 38.dp,
                                iconSize = 20.dp,
                                onClick = { viewModel.playPlaylist(playlist.songs) }
                            )
                        }
                        IconButton(onClick = { renameTarget = playlist }) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Rename",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "Create Playlist",
            initialName = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = "Rename Playlist",
            initialName = target.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renamePlaylist(target.id, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    isFavoritesMode: Boolean,
    viewModel: PlaylistsViewModel,
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    val playlist = if (!isFavoritesMode) playlists.firstOrNull { it.id == playlistId } else null
    val songs = if (isFavoritesMode) favorites else playlist?.songs.orEmpty()
    val title = if (isFavoritesMode) "Favorite Tracks" else (playlist?.name ?: "Playlist")
    val totalDurationMs = songs.sumOf { it.durationMs }

    var showAddSongsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val coverUri = songs.firstOrNull()?.artworkUri
        if (coverUri != null) {
            LiquidGlassBackdrop(
                artworkUri = coverUri,
                isPlaying = playerState.isPlaying,
                intensity = 1.08f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LiquidGlassTopBar(
                title = title,
                subtitle = "${songs.size} tracks • ${FormatUtils.formatDuration(totalDurationMs)}",
                leadingAction = {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        size = 40.dp,
                        iconSize = 20.dp,
                        onClick = onBack
                    )
                },
                trailingActions = {
                    if (!isFavoritesMode && playlist != null) {
                        LiquidGlassButton(
                            onClick = { showAddSongsDialog = true },
                            highlighted = true,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Add",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFavoritesMode) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = accent, spotColor = accent)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(NovaOrangeGlow, accent, NovaOrangeDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                } else {
                    PlaylistMosaicArtwork(
                        songs = songs,
                        modifier = Modifier
                            .size(82.dp)
                            .shadow(16.dp, RoundedCornerShape(20.dp))
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiquidGlassButton(
                            onClick = { viewModel.playPlaylist(songs, 0, false) },
                            enabled = songs.isNotEmpty(),
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
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Play All",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        LiquidGlassButton(
                            onClick = { viewModel.playPlaylist(songs, 0, true) },
                            enabled = songs.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Shuffle", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = bottomContentPadding + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = playerState.currentSong?.id == song.id
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        highlighted = isCurrent,
                        elevation = if (isCurrent) 12.dp else 4.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        onClick = { viewModel.playPlaylist(songs, index, false) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", index + 1),
                                style = NovaMonoTelemetry,
                                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(26.dp)
                            )
                            Box(contentAlignment = Alignment.Center) {
                                LiquidGlassArtwork(
                                    artworkUri = song.artworkUri,
                                    contentDescription = song.album,
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 8.dp
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.48f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedEqualizerBars(isPlaying = playerState.isPlaying, color = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
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
                            if (!isFavoritesMode && playlist != null) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { viewModel.moveSongInPlaylist(playlist, index, index - 1) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(17.dp))
                                    }
                                }
                                if (index < songs.lastIndex) {
                                    IconButton(
                                        onClick = { viewModel.moveSongInPlaylist(playlist, index, index + 1) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(17.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.RemoveCircleOutline,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else if (isFavoritesMode) {
                                IconButton(onClick = { viewModel.toggleFavorite(song.id) }) {
                                    Icon(
                                        Icons.Rounded.Favorite,
                                        contentDescription = "Unfavorite",
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsDialog && playlist != null) {
        val existingIds = playlist.songs.map { it.id }.toSet()
        val available = allSongs.filter { it.id !in existingIds }
        val selectedIds = remember { mutableStateListOf<Long>() }

        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            containerColor = novaColors.elevatedSurface,
            title = { Text("Add Songs to ${playlist.name}") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(available, key = { it.id }) { song ->
                        val checked = song.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(song.id) else selectedIds.add(song.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selectedIds.add(song.id) else selectedIds.remove(song.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedIds.forEach { songId ->
                            viewModel.addSongToPlaylist(playlist.id, songId)
                        }
                        showAddSongsDialog = false
                    }
                ) {
                    Text("Add (${selectedIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalNovaColors.current.elevatedSurface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
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
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PlaylistMosaicArtwork(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val distinctArts = songs.distinctBy { it.albumId }.take(4)
    val novaColors = LocalNovaColors.current
    if (distinctArts.size < 4) {
        LiquidGlassArtwork(
            artworkUri = songs.firstOrNull()?.artworkUri,
            contentDescription = null,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp,
            fallbackIcon = Icons.Rounded.QueueMusic
        )
    } else {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 0.75.dp,
                    brush = Brush.linearGradient(listOf(novaColors.glassBorderTop, novaColors.glassBorderBottom)),
                    shape = RoundedCornerShape(16.dp)
                ),
            verticalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                LiquidGlassArtwork(
                    artworkUri = distinctArts[0].artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    shadowElevation = 0.dp
                )
                LiquidGlassArtwork(
                    artworkUri = distinctArts[1].artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    shadowElevation = 0.dp
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                LiquidGlassArtwork(
                    artworkUri = distinctArts[2].artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    shadowElevation = 0.dp
                )
                LiquidGlassArtwork(
                    artworkUri = distinctArts[3].artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    shadowElevation = 0.dp
                )
            }
        }
    }
}
