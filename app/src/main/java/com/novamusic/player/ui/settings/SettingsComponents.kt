package com.novamusic.player.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.novamusic.player.BuildConfig
import com.novamusic.player.R
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.EqualizerState
import com.novamusic.player.ui.components.LiquidGlassSurface
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.rememberNovaHaptic
import com.novamusic.player.ui.theme.LocalNovaColors
import com.novamusic.player.ui.theme.NovaMonoTelemetry

@Composable
fun NovaGlassProfileCard(
    songCount: Int,
    librarySizeLabel: String,
    eqEnabled: Boolean,
    activePreset: String,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp,
        glassOpacity = 0.88f,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(8.dp, RoundedCornerShape(15.dp), ambientColor = accent, spotColor = accent)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.28f),
                                novaColors.recessedSurface
                            )
                        )
                    )
                    .border(0.75.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nova_logo),
                    contentDescription = "NOVA MUSIC",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NOVA MUSIC",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = NovaMonoTelemetry.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$songCount tracks • $librarySizeLabel • ${if (eqEnabled) "DSP: $activePreset" else "Direct Audio"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NovaGlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val novaColors = LocalNovaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.65f) else novaColors.glassBorderTop.copy(alpha = 0.45f),
        animationSpec = tween(180),
        label = "settings_search_border"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (novaColors.isLight) Color.White.copy(alpha = 0.78f)
                else novaColors.recessedSurface.copy(alpha = 0.78f)
            )
            .border(0.75.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = if (isFocused) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search settings...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun NovaSectionHeader(
    title: String,
    icon: ImageVector,
    trailingBadge: String? = null,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = title.uppercase(),
            style = NovaMonoTelemetry.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.05.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (!trailingBadge.isNullOrBlank()) {
            Text(
                text = trailingBadge,
                style = NovaMonoTelemetry.copy(fontSize = 10.sp),
                color = accent.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
fun NovaSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 8.dp,
        glassOpacity = 0.86f,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun NovaSettingsDivider() {
    val novaColors = LocalNovaColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp)
            .height(0.55.dp)
            .background(novaColors.borderSubtle.copy(alpha = 0.36f))
    )
}

@Composable
fun NovaGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val density = LocalDensity.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> novaColors.recessedSurface.copy(alpha = 0.45f)
            checked -> accent
            novaColors.isLight -> Color(0xFFD5DCE8)
            else -> Color.White.copy(alpha = 0.16f)
        },
        animationSpec = if (reducedMotion) snap() else tween(220, easing = FastOutSlowInEasing),
        label = "nova_switch_track"
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (isPressed && enabled) 26.dp else 23.dp,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "nova_switch_thumb_w"
    )
    val thumbTravelPx by animateFloatAsState(
        targetValue = if (checked) with(density) { 21.dp.toPx() } else 0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 440f
        ),
        label = "nova_switch_thumb_x"
    )

    Box(
        modifier = modifier
            .width(50.dp)
            .height(29.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                width = 0.75.dp,
                color = if (checked) Color.White.copy(alpha = 0.36f) else novaColors.glassBorderBottom,
                shape = CircleShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    performHaptic()
                    onCheckedChange(!checked)
                }
            )
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(thumbWidth)
                .height(23.dp)
                .graphicsLayer {
                    val maxShift = with(density) { (50.dp - 6.dp - thumbWidth).toPx() }
                    translationX = thumbTravelPx.coerceIn(0f, maxShift.coerceAtLeast(0f))
                }
                .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF2F4F8))
                    )
                )
        )
    }
}

