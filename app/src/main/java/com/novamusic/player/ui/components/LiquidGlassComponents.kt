package com.novamusic.player.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novamusic.player.data.model.AudioOutputDevice
import com.novamusic.player.data.model.NavBarAppearance
import com.novamusic.player.data.model.PlaybackMode
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.PlayerUiState
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.utils.FormatUtils
import kotlinx.coroutines.delay
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
    val icon: ImageVector,
    val hasBadge: Boolean = false
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
    } else if (novaColors.isAmoled) {
        lerp(Color.Black, palette.deepTint, 0.22f)
    } else {
        lerp(MaterialTheme.colorScheme.background, palette.deepTint, 0.36f)
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
                            (if (isLight) 0.34f else if (novaColors.isAmoled) 0.36f else 0.48f) * intensity.coerceIn(0.65f, 1.35f)
                        } else {
                            if (isLight) 0.16f else 0.18f
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

        // Atmospheric readability gradient veil anchored in pure #000000 black
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
                        } else if (novaColors.isAmoled) {
                            listOf(
                                Color(0x8A000000),
                                Color(0xBA000000),
                                Color(0xF5000000)
                            )
                        } else {
                            listOf(
                                Color(0x66040508),
                                Color(0x94040508),
                                Color(0xE6020204)
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
    opaqueSurface: Boolean = false,
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

    val baseBrush = if (opaqueSurface) {
        val solidBase = lerp(
            novaColors.elevatedSurface,
            palette.deepTint,
            if (isLight) 0.04f else 0.16f
        ).copy(alpha = 0.98f)
        val topTint = if (highlighted) {
            lerp(solidBase, accent, if (isLight) 0.10f else 0.14f)
        } else {
            lerp(solidBase, Color.White, if (isLight) 0.45f else 0.06f)
        }
        val bottomTint = if (isLight) {
            lerp(solidBase, Color(0xFFE9EEF7), 0.35f)
        } else {
            lerp(solidBase, Color.Black, 0.22f)
        }
        Brush.verticalGradient(colors = listOf(topTint, solidBase, bottomTint))
    } else {
        Brush.verticalGradient(
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
    }

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
 * Floating iOS Liquid Glass Frosted Bar container shared between the Mini-Player Pill
 * and Bottom Navigation Bar with authentic dark translucency, inner optical blur shimmer,
 * specular edge reflections, and thin semi-transparent white border.
 */
@Composable
private fun FloatingFrostedGlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape,
    elevation: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val novaColors = LocalNovaColors.current
    val isLight = novaColors.isLight
    val supportsHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurRadiusPx = with(LocalDensity.current) { 20.dp.toPx() }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.978f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fg_glass_scale"
    )

    val glassFill = if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xE8FFFFFF),
                Color(0xDCF3F6FC),
                Color(0xE8EAEFF8)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xD91A1C24),
                Color(0xE30E1016),
                Color(0xEE08090D)
            )
        )
    }

    val borderBrush = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                Color(0x330F172A),
                Color.White.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.22f)
            )
        )
    }

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
                ambientColor = if (isLight) Color(0x280F172A) else Color.Black.copy(alpha = 0.88f),
                spotColor = if (isLight) Color(0x2E0F172A) else Color.Black.copy(alpha = 0.94f)
            )
            .clip(shape)
            .background(glassFill)
            .then(clickableModifier)
    ) {
        // Subtle blurred internal ambient reflection layer for realistic frosted depth
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (supportsHardwareBlur) {
                        renderEffect = RenderEffect
                            .createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isLight) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.22f, size.height * 0.10f),
                            radius = size.width * 0.55f
                        ),
                        radius = size.width * 0.55f,
                        center = Offset(size.width * 0.22f, size.height * 0.10f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isLight) Color.White.copy(alpha = 0.30f) else Color(0xFF94A3B8).copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.82f, size.height * 0.85f),
                            radius = size.width * 0.45f
                        ),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.82f, size.height * 0.85f)
                    )
                }
        )

        // Optical specular top-edge sheen & inner depth shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isLight) 0.52f else 0.13f),
                                Color.White.copy(alpha = if (isLight) 0.14f else 0.03f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.48f
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (isLight) 0.04f else 0.24f)
                            ),
                            startY = size.height * 0.68f,
                            endY = size.height
                        )
                    )
                }
                .border(width = 0.85.dp, brush = borderBrush, shape = shape)
        )

        content()
    }
}

