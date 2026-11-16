package com.novamusic.player.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.PlaylistWithSongs
import com.novamusic.player.data.model.SleepTimerState
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils

@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(410, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(470, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = if (isPlaying) listOf(bar1, bar2, bar3, bar4) else listOf(0.35f, 0.6f, 0.42f, 0.28f)

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(2.75.dp)
                    .height((16f * fraction).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun NovaAlbumArtwork(
    artworkUri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    highRes: Boolean = true,
    fallbackIcon: ImageVector = Icons.Rounded.MusicNote
) {
    LiquidGlassArtwork(
        artworkUri = artworkUri,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        shadowElevation = if (cornerRadius >= 20.dp) 18.dp else 8.dp,
        highRes = highRes,
        fallbackIcon = fallbackIcon
    )
}

@Composable
fun SongListItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    highResArtwork: Boolean = true
) {
    val accent = MaterialTheme.colorScheme.primary
    val favScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "row_fav_scale"
    )

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                LiquidGlassArtwork(
                    artworkUri = song.artworkUri,
                    contentDescription = song.album,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    shadowElevation = if (isCurrentSong) 14.dp else 6.dp,
                    highRes = highResArtwork
                )
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.Black.copy(alpha = 0.48f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizerBars(isPlaying = isPlaying, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isCurrentSong) accent else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ${FormatUtils.formatDuration(song.durationMs)}",
                        style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    val ext = song.fileExtension
                    if (ext.isNotBlank() && ext in setOf("FLAC", "WAV", "OPUS", "ALAC")) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ext,
                            style = NovaMonoTelemetry.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent.copy(alpha = 0.14f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .size(38.dp)
                    .scale(favScale)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Track Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }

    if (isCurrentSong) {
        LiquidGlassSurface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            shape = RoundedCornerShape(20.dp),
            highlighted = true,
            elevation = 14.dp,
            onClick = onClick
        ) {
            rowContent()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
        ) {
            rowContent()
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp, end = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
            )
        }
    }
}

@Composable
fun MiniPlayer(
    playerState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassMiniPlayer(
        playerState = playerState,
        onTogglePlayPause = onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onOpenNowPlaying = onOpenNowPlaying,
        modifier = modifier
    )
}

fun shareAudioTrack(context: Context, song: Song) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = song.mimeType.ifBlank { "audio/*" }
        putExtra(Intent.EXTRA_STREAM, song.uri)
        putExtra(Intent.EXTRA_SUBJECT, song.title)
        putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        val chooser = Intent.createChooser(shareIntent, "Share \"${song.title}\"").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

@Composable
fun NovaSheetSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = NovaMonoTelemetry.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun NovaSheetGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 4.dp,
        glassOpacity = 0.84f,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun NovaSheetDivider() {
    val novaColors = LocalNovaColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp, end = 10.dp)
            .height(0.55.dp)
            .background(novaColors.borderSubtle.copy(alpha = 0.34f))
    )
}