@Composable
fun NovaSwitchSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                performHaptic()
                onCheckedChange(!checked)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIconTile(
            icon = icon,
            tint = if (checked && enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            highlighted = checked && enabled,
            recessedBg = novaColors.recessedSurface
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        NovaGlassSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun NovaActionSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 500f),
        label = "nova_action_press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    performHaptic()
                    onClick()
                }
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIconTile(
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            highlighted = false,
            recessedBg = novaColors.recessedSurface
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (trailingContent != null) {
            trailingContent()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!trailingText.isNullOrBlank()) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun <T> NovaDropdownSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    performHaptic()
                    expanded = true
                }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingIconTile(
                icon = icon,
                tint = accent,
                highlighted = false,
                recessedBg = novaColors.recessedSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.13f))
                    .border(0.6.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accent,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(novaColors.elevatedSurface)
                .border(0.65.dp, novaColors.glassBorderTop.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selectedValue
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        performHaptic()
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
fun <T> NovaSegmentedGlassBar(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current
    val reducedMotion = LocalReducedMotion.current
    val performHaptic = rememberNovaHaptic()
    val density = LocalDensity.current
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(novaColors.recessedSurface.copy(alpha = 0.84f))
            .border(0.65.dp, novaColors.glassBorderBottom, RoundedCornerShape(16.dp))
            .padding(3.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val itemWidth = maxWidth / count
        val itemWidthPx = with(density) { itemWidth.toPx() }.coerceAtLeast(1f)
        val indicatorX by animateFloatAsState(
            targetValue = itemWidthPx * selectedIndex,
            animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 380f),
            label = "nova_seg_slide"
        )

        Box(
            modifier = Modifier
                .width(itemWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = indicatorX }
                .shadow(4.dp, RoundedCornerShape(13.dp), ambientColor = accent.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (novaColors.isLight) {
                            listOf(Color.White, lerp(Color.White, accent, 0.10f))
                        } else {
                            listOf(lerp(Color.White, accent, 0.28f).copy(alpha = 0.24f), accent.copy(alpha = 0.14f))
                        }
                    )
                )
                .border(
                    width = 0.7.dp,
                    color = Color.White.copy(alpha = if (novaColors.isLight) 0.9f else 0.36f),
                    shape = RoundedCornerShape(13.dp)
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (value, label) ->
                val isSel = value == selected
                val textColor by animateColorAsState(
                    targetValue = if (isSel) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "nova_seg_txt"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(13.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                performHaptic()
                                onSelect(value)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun NovaGlassSliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val novaColors = LocalNovaColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingIconTile(
                icon = icon,
                tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                highlighted = false,
                recessedBg = novaColors.recessedSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) accent.copy(alpha = 0.14f) else novaColors.recessedSurface)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueLabel,
                    style = NovaMonoTelemetry.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                activeTrackColor = accent,
                inactiveTrackColor = novaColors.recessedSurface,
                disabledActiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

@Composable
fun NovaAccentColorPicker(
    selected: AccentColorOption,
    onSelect: (AccentColorOption) -> Unit
) {
    val novaColors = LocalNovaColors.current
    val performHaptic = rememberNovaHaptic()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = selected.label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(selected.hex)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccentColorOption.entries.forEach { option ->
                val isSelected = selected == option
                val swatchColor = Color(option.hex)
                val ringScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                    label = "accent_swatch_${option.name}"
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                        }
                        .clip(CircleShape)
                        .background(if (isSelected) swatchColor.copy(alpha = 0.20f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.4.dp else 0.7.dp,
                            color = if (isSelected) swatchColor else novaColors.glassBorderBottom,
                            shape = CircleShape
                        )
                        .clickable {
                            performHaptic()
                            onSelect(option)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(
                                elevation = if (isSelected) 8.dp else 2.dp,
                                shape = CircleShape,
                                ambientColor = swatchColor,
                                spotColor = swatchColor
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(swatchColor.copy(alpha = 0.88f), swatchColor)
                                )
                            )
                            .border(0.8.dp, Color.White.copy(alpha = 0.42f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = option.label,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NovaGlassSelectionDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    onDismissAction: (() -> Unit)? = null,
    confirmText: String = "Close",
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = 24.dp,
            glassOpacity = 1.08f,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                content()
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!dismissText.isNullOrBlank() && onDismissAction != null) {
                        TextButton(onClick = onDismissAction) {
                            Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NovaInlineEqCurve(
    eqState: EqualizerState,
    accent: Color
) {
    val novaColors = LocalNovaColors.current
    val bands = eqState.bands
    val curveColor = if (eqState.isEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(novaColors.recessedSurface.copy(alpha = 0.80f))
            .border(0.6.dp, novaColors.glassBorderBottom, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.dp.toPx()
            )

            if (bands.isEmpty()) return@Canvas
            val minMb = eqState.minLevelMillibels.toFloat()
            val maxMb = eqState.maxLevelMillibels.toFloat()
            val span = (maxMb - minMb).coerceAtLeast(1f)

            val points = bands.mapIndexed { idx, band ->
                val x = if (bands.size == 1) w / 2f else (idx.toFloat() / (bands.size - 1).toFloat()) * w
                val norm = ((band.levelMillibels.toFloat() - minMb) / span).coerceIn(0f, 1f)
                val y = h - (norm * h * 0.82f + h * 0.09f)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEach { pt ->
                drawCircle(
                    color = curveColor,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

@Composable
fun NovaInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = NovaMonoTelemetry.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SettingIconTile(
    icon: ImageVector,
    tint: Color,
    highlighted: Boolean,
    recessedBg: Color
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlighted) accent.copy(alpha = 0.16f) else recessedBg.copy(alpha = 0.82f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}