/**
 * Floating iOS Liquid Glass Rounded Rectangular Bottom Navigation Bar featuring:
 * - Five evenly spaced minimalist white icons with no text labels
 * - Larger horizontal rounded-rectangle (pill-shaped) dark glass active indicator with thin white border
 * - Smooth spring-driven sliding transition and continuous horizontal finger-tracking swipe/tap support
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<LiquidGlassNavTab>,
    currentRoute: String,
    onSelectTab: (LiquidGlassNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val novaColors = LocalNovaColors.current
    val isLight = novaColors.isLight
    val reducedMotion = LocalReducedMotion.current
    val animStyle = LocalPlayerAnimationStyle.current
    val navBarAppearance = LocalNavBarAppearance.current
    val performHaptic = rememberNovaHaptic()
    val density = LocalDensity.current
    val latestTabs by rememberUpdatedState(tabs)
    val latestOnSelectTab by rememberUpdatedState(onSelectTab)

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

    var dragSlot by remember { mutableStateOf<Float?>(null) }
    var dragVelocityX by remember { mutableFloatStateOf(0f) }

    val indicatorSlot by animateFloatAsState(
        targetValue = dragSlot ?: selectedIndex.toFloat(),
        animationSpec = when {
            dragSlot != null || reducedMotion -> snap()
            animStyle == PlayerAnimationStyle.MINIMAL -> tween(180, easing = FastOutSlowInEasing)
            else -> spring(dampingRatio = 0.78f, stiffness = 380f)
        },
        label = "lg_pill_indicator_slot"
    )

    val animatedVelocityX by animateFloatAsState(
        targetValue = dragVelocityX,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.76f, stiffness = 460f),
        label = "lg_pill_vel_stretch"
    )

    val barHeight = when (navBarAppearance) {
        NavBarAppearance.FLOATING_GLASS -> 56.dp
        NavBarAppearance.COMPACT -> 52.dp
        NavBarAppearance.MINIMAL_PILL -> 50.dp
    }
    val barShape = RoundedCornerShape(26.dp)
    val horizontalInset = 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset)
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingFrostedGlassContainer(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            shape = barShape,
            elevation = 22.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val count = tabs.size.coerceAtLeast(1)
                val tabWidth = maxWidth / count
                val tabWidthPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
                val innerPadPx = with(density) { 6.dp.toPx() }
                val indicatorHeight = minOf(38.dp, (maxHeight - 4.dp).coerceAtLeast(32.dp))
                val indicatorWidth = (tabWidth - 2.dp).coerceAtLeast(48.dp)
                val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
                val indicatorShape = RoundedCornerShape(20.dp)

                val isDragging = dragSlot != null
                val travelDelta = if (reducedMotion || animStyle == PlayerAnimationStyle.MINIMAL) {
                    0f
                } else {
                    val settleDist = if (isDragging) 0f else abs(selectedIndex.toFloat() - indicatorSlot)
                    val velStretch = (abs(animatedVelocityX) / 1700f).coerceIn(0f, 0.75f)
                    (settleDist * 0.35f + velStretch * 0.45f).coerceIn(0f, 0.75f)
                }
                val highlightScaleX = 1f + travelDelta * 0.09f
                val highlightScaleY = 1f - travelDelta * 0.04f
                val indicatorX = indicatorSlot * tabWidthPx + (tabWidthPx - indicatorWidthPx) / 2f

                // Horizontal pill-shaped dark glass active indicator smoothly sliding behind icons
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(indicatorWidth)
                        .height(indicatorHeight)
                        .graphicsLayer {
                            translationX = indicatorX
                            scaleX = highlightScaleX
                            scaleY = highlightScaleY
                        }
                        .clip(indicatorShape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isLight) {
                                    listOf(
                                        Color.White.copy(alpha = 0.96f),
                                        Color.White.copy(alpha = 0.76f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xCC252834),
                                        Color(0xD915171F),
                                        Color(0xE60D0F14)
                                    )
                                }
                            )
                        )
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isLight) 0.40f else 0.10f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.45f
                                )
                            )
                        }
                        .border(
                            width = 0.85.dp,
                            brush = Brush.linearGradient(
                                colors = if (isLight) {
                                    listOf(Color.White, Color(0x280F172A))
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.36f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.22f)
                                    )
                                }
                            ),
                            shape = indicatorShape
                        )
                )

                // Gesture overlay handling both tap and continuous horizontal finger-following swipe
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(count, tabWidthPx) {
                            val touchSlop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                val velocityTracker = VelocityTracker()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)
                                var isHorizontalTracking = false
                                var lastHoveredIdx = nearestSlotIndex(
                                    clampFingerSlot(down.position.x, size.width.toFloat(), innerPadPx, count),
                                    count
                                )
                                var pointerId = down.id

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                        ?: event.changes.firstOrNull { it.pressed }
                                        ?: break
                                    pointerId = change.id

                                    if (change.isConsumed && !isHorizontalTracking) {
                                        dragSlot = null
                                        dragVelocityX = 0f
                                        return@awaitEachGesture
                                    }

                                    if (!change.pressed) {
                                        val finalTabs = latestTabs
                                        if (isHorizontalTracking) {
                                            val activeSlot = dragSlot ?: clampFingerSlot(
                                                change.position.x,
                                                size.width.toFloat(),
                                                innerPadPx,
                                                finalTabs.size
                                            )
                                            val snappedIdx = nearestSlotIndex(activeSlot, finalTabs.size)
                                            dragSlot = null
                                            dragVelocityX = 0f
                                            if (finalTabs.isNotEmpty()) {
                                                performHaptic()
                                                latestOnSelectTab(finalTabs[snappedIdx])
                                            }
                                        } else {
                                            val tappedSlot = clampFingerSlot(
                                                down.position.x,
                                                size.width.toFloat(),
                                                innerPadPx,
                                                finalTabs.size
                                            )
                                            val tappedIdx = nearestSlotIndex(tappedSlot, finalTabs.size)
                                            dragSlot = null
                                            dragVelocityX = 0f
                                            if (finalTabs.isNotEmpty()) {
                                                performHaptic()
                                                latestOnSelectTab(finalTabs[tappedIdx])
                                            }
                                        }
                                        return@awaitEachGesture
                                    }

                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    val totalDx = change.position.x - down.position.x
                                    val totalDy = change.position.y - down.position.y

                                    if (!isHorizontalTracking) {
                                        if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 1.25f) {
                                            dragSlot = null
                                            dragVelocityX = 0f
                                            return@awaitEachGesture
                                        }
                                        if (abs(totalDx) > touchSlop * 0.55f && abs(totalDx) > abs(totalDy) * 1.1f) {
                                            isHorizontalTracking = true
                                            change.consume()
                                        }
                                    }

                                    if (isHorizontalTracking) {
                                        change.consume()
                                        val rawSlot = clampFingerSlot(
                                            pointerX = change.position.x,
                                            containerWidthPx = size.width.toFloat(),
                                            horizontalPaddingPx = innerPadPx,
                                            tabCount = count
                                        )
                                        dragSlot = applyMagneticSlotPull(rawSlot)
                                        val instVelX = velocityTracker.calculateVelocity().x
                                        dragVelocityX = dragVelocityX * 0.55f + instVelX * 0.45f

                                        val hoveredNow = nearestSlotIndex(rawSlot, count)
                                        if (hoveredNow != lastHoveredIdx) {
                                            lastHoveredIdx = hoveredNow
                                            performHaptic()
                                        }
                                    }
                                }

                                dragSlot = null
                                dragVelocityX = 0f
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val proximity = iconProximityWeight(indicatorSlot, index)
                        val isSelected = index == selectedIndex
                        val iconScale = if (reducedMotion) {
                            if (isSelected) 1.06f else 1f
                        } else {
                            1f + proximity * 0.08f
                        }
                        val baseForeground = if (isLight) Color(0xFF0E121B) else Color.White
                        val iconTint = baseForeground.copy(alpha = 0.62f + proximity * 0.38f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                if (tab.hasBadge) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .border(0.8.dp, Color.Black.copy(alpha = 0.65f), CircleShape)
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
 * Floating iOS Liquid Glass Horizontal Pill Mini-Player sitting smoothly above the Bottom Navigation Bar:
 * - Capsule pill shape with dark frosted glass, backdrop blur depth, subtle white border & glass reflections
 * - Track artwork thumbnail & track title/artist on the left
 * - Minimalist white Play/Pause and Next track buttons aligned to the right
 * - Preserves tap-to-open Now Playing and directional swipe gestures
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassMiniPlayer(
    playerState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissMiniPlayer: (() -> Unit)? = null,
    activeOutputDevice: AudioOutputDevice? = null,
    onOpenAudioOutput: (() -> Unit)? = null
) {
    val song = playerState.currentSong
    val reducedMotion = LocalReducedMotion.current
    val animStyle = LocalPlayerAnimationStyle.current
    val performHaptic = rememberNovaHaptic()
    val gestureConfig = DefaultNovaGestureConfig

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
            val isLight = LocalNovaColors.current.isLight
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

            // Subtle breathing lift while playing
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
                with(density) { (-1.2f * breathingRaw).dp.toPx() }
            } else {
                0f
            }

            val miniMaxPullUpPx = with(density) { 80.dp.toPx() }
            val miniMaxPullDownPx = with(density) { 96.dp.toPx() }
            val miniExpandThresholdPx = with(density) { gestureConfig.miniPlayerExpandDistanceDp.toPx() }
            val miniDismissThresholdPx = with(density) { gestureConfig.miniPlayerDismissDistanceDp.toPx() }
            val miniSwipeHorizThresholdPx = with(density) { 54.dp.toPx() }
            val miniHorizVelPx = with(density) { gestureConfig.horizontalSwipeVelocityDpPerSec.dp.toPx() }
            val miniVertVelPx = with(density) { gestureConfig.miniPlayerSwipeVelocityDpPerSec.dp.toPx() }

            var dragX by remember(song.id) { mutableFloatStateOf(0f) }
            var dragY by remember(song.id) { mutableFloatStateOf(0f) }
            val animatedDragX by animateFloatAsState(
                targetValue = dragX * 0.34f,
                animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.76f, stiffness = 420f),
                label = "lg_mini_drag_x"
            )
            val animatedDragY by animateFloatAsState(
                targetValue = dragY * 0.42f,
                animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.76f, stiffness = 420f),
                label = "lg_mini_drag_y"
            )
            val verticalDragFraction = if (animatedDragY > 0f) {
                (animatedDragY / (miniMaxPullDownPx * 0.42f)).coerceIn(0f, 1f)
            } else {
                0f
            }
            val expandLiftFraction = if (animatedDragY < 0f) {
                (-animatedDragY / (miniMaxPullUpPx * 0.42f)).coerceIn(0f, 1f)
            } else {
                0f
            }
            val miniGestureScale = if (reducedMotion) 1f else {
                1f + expandLiftFraction * 0.022f - verticalDragFraction * 0.045f
            }
            val miniGestureAlpha = (1f - verticalDragFraction * 0.45f).coerceIn(0.45f, 1f)

            val pillShape = RoundedCornerShape(50)
            val primaryInk = if (isLight) Color(0xFF0E121B) else Color.White
            val secondaryInk = if (isLight) Color(0xFF525A6B) else Color.White.copy(alpha = 0.68f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingFrostedGlassContainer(
                    modifier = Modifier
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = animatedDragX
                            translationY = animatedDragY + floatingYPx
                            scaleX = miniGestureScale
                            scaleY = miniGestureScale
                            alpha = miniGestureAlpha
                        }
                        .novaDirectionalDrag(
                            key = song.id,
                            enableHorizontal = true,
                            enableVertical = true,
                            edgeExclusionDp = gestureConfig.edgeExclusionDp,
                            directionLockRatio = gestureConfig.directionLockRatio,
                            onHorizontalDrag = { deltaX ->
                                dragX = (dragX + deltaX).coerceIn(-miniSwipeHorizThresholdPx * 2.2f, miniSwipeHorizThresholdPx * 2.2f)
                            },
                            onHorizontalEnd = { velocityX ->
                                when (resolveHorizontalTrackAction(dragX, velocityX, miniSwipeHorizThresholdPx, miniHorizVelPx)) {
                                    SwipeDirection.NEXT -> {
                                        performHaptic()
                                        onNext()
                                    }
                                    SwipeDirection.PREVIOUS -> {
                                        performHaptic()
                                        onPrevious()
                                    }
                                    SwipeDirection.NONE -> Unit
                                }
                                dragX = 0f
                            },
                            onHorizontalCancel = { dragX = 0f },
                            onVerticalDrag = { deltaY ->
                                val maxDown = if (onDismissMiniPlayer != null) miniMaxPullDownPx else 24f
                                dragY = (dragY + deltaY).coerceIn(-miniMaxPullUpPx, maxDown)
                            },
                            onVerticalEnd = { velocityY ->
                                when {
                                    shouldTriggerMiniPlayerExpand(dragY, velocityY, miniExpandThresholdPx, miniVertVelPx) -> {
                                        performHaptic()
                                        onOpenNowPlaying()
                                    }
                                    onDismissMiniPlayer != null &&
                                        shouldTriggerVerticalDismiss(dragY, velocityY, miniDismissThresholdPx, miniVertVelPx) -> {
                                        performHaptic()
                                        onDismissMiniPlayer()
                                    }
                                }
                                dragY = 0f
                            },
                            onVerticalCancel = { dragY = 0f }
                        ),
                    shape = pillShape,
                    elevation = 20.dp,
                    onClick = {
                        performHaptic()
                        onOpenNowPlaying()
                    }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
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
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        shadowElevation = 8.dp
                                    )
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = playerState.isPlaying,
                                        enter = fadeIn(tween(180)),
                                        exit = fadeOut(tween(160))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.36f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedEqualizerBars(isPlaying = true, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(11.dp))

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
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = targetSong.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = primaryInk,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "${targetSong.artist} • ${FormatUtils.formatDuration(playerState.positionMs)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = secondaryInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right-aligned minimalist Play/Pause pill button
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer {
                                        scaleX = playBtnScale
                                        scaleY = playBtnScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (isLight) {
                                            Color.White.copy(alpha = if (playerState.isPlaying) 0.96f else 0.82f)
                                        } else {
                                            Color.White.copy(alpha = if (playerState.isPlaying) 0.20f else 0.12f)
                                        }
                                    )
                                    .border(
                                        width = 0.75.dp,
                                        color = if (isLight) {
                                            Color(0x280F172A)
                                        } else {
                                            Color.White.copy(alpha = if (playerState.isPlaying) 0.38f else 0.24f)
                                        },
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
                                        tint = primaryInk,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Right-aligned Next Track button beside Play/Pause
                            val nextInteraction = remember { MutableInteractionSource() }
                            val isNextPressed by nextInteraction.collectIsPressedAsState()
                            val nextBtnScale by animateFloatAsState(
                                targetValue = if (isNextPressed) 0.86f else 1f,
                                animationSpec = if (reducedMotion) snap() else spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "lg_mini_next_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = nextBtnScale
                                        scaleY = nextBtnScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (isLight) Color.White.copy(alpha = 0.76f) else Color.White.copy(alpha = 0.10f)
                                    )
                                    .border(
                                        width = 0.65.dp,
                                        color = if (isLight) Color(0x200F172A) else Color.White.copy(alpha = 0.20f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = nextInteraction,
                                        indication = null,
                                        onClick = {
                                            performHaptic()
                                            onNext()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = primaryInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Subtle inset progress filament along the bottom of the pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(1.8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isLight) Color(0x180F172A) else Color.White.copy(alpha = 0.12f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(smoothProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isLight) Color(0xCC0E121B) else Color.White.copy(alpha = 0.85f)
                                    )
                            )
                        }
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
        Color(0xF8F5F7FC)
    } else {
        lerp(Color(0xF812141E), palette.deepTint, 0.28f)
    }
    val scrimTint = Color.Black.copy(alpha = if (novaColors.isLight) 0.42f else 0.62f)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = containerTint,
        scrimColor = scrimTint,
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
 * Unified Optical Liquid Glass Playback Mode Button cycling:
 * Sequential -> Shuffle -> Repeat All -> Repeat One -> Smart Shuffle -> Sequential.
 */
