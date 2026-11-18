package com.novamusic.player.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.novamusic.player.data.model.AccentColorOption
import com.novamusic.player.data.model.ThemeMode

data class NovaExtendedColors(
    val elevatedSurface: Color,
    val recessedSurface: Color,
    val dockSurface: Color,
    val borderSubtle: Color,
    val glassSurface: Color,
    val glassHighlight: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val glassShadow: Color,
    val innerShadowTop: Color,
    val innerShadowBottom: Color,
    val isAmoled: Boolean,
    val isLight: Boolean
)

val LocalNovaColors = staticCompositionLocalOf {
    NovaExtendedColors(
        elevatedSurface = NovaGlassSurfaceElevated,
        recessedSurface = NovaGlassSurfaceRecessed,
        dockSurface = NovaDockSurface,
        borderSubtle = NovaBorderSubtle,
        glassSurface = NovaGlassSurface,
        glassHighlight = NovaGlassHighlightDark,
        glassBorderTop = Color(0x3DFFFFFF),
        glassBorderBottom = NovaBorderSubtle,
        glassShadow = Color(0xCC000000),
        innerShadowTop = Color(0x26FFFFFF),
        innerShadowBottom = Color(0x52000000),
        isAmoled = true,
        isLight = false
    )
}

private val NovaShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun NovaMusicTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    accentColor: AccentColorOption = AccentColorOption.CRIMSON_PULSE,
    dynamicAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    // Enforce unified Red & Black Liquid Glass appearance across every screen and mode.
    val isPureBlack = themeMode == ThemeMode.AMOLED
    val accent = dynamicAccentColor ?: Color(accentColor.hex)
    val secondaryAccent = if (accentColor == AccentColorOption.DEEP_CRIMSON) {
        NovaRedPrimary
    } else {
        NovaRedSecondary
    }

    val backgroundColor = if (isPureBlack) NovaAmoledBackground else NovaBackgroundPrimary
    val surfaceColor = if (isPureBlack) NovaAmoledSurface else NovaDarkSurface
    val elevatedColor = if (isPureBlack) NovaAmoledSurfaceElevated else NovaDarkSurfaceElevated
    val recessedColor = if (isPureBlack) NovaAmoledSurfaceRecessed else NovaDarkSurfaceRecessed
    val dockColor = if (isPureBlack) NovaAmoledDock else NovaDarkDock

    val colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.22f),
        onPrimaryContainer = Color.White,
        secondary = secondaryAccent,
        onSecondary = Color.White,
        secondaryContainer = secondaryAccent.copy(alpha = 0.22f),
        onSecondaryContainer = Color.White,
        background = backgroundColor,
        onBackground = NovaTextPrimaryDark,
        surface = surfaceColor,
        onSurface = NovaTextPrimaryDark,
        surfaceVariant = NovaBackgroundSecondary,
        onSurfaceVariant = NovaTextSecondaryDark,
        surfaceContainer = NovaBackgroundSecondary,
        surfaceContainerLow = elevatedColor,
        surfaceContainerHigh = elevatedColor,
        surfaceContainerHighest = Color(0xFF181818),
        outline = NovaBorderSubtle,
        outlineVariant = Color(0xFF1F1F1F),
        error = NovaError,
        onError = Color.White
    )

    val extended = NovaExtendedColors(
        elevatedSurface = elevatedColor,
        recessedSurface = recessedColor,
        dockSurface = dockColor,
        borderSubtle = NovaBorderSubtle,
        glassSurface = surfaceColor,
        glassHighlight = NovaGlassHighlightDark,
        glassBorderTop = Color(0x3DFFFFFF),
        glassBorderBottom = NovaBorderSubtle,
        glassShadow = Color(0xCC000000),
        innerShadowTop = Color(0x26FFFFFF),
        innerShadowBottom = Color(0x52000000),
        isAmoled = isPureBlack,
        isLight = false
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalNovaColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaTypography,
            shapes = NovaShapes,
            content = content
        )
    }
}
