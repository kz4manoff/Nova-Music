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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Immutable
data class ArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val deepTint: Color
)

val LocalArtworkPalette = staticCompositionLocalOf {
    ArtworkPalette(
        primary = NovaPink,
        secondary = NovaPurple,
        glow = NovaPinkGlow,
        deepTint = NovaPurpleDeep
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
        if (fallbackAccent == NovaPink) {
            return ArtworkPalette(
                primary = NovaPink,
                secondary = NovaPurple,
                glow = NovaPinkGlow,
                deepTint = NovaPurpleDeep
            )
        }
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(fallbackAccent.toArgb(), hsv)
        val secondaryHue = (hsv[0] - 48f + 360f) % 360f
        val secondary = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(secondaryHue, (hsv[1] * 0.68f).coerceIn(0.32f, 0.80f), 0.62f)
            )
        )
        val glow = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hsv[0], (hsv[1] * 0.78f).coerceIn(0.35f, 0.9f), 0.98f)
            )
        )
        val deepTint = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(secondaryHue, (hsv[1] * 0.68f).coerceIn(0.26f, 0.72f), 0.16f)
            )
        )
        return ArtworkPalette(
            primary = fallbackAccent,
            secondary = secondary,
            glow = glow,
            deepTint = deepTint
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

    private fun analyzeBitmap(bitmap: Bitmap, fallbackAccent: Color): ArtworkPalette {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return defaultPalette(fallbackAccent)

        // Quantize colors into 12 hue buckets + neutral bucket
        val bucketScores = FloatArray(13)
        val bucketR = FloatArray(13)
        val bucketG = FloatArray(13)
        val bucketB = FloatArray(13)
        val bucketWeights = FloatArray(13)
        val hsv = FloatArray(3)

        val stepX = max(1, width / 24)
        val stepY = max(1, height / 24)

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = android.graphics.Color.alpha(pixel)
                if (alpha >= 180) {
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    android.graphics.Color.RGBToHSV(r, g, b, hsv)

                    val hue = hsv[0]
                    val sat = hsv[1]
                    val valB = hsv[2]

                    // Skip near-pure black/white noise slightly so rich artwork pigments dominate
                    val lumPenalty = if (valB < 0.08f || valB > 0.96f && sat < 0.08f) 0.2f else 1f
                    val vibrancyWeight = (0.35f + sat * 1.85f) * (0.3f + valB * 0.7f) * lumPenalty

                    val bucket = if (sat < 0.14f) 12 else ((hue / 30f).toInt().coerceIn(0, 11))
                    bucketScores[bucket] += vibrancyWeight
                    bucketWeights[bucket] += 1f
                    bucketR[bucket] += r
                    bucketG[bucket] += g
                    bucketB[bucket] += b
                }
                x += stepX
            }
            y += stepY
        }

        // Pick top chromatic bucket if meaningful, otherwise fallback nicely
        var bestBucket = 0
        var bestScore = -1f
        for (i in 0..11) {
            if (bucketScores[i] > bestScore && bucketWeights[i] > 0f) {
                bestScore = bucketScores[i]
                bestBucket = i
            }
        }

        if (bestScore <= 0.1f || bucketWeights[bestBucket] <= 0f) {
            return defaultPalette(fallbackAccent)
        }

        val primaryColor = averagedColor(
            r = bucketR[bestBucket] / bucketWeights[bestBucket],
            g = bucketG[bestBucket] / bucketWeights[bestBucket],
            b = bucketB[bestBucket] / bucketWeights[bestBucket],
            minVal = 0.42f,
            maxVal = 0.92f
        )

        // Pick second distinct chromatic bucket at least 2 buckets away on the color wheel
        var secondBucket = -1
        var secondScore = -1f
        for (i in 0..11) {
            val dist = min(kotlin.math.abs(i - bestBucket), 12 - kotlin.math.abs(i - bestBucket))
            if (dist >= 2 && bucketWeights[i] >= 2f && bucketScores[i] > secondScore) {
                secondScore = bucketScores[i]
                secondBucket = i
            }
        }

        val primaryHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryColor.toArgb(), primaryHsv)

        val secondaryColor = if (secondBucket != -1) {
            averagedColor(
                r = bucketR[secondBucket] / bucketWeights[secondBucket],
                g = bucketG[secondBucket] / bucketWeights[secondBucket],
                b = bucketB[secondBucket] / bucketWeights[secondBucket],
                minVal = 0.28f,
                maxVal = 0.78f
            )
        } else {
            val shiftedHue = (primaryHsv[0] + 52f) % 360f
            Color(
                android.graphics.Color.HSVToColor(
                    floatArrayOf(
                        shiftedHue,
                        (primaryHsv[1] * 0.78f).coerceIn(0.25f, 0.85f),
                        (primaryHsv[2] * 0.72f).coerceIn(0.28f, 0.72f)
                    )
                )
            )
        }

        val glowColor = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(
                    primaryHsv[0],
                    (primaryHsv[1] * 0.92f).coerceIn(0.38f, 0.95f),
                    0.98f
                )
            )
        )

        val deepTintColor = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(
                    primaryHsv[0],
                    (primaryHsv[1] * 0.65f).coerceIn(0.20f, 0.72f),
                    0.15f
                )
            )
        )

        return ArtworkPalette(
            primary = primaryColor,
            secondary = secondaryColor,
            glow = glowColor,
            deepTint = deepTintColor
        )
    }

    private fun averagedColor(
        r: Float,
        g: Float,
        b: Float,
        minVal: Float,
        maxVal: Float
    ): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            r.toInt().coerceIn(0, 255),
            g.toInt().coerceIn(0, 255),
            b.toInt().coerceIn(0, 255),
            hsv
        )
        hsv[1] = hsv[1].coerceIn(0.28f, 0.92f)
        hsv[2] = hsv[2].coerceIn(minVal, maxVal)
        return Color(android.graphics.Color.HSVToColor(hsv))
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
