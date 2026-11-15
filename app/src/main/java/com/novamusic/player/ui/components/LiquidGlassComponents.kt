package com.novamusic.player.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novamusic.player.data.model.NavBarAppearance
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.data.model.RepeatModeOption
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.utils.FormatUtils
import kotlin.math.abs

val LocalReducedMotion = compositionLocalOf { false }
val LocalHapticEnabled = compositionLocalOf { true }
val LocalBlurIntensity = compositionLocalOf { 1.0f }
val LocalPlayerAnimationStyle = compositionLocalOf { PlayerAnimationStyle.FLUID }
val LocalNavBarAppearance = compositionLocalOf { NavBarAppearance.FLOATING_GLASS }

@Composable
fun rememberNovaHaptic(): () -> Unit {
    val enabled = LocalHapticEnabled.current
    val haptic = LocalHapticFeedback.current
    return remember(enabled, haptic) {
        {
            if (enabled) {
                runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            }
        }
    }
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

data class LiquidGlassNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Full-bleed atmospheric backdrop driven by extracted live album artwork colors,
 * hardware RenderEffect blur (API 31+ with frosted fallback on API 26-30),
 * and gentle ambient light scattering orbs.
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
    val palette = LocalArtworkPalette.current
    val isLight = novaColors.isLight
    val supportsHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurRadiusPx = with(LocalDensity.current) { (92f * intensity).dp.toPx() }

    val ambientTransition = rememberInfiniteTransition(label = "lg_backdrop_ambient")
    val orbShift by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lg_orb_shift"
    )

    val backgroundBase = if (isLight) {
        lerp(MaterialTheme.colorScheme.background, palette.primary, 0.08f)
    } else {
        lerp(MaterialTheme.colorScheme.background, palette.deepTint, 0.55f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBase)
    ) {
        if (artworkUri != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(220)
                    .crossfade(650)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pulseScale = if (isPlaying) 1.38f + orbShift * 0.08f else 1.32f
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = if (supportsHardwareBlur) {
                            (if (isLight) 0.34f else 0.56f) * intensity.coerceIn(0.65f, 1.35f)
                        } else {
                            if (isLight) 0.16f else 0.24f
                        }
                        if (supportsHardwareBlur) {
                            renderEffect = RenderEffect
                                .createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.MIRROR)
                                .asComposeRenderEffect()
                        }
                    }
            )
        }

        // Dynamic multi-hue light scattering mesh from artwork palette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val playBoost = if (isPlaying) 1.22f else 0.92f

            val primaryOrb = Offset(
                x = w * (0.20f + 0.22f * orbShift),
                y = h * (0.16f + 0.08f * orbShift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = (if (isLight) 0.28f else 0.36f) * playBoost * intensity.coerceAtMost(1.2f)),
                        Color.Transparent
                    ),
                    center = primaryOrb,
                    radius = w * 0.92f
                ),
                radius = w * 0.92f,
                center = primaryOrb
            )

            val secondaryOrb = Offset(
                x = w * (0.84f - 0.22f * orbShift),
                y = h * (0.68f - 0.12f * orbShift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = (if (isLight) 0.22f else 0.30f) * intensity.coerceAtMost(1.2f)),
                        Color.Transparent
                    ),
                    center = secondaryOrb,
                    radius = w * 0.88f
                ),
                radius = w * 0.88f,
                center = secondaryOrb
            )

            val glowOrb = Offset(
                x = w * (0.50f + 0.12f * (0.5f - orbShift)),
                y = h * 0.38f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = (if (isLight) 0.14f else 0.14f) * playBoost),
                        Color.Transparent
                    ),
                    center = glowOrb,
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = glowOrb
            )
        }

        // Atmospheric readability gradient veil (rich contrast for crisp white/dark typography)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isLight) {
                            listOf(
                                Color(0x9EF5F7FC),
                                Color(0x88EEF2F9),
                                Color(0xDBEBEEF6)
                            )
                        } else {
                            listOf(
                                Color(0x47040407),
                                Color(0x70040407),
                                Color(0xCF030306)
                            )
                        }
                    )
                )
        )
    }
}

