package com.novamusic.player.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
        elevatedSurface = NovaAmoledSurfaceElevated,
        recessedSurface = NovaAmoledSurfaceRecessed,
        dockSurface = NovaAmoledDock,
        borderSubtle = NovaAmoledBorder,
        glassSurface = NovaAmoledSurface,
        glassHighlight = NovaGlassHighlightDark,
        glassBorderTop = Color(0x48FFFFFF),
        glassBorderBottom = NovaGlassBorderBottomDark,
        glassShadow = Color(0x94000000),
        innerShadowTop = Color(0x28FFFFFF),
        innerShadowBottom = Color(0x42000000),
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
    accentColor: AccentColorOption = AccentColorOption.ELECTRIC_ORANGE,
    content: @Composable () -> Unit
) {
    val accent = Color(accentColor.hex)
    val isLight = themeMode == ThemeMode.LIGHT

    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF070709),
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = Color(0xFF070709),
            background = NovaAmoledBackground,
            onBackground = NovaTextPrimaryDark,
            surface = NovaAmoledSurface,
            onSurface = NovaTextPrimaryDark,
            surfaceVariant = NovaAmoledSurfaceElevated,
            onSurfaceVariant = NovaTextSecondaryDark,
            outline = NovaAmoledBorder,
            error = NovaError
        )
        ThemeMode.DARK -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF070709),
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = Color(0xFF070709),
            background = NovaDarkBackground,
            onBackground = NovaTextPrimaryDark,
            surface = NovaDarkSurface,
            onSurface = NovaTextPrimaryDark,
            surfaceVariant = NovaDarkSurfaceElevated,
            onSurfaceVariant = NovaTextSecondaryDark,
            outline = NovaDarkBorder,
            error = NovaError
        )
        ThemeMode.LIGHT -> lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = Color.White,
            background = NovaLightBackground,
            onBackground = NovaTextPrimaryLight,
            surface = NovaLightSurface,
            onSurface = NovaTextPrimaryLight,
            surfaceVariant = NovaLightSurfaceElevated,
            onSurfaceVariant = NovaTextSecondaryLight,
            outline = NovaLightBorder,
            error = NovaError
        )
    }

    val extended = when (themeMode) {
        ThemeMode.AMOLED -> NovaExtendedColors(
            elevatedSurface = NovaAmoledSurfaceElevated,
            recessedSurface = NovaAmoledSurfaceRecessed,
            dockSurface = NovaAmoledDock,
            borderSubtle = NovaAmoledBorder,
            glassSurface = NovaAmoledSurface,
            glassHighlight = NovaGlassHighlightDark,
            glassBorderTop = Color(0x4AFFFFFF),
            glassBorderBottom = NovaGlassBorderBottomDark,
            glassShadow = Color(0x96000000),
            innerShadowTop = Color(0x28FFFFFF),
            innerShadowBottom = Color(0x45000000),
            isAmoled = true,
            isLight = false
        )
        ThemeMode.DARK -> NovaExtendedColors(
            elevatedSurface = NovaDarkSurfaceElevated,
            recessedSurface = NovaDarkSurfaceRecessed,
            dockSurface = NovaDarkDock,
            borderSubtle = NovaDarkBorder,
            glassSurface = NovaDarkSurface,
            glassHighlight = NovaGlassHighlightDark,
            glassBorderTop = Color(0x52FFFFFF),
            glassBorderBottom = NovaGlassBorderBottomDark,
            glassShadow = Color(0x78000000),
            innerShadowTop = Color(0x2CFFFFFF),
            innerShadowBottom = Color(0x36000000),
            isAmoled = false,
            isLight = false
        )
        ThemeMode.LIGHT -> NovaExtendedColors(
            elevatedSurface = NovaLightSurfaceElevated,
            recessedSurface = NovaLightSurfaceRecessed,
            dockSurface = NovaLightDock,
            borderSubtle = NovaLightBorder,
            glassSurface = NovaLightSurface,
            glassHighlight = NovaGlassHighlightLight,
            glassBorderTop = Color(0xF5FFFFFF),
            glassBorderBottom = NovaGlassBorderBottomLight,
            glassShadow = Color(0x220F172A),
            innerShadowTop = Color(0xD8FFFFFF),
            innerShadowBottom = Color(0x160F172A),
            isAmoled = false,
            isLight = true
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
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
