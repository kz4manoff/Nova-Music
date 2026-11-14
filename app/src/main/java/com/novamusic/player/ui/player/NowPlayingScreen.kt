package com.novamusic.player.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.ArtworkStyle
import com.novamusic.player.data.model.AudioFileInfo
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.components.AddToPlaylistDialog
import com.novamusic.player.ui.components.AnimatedEqualizerBars
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassCard
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassPlayerControls
import com.novamusic.player.ui.components.LiquidGlassSlider
import com.novamusic.player.ui.components.NovaAlbumArtwork
import com.novamusic.player.ui.components.SleepTimerDialog
import com.novamusic.player.ui.components.TrackInfoDialog
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.utils.FormatUtils
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: (Song) -> Unit
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val artworkStyle by viewModel.artworkStyle.collectAsStateWithLifecycle()
    val highResArtwork by viewModel.highResArtwork.collectAsStateWithLifecycle()
    val currentAudioInfo by viewModel.currentAudioInfo.collectAsStateWithLifecycle()
    val trackInfoDialog by viewModel.trackInfoDialog.collectAsStateWithLifecycle()
    val isEqEnabled by viewModel.isEqualizerEnabled.collectAsStateWithLifecycle()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }

    val song = playerState.currentSong
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isLandscapeOrTablet = screenWidth >= 640

    val animatedCollapseOffset by animateFloatAsState(
        targetValue = verticalDragOffset,
        animationSpec = spring(stiffness = 600f),
        label = "collapse_drag"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedCollapseOffset.roundToInt()) }
    ) {
        // Dynamic Full-Bleed Blurred Album Artwork + Ambient Liquid Refraction Backdrop
        LiquidGlassBackdrop(
            artworkUri = song?.artworkUri,
            isPlaying = playerState.isPlaying,
            intensity = 1.25f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Glass Top Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (verticalDragOffset > 140f) {
                                    onCollapse()
                                }
                                verticalDragOffset = 0f
                            },
                            onDragCancel = { verticalDragOffset = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                verticalDragOffset = (verticalDragOffset + dragAmount).coerceIn(0f, 320f)
                            }
                        )
                    }
                    .padding(top = 2.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(novaColors.glassBorderTop.copy(alpha = 0.7f))
                )
            }

            // Floating Liquid Glass Top Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LiquidGlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    size = 42.dp,
                    iconSize = 24.dp,
                    onClick = onCollapse
                )

                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    elevation = 8.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (playerState.queue.isNotEmpty()) {
                                "TRACK ${(playerState.currentQueueIndex + 1).coerceAtLeast(1)} OF ${playerState.queue.size}"
                            } else {
                                "NOW PLAYING"
                            },
                            style = NovaMonoTelemetry.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp
                            ),
                            color = accent
                        )
                        Text(
                            text = artworkStyle.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.7.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        LiquidGlassIconButton(
                            icon = Icons.Rounded.Style,
                            contentDescription = "Artwork Style",
                            active = showStyleMenu,
                            size = 42.dp,
                            iconSize = 20.dp,
                            onClick = { showStyleMenu = true }
                        )
                        DropdownMenu(
                            expanded = showStyleMenu,
                            onDismissRequest = { showStyleMenu = false },
                            containerColor = novaColors.elevatedSurface
                        ) {
                            ArtworkStyle.entries.forEach { style ->
                                val isSelected = style == artworkStyle
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = style.label,
                                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        viewModel.setArtworkStyle(style)
                                        showStyleMenu = false
                                    }
                                )
                            }
                        }
                    }

                    LiquidGlassIconButton(
                        icon = Icons.Rounded.Bedtime,
                        contentDescription = "Sleep Timer",
                        active = playerState.sleepTimer.isActive,
                        showDotWhenActive = true,
                        size = 42.dp,
                        iconSize = 20.dp,
                        onClick = { showSleepDialog = true }
                    )
                }
            }

            if (song == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(28.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Select a track to start playing",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (isLandscapeOrTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        SwipeableArtworkStage(
                            song = song,
                            isPlaying = playerState.isPlaying,
                            artworkStyle = artworkStyle,
                            highRes = highResArtwork,
                            onSwipeNext = viewModel::skipToNext,
                            onSwipePrevious = viewModel::skipToPrevious,
                            modifier = Modifier
                                .fillMaxHeight(0.84f)
                                .aspectRatio(1f)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlayerControlsBlock(
                            song = song,
                            audioInfo = currentAudioInfo,
                            playerState = playerState,
                            isFavorite = song.id in favoriteIds,
                            isEqEnabled = isEqEnabled,
                            onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                            onSeek = viewModel::seekTo,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onPrevious = viewModel::skipToPrevious,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::skipToNext,
                            onCycleRepeat = viewModel::cycleRepeatMode,
                            onOpenQueue = { showQueueSheet = true },
                            onOpenLyrics = { onOpenLyrics(song) },
                            onOpenEqualizer = onOpenEqualizer,
                            onOpenPlaylistDialog = { showPlaylistDialog = true },
                            onOpenInfo = { viewModel.openTrackInfoDialog(song) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SwipeableArtworkStage(
                            song = song,
                            isPlaying = playerState.isPlaying,
                            artworkStyle = artworkStyle,
                            highRes = highResArtwork,
                            onSwipeNext = viewModel::skipToNext,
                            onSwipePrevious = viewModel::skipToPrevious,
                            modifier = Modifier
                                .widthIn(max = 346.dp)
                                .fillMaxWidth(0.91f)
                                .aspectRatio(1f)
                        )
                    }

                    PlayerControlsBlock(
                        song = song,
                        audioInfo = currentAudioInfo,
                        playerState = playerState,
                        isFavorite = song.id in favoriteIds,
                        isEqEnabled = isEqEnabled,
                        onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                        onSeek = viewModel::seekTo,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onPrevious = viewModel::skipToPrevious,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onNext = viewModel::skipToNext,
                        onCycleRepeat = viewModel::cycleRepeatMode,
                        onOpenQueue = { showQueueSheet = true },
                        onOpenLyrics = { onOpenLyrics(song) },
                        onOpenEqualizer = onOpenEqualizer,
                        onOpenPlaylistDialog = { showPlaylistDialog = true },
                        onOpenInfo = { viewModel.openTrackInfoDialog(song) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playerState.queue,
            currentIndex = playerState.currentQueueIndex,
            isPlaying = playerState.isPlaying,
            onDismiss = { showQueueSheet = false },
            onJumpToIndex = viewModel::jumpToQueueIndex,
            onRemoveAt = viewModel::removeFromQueue,
            onMove = viewModel::moveQueueItem
        )
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            sleepTimerState = playerState.sleepTimer,
            onStartTimer = viewModel::startSleepTimer,
            onStopAfterCurrent = viewModel::setStopAfterCurrentTrack,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = { showSleepDialog = false }
        )
    }

    if (showPlaylistDialog && song != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onSelectPlaylist = { pid -> viewModel.addSongToPlaylist(pid, song.id) },
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAddSong(name, song.id) },
            onDismiss = { showPlaylistDialog = false }
        )
    }

    if (trackInfoDialog != null && song != null) {
        TrackInfoDialog(
            song = song,
            fileInfo = trackInfoDialog,
            onDismiss = viewModel::clearTrackInfoDialog
        )
    }
}