/**
 * Core iOS 26 Optical Liquid Glass Material:
 * - Multi-layered translucency tinted softly with the active artwork's extracted glow
 * - Dynamic position-reactive specular refraction sweep via onGloballyPositioned
 * - Subtle top & bottom inner bevel shadows for physical lens thickness
 * - Ultra-thin variable-alpha luminous rim (no heavy boxy borders or neon outlines)
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    accentTint: Color? = null,
    highlighted: Boolean = false,
    elevation: Dp = 14.dp,
    glassOpacity: Float = 1f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val novaColors = LocalNovaColors.current
    val palette = LocalArtworkPalette.current
    val accent = accentTint ?: palette.primary
    val isLight = novaColors.isLight
    val api31Boost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 1f else 1.15f

    var rootPos by remember { mutableFloatStateOf(0f) }
    var rootX by remember { mutableFloatStateOf(0f) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.968f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lg_surface_scale"
    )

    val topLayerAlpha = (if (isLight) 0.80f else 0.14f) * glassOpacity * api31Boost
    val midLayerAlpha = (if (isLight) 0.62f else 0.08f) * glassOpacity * api31Boost
    val bottomLayerAlpha = (if (isLight) 0.72f else 0.11f) * glassOpacity * api31Boost

    val baseBrush = Brush.verticalGradient(
        colors = listOf(
            if (highlighted) lerp(Color.White, accent, 0.24f).copy(alpha = (topLayerAlpha + 0.08f).coerceAtMost(0.96f))
            else Color.White.copy(alpha = topLayerAlpha.coerceAtMost(0.95f)),
            if (highlighted) accent.copy(alpha = (midLayerAlpha + 0.10f).coerceAtMost(0.88f))
            else lerp(novaColors.glassSurface, palette.deepTint, if (isLight) 0.06f else 0.35f)
                .copy(alpha = (if (isLight) 0.66f else 0.36f * glassOpacity).coerceAtMost(0.94f)),
            lerp(novaColors.glassSurface, Color.Black, if (isLight) 0.03f else 0.40f)
                .copy(alpha = (if (isLight) 0.74f else 0.44f * glassOpacity).coerceAtMost(0.95f))
        )
    )

    val rimTop = if (highlighted) lerp(Color.White, accent, 0.35f).copy(alpha = 0.62f) else novaColors.glassBorderTop
    val rimBottom = if (highlighted) accent.copy(alpha = 0.26f) else novaColors.glassBorderBottom

    val rimBrush = Brush.linearGradient(
        colors = listOf(
            rimTop,
            if (isLight) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
            rimBottom
        )
    )

    val clickableMod = if (onClick != null) {
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
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                rootPos = pos.y
                rootX = pos.x
            }
            .scale(pressScale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (highlighted) palette.glow.copy(alpha = 0.55f) else novaColors.glassShadow,
                spotColor = if (highlighted) accent.copy(alpha = 0.65f) else novaColors.glassShadow
            )
            .clip(shape)
            .background(baseBrush)
            .drawWithContent {
                drawContent()

                // Position-reactive diagonal specular refraction catchlight
                val shiftPhase = ((rootPos + rootX) * 0.0008f) % 1f
                val specularStart = Offset(size.width * (0.05f + shiftPhase * 0.25f), 0f)
                val specularEnd = Offset(size.width * (0.72f + shiftPhase * 0.18f), size.height * 0.78f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isLight) 0.34f else 0.13f),
                            Color.White.copy(alpha = if (isLight) 0.10f else 0.03f),
                            Color.Transparent
                        ),
                        start = specularStart,
                        end = specularEnd
                    )
                )

                // Soft upper internal lip highlight
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            novaColors.innerShadowTop,
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = (size.height * 0.14f).coerceAtMost(28f)
                    )
                )

                // Soft lower internal depth shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            novaColors.innerShadowBottom.copy(alpha = novaColors.innerShadowBottom.alpha * 0.65f)
                        ),
                        startY = (size.height * 0.80f).coerceAtLeast(size.height - 32f),
                        endY = size.height
                    )
                )
            }
            .border(width = 0.75.dp, brush = rimBrush, shape = shape)
            .then(clickableMod)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Compatibility wrapper preserving existing references while upgrading all surfaces
 * to the multi-layer optical LiquidGlassSurface shader.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentTint: Color? = null,
    highlighted: Boolean = false,
    elevation: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier,
        shape = shape,
        accentTint = accentTint,
        highlighted = highlighted,
        elevation = elevation,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content
    )
}

/**
 * Tactile iOS 26 Liquid Glass Pill Button with spring bounce and luminous gradient lens.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    shape: Shape = RoundedCornerShape(22.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val novaColors = LocalNovaColors.current
    val palette = LocalArtworkPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lg_btn_scale"
    )

    val bgBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                lerp(accent, Color.White, 0.24f).copy(alpha = if (enabled) 0.96f else 0.42f),
                accent.copy(alpha = if (enabled) 0.96f else 0.42f),
                lerp(accent, palette.secondary, 0.28f).copy(alpha = if (enabled) 0.96f else 0.42f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isLight) 0.88f else 0.16f),
                novaColors.glassSurface.copy(alpha = if (isLight) 0.72f else 0.30f)
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            if (highlighted) Color.White.copy(alpha = 0.68f) else novaColors.glassBorderTop,
            if (highlighted) accent.copy(alpha = 0.28f) else novaColors.glassBorderBottom
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (highlighted) 16.dp else 8.dp,
                shape = shape,
                ambientColor = if (highlighted) accent.copy(alpha = 0.65f) else novaColors.glassShadow,
                spotColor = if (highlighted) accent.copy(alpha = 0.65f) else novaColors.glassShadow
            )
            .clip(shape)
            .background(bgBrush)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (highlighted) 0.26f else 0.12f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                )
            }
            .border(0.8.dp, borderBrush, shape)
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
 * Circular Optical Liquid Glass Lens Icon Button with spring press rebound.
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
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lg_icon_scale"
    )
    val activeFraction by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(220, easing = FastOutSlowInEasing),
        label = "lg_icon_active"
    )
    val resolvedTint by animateColorAsState(
        targetValue = tint ?: if (active) accent else MaterialTheme.colorScheme.onBackground,
        animationSpec = if (reducedMotion) snap() else tween(200),
        label = "lg_icon_tint"
    )

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            lerp(
                Color.White.copy(alpha = if (isLight) 0.90f else 0.16f),
                accent.copy(alpha = 0.34f),
                activeFraction
            ),
            lerp(
                novaColors.glassSurface.copy(alpha = if (isLight) 0.66f else 0.24f),
                accent.copy(alpha = 0.14f),
                activeFraction
            )
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            lerp(novaColors.glassBorderTop, accent.copy(alpha = 0.78f), activeFraction),
            lerp(novaColors.glassBorderBottom, accent.copy(alpha = 0.22f), activeFraction)
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (active) 12.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (active) accent.copy(alpha = 0.55f) else novaColors.glassShadow,
                spotColor = if (active) accent.copy(alpha = 0.55f) else novaColors.glassShadow
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(0.75.dp, borderBrush, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    performHaptic()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(iconSize)
        )
        if (showDotWhenActive && activeFraction > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = activeFraction
                        scaleY = activeFraction
                        alpha = activeFraction
                    }
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

/**
 * Floating iOS 26 Liquid Glass Top Bar — airy, unboxed floating orbs flanking crisp title/subtitle.
 */
