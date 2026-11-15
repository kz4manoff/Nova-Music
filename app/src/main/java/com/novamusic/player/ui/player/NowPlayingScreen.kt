package com.novamusic.player.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassPlayerControls
import com.novamusic.player.ui.components.LiquidGlassSeekBar
import com.novamusic.player.ui.components.LiquidGlassSheet
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.SleepTimerDialog
import com.novamusic.player.ui.components.TrackInfoDialog
import com.novamusic.player.ui.theme.LocalArtworkPalette
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
    var showOptionsMenu by remember { mutableStateOf(false) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }

    val song = playerState.currentSong
    val palette = LocalArtworkPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isLandscapeOrTablet = screenWidth >= 640
    val reducedMotion = LocalReducedMotion.current

    val animatedCollapseOffset by animateFloatAsState(
        targetValue = verticalDragOffset,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
        label = "collapse_drag"
    )
    val collapseFraction = (animatedCollapseOffset / 340f).coerceIn(0f, 1f)
    val dragScale = if (reducedMotion) 1f else 1f - collapseFraction * 0.055f
    val dragCorners = (collapseFraction * 28f).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = animatedCollapseOffset
                scaleX = dragScale
                scaleY = dragScale
            }
            .clip(RoundedCornerShape(topStart = dragCorners, topEnd = dragCorners))
    ) {
        // Full-bleed deeply blurred album artwork + extracted color ambient light scatter
        LiquidGlassBackdrop(
            artworkUri = song?.artworkUri,
            isPlaying = playerState.isPlaying,
            intensity = 1.32f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            // Subtle iOS Grabber Pill + Swipe-Down Collapse Gesture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (verticalDragOffset > 130f) {
                                    onCollapse()
                                }
                                verticalDragOffset = 0f
                            },
                            onDragCancel = { verticalDragOffset = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                verticalDragOffset = (verticalDragOffset + dragAmount).coerceIn(0f, 340f)
                            }
                        )
                    }
                    .padding(top = 2.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (novaColors.isLight) Color.Black.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.36f)
                        )
                )
            }

            // TOP: Floating Glass Back Button, Centered Playing From Label, Floating Glass Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LiquidGlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    size = 40.dp,
                    iconSize = 24.dp,
                    onClick = onCollapse
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    AnimatedContent(
                        targetState = song?.album?.takeIf { it.isNotBlank() } ?: "Offline Library",
                        transitionSpec = {
                            if (reducedMotion) fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                            else fadeIn(tween(240)) togetherWith fadeOut(tween(160))
                        },
                        label = "np_album_header"
                    ) { albumLabel ->
                        Text(
                            text = albumLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.MoreHoriz,
                        contentDescription = "Player Options",
                        active = showOptionsMenu,
                        size = 40.dp,
                        iconSize = 20.dp,
                        onClick = { showOptionsMenu = true }
                    )
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        containerColor = novaColors.elevatedSurface
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = accent) },
                            text = { Text(if (isEqEnabled) "Equalizer DSP (Active)" else "Equalizer & DSP") },
                            onClick = {
                                showOptionsMenu = false
                                onOpenEqualizer()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = accent) },
                            text = { Text("Sleep Timer") },
                            onClick = {
                                showOptionsMenu = false
                                showSleepDialog = true
                            }
                        )
                        if (song != null) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = accent) },
                                text = { Text("Add to Playlist") },
                                onClick = {
                                    showOptionsMenu = false
                                    showPlaylistDialog = true
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = accent) },
                                text = { Text("Audio File Info") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.openTrackInfoDialog(song)
                                }
                            )
                        }
                        HorizontalDivider(color = novaColors.borderSubtle.copy(alpha = 0.4f))
                        ArtworkStyle.entries.forEach { style ->
                            val isSelected = style == artworkStyle
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Style,
                                        contentDescription = null,
                                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
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
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (song == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(30.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Select a track to play",
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
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
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
                                .fillMaxHeight(0.86f)
                                .aspectRatio(1f)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AppleMusicPlayerControlsBlock(
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
                            onOpenEqualizer = onOpenEqualizer
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // CENTER: Large Floating Album Artwork with Dynamic Colored Shadow
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
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
                                .widthIn(max = 368.dp)
                                .fillMaxWidth(0.95f)
                                .aspectRatio(1f)
                        )
                    }

                    // BELOW ARTWORK + PLAYBACK + BOTTOM ACTIONS
                    AppleMusicPlayerControlsBlock(
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
                        onOpenEqualizer = onOpenEqualizer
                    )

                    Spacer(modifier = Modifier.height(8.dp))
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
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "artwork_drag"
    )
    val playStateScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.91f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
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
                            dragOffsetX < -115f -> onSwipeNext()
                            dragOffsetX > 115f -> onSwipePrevious()
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
    val palette = LocalArtworkPalette.current
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
        initialValue = 0.95f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    AnimatedContent(
        targetState = song.id,
        transitionSpec = {
            (fadeIn(tween(380)) + scaleIn(
                initialScale = 0.90f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            )) togetherWith (fadeOut(tween(240)) + scaleOut(targetScale = 0.94f, animationSpec = tween(240)))
        },
        label = "artwork_transition"
    ) { _ ->
        when (artworkStyle) {
            ArtworkStyle.ROUNDED_CARD -> {
                LiquidGlassArtwork(
                    artworkUri = song.artworkUri,
                    contentDescription = song.album,
                    modifier = modifier,
                    shape = RoundedCornerShape(30.dp),
                    shadowElevation = if (isPlaying) 36.dp else 18.dp,
                    glowTint = palette.glow,
                    highRes = highRes
                )
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
                                        palette.glow.copy(alpha = if (isPlaying) 0.28f else 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .rotate(rotation.value)
                            .shadow(30.dp, CircleShape, ambientColor = palette.glow, spotColor = palette.primary)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF282A33),
                                        Color(0xFF0A0B0F),
                                        Color(0xFF1E2029),
                                        Color(0xFF060709)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
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
                                    color = Color.White.copy(alpha = if (idx % 2 == 0) 0.08f else 0.03f),
                                    radius = size.minDimension * factor,
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }

                        LiquidGlassArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier.fillMaxSize(0.62f),
                            shape = CircleShape,
                            shadowElevation = 12.dp,
                            highRes = highRes
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF09090B))
                                .border(2.2.dp, palette.glow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.45f))
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
                                .clip(RoundedCornerShape(36.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            palette.glow.copy(alpha = 0.42f),
                                            palette.secondary.copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    LiquidGlassArtwork(
                        artworkUri = song.artworkUri,
                        contentDescription = song.album,
                        modifier = Modifier.fillMaxSize(0.94f),
                        shape = RoundedCornerShape(30.dp),
                        shadowElevation = 34.dp,
                        glowTint = palette.glow,
                        highRes = highRes
                    )
                }
            }

            ArtworkStyle.EDITORIAL_SQUARE -> {
                LiquidGlassSurface(
                    modifier = modifier,
                    shape = RoundedCornerShape(24.dp),
                    elevation = 24.dp,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LiquidGlassArtwork(
                            artworkUri = song.artworkUri,
                            contentDescription = song.album,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 14.dp,
                            highRes = highRes
                        )
                        if (song.fileExtension.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.56f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = song.fileExtension,
                                    style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                    color = palette.glow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicPlayerControlsBlock(
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
    onOpenEqualizer: () -> Unit
) {
    val palette = LocalArtworkPalette.current
    val accent = MaterialTheme.colorScheme.primary
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var showRemainingTime by remember { mutableStateOf(true) }

    val durationMs = playerState.durationMs.coerceAtLeast(1L)
    val displayPositionMs = if (isScrubbing) scrubPositionMs.toLong() else playerState.positionMs

    val favScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "fav_scale"
    )

    val reducedMotion = LocalReducedMotion.current
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = playerState.playbackError != null,
            enter = expandVertically() + fadeIn(tween(200)),
            exit = shrinkVertically() + fadeOut(tween(160))
        ) {
            playerState.playbackError?.let { errorMsg ->
                LiquidGlassSurface(
                    shape = RoundedCornerShape(14.dp),
                    accentTint = MaterialTheme.colorScheme.error,
                    highlighted = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // BELOW ARTWORK: Crisp Unboxed Song Title, Artist Name, Subtle Hi-Res Tag & Glass Favorite Orb
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        (fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 5 }) togetherWith
                            (fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 5 })
                    }
                },
                contentKey = { it.id },
                label = "np_track_meta",
                modifier = Modifier.weight(1f)
            ) { current ->
                Column {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.artist,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        val qualityBadge = buildString {
                            val ext = current.fileExtension
                            if (ext.isNotBlank()) append(ext)
                            audioInfo?.sampleRateHz?.let { sr ->
                                if (isNotEmpty()) append(" • ")
                                append(FormatUtils.formatSampleRate(sr))
                            }
                        }
                        if (qualityBadge.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = qualityBadge,
                                style = NovaMonoTelemetry.copy(fontSize = 9.5.sp),
                                color = palette.glow.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            LiquidGlassIconButton(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                active = isFavorite,
                size = 44.dp,
                iconSize = 22.dp,
                tint = if (isFavorite) accent else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.scale(favScale),
                onClick = onToggleFavorite
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrubbing Timestamp Pill
        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(12.dp),
                    highlighted = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(displayPositionMs),
                        style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // PLAYBACK: Minimal Apple Music Liquid Glass Seek Bar
        LiquidGlassSeekBar(
            progress = (displayPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
            isScrubbing = isScrubbing,
            accentColor = palette.glow,
            onScrub = { fraction ->
                isScrubbing = true
                scrubPositionMs = fraction * durationMs.toFloat()
            },
            onScrubFinished = { fraction ->
                onSeek((fraction * durationMs.toFloat()).toLong())
                isScrubbing = false
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = FormatUtils.formatDuration(displayPositionMs),
                style = NovaMonoTelemetry,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
            val rightLabel = if (showRemainingTime) {
                "-${FormatUtils.formatDuration((playerState.durationMs - displayPositionMs).coerceAtLeast(0L))}"
            } else {
                FormatUtils.formatDuration(playerState.durationMs)
            }
            Text(
                text = rightLabel,
                style = NovaMonoTelemetry,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Floating Optical Liquid Glass Transport Controls
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

        Spacer(modifier = Modifier.height(22.dp))

        // BOTTOM: Floating Glass Lyrics & Queue Controls (+ quick DSP pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassSurface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                elevation = 10.dp,
                contentPadding = PaddingValues(vertical = 12.dp),
                onClick = onOpenLyrics
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            AnimatedVisibility(
                visible = isEqEnabled,
                enter = fadeIn(tween(200)) + expandHorizontally(),
                exit = fadeOut(tween(160)) + shrinkHorizontally()
            ) {
                LiquidGlassIconButton(
                    icon = Icons.Rounded.Equalizer,
                    contentDescription = "Equalizer Active",
                    active = true,
                    showDotWhenActive = true,
                    size = 44.dp,
                    iconSize = 20.dp,
                    onClick = onOpenEqualizer
                )
            }

            LiquidGlassSurface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                elevation = 10.dp,
                contentPadding = PaddingValues(vertical = 12.dp),
                onClick = onOpenQueue
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (playerState.queue.isNotEmpty()) "Queue (${playerState.queue.size})" else "Queue",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
            }
        }
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
    LiquidGlassSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp)
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
                        text = "Playing Next",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${queue.size} tracks in session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queue, key = { idx, s -> "${idx}_${s.id}" }) { index, track ->
                    val isActive = index == currentIndex
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        highlighted = isActive,
                        elevation = if (isActive) 10.dp else 0.dp,
                        glassOpacity = if (isActive) 1f else 0.55f,
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
                                LiquidGlassArtwork(
                                    artworkUri = track.artworkUri,
                                    contentDescription = track.album,
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(11.dp),
                                    shadowElevation = 8.dp
                                )
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color.Black.copy(alpha = 0.48f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedEqualizerBars(isPlaying = isPlaying, color = Color.White)
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
