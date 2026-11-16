package com.novamusic.player.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.AudioOutputDevice
import com.novamusic.player.data.model.SleepTimerState
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSheet
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.NovaSheetActionRow
import com.novamusic.player.ui.components.NovaSheetDivider
import com.novamusic.player.ui.components.NovaSheetGroupCard
import com.novamusic.player.ui.components.NovaSheetSectionLabel
import com.novamusic.player.ui.components.audioOutputIcon
import com.novamusic.player.ui.components.rememberNovaHaptic
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsBottomSheet(
    song: Song?,
    audioInfo: AudioFileInfo?,
    isFavorite: Boolean,
    queueCount: Int,
    isEqEnabled: Boolean,
    activeOutputDevice: AudioOutputDevice?,
    sleepTimerState: SleepTimerState,
    artworkStyle: ArtworkStyle,
    highResArtwork: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenAlbum: ((Long) -> Unit)?,
    onOpenArtist: ((String) -> Unit)?,
    onViewTrackDetails: () -> Unit,
    onShare: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onSelectArtworkStyle: (ArtworkStyle) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()
    var actionDispatched by remember { mutableStateOf(false) }

    val runSingleAction: (() -> Unit) -> Unit = { action ->
        if (!actionDispatched) {
            actionDispatched = true
            onDismiss()
            action()
        }
    }

    val sleepBadge = when {
        !sleepTimerState.isActive -> "Off"
        sleepTimerState.stopAfterCurrentTrack -> "Track End"
        else -> FormatUtils.formatTimerCountdown(sleepTimerState.remainingMs)
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
            // Track Identity / Player Options Header Card
            LiquidGlassSurface(
                shape = RoundedCornerShape(22.dp),
                elevation = 6.dp,
                glassOpacity = 0.90f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (song != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 10.dp,
                            highRes = highResArtwork
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
                            val metaSummary = buildString {
                                append(song.fileExtension.ifBlank { "AUDIO" })
                                audioInfo?.sampleRateHz?.let { sr ->
                                    append(" • ")
                                    append(FormatUtils.formatSampleRate(sr))
                                }
                                append(" • ")
                                append(FormatUtils.formatDuration(song.durationMs))
                            }
                            Text(
                                text = metaSummary,
                                style = NovaMonoTelemetry.copy(fontSize = 10.5.sp),
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                onToggleFavorite()
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Player Options",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Configure audio output, DSP & player presentation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Group 1: Queue & Library Actions
            if (song != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NovaSheetSectionLabel("Queue & Library")
                    NovaSheetGroupCard {
                        NovaSheetActionRow(
                            icon = Icons.Rounded.PlaylistPlay,
                            title = "Play Next",
                            subtitle = "Insert right after current track",
                            onClick = { runSingleAction(onPlayNext) }
                        )
                        NovaSheetDivider()
                        NovaSheetActionRow(
                            icon = Icons.Rounded.QueueMusic,
                            title = "Add to Queue",
                            subtitle = "Append to end of playing queue",
                            onClick = { runSingleAction(onAddToQueue) }
                        )
                        NovaSheetDivider()
                        NovaSheetActionRow(
                            icon = Icons.Rounded.QueueMusic,
                            title = "Playing Queue",
                            subtitle = "View and reorder upcoming tracks",
                            trailingBadge = if (queueCount > 0) "$queueCount" else null,
                            showChevron = true,
                            onClick = { runSingleAction(onOpenQueue) }
                        )
                        NovaSheetDivider()
                        NovaSheetActionRow(
                            icon = Icons.Rounded.PlaylistAdd,
                            title = "Add to Playlist",
                            subtitle = "Save this track to a playlist",
                            showChevron = true,
                            onClick = { runSingleAction(onAddToPlaylist) }
                        )
                        NovaSheetDivider()
                        NovaSheetActionRow(
                            icon = Icons.Rounded.Lyrics,
                            title = "View Lyrics",
                            subtitle = "Synchronized lyrics & LRC editor",
                            showChevron = true,
                            onClick = { runSingleAction(onOpenLyrics) }
                        )
                    }
                }

                // Group 2: Navigate, Track Details & Share
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NovaSheetSectionLabel("Track & Info")
                    NovaSheetGroupCard {
                        if (onOpenAlbum != null && song.id > 0L && song.albumId > 0L) {
                            NovaSheetActionRow(
                                icon = Icons.Rounded.Album,
                                title = "Go to Album",
                                subtitle = song.album,
                                showChevron = true,
                                onClick = { runSingleAction { onOpenAlbum(song.albumId) } }
                            )
                            NovaSheetDivider()
                        }
                        if (onOpenArtist != null && song.id > 0L && song.artist.isNotBlank()) {
                            NovaSheetActionRow(
                                icon = Icons.Rounded.Person,
                                title = "Go to Artist",
                                subtitle = song.artist,
                                showChevron = true,
                                onClick = { runSingleAction { onOpenArtist(song.artist) } }
                            )
                            NovaSheetDivider()
                        }
                        NovaSheetActionRow(
                            icon = Icons.Rounded.Info,
                            title = "View Track Details",
                            subtitle = "Format, bitrate, sample rate & file path",
                            showChevron = true,
                            onClick = { runSingleAction(onViewTrackDetails) }
                        )
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

            // Group 3: Audio Output, Equalizer & Sleep Timer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NovaSheetSectionLabel("Audio & Sleep")
                NovaSheetGroupCard {
                    val outIcon = activeOutputDevice?.let { audioOutputIcon(it.type) } ?: Icons.Rounded.VolumeUp
                    val isExternal = activeOutputDevice?.type?.isExternal == true
                    NovaSheetActionRow(
                        icon = outIcon,
                        title = "Audio Output",
                        subtitle = activeOutputDevice?.name ?: "Phone Speaker",
                        trailingBadge = activeOutputDevice?.type?.shortLabel ?: "Speaker",
                        highlighted = isExternal,
                        showChevron = true,
                        onClick = { runSingleAction(onOpenAudioOutput) }
                    )
                    NovaSheetDivider()
                    NovaSheetActionRow(
                        icon = Icons.Rounded.Equalizer,
                        title = "Equalizer & DSP",
                        subtitle = if (isEqEnabled) "Hardware equalizer and effects active" else "10-band equalizer, bass boost & virtualizer",
                        trailingBadge = if (isEqEnabled) "Active" else "Off",
                        highlighted = isEqEnabled,
                        showChevron = true,
                        onClick = { runSingleAction(onOpenEqualizer) }
                    )
                    NovaSheetDivider()
                    NovaSheetActionRow(
                        icon = Icons.Rounded.Bedtime,
                        title = "Sleep Timer",
                        subtitle = "Pause playback automatically after countdown",
                        trailingBadge = sleepBadge,
                        highlighted = sleepTimerState.isActive,
                        showChevron = true,
                        onClick = { runSingleAction(onOpenSleepTimer) }
                    )
                }
            }

            // Group 4: Artwork Style Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NovaSheetSectionLabel("Artwork Presentation")
                    Text(
                        text = artworkStyle.label,
                        style = NovaMonoTelemetry.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = accent,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                NovaSheetGroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArtworkStyle.entries.chunked(2).forEach { rowStyles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStyles.forEach { style ->
                                    ArtworkStyleTile(
                                        style = style,
                                        selected = style == artworkStyle,
                                        isLight = novaColors.isLight,
                                        recessedBg = novaColors.recessedSurface,
                                        subtleBorder = novaColors.borderSubtle,
                                        onClick = { runSingleAction { onSelectArtworkStyle(style) } },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkStyleTile(
    style: ArtworkStyle,
    selected: Boolean,
    isLight: Boolean,
    recessedBg: Color,
    subtleBorder: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.97f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 480f),
        label = "art_tile_scale_${style.name}"
    )
    val tileBg by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = if (isLight) 0.14f else 0.18f)
            else -> recessedBg.copy(alpha = if (isLight) 0.88f else 0.68f)
        },
        animationSpec = tween(180),
        label = "art_tile_bg_${style.name}"
    )
    val tileBorder by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.55f) else subtleBorder.copy(alpha = 0.36f),
        animationSpec = tween(180),
        label = "art_tile_border_${style.name}"
    )

    val icon: ImageVector = when (style) {
        ArtworkStyle.ROUNDED_CARD -> Icons.Rounded.Image
        ArtworkStyle.ROTATING_VINYL -> Icons.Rounded.Album
        ArtworkStyle.IMMERSIVE_GLOW -> Icons.Rounded.AutoAwesome
        ArtworkStyle.EDITORIAL_SQUARE -> Icons.Rounded.Style
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .border(0.75.dp, tileBorder, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    performHaptic()
                    onClick()
                }
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
