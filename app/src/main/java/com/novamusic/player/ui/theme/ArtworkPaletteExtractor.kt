package com.novamusic.player.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Immutable
data class ArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val deepTint: Color
)

val LocalArtworkPalette = staticCompositionLocalOf {
    ArtworkPalette(
        primary = NovaRedPrimary,
        secondary = NovaRedSecondary,
        glow = NovaRedGlow,
        deepTint = NovaCrimsonTint
    )
}

object ArtworkColorExtractor {
    private val cache = LruCache<String, ArtworkPalette>(64)

    suspend fun extract(
        context: Context,
        artworkUri: Uri?,
        fallbackAccent: Color
    ): ArtworkPalette {
        if (artworkUri == null) {
            return defaultPalette(fallbackAccent)
        }
        val cacheKey = "${artworkUri}_${fallbackAccent.toArgb()}"
        cache.get(cacheKey)?.let { return it }

        val extracted = withContext(Dispatchers.IO) {
            val bitmap = decodeSampledBitmap(context, artworkUri)
            if (bitmap == null) {
                defaultPalette(fallbackAccent)
            } else {
                try {
                    analyzeBitmap(bitmap, fallbackAccent)
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        }
        cache.put(cacheKey, extracted)
        return extracted
    }

    fun defaultPalette(fallbackAccent: Color): ArtworkPalette {
        val redBase = constrainToCrimson(fallbackAccent)
        return ArtworkPalette(
            primary = redBase,
            secondary = lerp(redBase, NovaRedSecondary, 0.55f),
            glow = lerp(redBase, NovaRedGlow, 0.50f),
            deepTint = NovaCrimsonTint
        )
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
            val sampleSize = if (maxDim > 64) {
                Integer.highestOneBit((maxDim / 48).coerceAtLeast(1)).coerceAtLeast(1)
            } else {
                1
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }.getOrNull()
    }

    /**
     * Derives subtle crimson luminance & warmth intensity from the active artwork while strictly
     * keeping every hue locked within the Red & Crimson spectrum (no green/blue/purple/pink/yellow).
     */
    private fun analyzeBitmap(bitmap: Bitmap, fallbackAccent: Color): ArtworkPalette {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return defaultPalette(fallbackAccent)

        val hsv = FloatArray(3)
        val stepX = max(1, width / 16)
        val stepY = max(1, height / 16)
        var energySum = 0f
        var warmthSum = 0f
        var samples = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) >= 160) {
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    android.graphics.Color.RGBToHSV(r, g, b, hsv)
                    energySum += (hsv[1] * 0.65f + hsv[2] * 0.35f)
                    warmthSum += ((r - b) / 255f + 1f) * 0.5f
                    samples++
                }
                x += stepX
            }
            y += stepY
        }

        if (samples == 0) return defaultPalette(fallbackAccent)

        val energy = (energySum / samples).coerceIn(0f, 1f)
        val warmth = (warmthSum / samples).coerceIn(0f, 1f)
        val base = constrainToCrimson(fallbackAccent)

        val primary = lerp(NovaRedSecondary, lerp(base, NovaRedPrimary, warmth), 0.45f + energy * 0.55f)
        val secondary = lerp(NovaRedMist, NovaRedSecondary, 0.45f + energy * 0.45f)
        val glow = lerp(primary, NovaRedGlow, 0.40f + energy * 0.35f)
        val deepTint = lerp(Color(0xFF0E0205), NovaCrimsonTint, 0.45f + energy * 0.55f)

        return ArtworkPalette(
            primary = primary,
            secondary = secondary,
            glow = glow,
            deepTint = deepTint
        )
    }

    private fun constrainToCrimson(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        // Lock strictly inside true red / crimson hues (346°..360° or 0°..6° -> never pink/orange)
        val hue = if (hsv[0] in 7f..345f) {
            if (hsv[0] < 180f) 354f else 348f
        } else {
            hsv[0]
        }
        val sat = hsv[1].coerceIn(0.72f, 0.96f)
        val valB = hsv[2].coerceIn(0.72f, 1.0f)
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valB)))
    }
}

@Composable
fun rememberArtworkPalette(
    artworkUri: Uri?,
    fallbackAccent: Color
): ArtworkPalette {
    val context = LocalContext.current
    var targetPalette by remember(fallbackAccent) {
        mutableStateOf(ArtworkColorExtractor.defaultPalette(fallbackAccent))
    }

    LaunchedEffect(artworkUri, fallbackAccent) {
        targetPalette = ArtworkColorExtractor.extract(context, artworkUri, fallbackAccent)
    }

    val animSpec = tween<Color>(durationMillis = 780, easing = FastOutSlowInEasing)
    val animatedPrimary by animateColorAsState(targetPalette.primary, animSpec, label = "art_primary")
    val animatedSecondary by animateColorAsState(targetPalette.secondary, animSpec, label = "art_secondary")
    val animatedGlow by animateColorAsState(targetPalette.glow, animSpec, label = "art_glow")
    val animatedDeep by animateColorAsState(targetPalette.deepTint, animSpec, label = "art_deep")

    return ArtworkPalette(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        glow = animatedGlow,
        deepTint = animatedDeep
    )
}
