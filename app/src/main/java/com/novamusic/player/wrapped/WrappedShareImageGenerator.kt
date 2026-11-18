package com.novamusic.player.wrapped

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.novamusic.player.ui.theme.ArtworkColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Generates high-resolution 1080x1920 Liquid Glass summary poster images for sharing and saving.
 */
object WrappedShareImageGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    suspend fun createShareUri(
        context: Context,
        stats: WrappedSummaryStats
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = renderPosterBitmap(context, stats)
            try {
                val wrappedDir = File(context.cacheDir, "wrapped").apply { mkdirs() }
                // Clean up older generated posters to keep cache compact
                wrappedDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach {
                    runCatching { it.delete() }
                }
                val file = File(wrappedDir, "nova_wrapped_${stats.periodRange.period.name.lowercase()}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    fun launchShareSheet(context: Context, imageUri: Uri, periodTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_SUBJECT, "My NOVA MUSIC Wrapped — $periodTitle")
            putExtra(Intent.EXTRA_TEXT, "My $periodTitle listening stats on NOVA MUSIC.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share NOVA WRAPPED").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun savePosterToPictures(
        context: Context,
        stats: WrappedSummaryStats
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = renderPosterBitmap(context, stats)
            try {
                val fileName = "NOVA_WRAPPED_${stats.periodRange.period.name}_${System.currentTimeMillis()}.png"
                val resolver = context.contentResolver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NOVA MUSIC")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Unable to create MediaStore image entry")
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        } ?: throw IllegalStateException("Unable to open image output stream")
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        "Saved to Pictures/NOVA MUSIC"
                    } catch (t: Throwable) {
                        runCatching { resolver.delete(uri, null, null) }
                        throw t
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val inserted = MediaStore.Images.Media.insertImage(
                        resolver,
                        bitmap,
                        fileName,
                        "NOVA MUSIC Wrapped Summary"
                    ) ?: throw IllegalStateException("Unable to save image to Pictures")
                    if (inserted.isNotBlank()) "Saved to Pictures" else "Saved"
                }
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    private suspend fun renderPosterBitmap(
        context: Context,
        stats: WrappedSummaryStats
    ): Bitmap {
        val artworkUri = stats.primaryArtworkUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        val palette = ArtworkColorExtractor.extract(
            context = context,
            artworkUri = artworkUri,
            fallbackAccent = androidx.compose.ui.graphics.Color(0xFFFF1744L)
        )
        val accentArgb = palette.primary.toArgb()
        val glowArgb = palette.glow.toArgb()
        val artworkBitmap = artworkUri?.let { decodeSquareArtwork(context, it, 320) }

        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Obsidian Backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                intArrayOf(0xFF050508.toInt(), 0xFF0D080D.toInt(), 0xFF050507.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // 2. Dynamic Liquid Glass Ambient Blooms
        drawRadialBloom(canvas, WIDTH * 0.22f, HEIGHT * 0.22f, 680f, withAlpha(accentArgb, 92))
        drawRadialBloom(canvas, WIDTH * 0.85f, HEIGHT * 0.52f, 560f, withAlpha(glowArgb, 62))
        drawRadialBloom(canvas, WIDTH * 0.30f, HEIGHT * 0.88f, 620f, withAlpha(accentArgb, 58))

        // 3. Outer Liquid Glass Story Card
        val cardRect = RectF(64f, 84f, WIDTH - 64f, HEIGHT - 84f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                intArrayOf(0xD6161822.toInt(), 0xE60B0C12.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, cardFill)

        // Specular catchlight & frosted border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.left, cardRect.bottom,
                intArrayOf(0x70FFFFFF, 0x22FFFFFF, 0x38FFFFFF),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, borderPaint)

        // 4. Header Section
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = 30f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("NOVA MUSIC", 124f, 178f, brandPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 66f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("NOVA WRAPPED", 124f, 254f, titlePaint)

        val periodPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xB8E2E6F0.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("${stats.periodRange.displayTitle}  •  ${stats.periodRange.subtitle}", 124f, 304f, periodPaint)

        // 5. Top Artist + Artwork Spotlight Row
        val spotlightRect = RectF(118f, 348f, WIDTH - 118f, 650f)
        drawGlassSubPanel(canvas, spotlightRect, 36f)

        val artSize = 236f
        val artRect = RectF(
            spotlightRect.left + 32f,
            spotlightRect.top + (spotlightRect.height() - artSize) * 0.5f,
            spotlightRect.left + 32f + artSize,
            spotlightRect.top + (spotlightRect.height() + artSize) * 0.5f
        )
        if (artworkBitmap != null) {
            drawRoundedBitmap(canvas, artworkBitmap, artRect, 34f)
            if (!artworkBitmap.isRecycled) artworkBitmap.recycle()
        } else {
            val fallbackArtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(accentArgb, 56)
            }
            canvas.drawRoundRect(artRect, 34f, 34f, fallbackArtPaint)
            val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 72f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("♪", artRect.centerX(), artRect.centerY() + 24f, notePaint)
        }

        val textStartX = artRect.right + 32f
        val availArtTextW = (spotlightRect.right - 28f - textStartX).coerceAtLeast(100f)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = 24f
            letterSpacing = 0.14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TOP ARTIST", textStartX, artRect.top + 48f, labelPaint)

        val topArtistName = stats.topArtist?.name ?: "Your Library"
        val artistNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val clippedArtist = TextUtils.ellipsize(topArtistName, artistNamePaint, availArtTextW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clippedArtist, textStartX, artRect.top + 114f, artistNamePaint)

        val artistStatsLine = stats.topArtist?.let {
            "${formatMinutesLabel(it.totalListeningMs)} listened • ${it.playCount} plays"
        } ?: "${stats.uniqueArtists} artists explored"
        val subDetailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCA7ADB9.toInt()
            textSize = 26f
        }
        val clippedArtistSub = TextUtils.ellipsize(artistStatsLine, subDetailPaint, availArtTextW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clippedArtistSub, textStartX, artRect.top + 162f, subDetailPaint)

        stats.topGenres.firstOrNull()?.let { topGenre ->
            val genreLine = "Top sound: ${topGenre.name} (${topGenre.percent}%)"
            val clippedGenre = TextUtils.ellipsize(genreLine, subDetailPaint, availArtTextW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(clippedGenre, textStartX, artRect.top + 206f, subDetailPaint)
        }

        // 6. 2x2 Key Metrics Grid
        val gridTop = 682f
        val colGap = 24f
        val cellW = (WIDTH - 236f - colGap) / 2f
        val cellH = 152f

        val rawMins = stats.totalListeningMs / 60_000L
        val minutesVal = if (rawMins >= 1L) rawMins.toString() else "<1"
        drawMetricTile(
            canvas = canvas,
            rect = RectF(118f, gridTop, 118f + cellW, gridTop + cellH),
            label = "MINUTES LISTENED",
            value = minutesVal,
            accentArgb = accentArgb
        )
        drawMetricTile(
            canvas = canvas,
            rect = RectF(118f + cellW + colGap, gridTop, WIDTH - 118f, gridTop + cellH),
            label = "TOTAL PLAYS",
            value = "${stats.totalPlays}",
            accentArgb = accentArgb
        )
        drawMetricTile(
            canvas = canvas,
            rect = RectF(118f, gridTop + cellH + 22f, 118f + cellW, gridTop + cellH * 2f + 22f),
            label = "PEAK HOUR",
            value = WrappedAnalyticsEngine.formatHourLabel(stats.peakHour),
            accentArgb = accentArgb
        )
        drawMetricTile(
            canvas = canvas,
            rect = RectF(118f + cellW + colGap, gridTop + cellH + 22f, WIDTH - 118f, gridTop + cellH * 2f + 22f),
            label = "BEST STREAK",
            value = "${stats.longestStreakDays} days",
            accentArgb = accentArgb
        )

        // 7. Top Songs List Panel
        val songsTop = gridTop + cellH * 2f + 56f
        val songsBottom = 1586f
        val songsRect = RectF(118f, songsTop, WIDTH - 118f, songsBottom)
        drawGlassSubPanel(canvas, songsRect, 36f)

        canvas.drawText("TOP SONGS", songsRect.left + 34f, songsRect.top + 56f, labelPaint)

        val rowTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowArtistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xB8A6ACB9.toInt()
            textSize = 23f
        }
        val rankPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val countPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xDDF4F5F8.toInt()
            textSize = 24f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val songsToDraw = stats.topSongs.take(5)
        var rowY = songsRect.top + 114f
        val rowStep = 82f
        val maxSongTitleW = songsRect.width() - 250f
        songsToDraw.forEach { song ->
            canvas.drawText("#${song.rank}", songsRect.left + 34f, rowY, rankPaint)
            val clippedTitle = TextUtils.ellipsize(song.title, rowTitlePaint, maxSongTitleW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(clippedTitle, songsRect.left + 104f, rowY - 4f, rowTitlePaint)
            val clippedArtist = TextUtils.ellipsize(song.artist, rowArtistPaint, maxSongTitleW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(clippedArtist, songsRect.left + 104f, rowY + 26f, rowArtistPaint)
            canvas.drawText("${song.playCount}x", songsRect.right - 34f, rowY + 4f, countPaint)
            rowY += rowStep
        }

        // 8. Personality + Brand Footer
        val badgeRect = RectF(118f, 1618f, WIDTH - 118f, 1756f)
        drawGlassSubPanel(canvas, badgeRect, 32f, fillArgb = withAlpha(accentArgb, 38))
        stats.personality?.let { persona ->
            canvas.drawText("LISTENING PERSONALITY", badgeRect.left + 32f, badgeRect.top + 48f, labelPaint)
            val personaTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(persona.archetypeTitle, badgeRect.left + 32f, badgeRect.top + 104f, personaTitlePaint)
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88FFFFFF.toInt()
            textSize = 24f
            letterSpacing = 0.16f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("NOVA MUSIC • PRIVATE ON-DEVICE WRAPPED", WIDTH / 2f, 1806f, footerPaint)

        return bitmap
    }

    private fun drawGlassSubPanel(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        fillArgb: Int = 0x16FFFFFF
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillArgb }
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = 0x2FFFFFFF
        }
        canvas.drawRoundRect(rect, radius, radius, strokePaint)
    }

    private fun drawMetricTile(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        accentArgb: Int
    ) {
        drawGlassSubPanel(canvas, rect, 30f)
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xB0C7CCD9.toInt()
            textSize = 21f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
        canvas.drawCircle(rect.left + 30f, rect.top + 40f, 6f, dotPaint)
        canvas.drawText(label, rect.left + 46f, rect.top + 48f, labelPaint)
        canvas.drawText(value, rect.left + 28f, rect.top + 114f, valuePaint)
    }

    private fun drawRadialBloom(canvas: Canvas, cx: Float, cy: Float, radius: Float, centerColor: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(centerColor, 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun decodeSquareArtwork(context: Context, uri: Uri, targetPx: Int): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            if (maxDim > targetPx) {
                while ((maxDim / (sample * 2)) >= targetPx) sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    private fun drawRoundedBitmap(canvas: Canvas, source: Bitmap, destRect: RectF, radius: Float) {
        val srcW = source.width.toFloat().coerceAtLeast(1f)
        val srcH = source.height.toFloat().coerceAtLeast(1f)
        val scale = max(destRect.width() / srcW, destRect.height() / srcH)
        val dx = destRect.left + (destRect.width() - srcW * scale) * 0.5f
        val dy = destRect.top + (destRect.height() - srcH * scale) * 0.5f

        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        shader.setLocalMatrix(matrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
        }
        canvas.drawRoundRect(destRect, radius, radius, paint)
    }

    private fun formatMinutesLabel(ms: Long): String {
        val mins = ms / 60_000L
        return if (mins >= 60L) {
            val h = mins / 60L
            val m = mins % 60L
            if (m > 0L) "${h}h ${m}m" else "${h}h"
        } else {
            "${max(1L, mins)}m"
        }
    }

    private fun withAlpha(argb: Int, alpha: Int): Int {
        return (argb and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
