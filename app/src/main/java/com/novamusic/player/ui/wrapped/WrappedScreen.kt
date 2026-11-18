package com.novamusic.player.ui.wrapped

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.ui.components.LiquidGlassArtwork
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.ui.theme.NovaRedPrimary
import com.novamusic.player.ui.theme.rememberArtworkPalette
import com.novamusic.player.wrapped.WrappedAnalyticsEngine
import com.novamusic.player.wrapped.WrappedPeriod
import com.novamusic.player.wrapped.WrappedPeriodRange
import com.novamusic.player.wrapped.WrappedSummaryStats
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private const val STORY_PAGE_COUNT = 8

@Composable
fun WrappedScreen(
    viewModel: WrappedViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var replaySeed by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { STORY_PAGE_COUNT })

    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { event ->
            when (event) {
                is WrappedFeedbackEvent.Message -> {
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    val stats = uiState.stats
    val backdropUri = remember(stats, pagerState.currentPage) {
        val raw = when (pagerState.currentPage) {
            1 -> stats?.topArtist?.artworkUri
            3 -> stats?.topAlbums?.firstOrNull()?.artworkUri
            else -> stats?.primaryArtworkUri
        }
        raw?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }
    val dynamicPalette = rememberArtworkPalette(
        artworkUri = backdropUri,
        fallbackAccent = NovaRedPrimary
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040508))
    ) {
        // Dynamic Liquid Glass ambient lighting backdrop reacting to active story page artwork
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06070B),
                        lerp(Color(0xFF0C0508), dynamicPalette.deepTint, 0.55f),
                        Color(0xFF040406)
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dynamicPalette.primary.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(w * 0.20f, h * 0.18f),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * 0.20f, h * 0.18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dynamicPalette.glow.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * 0.84f, h * 0.72f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.84f, h * 0.72f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Story Progress Segments
            if (stats?.hasData == true) {
                StoryProgressRow(
                    totalPages = STORY_PAGE_COUNT,
                    currentPage = pagerState.currentPage,
                    accentColor = dynamicPalette.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Header: Brand, Replay, Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOVA WRAPPED",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = stats?.periodRange?.subtitle ?: "Listening Insights",
                        style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                        color = dynamicPalette.glow
                    )
                }

                if (stats?.hasData == true) {
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.Replay,
                        contentDescription = "Replay Wrapped",
                        onClick = {
                            replaySeed++
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        size = 40.dp,
                        iconSize = 19.dp,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                LiquidGlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close Wrapped",
                    onClick = onBack,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = Color.White
                )
            }

            // Time Period Filter Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WrappedPeriod.entries) { period ->
                    val selected = uiState.selectedPeriod == period
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        highlighted = selected,
                        accentTint = dynamicPalette.primary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        onClick = {
                            if (uiState.selectedPeriod != period) {
                                viewModel.selectPeriod(period)
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                        }
                    ) {
                        Text(
                            text = period.shortLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            // Main Content Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading || stats == null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = dynamicPalette.primary)
                        }
                    }

                    !stats.hasData -> {
                        WrappedEmptyStateCard(
                            periodRange = stats.periodRange,
                            accentColor = dynamicPalette.primary,
                            onSelectAllTime = { viewModel.selectPeriod(WrappedPeriod.ALL_TIME) },
                            onSelectThisMonth = { viewModel.selectPeriod(WrappedPeriod.THIS_MONTH) },
                            onClose = onBack
                        )
                    }

                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) { pageIndex ->
                                val pageOffset = ((pagerState.currentPage - pageIndex) +
                                    pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                                val scale = 1f - abs(pageOffset) * 0.055f
                                val alpha = 1f - abs(pageOffset) * 0.35f

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            this.alpha = alpha
                                        }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    when (pageIndex) {
                                        0 -> StoryYearInMusicPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary,
                                            isActive = pagerState.currentPage == 0,
                                            replaySeed = replaySeed
                                        )
                                        1 -> StoryTopArtistPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary,
                                            isActive = pagerState.currentPage == 1,
                                            replaySeed = replaySeed
                                        )
                                        2 -> StoryTopSongsPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary,
                                            onPlaySong = viewModel::playSongFromWrapped
                                        )
                                        3 -> StoryTopAlbumsPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary
                                        )
                                        4 -> StoryMusicTastePage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary
                                        )
                                        5 -> StoryListeningHoursPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary,
                                            isActive = pagerState.currentPage == 5,
                                            replaySeed = replaySeed
                                        )
                                        6 -> StoryPersonalityPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary
                                        )
                                        7 -> StoryFinalSummaryPage(
                                            stats = stats,
                                            accentColor = dynamicPalette.primary,
                                            isGeneratingImage = uiState.isGeneratingImage,
                                            onReplay = {
                                                replaySeed++
                                                scope.launch { pagerState.animateScrollToPage(0) }
                                            },
                                            onShare = { viewModel.shareWrappedImage(context) },
                                            onSaveImage = { viewModel.saveWrappedImage(context) }
                                        )
                                    }
                                }
                            }

                            // Subtle bottom story navigation controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LiquidGlassIconButton(
                                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Previous Story",
                                    onClick = {
                                        if (pagerState.currentPage > 0) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                        }
                                    },
                                    size = 38.dp,
                                    iconSize = 22.dp,
                                    tint = Color.White.copy(alpha = if (pagerState.currentPage > 0) 0.95f else 0.30f)
                                )

                                Text(
                                    text = "${pagerState.currentPage + 1} / $STORY_PAGE_COUNT • Swipe to explore",
                                    style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.62f)
                                )

                                LiquidGlassIconButton(
                                    icon = if (pagerState.currentPage < STORY_PAGE_COUNT - 1) {
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight
                                    } else {
                                        Icons.Rounded.Replay
                                    },
                                    contentDescription = "Next Story",
                                    onClick = {
                                        if (pagerState.currentPage < STORY_PAGE_COUNT - 1) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                        } else {
                                            replaySeed++
                                            scope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    },
                                    size = 38.dp,
                                    iconSize = 20.dp,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun StoryProgressRow(
    totalPages: Int,
    currentPage: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (i in 0 until totalPages) {
            val targetProgress = when {
                i < currentPage -> 1f
                i == currentPage -> 1f
                else -> 0f
            }
            val animatedFill by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                label = "story_bar_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFill)
                        .fillMaxHeight()
                        .background(
                            if (i == currentPage) accentColor else Color.White.copy(alpha = 0.92f)
                        )
                )
            }
        }
    }
}

