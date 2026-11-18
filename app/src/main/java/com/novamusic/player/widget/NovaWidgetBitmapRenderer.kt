package com.novamusic.player.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.novamusic.player.ui.theme.ArtworkColorExtractor
import com.novamusic.player.ui.theme.ArtworkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Renders lightweight rounded album artwork bitmaps and album-reactive Liquid Glass
 * ambient glow layers for AppWidget RemoteViews with LRU caching so Binder IPC stays tiny
 * and fast.
 */
object NovaWidgetBitmapRenderer {

    private const val ARTWORK_TARGET_PX = 208
    private const val GLOW_WIDTH_PX = 240
    private const val GLOW_HEIGHT_PX = 136

    private val artworkCache = LruCache<String, Bitmap>(12)
    private val glowCache = LruCache<String, Bitmap>(8)

    fun isSystemNightMode(context: Context): Boolean {
        val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMask == Configuration.UI_MODE_NIGHT_YES
    }

    suspend fun loadRoundedArtwork(
        context: Context,
        artworkUri: Uri?,
        cornerRadiusDp: Float = 16f
    ): Bitmap? {
        if (artworkUri == null || artworkUri == Uri.EMPTY) return null
        val density = context.resources.displayMetrics.density.coerceIn(1f, 4f)
        val cacheKey = "${artworkUri}_${(cornerRadiusDp * density).toInt()}"
        artworkCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return it }

        val rendered = withContext(Dispatchers.IO) {
            val raw = decodeSampledBitmap(context, artworkUri, ARTWORK_TARGET_PX)
                ?: return@withContext null
            try {
                createRoundedSquareArtwork(
                    source = raw,
                    sizePx = ARTWORK_TARGET_PX,
                    cornerRadiusPx = cornerRadiusDp * density,
                    strokeWidthPx = 1.25f * density
                )
            } finally {
                if (!raw.isRecycled) {
                    raw.recycle()
                }
            }
        }
        if (rendered != null) {
            artworkCache.put(cacheKey, rendered)
        }
        return rendered
    }

    suspend fun renderLiquidGlassGlow(
        context: Context,
        artworkUri: Uri?,
        dynamicColorsEnabled: Boolean,
        fallbackAccentHex: Long,
        isNightMode: Boolean
    ): Bitmap {
        val effectiveUri = if (dynamicColorsEnabled) artworkUri else null
        val cacheKey = "${effectiveUri ?: "default"}_${fallbackAccentHex}_$isNightMode"
        glowCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return it }

        val fallbackColor = Color(fallbackAccentHex)
        val palette: ArtworkPalette = if (effectiveUri != null && effectiveUri != Uri.EMPTY) {
            ArtworkColorExtractor.extract(context, effectiveUri, fallbackColor)
        } else {
            ArtworkColorExtractor.defaultPalette(fallbackColor)
        }

        val density = context.resources.displayMetrics.density.coerceIn(1f, 4f)
        val bitmap = withContext(Dispatchers.Default) {
            createAmbientGlowBitmap(
                palette = palette,
                isNightMode = isNightMode,
                outerCornerPx = (26f * density).coerceAtMost(GLOW_HEIGHT_PX * 0.42f)
            )
        }
        glowCache.put(cacheKey, bitmap)
        return bitmap
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, targetPx: Int): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
            var sampleSize = 1
            if (maxDim > targetPx) {
                while ((maxDim / (sampleSize * 2)) >= targetPx) {
                    sampleSize *= 2
                }
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

    private fun createRoundedSquareArtwork(
        source: Bitmap,
        sizePx: Int,
        cornerRadiusPx: Float,
        strokeWidthPx: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val srcWidth = source.width.toFloat().coerceAtLeast(1f)
        val srcHeight = source.height.toFloat().coerceAtLeast(1f)
        val scale = max(sizePx / srcWidth, sizePx / srcHeight)
        val dx = (sizePx - srcWidth * scale) * 0.5f
        val dy = (sizePx - srcHeight * scale) * 0.5f

        val shader = android.graphics.BitmapShader(
            source,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        shader.setLocalMatrix(matrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
        }

        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        val radius = min(cornerRadiusPx, sizePx * 0.28f)
        canvas.drawRoundRect(rect, radius, radius, paint)

        // Subtle inner glass edge rim for tactile depth
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            color = 0x3DFFFFFF
        }
        val inset = strokeWidthPx * 0.5f
        rect.inset(inset, inset)
        canvas.drawRoundRect(rect, (radius - inset).coerceAtLeast(2f), (radius - inset).coerceAtLeast(2f), borderPaint)

        return output
    }

    private fun createAmbientGlowBitmap(
        palette: ArtworkPalette,
        isNightMode: Boolean,
        outerCornerPx: Float
    ): Bitmap {
        val w = GLOW_WIDTH_PX.toFloat()
        val h = GLOW_HEIGHT_PX.toFloat()
        val bitmap = Bitmap.createBitmap(GLOW_WIDTH_PX, GLOW_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clip cleanly to the widget container's rounded corners so zero pixels bleed outside API 26-30 bounds
        val clipPath = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), outerCornerPx, outerCornerPx, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val primaryColor = palette.primary.toArgb()
        val glowColor = palette.glow.toArgb()

        val primaryAlpha = if (isNightMode) 74 else 42
        val secondaryAlpha = if (isNightMode) 42 else 24

        // Primary radial bloom emanating behind the artwork corner
        val primaryCenterX = w * 0.16f
        val primaryCenterY = h * 0.48f
        val primaryRadius = w * 0.56f
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                primaryCenterX,
                primaryCenterY,
                primaryRadius,
                intArrayOf(
                    withAlpha(primaryColor, primaryAlpha),
                    withAlpha(glowColor, primaryAlpha / 3),
                    0x00000000
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, primaryPaint)

        // Subtle secondary ambient reflection near controls
        val secondaryCenterX = w * 0.82f
        val secondaryCenterY = h * 0.62f
        val secondaryRadius = w * 0.44f
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                secondaryCenterX,
                secondaryCenterY,
                secondaryRadius,
                intArrayOf(
                    withAlpha(glowColor, secondaryAlpha),
                    0x00000000
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, secondaryPaint)

        // Soft diagonal Liquid Glass refraction highlight across upper surface
        val sheenAlpha = if (isNightMode) 22 else 64
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                w * 0.72f,
                h * 0.85f,
                intArrayOf(
                    withAlpha(0xFFFFFFFF.toInt(), sheenAlpha),
                    withAlpha(0xFFFFFFFF.toInt(), sheenAlpha / 4),
                    0x00FFFFFF
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, sheenPaint)

        return bitmap
    }

    private fun withAlpha(argb: Int, alpha: Int): Int {
        return (argb and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    }
}
