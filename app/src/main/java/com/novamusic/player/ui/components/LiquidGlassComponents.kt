package com.novamusic.player.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novamusic.player.data.model.RepeatModeOption
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaOrangeDeep
import com.novamusic.player.ui.theme.NovaOrangeGlow

data class LiquidGlassNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Dynamic ambient Liquid Glass backdrop that renders blurred live album artwork (API 31+ RenderEffect blur)
 * layered with floating ambient radial refraction orbs and graceful pre-API-31 frosted fallbacks.
 */
@Composable
fun LiquidGlassBackdrop(
    artworkUri: Uri?,
    isPlaying: Boolean,
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val supportsHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val ambientTransition = rememberInfiniteTransition(label = "glass_ambient")
    val orbShift by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_shift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (artworkUri != null) {
            val blurModifier = if (supportsHardwareBlur) {
                Modifier.blur(radius = (86f * intensity).dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            }
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(256)
                    .crossfade(450)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.32f + orbShift * 0.08f
                        scaleY = 1.32f + orbShift * 0.08f
                        alpha = if (supportsHardwareBlur) {
                            (if (isLight) 0.30f else 0.44f) * intensity.coerceIn(0.6f, 1.3f)
                        } else {
                            if (isLight) 0.14f else 0.18f
                        }
                    }
                    .then(blurModifier)
            )
        }

        // Dynamic radial refraction light orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val playBoost = if (isPlaying) 1.18f else 0.9f

            val topOrbCenter = Offset(
                x = w * (0.22f + 0.18f * orbShift),
                y = h * (0.14f + 0.06f * orbShift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = (if (isLight) 0.22f else 0.26f) * playBoost * intensity),
                        Color.Transparent
                    ),
                    center = topOrbCenter,
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = topOrbCenter
            )

            val bottomOrbCenter = Offset(
                x = w * (0.82f - 0.18f * orbShift),
                y = h * (0.78f - 0.08f * orbShift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NovaOrangeGlow.copy(alpha = (if (isLight) 0.16f else 0.15f) * intensity),
                        Color.Transparent
                    ),
                    center = bottomOrbCenter,
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = bottomOrbCenter
            )
        }

        // Atmospheric glass readability veil (stronger on API < 31 fallback)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isLight) {
                            listOf(
                                Color(0xCCFFFFFF),
                                Color(0x99F3F5FA),
                                Color(0xE8F1F4F9)
                            )
                        } else {
                            listOf(
                                Color(0x66060609),
                                Color(0x8A060609),
                                Color(0xD8050507)
                            )
                        }
                    )
                )
        )
    }
}

/**
 * Multi-layered iOS 26 Liquid Glass container with directional specular border,
 * upper internal refraction reflection, and smooth ambient depth shadow.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentTint: Color? = null,
    highlighted: Boolean = false,
    elevation: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val novaColors = LocalNovaColors.current
    val accent = accentTint ?: MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val preApi31FallbackBoost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 1f else 1.12f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "glass_card_scale"
    )

    val baseFill = if (highlighted) {
        if (isLight) Color(0xD9FFFFFF) else Color(0xAE1D1E26)
    } else {
        novaColors.glassSurface.copy(
            alpha = (novaColors.glassSurface.alpha * preApi31FallbackBoost).coerceAtMost(0.95f)
        )
    }

    val topBorder = if (highlighted) accent.copy(alpha = 0.75f) else novaColors.glassBorderTop
    val bottomBorder = if (highlighted) accent.copy(alpha = 0.24f) else novaColors.glassBorderBottom

    val glassFillBrush = Brush.verticalGradient(
        colors = listOf(
            if (isLight) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.11f),
            if (highlighted) accent.copy(alpha = 0.12f) else baseFill,
            baseFill
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            topBorder,
            if (isLight) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f),
            bottomBorder
        )
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(pressScale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (highlighted) accent else novaColors.glassShadow,
                spotColor = if (highlighted) accent else novaColors.glassShadow
            )
            .clip(shape)
            .background(glassFillBrush)
            .drawWithContent {
                drawContent()
                // Subtle upper specular reflection streak
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isLight) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.36f
                    )
                )
            }
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Floating iOS 26 Liquid Glass pill button with tactile spring scale and specular rim.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "glass_btn_scale"
    )

    val bgBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                NovaOrangeGlow.copy(alpha = if (enabled) 0.96f else 0.45f),
                accent.copy(alpha = if (enabled) 0.96f else 0.45f),
                NovaOrangeDeep.copy(alpha = if (enabled) 0.96f else 0.45f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (isLight) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f),
                novaColors.glassSurface
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            if (highlighted) Color.White.copy(alpha = 0.65f) else novaColors.glassBorderTop,
            if (highlighted) accent.copy(alpha = 0.4f) else novaColors.glassBorderBottom
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (highlighted) 14.dp else 8.dp,
                shape = shape,
                ambientColor = if (highlighted) accent else novaColors.glassShadow,
                spotColor = if (highlighted) accent else novaColors.glassShadow
            )
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Circular Liquid Glass icon orb with spring press animation and optional glowing active halo.
 */