@Composable
private fun StoryYearInMusicPage(
    stats: WrappedSummaryStats,
    accentColor: Color,
    isActive: Boolean,
    replaySeed: Int
) {
    var triggerAnim by remember(replaySeed) { mutableStateOf(false) }
    LaunchedEffect(isActive, replaySeed) {
        if (isActive) triggerAnim = true
    }

    val isSubMinute = stats.totalListeningMs < 60_000L
    val targetPrimaryUnit = if (isSubMinute) {
        max(1, (stats.totalListeningMs / 1_000L).toInt())
    } else {
        (stats.totalListeningMs / 60_000L).toInt()
    }
    val animPrimaryUnit by animateIntAsState(
        targetValue = if (triggerAnim) targetPrimaryUnit else 0,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "anim_minutes"
    )
    val animPlays by animateIntAsState(
        targetValue = if (triggerAnim) stats.totalPlays else 0,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "anim_plays"
    )
    val animTracks by animateIntAsState(
        targetValue = if (triggerAnim) stats.uniqueTracks else 0,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "anim_tracks"
    )

    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                StoryEyebrowBadge(text = "01 • YOUR MUSIC IN NUMBERS", accentColor = accentColor)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stats.periodRange.displayTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Every real play logged from your offline collection inside NOVA MUSIC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            Column(
                modifier = Modifier.padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(1.dp, accentColor.copy(alpha = 0.36f), RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL LISTENING TIME",
                            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSubMinute) "$animPrimaryUnit sec" else "$animPrimaryUnit min",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 46.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatHoursAndMinutes(stats.totalListeningMs),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WrappedMiniStatTile(
                        label = "TRACK PLAYS",
                        value = "$animPlays",
                        subtitle = "${stats.completedPlays} completed",
                        modifier = Modifier.weight(1f)
                    )
                    WrappedMiniStatTile(
                        label = "UNIQUE TRACKS",
                        value = "$animTracks",
                        subtitle = "${stats.uniqueArtists} artists",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WrappedMiniStatTile(
                        label = "ACTIVE DAYS",
                        value = "${stats.activeDaysCount}",
                        subtitle = "listening days",
                        modifier = Modifier.weight(1f)
                    )
                    WrappedMiniStatTile(
                        label = "BEST STREAK",
                        value = "${stats.longestStreakDays}d",
                        subtitle = "consecutive days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "Verified real-time Media3 listening logs • 100% on-device",
                style = NovaMonoTelemetry.copy(fontSize = 9.5.sp),
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun StoryTopArtistPage(
    stats: WrappedSummaryStats,
    accentColor: Color,
    isActive: Boolean,
    replaySeed: Int
) {
    val topArtist = stats.topArtist
    val animShare by animateIntAsState(
        targetValue = if (isActive && topArtist != null) topArtist.sharePercent else 0,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "artist_share_$replaySeed"
    )

    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StoryEyebrowBadge(text = "02 • YOUR TOP ARTIST", accentColor = accentColor)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (topArtist != null) "Your #1 Sound" else "Artist Spotlight",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
            }

            if (topArtist != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 18.dp)
                ) {
                    LiquidGlassArtwork(
                        artworkUri = topArtist.artworkUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                        contentDescription = topArtist.name,
                        modifier = Modifier.size(188.dp),
                        shape = RoundedCornerShape(36.dp),
                        shadowElevation = 28.dp,
                        glowTint = accentColor,
                        fallbackIcon = Icons.Rounded.Person
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = topArtist.name,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Captured $animShare% of your listening time",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WrappedMiniStatTile(
                        label = "PLAYS",
                        value = "${topArtist.playCount}",
                        subtitle = "${topArtist.uniqueTracksCount} songs",
                        modifier = Modifier.weight(1f)
                    )
                    WrappedMiniStatTile(
                        label = "LISTEN TIME",
                        value = formatHoursAndMinutes(topArtist.totalListeningMs),
                        subtitle = "active audio",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "Artist tags were missing from the tracks played in this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StoryTopSongsPage(
    stats: WrappedSummaryStats,
    accentColor: Color,
    onPlaySong: (Long) -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            StoryEyebrowBadge(text = "03 • YOUR TOP 5 SONGS", accentColor = accentColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Most Played Tracks",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            Text(
                text = "Tap any track to jump straight back into it.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (stats.topSongs.isEmpty()) {
                Text(
                    text = "Play at least 30 seconds of a track to rank your top songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.70f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.topSongs.forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (song.rank == 1) accentColor.copy(alpha = 0.16f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (song.rank == 1) accentColor.copy(alpha = 0.42f)
                                    else Color.White.copy(alpha = 0.10f),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable(enabled = song.songId > 0L) {
                                    onPlaySong(song.songId)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${song.rank}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = if (song.rank == 1) accentColor else Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.width(34.dp)
                            )
                            LiquidGlassArtwork(
                                artworkUri = song.artworkUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                                contentDescription = song.title,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 8.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${song.playCount} plays",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = formatHoursAndMinutes(song.totalListeningMs),
                                    style = NovaMonoTelemetry.copy(fontSize = 9.sp),
                                    color = Color.White.copy(alpha = 0.56f)
                                )
                            }
                            if (song.songId > 0L) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play ${song.title}",
                                    tint = accentColor,
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

@Composable
private fun StoryTopAlbumsPage(
    stats: WrappedSummaryStats,
    accentColor: Color
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            StoryEyebrowBadge(text = "04 • YOUR TOP ALBUMS", accentColor = accentColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Records You Lived In",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(18.dp))

            val leadAlbum = stats.topAlbums.firstOrNull()
            if (leadAlbum == null) {
                Text(
                    text = "No tagged album metadata was available for tracks played in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            } else {
                // Featured #1 Album
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassArtwork(
                        artworkUri = leadAlbum.artworkUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                        contentDescription = leadAlbum.name,
                        modifier = Modifier.size(92.dp),
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 18.dp,
                        fallbackIcon = Icons.Rounded.Album
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#1 TOP ALBUM",
                            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = leadAlbum.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = leadAlbum.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${formatHoursAndMinutes(leadAlbum.totalListeningMs)} • ${leadAlbum.playCount} plays",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                val runnersUp = stats.topAlbums.drop(1)
                if (runnersUp.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        runnersUp.forEach { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${album.rank}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.width(34.dp)
                                )
                                LiquidGlassArtwork(
                                    artworkUri = album.artworkUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                                    contentDescription = album.name,
                                    modifier = Modifier.size(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 6.dp,
                                    fallbackIcon = Icons.Rounded.Album
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = album.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatHoursAndMinutes(album.totalListeningMs),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryMusicTastePage(
    stats: WrappedSummaryStats,
    accentColor: Color
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                StoryEyebrowBadge(text = "05 • YOUR MUSIC TASTE", accentColor = accentColor)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (stats.hasReliableGenres) "Your Genre Spectrum" else "Your Listening Diversity",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (stats.hasReliableGenres) {
                        "Calculated strictly from verified genre tags across your played songs."
                    } else {
                        "Your audio files in this period do not carry reliable genre ID3 tags, so here is your authentic variety breakdown instead."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }

            if (stats.hasReliableGenres) {
                Column(
                    modifier = Modifier.padding(vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val maxPct = max(1, stats.topGenres.maxOf { it.percent })
                    stats.topGenres.forEachIndexed { idx, genre ->
                        val barFraction = (genre.percent.toFloat() / maxPct.toFloat()).coerceIn(0.12f, 1f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "${genre.percent}%",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (idx == 0) accentColor else Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (idx == 0) accentColor else accentColor.copy(alpha = 0.62f)
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WrappedMiniStatTile(
                            label = "ARTISTS",
                            value = "${stats.uniqueArtists}",
                            subtitle = "unique artists",
                            modifier = Modifier.weight(1f)
                        )
                        WrappedMiniStatTile(
                            label = "SONGS",
                            value = "${stats.uniqueTracks}",
                            subtitle = "unique tracks",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val compPct = if (stats.totalSessions > 0) {
                            ((stats.completedPlays * 100f) / stats.totalSessions).toInt()
                        } else 0
                        WrappedMiniStatTile(
                            label = "COMPLETION",
                            value = "$compPct%",
                            subtitle = "tracks finished",
                            modifier = Modifier.weight(1f)
                        )
                        WrappedMiniStatTile(
                            label = "SKIPS",
                            value = "${stats.skippedCount}",
                            subtitle = "early skips",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text(
                text = "${stats.uniqueArtists} artists • ${stats.uniqueTracks} unique songs",
                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.50f)
            )
        }
    }
}

@Composable
private fun StoryListeningHoursPage(
    stats: WrappedSummaryStats,
    accentColor: Color,
    isActive: Boolean,
    replaySeed: Int
) {
    val barScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "hour_bars_$replaySeed"
    )

    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                StoryEyebrowBadge(text = "06 • YOUR LISTENING HOURS", accentColor = accentColor)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stats.peakWindowLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your peak hour was ${WrappedAnalyticsEngine.formatHourLabel(stats.peakHour)}, with ${stats.mostActiveDayLabel} leading the week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            // 24-Hour Histogram Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 18.dp)
            ) {
                Column {
                    val maxBucketMs = max(1L, stats.hourlyActivity.maxOfOrNull { it.totalListeningMs } ?: 1L)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(164.dp)
                    ) {
                        val barCount = 24
                        val spacing = 5.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barW = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)
                        val maxH = size.height

                        stats.hourlyActivity.forEachIndexed { idx, bucket ->
                            val ratio = (bucket.totalListeningMs.toFloat() / maxBucketMs.toFloat()).coerceIn(0f, 1f)
                            val barH = max(6f, maxH * ratio * barScale)
                            val left = idx * (barW + spacing)
                            val top = maxH - barH
                            val isPeak = idx == stats.peakHour && bucket.totalListeningMs > 0L

                            drawRoundRect(
                                color = if (isPeak) accentColor else Color.White.copy(
                                    alpha = if (bucket.totalListeningMs > 0L) 0.42f else 0.10f
                                ),
                                topLeft = Offset(left, top),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM").forEach { mark ->
                            Text(
                                text = mark,
                                style = NovaMonoTelemetry.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.52f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WrappedMiniStatTile(
                    label = "PEAK HOUR",
                    value = WrappedAnalyticsEngine.formatHourLabel(stats.peakHour),
                    subtitle = "most active hour",
                    modifier = Modifier.weight(1f)
                )
                WrappedMiniStatTile(
                    label = "TOP DAY",
                    value = stats.mostActiveDayLabel.take(3).uppercase(),
                    subtitle = stats.mostActiveDayLabel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StoryPersonalityPage(
    stats: WrappedSummaryStats,
    accentColor: Color
) {
    val persona = stats.personality

    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 16.dp,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                StoryEyebrowBadge(text = "07 • YOUR LISTENING PERSONALITY", accentColor = accentColor)
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (persona != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.55f), accentColor.copy(alpha = 0.18f))
                                )
                            )
                            .border(1.2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = persona.badgeLabel,
                        style = NovaMonoTelemetry.copy(fontSize = 11.sp),
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = persona.archetypeTitle,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 31.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = persona.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    persona.highlights.forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryFinalSummaryPage(
    stats: WrappedSummaryStats,
    accentColor: Color,
    isGeneratingImage: Boolean,
    onReplay: () -> Unit,
    onShare: () -> Unit,
    onSaveImage: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        accentTint = accentColor,
        highlighted = true,
        elevation = 18.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                StoryEyebrowBadge(text = "08 • YOUR NOVA SUMMARY", accentColor = accentColor)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stats.periodRange.displayTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Compact Summary Card Preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiquidGlassArtwork(
                            artworkUri = stats.primaryArtworkUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                            contentDescription = "Summary Artwork",
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 10.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TOP ARTIST",
                                style = NovaMonoTelemetry.copy(fontSize = 9.sp),
                                color = accentColor
                            )
                            Text(
                                text = stats.topArtist?.name ?: "Your Library",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatHoursAndMinutes(stats.totalListeningMs)} • ${stats.totalPlays} plays",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    if (stats.topSongs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TOP TRACKS",
                                style = NovaMonoTelemetry.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.56f)
                            )
                            stats.topSongs.take(3).forEach { song ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${song.rank}. ${song.title}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${song.playCount}x",
                                        style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }

                    stats.personality?.let { persona ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.16f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = persona.archetypeTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${stats.longestStreakDays}d streak",
                                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                color = accentColor
                            )
                        }
                    }
                }
            }

            // Share / Save / Replay Actions
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassButton(
                        onClick = onShare,
                        enabled = !isGeneratingImage,
                        highlighted = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isGeneratingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    LiquidGlassButton(
                        onClick = onSaveImage,
                        enabled = !isGeneratingImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Image",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                LiquidGlassButton(
                    onClick = onReplay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Replay Wrapped",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WrappedEmptyStateCard(
    periodRange: WrappedPeriodRange,
    accentColor: Color,
    onSelectAllTime: () -> Unit,
    onSelectThisMonth: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            accentTint = accentColor,
            highlighted = true,
            contentPadding = PaddingValues(26.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "Not Enough History for ${periodRange.displayTitle}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "NOVA WRAPPED only displays real recorded listening sessions from ${periodRange.subtitle}. Listen to at least 30 seconds of your tracks to build this timeline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (periodRange.period != WrappedPeriod.ALL_TIME) {
                        LiquidGlassButton(
                            onClick = onSelectAllTime,
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "All Time",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    if (periodRange.period != WrappedPeriod.THIS_MONTH) {
                        LiquidGlassButton(
                            onClick = onSelectThisMonth,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "This Month",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }

                LiquidGlassButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Back to Music",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.86f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryEyebrowBadge(
    text: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.16f))
            .border(0.8.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

@Composable
private fun WrappedMiniStatTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = label,
            style = NovaMonoTelemetry.copy(fontSize = 9.sp),
            color = Color.White.copy(alpha = 0.62f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatHoursAndMinutes(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        totalMinutes > 0L -> "${totalMinutes}m"
        else -> "${(ms / 1_000L).coerceAtLeast(1L)}s"
    }
}