@Composable
fun NovaSheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingBadge: String? = null,
    highlighted: Boolean = false,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion && enabled) 0.982f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 500f),
        label = "sheet_row_scale"
    )
    val pressBg by animateColorAsState(
        targetValue = if (isPressed && enabled) {
            accent.copy(alpha = if (novaColors.isLight) 0.08f else 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(130),
        label = "sheet_row_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(pressBg)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    performHaptic()
                    onClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (highlighted) accent.copy(alpha = 0.16f)
                    else novaColors.recessedSurface.copy(alpha = if (novaColors.isLight) 0.92f else 0.78f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (highlighted) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!trailingBadge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (highlighted) accent.copy(alpha = 0.14f)
                        else novaColors.recessedSurface.copy(alpha = 0.92f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (highlighted && !novaColors.isLight) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onViewLyrics: () -> Unit,
    onViewInfo: () -> Unit,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val performHaptic = rememberNovaHaptic()
    var actionDispatched by remember { mutableStateOf(false) }

    val runSingleAction: (() -> Unit) -> Unit = { action ->
        if (!actionDispatched) {
            actionDispatched = true
            onDismiss()
            action()
        }
    }

    LiquidGlassSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Track Identity Header Card
            LiquidGlassSurface(
                shape = RoundedCornerShape(22.dp),
                elevation = 6.dp,
                glassOpacity = 0.90f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassArtwork(
                        artworkUri = song.artworkUri,
                        contentDescription = song.album,
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 10.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.fileExtension.ifBlank { "AUDIO" }} • ${FormatUtils.formatDuration(song.durationMs)}",
                            style = NovaMonoTelemetry.copy(fontSize = 10.5.sp),
                            color = accent,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    LiquidGlassIconButton(
                        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        active = isFavorite,
                        size = 38.dp,
                        iconSize = 19.dp,
                        tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            performHaptic()
                            runSingleAction(onToggleFavorite)
                        }
                    )
                }
            }

            // Playback & Queue Group
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NovaSheetSectionLabel("Queue & Library")
                NovaSheetGroupCard {
                    NovaSheetActionRow(
                        icon = Icons.Rounded.PlaylistPlay,
                        title = "Play Next",
                        subtitle = "Queue right after the current track",
                        onClick = { runSingleAction(onPlayNext) }
                    )
                    NovaSheetDivider()
                    NovaSheetActionRow(
                        icon = Icons.Rounded.QueueMusic,
                        title = "Add to Queue",
                        subtitle = "Append to the end of playing queue",
                        onClick = { runSingleAction(onAddToQueue) }
                    )
                    NovaSheetDivider()
                    NovaSheetActionRow(
                        icon = Icons.Rounded.PlaylistAdd,
                        title = "Add to Playlist",
                        subtitle = "Save track to a playlist",
                        showChevron = true,
                        onClick = { runSingleAction(onAddToPlaylist) }
                    )
                    NovaSheetDivider()
                    NovaSheetActionRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "View Lyrics",
                        subtitle = "Open synchronized lyrics & LRC editor",
                        showChevron = true,
                        onClick = { runSingleAction(onViewLyrics) }
                    )
                }
            }

            // Navigate, Details & Share Group
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NovaSheetSectionLabel("Track & Info")
                NovaSheetGroupCard {
                    if (onGoToAlbum != null && song.id > 0L && song.albumId > 0L) {
                        NovaSheetActionRow(
                            icon = Icons.Rounded.Album,
                            title = "Go to Album",
                            subtitle = song.album,
                            showChevron = true,
                            onClick = { runSingleAction(onGoToAlbum) }
                        )
                        NovaSheetDivider()
                    }
                    if (onGoToArtist != null && song.id > 0L && song.artist.isNotBlank()) {
                        NovaSheetActionRow(
                            icon = Icons.Rounded.Person,
                            title = "Go to Artist",
                            subtitle = song.artist,
                            showChevron = true,
                            onClick = { runSingleAction(onGoToArtist) }
                        )
                        NovaSheetDivider()
                    }
                    NovaSheetActionRow(
                        icon = Icons.Rounded.Info,
                        title = "View Track Details",
                        subtitle = "Format, bitrate, sample rate & file path",
                        showChevron = true,
                        onClick = { runSingleAction(onViewInfo) }
                    )
                    if (onShare != null) {
                        NovaSheetDivider()
                        NovaSheetActionRow(
                            icon = Icons.Rounded.Share,
                            title = "Share",
                            subtitle = "Send audio track via system share sheet",
                            showChevron = true,
                            onClick = { runSingleAction(onShare) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackInfoDialog(
    song: Song,
    fileInfo: AudioFileInfo?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalNovaColors.current.elevatedSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio Telemetry & File Info", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Title", song.title)
                InfoRow("Artist", song.artist)
                InfoRow("Album", song.album)
                InfoRow("Genre", song.genre)
                InfoRow("Format", fileInfo?.format ?: song.fileExtension)
                InfoRow("MIME Type", fileInfo?.mimeType ?: song.mimeType)
                InfoRow("Duration", FormatUtils.formatDuration(song.durationMs))
                InfoRow("Size", FormatUtils.formatFileSize(fileInfo?.sizeBytes ?: song.sizeBytes))
                InfoRow("Bitrate", FormatUtils.formatBitrate(fileInfo?.bitrateKbps))
                InfoRow("Sample Rate", FormatUtils.formatSampleRate(fileInfo?.sampleRateHz))
                InfoRow("Path", fileInfo?.filePath ?: song.path)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = NovaMonoTelemetry.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistWithSongs>,
    onSelectPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalNovaColors.current.elevatedSurface,
        title = { Text("Add to Playlist", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("New playlist name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreatePlaylist(newName.trim())
                                newName = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet. Create one above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectPlaylist(playlist.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${playlist.songs.size} tracks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onStartTimer: (Int) -> Unit,
    onStopAfterCurrent: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalNovaColors.current.elevatedSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sleepTimerState.isActive) {
                    val statusText = if (sleepTimerState.stopAfterCurrentTrack) {
                        "Active: Stopping after current track"
                    } else {
                        "Active: ${FormatUtils.formatTimerCountdown(sleepTimerState.remainingMs)} remaining"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        OutlinedButton(
                            onClick = {
                                onStartTimer(mins)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("${mins}m", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onStopAfterCurrent()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Stop after current track")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customMinutes = input
                            }
                        },
                        label = { Text("Custom minutes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val mins = customMinutes.toIntOrNull() ?: 0
                            if (mins > 0) {
                                onStartTimer(mins)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("Set")
                    }
                }
            }
        },
        confirmButton = {
            if (sleepTimerState.isActive) {
                TextButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    }
                ) {
                    Text("Turn Off", color = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