@Composable
fun LiquidGlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leadingAction: (@Composable () -> Unit)? = null,
    trailingActions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingAction != null) {
            leadingAction()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingActions != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailingActions
            )
        }
    }
}

/**
 * Floating iOS 26 Liquid Glass 5-Tab Pill Bar with fluid GPU-driven spring capsule,
 * elastic travel morphing, soft radial glow, tactile press-down, and overshoot/settle icons.
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<LiquidGlassNavTab>,
    currentRoute: String,
    onSelectTab: (LiquidGlassNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val novaColors = LocalNovaColors.current
    val palette = LocalArtworkPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val reducedMotion = LocalReducedMotion.current
    val animStyle = LocalPlayerAnimationStyle.current
    val navAppearance = LocalNavBarAppearance.current
    val performHaptic = rememberNovaHaptic()
    val density = LocalDensity.current

    val showLabels = navAppearance != NavBarAppearance.MINIMAL_PILL
    val horizontalPad = when (navAppearance) {
        NavBarAppearance.FLOATING_GLASS -> 14.dp
        NavBarAppearance.COMPACT -> 18.dp
        NavBarAppearance.MINIMAL_PILL -> 34.dp
    }
    val barHeight = when (navAppearance) {
        NavBarAppearance.FLOATING_GLASS -> 66.dp
        NavBarAppearance.COMPACT -> 56.dp
        NavBarAppearance.MINIMAL_PILL -> 52.dp
    }
    val cornerRadius = when (navAppearance) {
        NavBarAppearance.FLOATING_GLASS -> 32.dp
        NavBarAppearance.COMPACT -> 28.dp
        NavBarAppearance.MINIMAL_PILL -> 26.dp
    }

    var lastValidIndex by remember { mutableIntStateOf(0) }
    val resolvedIndex = remember(currentRoute, tabs) {
        val direct = tabs.indexOfFirst { currentRoute.startsWith(it.route) }
        when {
            direct >= 0 -> direct
            currentRoute == "about" -> tabs.indexOfFirst { it.route == "settings" }.takeIf { it >= 0 }
            currentRoute.startsWith("playlist") || currentRoute == "favorites" ->
                tabs.indexOfFirst { it.route == "playlists" }.takeIf { it >= 0 }
            else -> null
        }
    }
    if (resolvedIndex != null) {
        lastValidIndex = resolvedIndex
    }
    val selectedIndex = (resolvedIndex ?: lastValidIndex).coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalPad, end = horizontalPad, bottom = 10.dp),
        shape = RoundedCornerShape(cornerRadius),
        elevation = 22.dp,
        glassOpacity = 1.05f
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .padding(5.dp)
        ) {
            val count = tabs.size.coerceAtLeast(1)
            val tabWidth = maxWidth / count
            val tabWidthPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
            val targetX = tabWidthPx * selectedIndex

            val capsuleX by animateFloatAsState(
                targetValue = targetX,
                animationSpec = when {
                    reducedMotion -> snap()
                    animStyle == PlayerAnimationStyle.MINIMAL -> tween(170, easing = FastOutSlowInEasing)
                    animStyle == PlayerAnimationStyle.BALANCED -> spring(dampingRatio = 0.84f, stiffness = 390f)
                    else -> spring(dampingRatio = 0.74f, stiffness = 340f)
                },
                label = "lg_tab_slide_px"
            )

            val travelFraction = if (reducedMotion || animStyle == PlayerAnimationStyle.MINIMAL) {
                0f
            } else {
                (abs(targetX - capsuleX) / tabWidthPx).coerceIn(0f, 1.35f)
            }
            val capsuleScaleX = 1f + travelFraction * 0.13f
            val capsuleScaleY = 1f - travelFraction * 0.045f

            // Sliding Optical Glass Capsule Indicator (pure GPU graphicsLayer transform — zero layout shifts)
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = capsuleX
                        scaleX = capsuleScaleX
                        scaleY = capsuleScaleY
                    }
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = palette.glow.copy(alpha = if (isLight) 0.30f else 0.48f),
                        spotColor = accent.copy(alpha = if (isLight) 0.32f else 0.55f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLight) {
                                listOf(
                                    Color.White.copy(alpha = 0.96f),
                                    lerp(Color.White, palette.primary, 0.14f).copy(alpha = 0.86f)
                                )
                            } else {
                                listOf(
                                    lerp(Color.White, palette.glow, 0.28f).copy(alpha = 0.24f),
                                    palette.primary.copy(alpha = 0.12f)
                                )
                            }
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        // Subtle moving optical catchlight sheen across the sliding lens
                        val sheenPos = ((capsuleX / (tabWidthPx * count.coerceAtLeast(1))) * 0.5f)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isLight) 0.45f else 0.18f),
                                    Color.White.copy(alpha = if (isLight) 0.12f else 0.04f),
                                    Color.Transparent
                                ),
                                start = Offset(size.width * sheenPos, 0f),
                                end = Offset(size.width * (0.75f + sheenPos * 0.3f), size.height)
                            )
                        )
                    }
                    .border(
                        width = 0.85.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isLight) 0.96f else 0.52f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.89f else 1f,
                        animationSpec = if (reducedMotion) {
                            snap()
                        } else {
                            spring(
                                dampingRatio = 0.62f,
                                stiffness = 540f
                            )
                        },
                        label = "lg_tab_press_${tab.route}"
                    )

                    val tabTransition = updateTransition(
                        targetState = selected,
                        label = "lg_tab_state_${tab.route}"
                    )

                    val iconScale by tabTransition.animateFloat(
                        transitionSpec = {
                            if (reducedMotion) snap()
                            else if (targetState) {
                                spring(
                                    dampingRatio = if (animStyle == PlayerAnimationStyle.FLUID) 0.50f else 0.72f,
                                    stiffness = 380f
                                )
                            } else {
                                tween(durationMillis = 200, easing = FastOutSlowInEasing)
                            }
                        },
                        label = "lg_tab_icon_scale"
                    ) { isSel -> if (isSel) 1.13f else 1f }

                    val iconLiftPx by tabTransition.animateFloat(
                        transitionSpec = {
                            if (reducedMotion) snap()
                            else spring(dampingRatio = 0.68f, stiffness = 360f)
                        },
                        label = "lg_tab_icon_lift"
                    ) { isSel ->
                        if (isSel && showLabels) with(density) { -1.6.dp.toPx() } else 0f
                    }

                    val glowAlpha by tabTransition.animateFloat(
                        transitionSpec = {
                            if (reducedMotion) snap()
                            else tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        },
                        label = "lg_tab_glow_alpha"
                    ) { isSel -> if (isSel) 1f else 0f }

                    val labelAlpha by tabTransition.animateFloat(
                        transitionSpec = {
                            if (reducedMotion) snap()
                            else tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        },
                        label = "lg_tab_label_alpha"
                    ) { isSel -> if (isSel) 1f else 0.72f }

                    val labelOffsetPx by tabTransition.animateFloat(
                        transitionSpec = {
                            if (reducedMotion) snap()
                            else spring(dampingRatio = 0.76f, stiffness = 380f)
                        },
                        label = "lg_tab_label_offset"
                    ) { isSel ->
                        if (isSel) 0f else with(density) { 1.2.dp.toPx() }
                    }

                    val contentTint by tabTransition.animateColor(
                        transitionSpec = {
                            if (reducedMotion) snap()
                            else tween(durationMillis = 230, easing = FastOutSlowInEasing)
                        },
                        label = "lg_tab_tint"
                    ) { isSel ->
                        if (isSel) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .drawBehind {
                                if (glowAlpha > 0.01f) {
                                    val radius = size.minDimension * 0.62f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                accent.copy(alpha = (if (isLight) 0.18f else 0.30f) * glowAlpha),
                                                palette.glow.copy(alpha = (if (isLight) 0.08f else 0.14f) * glowAlpha),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height * if (showLabels) 0.42f else 0.50f),
                                            radius = radius
                                        ),
                                        radius = radius,
                                        center = Offset(size.width / 2f, size.height * if (showLabels) 0.42f else 0.50f)
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    performHaptic()
                                    onSelectTab(tab)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = contentTint,
                                modifier = Modifier
                                    .size(if (showLabels) 21.dp else 23.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        translationY = iconLiftPx
                                    }
                            )
                            if (showLabels) {
                                Spacer(modifier = Modifier.height(2.5.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (navAppearance == NavBarAppearance.COMPACT) 9.5.sp else 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = contentTint,
                                    maxLines = 1,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = labelAlpha
                                        translationY = labelOffsetPx
                                        val lblScale = if (selected) 1.02f else 0.96f
                                        scaleX = lblScale
                                        scaleY = lblScale
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Multi-layered Liquid Glass Album Artwork with realistic colored ambient shadow,
 * subtle optical inner bevel rim, and optional breathing scale when playing.
 */