@Composable
fun LiquidGlassPlaybackModeButton(
    playbackMode: PlaybackMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val isLight = novaColors.isLight
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val isActive = playbackMode.isActiveMode

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lg_mode_scale"
    )
    val activeFraction by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(220, easing = FastOutSlowInEasing),
        label = "lg_mode_active"
    )
    val resolvedTint by animateColorAsState(
        targetValue = when {
            playbackMode == PlaybackMode.SMART_SHUFFLE -> lerp(accent, Color.White, 0.18f)
            isActive -> accent
            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
        },
        animationSpec = if (reducedMotion) snap() else tween(200),
        label = "lg_mode_tint"
    )

    var showModeLabel by remember { mutableStateOf(false) }
    var lastObservedMode by remember { mutableStateOf(playbackMode) }
    LaunchedEffect(playbackMode) {
        if (lastObservedMode != playbackMode) {
            lastObservedMode = playbackMode
            showModeLabel = true
            delay(1450L)
            showModeLabel = false
        }
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            lerp(
                Color.White.copy(alpha = if (isLight) 0.90f else 0.16f),
                accent.copy(alpha = if (playbackMode == PlaybackMode.SMART_SHUFFLE) 0.42f else 0.34f),
                activeFraction
            ),
            lerp(
                novaColors.glassSurface.copy(alpha = if (isLight) 0.66f else 0.24f),
                accent.copy(alpha = 0.16f),
                activeFraction
            )
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            lerp(novaColors.glassBorderTop, accent.copy(alpha = 0.82f), activeFraction),
            lerp(novaColors.glassBorderBottom, accent.copy(alpha = 0.24f), activeFraction)
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (isActive) 12.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = if (isActive) accent.copy(alpha = 0.58f) else novaColors.glassShadow,
                    spotColor = if (isActive) accent.copy(alpha = 0.58f) else novaColors.glassShadow
                )
                .clip(CircleShape)
                .background(bgBrush)
                .border(0.75.dp, borderBrush, CircleShape)
                .semantics {
                    stateDescription = playbackMode.label
                }
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
            AnimatedContent(
                targetState = playbackMode,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.72f, animationSpec = tween(180))) togetherWith
                            (fadeOut(tween(120)) + scaleOut(targetScale = 0.72f, animationSpec = tween(120)))
                    }
                },
                label = "lg_playback_mode_icon"
            ) { mode ->
                when (mode) {
                    PlaybackMode.SEQUENTIAL -> Icon(
                        imageVector = Icons.Rounded.PlaylistPlay,
                        contentDescription = "Playback mode: Sequential",
                        tint = resolvedTint,
                        modifier = Modifier.size(22.dp)
                    )

                    PlaybackMode.SHUFFLE -> Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Playback mode: Shuffle",
                        tint = resolvedTint,
                        modifier = Modifier.size(20.dp)
                    )

                    PlaybackMode.REPEAT_ALL -> Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Playback mode: Repeat All",
                        tint = resolvedTint,
                        modifier = Modifier.size(20.dp)
                    )

                    PlaybackMode.REPEAT_ONE -> Icon(
                        imageVector = Icons.Rounded.RepeatOne,
                        contentDescription = "Playback mode: Repeat One",
                        tint = resolvedTint,
                        modifier = Modifier.size(20.dp)
                    )

                    PlaybackMode.SMART_SHUFFLE -> Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Playback mode: Smart Shuffle",
                            tint = resolvedTint,
                            modifier = Modifier
                                .size(19.dp)
                                .offset(x = (-1).dp, y = 1.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = lerp(accent, Color.White, 0.42f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(11.dp)
                        )
                    }
                }
            }

            if (activeFraction > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
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

        // Transient floating Liquid Glass mode badge when cycling modes (non-displacing overlay)
        AnimatedVisibility(
            visible = showModeLabel,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.88f, animationSpec = tween(150)),
            exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.92f, animationSpec = tween(220)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = (-34).dp)
                .wrapContentSize(align = Alignment.BottomStart, unbounded = true)
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(12.dp),
                highlighted = playbackMode.isActiveMode,
                elevation = 8.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = playbackMode.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (playbackMode.isActiveMode) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Floating Optical Liquid Glass Transport Controls for NowPlayingScreen.
 */
@Composable
fun LiquidGlassPlayerControls(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            LiquidGlassPlaybackModeButton(
                playbackMode = playbackMode,
                onClick = onCyclePlaybackMode
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
        }

        Spacer(modifier = Modifier.weight(1f))
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
