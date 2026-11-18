package com.novamusic.player.ui.equalizer

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novamusic.player.data.model.EqualizerBandInfo
import com.novamusic.player.ui.components.LiquidGlassButton
import com.novamusic.player.ui.components.LiquidGlassIconButton
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LiquidGlassTopBar
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry
import com.novamusic.player.ui.theme.NovaRedPrimary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onBack: (() -> Unit)? = null,
    bottomContentPadding: Dp
) {
    val state by viewModel.eqState.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    var showSavePresetDialog by remember { mutableStateOf(false) }

    val palette = LocalArtworkPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Floating iOS Top Bar
        item {
            LiquidGlassTopBar(
                title = "Equalizer",
                subtitle = if (state.audioSessionId > 0) {
                    "${state.engineDescription} • Session #${state.audioSessionId}"
                } else {
                    "Play a track to attach audio session"
                },
                leadingAction = if (onBack != null) {
                    {
                        LiquidGlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            size = 40.dp,
                            iconSize = 20.dp,
                            onClick = onBack
                        )
                    }
                } else null,
                trailingActions = {
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.BookmarkAdd,
                        contentDescription = "Save Custom Preset",
                        size = 38.dp,
                        iconSize = 18.dp,
                        onClick = { showSavePresetDialog = true }
                    )
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset Equalizer",
                        size = 38.dp,
                        iconSize = 18.dp,
                        onClick = viewModel::resetEqualizer
                    )
                    LiquidGlassButton(
                        onClick = { viewModel.setEnabled(!state.isEnabled) },
                        highlighted = state.isEnabled,
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = if (state.isEnabled) "Disable EQ" else "Enable EQ",
                                tint = if (state.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (state.isEnabled) "ON" else "OFF",
                                style = NovaMonoTelemetry.copy(fontWeight = FontWeight.Bold),
                                color = if (state.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        // Glass Presets Selector
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        state.isEnabled && state.audioSessionId > 0 -> NovaRedPrimary
                                        state.isSupported -> accent
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                        )
                        AnimatedContent(
                            targetState = state.activePresetName.uppercase(),
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                            label = "eq_preset_badge"
                        ) { presetTitle ->
                            Text(
                                text = presetTitle,
                                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                                color = accent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.builtInPresetNames) { presetName ->
                        val isSelected = state.activePresetName == presetName
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            highlighted = isSelected,
                            elevation = if (isSelected) 12.dp else 4.dp,
                            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 9.dp),
                            onClick = { viewModel.applyBuiltInPreset(presetName) }
                        ) {
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    items(customPresets, key = { "custom_${it.id}" }) { custom ->
                        val isSelected = state.activePresetName == custom.name
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            highlighted = isSelected,
                            elevation = if (isSelected) 12.dp else 4.dp,
                            contentPadding = PaddingValues(start = 14.dp, end = 9.dp, top = 9.dp, bottom = 9.dp),
                            onClick = { viewModel.applyCustomPreset(custom) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = custom.name,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete Preset",
                                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.deleteCustomPreset(custom.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Translucent Optical Glass Frequency Response + Floating Faders
        item {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(30.dp),
                elevation = 18.dp,
                contentPadding = PaddingValues(vertical = 18.dp, horizontal = 14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${state.bands.size}-Band Audio Response",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        AnimatedLevelVisualizer(
                            enabled = state.isEnabled && state.audioSessionId > 0,
                            bandCount = 6,
                            accentColor = palette.glow
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FrequencyResponseCurve(
                        bands = state.bands,
                        minMb = state.minLevelMillibels,
                        maxMb = state.maxLevelMillibels,
                        enabled = state.isEnabled,
                        accentColor = accent,
                        glowColor = palette.glow,
                        gridColor = novaColors.borderSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(98.dp)
                            .padding(horizontal = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.bands.forEach { band ->
                            VerticalEqBandFader(
                                band = band,
                                minMb = state.minLevelMillibels,
                                maxMb = state.maxLevelMillibels,
                                enabled = state.isEnabled,
                                accentColor = accent,
                                glowColor = palette.glow,
                                onValueChange = { newMb -> viewModel.setBandLevel(band.index, newMb) }
                            )
                        }
                    }
                }
            }
        }

        // Translucent Optical Glass Tone & Spatial FX Dials
        item {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(30.dp),
                elevation = 18.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tone & Spatial FX",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "DRAG VERTICALLY ↕",
                            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RotaryAudioKnob(
                            title = "PREAMP",
                            value = (state.preampGainDb + 12f) / 24f,
                            valueText = if (state.preampSupported) {
                                String.format(Locale.US, "%+.1fdB", state.preampGainDb)
                            } else "N/A",
                            enabled = state.isEnabled && state.preampSupported,
                            bipolar = true,
                            accentColor = accent,
                            onValueChange = { frac ->
                                viewModel.setPreampGain(-12f + frac * 24f)
                            }
                        )

                        RotaryAudioKnob(
                            title = "BASS",
                            value = state.bassBoostStrength / 1000f,
                            valueText = if (state.bassBoostSupported) "${state.bassBoostStrength / 10}%" else "N/A",
                            enabled = state.isEnabled && state.bassBoostSupported,
                            bipolar = false,
                            accentColor = accent,
                            onValueChange = { frac ->
                                viewModel.setBassBoost((frac * 1000f).roundToInt())
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RotaryAudioKnob(
                            title = "STEREO",
                            value = state.virtualizerStrength / 1000f,
                            valueText = if (state.virtualizerSupported) "${state.virtualizerStrength / 10}%" else "N/A",
                            enabled = state.isEnabled && state.virtualizerSupported,
                            bipolar = false,
                            accentColor = accent,
                            onValueChange = { frac ->
                                viewModel.setVirtualizer((frac * 1000f).roundToInt())
                            }
                        )

                        RotaryAudioKnob(
                            title = "LOUDNESS",
                            value = state.loudnessGainMillibels / 1500f,
                            valueText = if (state.loudnessSupported) {
                                "+${String.format(Locale.US, "%.1fdB", state.loudnessGainMillibels / 100f)}"
                            } else "N/A",
                            enabled = state.isEnabled && state.loudnessSupported,
                            bipolar = false,
                            accentColor = accent,
                            onValueChange = { frac ->
                                viewModel.setLoudnessGain((frac * 1500f).roundToInt())
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = novaColors.elevatedSurface,
            title = { Text("Save Custom Preset", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            viewModel.saveCustomPreset(presetName.trim())
                            showSavePresetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AnimatedLevelVisualizer(
    enabled: Boolean,
    bandCount: Int,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "eq_level_viz")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_viz_phase"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LocalNovaColors.current.recessedSurface)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until bandCount) {
            val wave = if (enabled) {
                0.3f + 0.7f * abs(sin(phase + i * 0.75f))
            } else {
                0.24f
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14f * wave).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun FrequencyResponseCurve(
    bands: List<EqualizerBandInfo>,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    accentColor: Color,
    glowColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val activeColor by animateColorAsState(
        targetValue = if (enabled) accentColor else Color(0xFF6B6B6B),
        animationSpec = if (reducedMotion) snap() else tween(260),
        label = "eq_curve_color"
    )
    val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
    val animatedNorms = bands.map { band ->
        val norm by animateFloatAsState(
            targetValue = ((band.levelMillibels - minMb) / range).coerceIn(0f, 1f),
            animationSpec = if (reducedMotion) snap() else spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "eq_curve_band_${band.index}"
        )
        norm
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(novaColors.recessedSurface)
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(listOf(novaColors.glassBorderTop, novaColors.glassBorderBottom)),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "+${maxMb / 100}",
                style = NovaMonoTelemetry.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "0dB",
                style = NovaMonoTelemetry.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "${minMb / 100}",
                style = NovaMonoTelemetry.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padStart = 30.dp.toPx()
            val padEnd = 14.dp.toPx()
            val padY = 12.dp.toPx()
            val usableW = (w - padStart - padEnd).coerceAtLeast(1f)
            val usableH = (h - padY * 2).coerceAtLeast(1f)
            val midY = padY + usableH / 2f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            drawLine(
                color = gridColor.copy(alpha = 0.55f),
                start = Offset(padStart, midY),
                end = Offset(w - padEnd, midY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor.copy(alpha = 0.30f),
                start = Offset(padStart, padY),
                end = Offset(w - padEnd, padY),
                strokeWidth = 0.8.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = gridColor.copy(alpha = 0.30f),
                start = Offset(padStart, h - padY),
                end = Offset(w - padEnd, h - padY),
                strokeWidth = 0.8.dp.toPx(),
                pathEffect = dashEffect
            )

            if (animatedNorms.isEmpty()) return@Canvas

            val points = animatedNorms.mapIndexed { index, norm ->
                val x = if (animatedNorms.size == 1) w / 2f else padStart + (index.toFloat() / (animatedNorms.size - 1)) * usableW
                val y = padY + (1f - norm) * usableH
                drawLine(
                    color = gridColor.copy(alpha = 0.18f),
                    start = Offset(x, padY),
                    end = Offset(x, h - padY),
                    strokeWidth = 0.7.dp.toPx()
                )
                Offset(x, y)
            }

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, midY)
                lineTo(points.first().x, midY)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (enabled) 0.42f else 0.18f),
                        activeColor.copy(alpha = 0.04f)
                    )
                )
            )

            if (enabled) {
                drawPath(
                    path = linePath,
                    color = glowColor.copy(alpha = 0.30f),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            drawPath(
                path = linePath,
                color = activeColor,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEach { pt ->
                drawCircle(
                    color = activeColor.copy(alpha = 0.38f),
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.2.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun RotaryAudioKnob(
    title: String,
    value: Float,
    valueText: String,
    enabled: Boolean,
    bipolar: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val clamped = value.coerceIn(0f, 1f)
    val animatedClamped by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "eq_knob_$title"
    )
    val activeColor = if (enabled) accentColor else Color(0xFF6B6B6B)
    val trackColor = novaColors.recessedSurface
    val border = novaColors.borderSubtle
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestValue by rememberUpdatedState(clamped)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(126.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(title) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (!latestEnabled) return@detectVerticalDragGestures
                        change.consume()
                        val delta = -dragAmount / 220f
                        latestOnChange((latestValue + delta).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val startAngle = 135f
                val totalSweep = 270f

                val tickOuterR = (size.minDimension / 2f) - 2.dp.toPx()
                val tickInnerR = tickOuterR - 4.dp.toPx()
                for (step in 0..12) {
                    val deg = startAngle + (step / 12f) * totalSweep
                    val rad = Math.toRadians(deg.toDouble())
                    val isLit = (step / 12f) <= animatedClamped + 0.01f
                    drawLine(
                        color = if (isLit && enabled) activeColor.copy(alpha = 0.9f) else border,
                        start = Offset(center.x + (cos(rad) * tickInnerR).toFloat(), center.y + (sin(rad) * tickInnerR).toFloat()),
                        end = Offset(center.x + (cos(rad) * tickOuterR).toFloat(), center.y + (sin(rad) * tickOuterR).toFloat()),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val strokeWidth = 6.5.dp.toPx()
                val arcInset = 13.dp.toPx()
                val arcSize = Size(size.width - arcInset * 2, size.height - arcInset * 2)
                val topLeft = Offset(arcInset, arcInset)

                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (bipolar) {
                    val midAngle = startAngle + totalSweep / 2f
                    val sweep = (animatedClamped - 0.5f) * totalSweep
                    drawArc(
                        color = activeColor,
                        startAngle = midAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                } else {
                    drawArc(
                        color = activeColor,
                        startAngle = startAngle,
                        sweepAngle = animatedClamped * totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                val knobRadius = (size.minDimension / 2f) - 21.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF222222), Color(0xFF0E0E0E)),
                        center = Offset(center.x - knobRadius * 0.25f, center.y - knobRadius * 0.25f),
                        radius = knobRadius * 1.4f
                    ),
                    radius = knobRadius,
                    center = center
                )
                drawCircle(
                    color = novaColors.glassBorderTop.copy(alpha = 0.45f),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )

                val angleRad = Math.toRadians((startAngle + animatedClamped * totalSweep).toDouble())
                val innerR = knobRadius * 0.42f
                val outerR = knobRadius * 0.8f
                drawLine(
                    color = activeColor,
                    start = Offset(
                        center.x + (cos(angleRad) * innerR).toFloat(),
                        center.y + (sin(angleRad) * innerR).toFloat()
                    ),
                    end = Offset(
                        center.x + (cos(angleRad) * outerR).toFloat(),
                        center.y + (sin(angleRad) * outerR).toFloat()
                    ),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(novaColors.recessedSurface)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = valueText,
                style = NovaMonoTelemetry,
                color = if (enabled) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerticalEqBandFader(
    band: EqualizerBandInfo,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    accentColor: Color,
    glowColor: Color,
    onValueChange: (Int) -> Unit
) {
    val novaColors = LocalNovaColors.current
    val dbValue = band.levelMillibels / 100f
    val dbLabel = String.format(Locale.US, "%+.0f", dbValue)
    val trackBg = Color.White.copy(alpha = 0.14f)
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    val activeFill = if (enabled) accentColor else inactiveColor

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestMin by rememberUpdatedState(minMb)
    val latestMax by rememberUpdatedState(maxMb)
    val latestEnabled by rememberUpdatedState(enabled)

    val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
    val targetNorm = ((band.levelMillibels - minMb) / range).coerceIn(0f, 1f)
    val animatedNorm by animateFloatAsState(
        targetValue = targetNorm,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "eq_band_norm_${band.index}"
    )

    Column(
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(novaColors.recessedSurface)
            .border(0.6.dp, novaColors.glassBorderTop.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dbLabel,
            style = NovaMonoTelemetry.copy(fontSize = 10.sp),
            color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(172.dp)
                .pointerInput(band.index) {
                    detectTapGestures { offset ->
                        if (!latestEnabled) return@detectTapGestures
                        val fraction = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                        val newLevel = (latestMin + fraction * (latestMax - latestMin)).roundToInt()
                        latestOnValueChange(newLevel)
                    }
                }
                .pointerInput(band.index) {
                    detectVerticalDragGestures { change, _ ->
                        if (!latestEnabled) return@detectVerticalDragGestures
                        change.consume()
                        val fraction = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                        val newLevel = (latestMin + fraction * (latestMax - latestMin)).roundToInt()
                        latestOnValueChange(newLevel)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val zeroNormalized = ((0 - minMb) / range).coerceIn(0f, 1f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 6.5.dp.toPx()
                val padY = 12.dp.toPx()
                val usableHeight = size.height - padY * 2
                val centerX = size.width / 2f

                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(centerX - trackWidth / 2f, padY),
                    size = Size(trackWidth, usableHeight),
                    cornerRadius = CornerRadius(trackWidth / 2f)
                )

                val thumbY = padY + (1f - animatedNorm) * usableHeight
                val zeroY = padY + (1f - zeroNormalized) * usableHeight

                drawLine(
                    color = inactiveColor,
                    start = Offset(centerX - 11.dp.toPx(), zeroY),
                    end = Offset(centerX + 11.dp.toPx(), zeroY),
                    strokeWidth = 1.2.dp.toPx()
                )

                val topY = minOf(thumbY, zeroY)
                val barHeight = abs(thumbY - zeroY)
                if (barHeight > 1f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(glowColor, activeFill)
                        ),
                        topLeft = Offset(centerX - trackWidth / 2f, topY),
                        size = Size(trackWidth, barHeight),
                        cornerRadius = CornerRadius(trackWidth / 2f)
                    )
                }

                drawCircle(
                    color = activeFill.copy(alpha = 0.30f),
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = activeFill,
                    radius = 3.8.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = band.label,
            style = NovaMonoTelemetry.copy(fontSize = 9.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