@Composable
fun LiquidGlassArtwork(
    artworkUri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    shadowElevation: Dp = 28.dp,
    glowTint: Color? = null,
    highRes: Boolean = true,
    fallbackIcon: ImageVector = Icons.Rounded.MusicNote
) {
    val context = LocalContext.current
    val palette = LocalArtworkPalette.current
    val novaColors = LocalNovaColors.current
    val shadowHue = glowTint ?: palette.glow

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = shadowHue.copy(alpha = 0.72f),
                spotColor = palette.primary.copy(alpha = 0.85f)
            )
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = if (novaColors.isLight) {
                        listOf(palette.primary.copy(alpha = 0.24f), Color(0xFFE5EAF3), Color(0xFFD9E0EC))
                    } else {
                        listOf(palette.primary.copy(alpha = 0.34f), Color(0xFF171923), Color(0xFF090A0E))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUri != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .crossfade(350)
                    .apply {
                        if (!highRes) size(256)
                    }
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = palette.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = palette.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(34.dp)
            )
        }

        // Delicate top-left optical glass specular edge catchlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 0.75.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (novaColors.isLight) 0.75f else 0.38f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = shape
                )
        )
    }
}

/**
 * Floating iOS 26 Apple Music–inspired Compact Mini Player sitting smoothly above the Tab Bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassMiniPlayer(
    playerState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong
    val reducedMotion = LocalReducedMotion.current
    val animStyle = LocalPlayerAnimationStyle.current
    val performHaptic = rememberNovaHaptic()

    AnimatedVisibility(
        visible = song != null,
        enter = when {
            reducedMotion -> fadeIn(tween(100))
            animStyle == PlayerAnimationStyle.MINIMAL -> fadeIn(tween(180))
            else -> slideInVertically(
                animationSpec = spring(
                    dampingRatio = if (animStyle == PlayerAnimationStyle.FLUID) 0.78f else 0.88f,
                    stiffness = 360f
                ),
                initialOffsetY = { it }
            ) + fadeIn(tween(240))
        },
        exit = if (reducedMotion) fadeOut(tween(100)) else slideOutVertically(
            animationSpec = tween(220),
            targetOffsetY = { it }
        ) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        if (song != null) {
            val palette = LocalArtworkPalette.current
            val accent = MaterialTheme.colorScheme.primary
            val density = LocalDensity.current
            val rawProgress = if (playerState.durationMs > 0L) {
                (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val smoothProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = if (reducedMotion) snap() else tween(durationMillis = 180, easing = LinearEasing),
                label = "lg_mini_progress"
            )

            // Almost-imperceptible alive floating breath while playing
            val floatTransition = rememberInfiniteTransition(label = "lg_mini_float")
            val breathingRaw by floatTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "lg_mini_float_phase"
            )
            val floatingYPx = if (playerState.isPlaying && !reducedMotion && animStyle != PlayerAnimationStyle.MINIMAL) {
                with(density) { (-1.5f * breathingRaw).dp.toPx() }
            } else {
                0f
            }

            var dragX by remember { mutableFloatStateOf(0f) }
            var dragY by remember { mutableFloatStateOf(0f) }
            val animatedDragX by animateFloatAsState(
                targetValue = dragX * 0.28f,
                animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.75f, stiffness = 420f),
                label = "lg_mini_drag_x"
            )
            val animatedDragY by animateFloatAsState(
                targetValue = (dragY * 0.28f).coerceIn(-26f, 0f),
                animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.75f, stiffness = 420f),
                label = "lg_mini_drag_y"
            )

            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = animatedDragX
                        translationY = animatedDragY + floatingYPx
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .pointerInput(song.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragX < -65f) {
                                    performHaptic()
                                    onNext()
                                } else if (dragX > 65f) {
                                    performHaptic()
                                    onPrevious()
                                }
                                dragX = 0f
                            },
                            onDragCancel = { dragX = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragX = (dragX + dragAmount).coerceIn(-160f, 160f) }
                        )
                    }
                    .pointerInput(song.id) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragY < -50f) {
                                    performHaptic()
                                    onOpenNowPlaying()
                                }
                                dragY = 0f
                            },
                            onDragCancel = { dragY = 0f },
                            onVerticalDrag = { _, dragAmount -> dragY = (dragY + dragAmount).coerceIn(-140f, 30f) }
                        )
                    },
                shape = RoundedCornerShape(24.dp),
                accentTint = palette.primary,
                highlighted = true,
                elevation = 20.dp,
                onClick = {
                    performHaptic()
                    onOpenNowPlaying()
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = song.artworkUri,
                            transitionSpec = {
                                if (reducedMotion) {
                                    fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                } else {
                                    (fadeIn(tween(300, easing = FastOutSlowInEasing)) + scaleIn(
                                        initialScale = 0.88f,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
                                    )) togetherWith (fadeOut(tween(180)) + scaleOut(
                                        targetScale = 0.94f,
                                        animationSpec = tween(180)
                                    ))
                                }
                            },
                            label = "lg_mini_art_crossfade"
                        ) { targetArtwork ->
                            Box(contentAlignment = Alignment.Center) {
                                LiquidGlassArtwork(
                                    artworkUri = targetArtwork,
                                    contentDescription = song.album,
                                    modifier = Modifier.size(46.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    shadowElevation = 10.dp
                                )
                                AnimatedVisibility(
                                    visible = playerState.isPlaying,
                                    enter = fadeIn(tween(180)),
                                    exit = fadeOut(tween(160))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(Color.Black.copy(alpha = 0.36f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedEqualizerBars(isPlaying = true, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        AnimatedContent(
                            targetState = song,
                            transitionSpec = {
                                if (reducedMotion) {
                                    fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                                } else {
                                    (fadeIn(tween(260, easing = FastOutSlowInEasing)) + slideInHorizontally(
                                        tween(260, easing = FastOutSlowInEasing)
                                    ) { it / 8 }) togetherWith
                                        (fadeOut(tween(160)) + slideOutHorizontally(
                                            tween(170, easing = FastOutSlowInEasing)
                                        ) { -it / 8 })
                                }
                            },
                            contentKey = { it.id },
                            label = "lg_mini_track_info",
                            modifier = Modifier.weight(1f)
                        ) { targetSong ->
                            Column {
                                Text(
                                    text = targetSong.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = "${targetSong.artist} • ${FormatUtils.formatDuration(playerState.positionMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val playInteraction = remember { MutableInteractionSource() }
                        val isPlayPressed by playInteraction.collectIsPressedAsState()
                        val playBtnScale by animateFloatAsState(
                            targetValue = if (isPlayPressed) 0.86f else 1f,
                            animationSpec = if (reducedMotion) snap() else spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "lg_mini_play_scale"
                        )
                        val isLightMini = LocalNovaColors.current.isLight
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .graphicsLayer {
                                    scaleX = playBtnScale
                                    scaleY = playBtnScale
                                }
                                .clip(CircleShape)
                                .background(
                                    if (playerState.isPlaying) accent.copy(alpha = 0.24f)
                                    else Color.White.copy(alpha = if (isLightMini) 0.86f else 0.14f)
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (playerState.isPlaying) accent.copy(alpha = 0.65f)
                                    else Color.White.copy(alpha = 0.28f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = playInteraction,
                                    indication = null,
                                    onClick = {
                                        performHaptic()
                                        onTogglePlayPause()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = playerState.isPlaying,
                                transitionSpec = {
                                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.72f, animationSpec = tween(150))) togetherWith
                                        (fadeOut(tween(110)) + scaleOut(targetScale = 0.72f, animationSpec = tween(110)))
                                },
                                label = "lg_mini_play_morph"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = if (playing) accent else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        LiquidGlassIconButton(
                            icon = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            size = 36.dp,
                            iconSize = 20.dp,
                            onClick = onNext
                        )
                    }

                    // Hairline optical progress filament along bottom edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.2.dp)
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(smoothProgress)
                                .height(2.2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(palette.glow, accent)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal Apple Music–style Liquid Glass Seek Bar that expands smoothly under touch.
 */