@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    active: Boolean = false,
    showDotWhenActive: Boolean = false,
    tint: Color? = null
) {
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "glass_icon_scale"
    )

    val bgBrush = if (active) {
        Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.32f),
                accent.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (isLight) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.14f),
                novaColors.glassSurface
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            if (active) accent.copy(alpha = 0.85f) else novaColors.glassBorderTop,
            if (active) accent.copy(alpha = 0.30f) else novaColors.glassBorderBottom
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (active) 10.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (active) accent else novaColors.glassShadow,
                spotColor = if (active) accent else novaColors.glassShadow
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, borderBrush, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: if (active) accent else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(iconSize)
        )
        if (active && showDotWhenActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

/**
 * Floating iOS 26 Liquid Glass pill bottom navigation bar with translucent glass reflection,
 * luminous directional border, and animated morphing active tab capsule.
 */
@Composable
fun LiquidGlassNavigationBar(
    tabs: List<LiquidGlassNavTab>,
    currentRoute: String,
    isEqualizerActive: Boolean,
    onSelectTab: (LiquidGlassNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        shape = RoundedCornerShape(30.dp),
        elevation = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute.startsWith(tab.route)
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.14f else 1f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
                    label = "lg_nav_scale"
                )
                val pillAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(220),
                    label = "lg_nav_alpha"
                )
                val contentTint by animateColorAsState(
                    targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "lg_nav_tint"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (selected) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = (if (isLight) 0.16f else 0.24f) * pillAlpha),
                                        accent.copy(alpha = 0.06f * pillAlpha)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (selected) 0.9.dp else 0.dp,
                            color = if (selected) accent.copy(alpha = 0.45f * pillAlpha) else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectTab(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = contentTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                            if (tab.route == "equalizer" && isEqualizerActive) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 5.dp, y = (-2).dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentTint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Liquid Glass horizontal seek slider with glowing fluid fill and expanding glass thumb.
 */
@Composable
fun LiquidGlassSlider(
    progress: Float,
    isScrubbing: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val novaColors = LocalNovaColors.current
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestOnFinished by rememberUpdatedState(onScrubFinished)
    var lastFraction by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    val clampedProgress = progress.coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 11.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "glass_slider_height"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    latestOnFinished(f)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        lastFraction = f
                        latestOnScrub(f)
                    },
                    onDragEnd = { latestOnFinished(lastFraction) },
                    onDragCancel = { latestOnFinished(lastFraction) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val f = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        lastFraction = f
                        latestOnScrub(f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = trackHeight.toPx()
            val y = (size.height - h) / 2f
            val corner = CornerRadius(h / 2f, h / 2f)

            // Recessed translucent glass channel
            drawRoundRect(
                color = if (novaColors.isLight) Color(0x220F172A) else Color.White.copy(alpha = 0.14f),
                topLeft = Offset(0f, y),
                size = Size(size.width, h),
                cornerRadius = corner
            )

            // Luminous liquid accent fill
            val fillWidth = (clampedProgress * size.width).coerceAtLeast(h)
            if (clampedProgress > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.72f),
                            NovaOrangeGlow,
                            accentColor
                        )
                    ),
                    topLeft = Offset(0f, y),
                    size = Size(fillWidth.coerceAtMost(size.width), h),
                    cornerRadius = corner
                )
            }

            // Glass thumb orb
            val thumbRadius = if (isScrubbing) 10.dp.toPx() else 7.5.dp.toPx()
            val thumbX = (clampedProgress * size.width).coerceIn(thumbRadius, size.width - thumbRadius)
            drawCircle(
                color = accentColor.copy(alpha = 0.34f),
                radius = thumbRadius + 4.dp.toPx(),
                center = Offset(thumbX, size.height / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, size.height / 2f)
            )
        }
    }
}

/**
 * Reusable iOS 26 Liquid Glass Player Transport Controls with animated Play/Pause morph,
 * luminous breathing halo, and floating glass Shuffle / Previous / Next / Repeat buttons.
 */
@Composable
fun LiquidGlassPlayerControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatModeOption,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val pulseTransition = rememberInfiniteTransition(label = "lg_play_halo")
    val haloAlpha by pulseTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lg_halo_alpha"
    )

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 480f),
        label = "lg_play_scale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LiquidGlassIconButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "Shuffle",
            active = shuffleEnabled,
            showDotWhenActive = true,
            size = 44.dp,
            iconSize = 20.dp,
            onClick = onToggleShuffle
        )

        LiquidGlassIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            size = 56.dp,
            iconSize = 30.dp,
            onClick = onPrevious
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(92.dp)
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(playScale)
                    .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NovaOrangeGlow, accent, NovaOrangeDeep)
                        )
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.16f))
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null,
                        onClick = onTogglePlayPause
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.75f, animationSpec = tween(150))) togetherWith
                            (fadeOut(tween(120)) + scaleOut(targetScale = 0.75f, animationSpec = tween(120)))
                    },
                    label = "play_pause_morph"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        LiquidGlassIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next",
            size = 56.dp,
            iconSize = 30.dp,
            onClick = onNext
        )

        LiquidGlassIconButton(
            icon = if (repeatMode == RepeatModeOption.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = "Repeat Mode",
            active = repeatMode != RepeatModeOption.OFF,
            showDotWhenActive = true,
            size = 44.dp,
            iconSize = 20.dp,
            onClick = onCycleRepeat
        )
    }
}

/**
 * Frosted Liquid Glass Search Bar supporting both interactive text entry and read-only launcher mode.
 */
@Composable
fun LiquidGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search songs, albums, artists...",
    modifier: Modifier = Modifier,
    readOnlyOnClick: (() -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 10.dp,
        highlighted = query.isNotEmpty(),
        onClick = readOnlyOnClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (readOnlyOnClick != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = hintColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = hintColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                        cursorBrush = SolidColor(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions.Default,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (readOnlyOnClick == null && query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        tint = textColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
