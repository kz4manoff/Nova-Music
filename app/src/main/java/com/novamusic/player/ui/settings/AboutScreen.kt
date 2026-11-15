package com.novamusic.player.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamusic.player.BuildConfig
import com.novamusic.player.R
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import kotlinx.coroutines.delay

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    bottomContentPadding: Dp
) {
    val palette = LocalArtworkPalette.current
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val dividerColor = novaColors.borderSubtle.copy(alpha = 0.45f)

    var revealStage by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        revealStage = 1
        delay(80)
        revealStage = 2
        delay(80)
        revealStage = 3
    }

    val haloTransition = rememberInfiniteTransition(label = "about_halo")
    val haloScale by haloTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding + 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            LiquidGlassTopBar(
                title = "About",
                subtitle = "NOVA MUSIC • v${BuildConfig.VERSION_NAME}",
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
        }

        // Hero Liquid Glass App Identity Card
        item {
            AnimatedVisibility(
                visible = revealStage >= 1,
                enter = fadeIn(tween(420)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    highlighted = true,
                    tint = palette.dominant.copy(alpha = 0.14f),
                    elevation = 20.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(116.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Breathing optical aura
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .graphicsLayer {
                                        scaleX = haloScale
                                        scaleY = haloScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                accent.copy(alpha = haloAlpha),
                                                palette.glow.copy(alpha = haloAlpha * 0.55f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Elevated frosted glass emblem lens
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .shadow(
                                        elevation = 18.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        ambientColor = accent,
                                        spotColor = palette.glow
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                novaColors.glassFillTop.copy(alpha = 0.92f),
                                                novaColors.glassFillBottom.copy(alpha = 0.86f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 0.9.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                novaColors.glassBorderTop,
                                                novaColors.glassBorderBottom
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_nova_logo),
                                    contentDescription = "NOVA MUSIC Logo",
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "NOVA MUSIC",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Audiophile Offline Music Player",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LiquidGlassSurface(
                            shape = CircleShape,
                            elevation = 4.dp,
                            tint = accent.copy(alpha = 0.14f),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "VERSION ${BuildConfig.VERSION_NAME} • BUILD ${BuildConfig.VERSION_CODE}",
                                style = NovaMonoTelemetry.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.9.sp
                                ),
                                color = accent
                            )
                        }
                    }
                }
            }
        }

        // Developer Identity Spotlight Card
        item {
            AnimatedVisibility(
                visible = revealStage >= 2,
                enter = fadeIn(tween(420)) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    tint = palette.secondary.copy(alpha = 0.10f),
                    elevation = 14.dp,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "DEVELOPER & CREATOR",
                                style = NovaMonoTelemetry.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp
                                ),
                                color = accent
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Created & Developed by kz4man, Mohamed",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Engineered for bit-accurate offline playback, real hardware DSP audio shaping, and a fluid iOS 26 Liquid Glass interface.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        AboutMetaRow(label = "Author", value = "kz4man, Mohamed")
                        AboutMetaRow(label = "Architecture", value = "Kotlin • Jetpack Compose")
                        AboutMetaRow(label = "Platform", value = "Android 8.0+ (API 26–35)")
                    }
                }
            }
        }

        // Core Engineering Highlights
        item {
            AnimatedVisibility(
                visible = revealStage >= 3,
                enter = fadeIn(tween(460)) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    elevation = 12.dp,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "CRAFT & CAPABILITIES",
                            style = NovaMonoTelemetry.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            ),
                            color = accent
                        )

                        AboutFeatureRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Media3 ExoPlayer & Hardware DSP",
                            description = "10-Band parametric equalizer, bass boost, spatial virtualizer, loudness enhancer, and preamp."
                        )
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        AboutFeatureRow(
                            icon = Icons.Rounded.Layers,
                            title = "iOS 26 Liquid Glass Material",
                            description = "Multi-layer optical refraction, dynamic artwork color extraction, and spring motion physics."
                        )
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        AboutFeatureRow(
                            icon = Icons.Rounded.HighQuality,
                            title = "Hi-Res Local Audio Formats",
                            description = "Native support for FLAC, WAV, MP3, AAC, M4A, OGG, and OPUS with synchronized lyrics."
                        )
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                        AboutFeatureRow(
                            icon = Icons.Rounded.OfflineBolt,
                            title = "100% Private & Offline",
                            description = "Zero telemetry, zero cloud tracking. Your library and playlists stay strictly on your device."
                        )
                    }
                }
            }
        }

        // Bottom Copyright Notice
        item {
            AnimatedVisibility(
                visible = revealStage >= 3,
                enter = fadeIn(tween(520))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.72f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2026 kz4man, Mohamed. All Rights Reserved.",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = NovaMonoTelemetry.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AboutFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    val novaColors = LocalNovaColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(novaColors.recessedSurface.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