@Composable
fun LiquidGlassSeekBar(
    progress: Float,
    isScrubbing: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalArtworkPalette.current.glow
) {
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestOnFinished by rememberUpdatedState(onScrubFinished)
    var lastFraction by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    val clampedProgress = progress.coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 10.dp else 5.5.dp,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "lg_seek_height"
    )
    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isScrubbing) 9.5.dp else 5.5.dp,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "lg_seek_thumb_radius"
    )
    val thumbAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0.76f,
        animationSpec = if (reducedMotion) snap() else tween(180),
        label = "lg_seek_thumb_alpha"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 0.36f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(200),
        label = "lg_seek_halo_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
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

            drawRoundRect(
                color = if (novaColors.isLight) Color(0x240F172A) else Color.White.copy(alpha = 0.22f),
                topLeft = Offset(0f, y),
                size = Size(size.width, h),
                cornerRadius = corner
            )

            val fillWidth = (clampedProgress * size.width).coerceAtLeast(h)
            if (clampedProgress > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            accentColor
                        )
                    ),
                    topLeft = Offset(0f, y),
                    size = Size(fillWidth.coerceAtMost(size.width), h),
                    cornerRadius = corner
                )
            }

            val thumbRadius = thumbRadiusDp.toPx()
            val thumbX = (clampedProgress * size.width).coerceIn(thumbRadius, size.width - thumbRadius)
            if (haloAlpha > 0.01f) {
                drawCircle(
                    color = accentColor.copy(alpha = haloAlpha),
                    radius = thumbRadius + 5.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = thumbAlpha),
                radius = thumbRadius,
                center = Offset(thumbX, size.height / 2f)
            )
        }
    }
}