@Composable
private fun SwipeableArtworkStage(
    song: Song,
    isPlaying: Boolean,
    artworkStyle: ArtworkStyle,
    highRes: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember(song.id) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = 500f),
        label = "artwork_drag"
    )
    val playStateScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "artwork_play_scale"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .scale(playStateScale)
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffsetX < -120f -> onSwipeNext()
                            dragOffsetX > 120f -> onSwipePrevious()
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX = (dragOffsetX + dragAmount * 0.65f).coerceIn(-220f, 220f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ArtworkShowcase(
            song = song,
            isPlaying = isPlaying,
            artworkStyle = artworkStyle,
            highRes = highRes,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ArtworkShowcase(
    song: Song,
    isPlaying: Boolean,
    artworkStyle: ArtworkStyle,
    highRes: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    LaunchedEffect(isPlaying, artworkStyle) {
        if (isPlaying && artworkStyle == ArtworkStyle.ROTATING_VINYL) {
            while (true) {
                val target = rotation.value + 360f
                rotation.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 12_000, easing = LinearEasing)
                )
            }
        }
    }

    val auraTransition = rememberInfiniteTransition(label = "artwork_aura")
    val auraPulse by auraTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    AnimatedContent(
        targetState = song.id,
        transitionSpec = {
            (fadeIn(tween(340)) + scaleIn(initialScale = 0.92f, animationSpec = tween(340))) togetherWith
                fadeOut(tween(220))
        },
        label = "artwork_transition"
    ) { _ ->
        when (artworkStyle) {
            ArtworkStyle.ROUNDED_CARD -> {
                LiquidGlassCard(
                    modifier = modifier,
                    shape = RoundedCornerShape(32.dp),
                    elevation = 28.dp,
                    highlighted = isPlaying,
                    contentPadding = PaddingValues(6.dp)
                ) {
                    NovaAlbumArtwork(
                        artworkUri = song.artworkUri,
                        contentDescription = song.album,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 26.dp,
                        highRes = highRes
                    )
                }
            }

            ArtworkStyle.ROTATING_VINYL -> {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = if (isPlaying) 0.24f else 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .rotate(rotation.value)
                            .shadow(28.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2B2C34),
                                        Color(0xFF0A0B0E),
                                        Color(0xFF1F2028),
                                        Color(0xFF060709)
                                    )
                                )
                            )
                            .border(
                                width = 1.8.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(novaColors.glassBorderTop, novaColors.glassBorderBottom)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            listOf(0.47f, 0.43f, 0.39f, 0.35f).forEachIndexed { idx, factor ->
                                drawCircle(
                                    color = Color.White.copy(alpha = if (idx % 2 == 0) 0.07f else 0.03f),
                                    radius = size.minDimension * factor,
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }

                        NovaAlbumArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier
                                .fillMaxSize(0.62f)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            cornerRadius = 180.dp,
                            highRes = highRes
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF09090B))
                                .border(2.5.dp, accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            ArtworkStyle.IMMERSIVE_GLOW -> {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(auraPulse)
                                .clip(RoundedCornerShape(38.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accent.copy(alpha = 0.36f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxSize(0.95f),
                        shape = RoundedCornerShape(34.dp),
                        highlighted = true,
                        elevation = 24.dp,
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        NovaAlbumArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 26.dp,
                            highRes = highRes
                        )
                    }
                }
            }

            ArtworkStyle.EDITORIAL_SQUARE -> {
                LiquidGlassCard(
                    modifier = modifier,
                    shape = RoundedCornerShape(22.dp),
                    elevation = 20.dp,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NovaAlbumArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 14.dp,
                            highRes = highRes
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = song.fileExtension.ifBlank { "AUDIO" },
                                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerControlsBlock(
    song: Song,
    audioInfo: AudioFileInfo?,
    playerState: PlayerUiState,
    isFavorite: Boolean,
    isEqEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenPlaylistDialog: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var showRemainingTime by remember { mutableStateOf(false) }

    val durationMs = playerState.durationMs.coerceAtLeast(1L)
    val displayPositionMs = if (isScrubbing) scrubPositionMs.toLong() else playerState.positionMs

    val favScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "fav_scale"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (playerState.playbackError != null) {
            LiquidGlassCard(
                shape = RoundedCornerShape(14.dp),
                accentTint = MaterialTheme.colorScheme.error,
                highlighted = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = playerState.playbackError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Title, Artist & Floating Glass Favorite Orb
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${song.artist}  •  ${song.album}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            LiquidGlassIconButton(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                active = isFavorite,
                size = 46.dp,
                iconSize = 22.dp,
                modifier = Modifier.scale(favScale),
                onClick = onToggleFavorite
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Translucent Audio Telemetry Badges
        val telemetryChips = buildList {
            add(song.fileExtension.ifBlank { "AUDIO" } to true)
            audioInfo?.sampleRateHz?.let { add(FormatUtils.formatSampleRate(it) to false) }
            audioInfo?.bitrateKbps?.let { add("$it kbps" to false) }
            if (isEqEnabled) add("DSP ACTIVE" to true)
            if (playerState.sleepTimer.isActive) {
                val timerText = if (playerState.sleepTimer.stopAfterCurrentTrack) {
                    "STOP AFTER TRACK"
                } else {
                    "SLEEP ${FormatUtils.formatTimerCountdown(playerState.sleepTimer.remainingMs)}"
                }
                add(timerText to true)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            telemetryChips.forEach { (label, highlighted) ->
                LiquidGlassCard(
                    shape = RoundedCornerShape(10.dp),
                    highlighted = highlighted,
                    elevation = 4.dp,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                        color = if (highlighted) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Scrubbing Timestamp Floating Glass Pill
        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    shape = RoundedCornerShape(10.dp),
                    highlighted = true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(displayPositionMs),
                        style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            }
        }

        // Liquid Glass Seek Bar
        LiquidGlassSlider(
            progress = (displayPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
            isScrubbing = isScrubbing,
            accentColor = accent,
            onScrub = { fraction ->
                isScrubbing = true
                scrubPositionMs = fraction * durationMs.toFloat()
            },
            onScrubFinished = { fraction ->
                onSeek((fraction * durationMs.toFloat()).toLong())
                isScrubbing = false
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = FormatUtils.formatDuration(displayPositionMs),
                style = NovaMonoTelemetry,
                color = if (isScrubbing) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            val rightLabel = if (showRemainingTime) {
                "-${FormatUtils.formatDuration((playerState.durationMs - displayPositionMs).coerceAtLeast(0L))}"
            } else {
                FormatUtils.formatDuration(playerState.durationMs)
            }
            Text(
                text = rightLabel,
                style = NovaMonoTelemetry,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Liquid Glass Transport Controls
        LiquidGlassPlayerControls(
            isPlaying = playerState.isPlaying,
            shuffleEnabled = playerState.shuffleEnabled,
            repeatMode = playerState.repeatMode,
            onToggleShuffle = onToggleShuffle,
            onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            onCycleRepeat = onCycleRepeat
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Floating Liquid Glass Utility Dock
        LiquidGlassCard(
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilityAction(Icons.Rounded.Lyrics, "Lyrics", false, onOpenLyrics)
                UtilityAction(Icons.Rounded.Equalizer, "EQ", isEqEnabled, onOpenEqualizer)
                UtilityAction(
                    Icons.Rounded.QueueMusic,
                    if (playerState.queue.isNotEmpty()) "Queue (${playerState.queue.size})" else "Queue",
                    false,
                    onOpenQueue
                )
                UtilityAction(Icons.Rounded.PlaylistAdd, "Save", false, onOpenPlaylistDialog)
                UtilityAction(Icons.Rounded.Info, "Info", false, onOpenInfo)
            }
        }
    }
}

@Composable
private fun UtilityAction(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            if (highlighted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-2).dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onJumpToIndex: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalNovaColors.current.elevatedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Playback Queue",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "UP NEXT & ACTIVE SESSION",
                        style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LiquidGlassCard(
                    shape = RoundedCornerShape(12.dp),
                    highlighted = true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${queue.size} tracks",
                        style = NovaMonoTelemetry,
                        color = accent
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queue, key = { idx, s -> "${idx}_${s.id}" }) { index, track ->
                    val isActive = index == currentIndex
                    LiquidGlassCard(
                        shape = RoundedCornerShape(16.dp),
                        highlighted = isActive,
                        elevation = if (isActive) 8.dp else 2.dp,
                        onClick = { onJumpToIndex(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", index + 1),
                                style = NovaMonoTelemetry,
                                color = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(26.dp)
                            )
                            Box(contentAlignment = Alignment.Center) {
                                NovaAlbumArtwork(
                                    artworkUri = track.artworkUri,
                                    contentDescription = track.album,
                                    modifier = Modifier.size(44.dp),
                                    cornerRadius = 11.dp
                                )
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color.Black.copy(alpha = 0.56f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedEqualizerBars(isPlaying = isPlaying, color = accent)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isActive) accent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${FormatUtils.formatDuration(track.durationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (index > 0) {
                                IconButton(onClick = { onMove(index, index - 1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(17.dp))
                                }
                            }
                            if (index < queue.lastIndex) {
                                IconButton(onClick = { onMove(index, index + 1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(17.dp))
                                }
                            }
                            IconButton(onClick = { onRemoveAt(index) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
