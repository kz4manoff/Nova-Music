package com.novamusic.player.ui.theme

import androidx.compose.ui.graphics.Color

// NOVA MUSIC — Unified Red & Black Liquid Glass Palette
val NovaAmoledBackground = Color(0xFF000000)
val NovaBackgroundPrimary = Color(0xFF050505)
val NovaBackgroundSecondary = Color(0xFF101010)

// Translucent Charcoal Glass Surfaces (#181818 with layered translucency)
val NovaGlassSurface = Color(0xBF181818)
val NovaGlassSurfaceElevated = Color(0xF2121212)
val NovaGlassSurfaceRecessed = Color(0x940D0D0D)
val NovaDockSurface = Color(0xE6101010)
val NovaBorderSubtle = Color(0xFF262626)

// Legacy aliases mapped cleanly onto the unified Red & Black system
val NovaAmoledSurface = NovaGlassSurface
val NovaAmoledSurfaceElevated = NovaGlassSurfaceElevated
val NovaAmoledSurfaceRecessed = NovaGlassSurfaceRecessed
val NovaAmoledDock = NovaDockSurface
val NovaAmoledBorder = NovaBorderSubtle

val NovaDarkBackground = NovaBackgroundPrimary
val NovaDarkSurface = Color(0xCC181818)
val NovaDarkSurfaceElevated = Color(0xF5141414)
val NovaDarkSurfaceRecessed = Color(0x99101010)
val NovaDarkDock = NovaDockSurface
val NovaDarkBorder = NovaBorderSubtle

// Optical Specular & Crimson Rim Highlights (no purple/blue/pink)
val NovaGlassHighlightDark = Color(0x36FFFFFF)
val NovaGlassBorderBottomDark = Color(0xFF262626)

// Unified Vibrant Crimson & Deep Red Accents
val NovaRedPrimary = Color(0xFFFF1744)
val NovaRedSecondary = Color(0xFFD50032)
val NovaRedGlow = Color(0xFFFF4569)
val NovaRedMist = Color(0xFF8E001A)
val NovaCrimsonTint = Color(0xFF1A0308)

// Typography Inks (Pure White & Soft Neutral Gray)
val NovaTextPrimaryDark = Color(0xFFFFFFFF)
val NovaTextSecondaryDark = Color(0xFFA0A0A0)

// Semantic Red Feedback (strictly within Red & Black palette)
val NovaError = Color(0xFFFF1744)