/**
 * Translucent Frosted Optical Liquid Glass Modal Sheet with rounded glass crown and grabber pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val novaColors = LocalNovaColors.current
    val palette = LocalArtworkPalette.current
    val containerTint = if (novaColors.isLight) {
        Color(0xECF4F7FC)
    } else {
        lerp(Color(0xEC11131B), palette.deepTint, 0.42f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = containerTint,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (novaColors.isLight) Color(0x330F172A) else Color.White.copy(alpha = 0.36f)
                    )
            )
        },
        modifier = modifier,
        content = content
    )
}

/**
 * Floating Optical Liquid Glass Transport Controls for NowPlayingScreen.
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
    val palette = LocalArtworkPalette.current
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lg_play_lens_scale"
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
            size = 42.dp,
            iconSize = 20.dp,
            onClick = onToggleShuffle
        )

        LiquidGlassIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            size = 56.dp,
            iconSize = 32.dp,
            onClick = onPrevious
        )

        // Large Floating Optical Liquid Glass Play/Pause Lens
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(playScale)
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = palette.glow.copy(alpha = 0.65f),
                    spotColor = accent.copy(alpha = 0.72f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (novaColors.isLight) {
                            listOf(
                                Color.White.copy(alpha = 0.96f),
                                lerp(Color.White, palette.primary, 0.16f).copy(alpha = 0.88f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                lerp(Color.White, palette.primary, 0.35f).copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (novaColors.isLight) 1f else 0.68f),
                            Color.White.copy(alpha = 0.14f)
                        )
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
                    (fadeIn(tween(160)) + scaleIn(initialScale = 0.72f, animationSpec = tween(160))) togetherWith
                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.72f, animationSpec = tween(120)))
                },
                label = "lg_play_pause_morph"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = if (novaColors.isLight) MaterialTheme.colorScheme.onBackground else Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        LiquidGlassIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next",
            size = 56.dp,
            iconSize = 32.dp,
            onClick = onNext
        )

        LiquidGlassIconButton(
            icon = if (repeatMode == RepeatModeOption.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = "Repeat Mode",
            active = repeatMode != RepeatModeOption.OFF,
            showDotWhenActive = true,
            size = 42.dp,
            iconSize = 20.dp,
            onClick = onCycleRepeat
        )
    }
}

/**
 * Frosted Liquid Glass Search Bar supporting both interactive text entry and launcher mode.
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

    LiquidGlassSurface(
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
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
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
