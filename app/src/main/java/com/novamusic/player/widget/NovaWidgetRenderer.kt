package com.novamusic.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.novamusic.player.MainActivity
import com.novamusic.player.R
import com.novamusic.player.utils.FormatUtils

/**
 * Builds RemoteViews instances for NOVA MUSIC Home Screen Widgets (Medium & Large)
 * and lightweight partial RemoteViews for live progress ticks without resending bitmaps.
 */
object NovaWidgetRenderer {

    suspend fun buildRemoteViews(
        context: Context,
        providerClass: Class<*>,
        widgetSize: NovaWidgetSize,
        state: NovaWidgetState
    ): RemoteViews {
        val layoutRes = when (widgetSize) {
            NovaWidgetSize.MEDIUM -> R.layout.widget_nova_medium
            NovaWidgetSize.LARGE -> R.layout.widget_nova_large
        }
        val views = RemoteViews(context.packageName, layoutRes)

        val emptyTitle = context.getString(R.string.widget_empty_title)
        val emptySubtitle = context.getString(R.string.widget_empty_subtitle)

        val displayTitle = NovaWidgetLogic.resolveTitle(
            title = if (state.hasTrack) state.title else null,
            emptyFallback = emptyTitle
        )
        val displayArtist = NovaWidgetLogic.resolveArtist(
            artist = state.artist,
            hasTrack = state.hasTrack,
            emptyStateSubtitle = emptySubtitle
        )

        views.setTextViewText(R.id.widget_title, displayTitle)
        views.setTextViewText(R.id.widget_artist, displayArtist)

        val playPauseIconRes = if (state.isPlaying) {
            R.drawable.ic_widget_pause
        } else {
            R.drawable.ic_widget_play
        }
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIconRes)

        val progress = NovaWidgetLogic.computeProgress(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            maxProgress = NovaWidgetContract.PROGRESS_MAX
        )
        views.setProgressBar(
            R.id.widget_progress,
            NovaWidgetContract.PROGRESS_MAX,
            progress,
            false
        )

        if (widgetSize == NovaWidgetSize.LARGE) {
            views.setTextViewText(
                R.id.widget_time_elapsed,
                FormatUtils.formatDuration(state.positionMs.coerceAtLeast(0L))
            )
            views.setTextViewText(
                R.id.widget_time_total,
                FormatUtils.formatDuration(state.durationMs.coerceAtLeast(0L))
            )
        }

        val isNight = NovaWidgetBitmapRenderer.isSystemNightMode(context)
        val glowBitmap = NovaWidgetBitmapRenderer.renderLiquidGlassGlow(
            context = context,
            artworkUri = state.artworkUri,
            dynamicColorsEnabled = state.dynamicColorsEnabled,
            fallbackAccentHex = state.accentHex,
            isNightMode = isNight
        )
        views.setImageViewBitmap(R.id.widget_dynamic_glow, glowBitmap)

        val artworkBitmap = if (state.hasTrack) {
            NovaWidgetBitmapRenderer.loadRoundedArtwork(
                context = context,
                artworkUri = state.artworkUri,
                cornerRadiusDp = 16f
            )
        } else {
            null
        }
        if (artworkBitmap != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artworkBitmap)
            views.setViewVisibility(R.id.widget_artwork, View.VISIBLE)
            views.setViewVisibility(R.id.widget_artwork_placeholder, View.GONE)
        } else {
            views.setImageViewResource(R.id.widget_artwork, android.R.color.transparent)
            views.setViewVisibility(R.id.widget_artwork, View.GONE)
            views.setViewVisibility(R.id.widget_artwork_placeholder, View.VISIBLE)
        }

        bindClickPendingIntents(context, providerClass, views, state.hasTrack)
        return views
    }

    fun buildProgressPartialRemoteViews(
        context: Context,
        widgetSize: NovaWidgetSize,
        positionMs: Long,
        durationMs: Long
    ): RemoteViews {
        val layoutRes = when (widgetSize) {
            NovaWidgetSize.MEDIUM -> R.layout.widget_nova_medium
            NovaWidgetSize.LARGE -> R.layout.widget_nova_large
        }
        val views = RemoteViews(context.packageName, layoutRes)
        val progress = NovaWidgetLogic.computeProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            maxProgress = NovaWidgetContract.PROGRESS_MAX
        )
        views.setProgressBar(
            R.id.widget_progress,
            NovaWidgetContract.PROGRESS_MAX,
            progress,
            false
        )
        if (widgetSize == NovaWidgetSize.LARGE) {
            views.setTextViewText(
                R.id.widget_time_elapsed,
                FormatUtils.formatDuration(positionMs.coerceAtLeast(0L))
            )
            views.setTextViewText(
                R.id.widget_time_total,
                FormatUtils.formatDuration(durationMs.coerceAtLeast(0L))
            )
        }
        return views
    }

    fun resolveWidgetSizeForId(
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        defaultSize: NovaWidgetSize
    ): NovaWidgetSize {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        return NovaWidgetLogic.selectWidgetSize(
            defaultSize = defaultSize,
            minHeightDp = minHeightDp,
            maxHeightDp = maxHeightDp
        )
    }

    private fun bindClickPendingIntents(
        context: Context,
        providerClass: Class<*>,
        views: RemoteViews,
        openNowPlaying: Boolean
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openNowPlaying) {
                putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
            }
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            if (openNowPlaying) 4001 else 4000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_open_app_area, openAppPendingIntent)

        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            createActionPendingIntent(
                context = context,
                providerClass = providerClass,
                action = NovaWidgetContract.ACTION_PREVIOUS,
                requestCode = 4010
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            createActionPendingIntent(
                context = context,
                providerClass = providerClass,
                action = NovaWidgetContract.ACTION_PLAY_PAUSE,
                requestCode = 4011
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            createActionPendingIntent(
                context = context,
                providerClass = providerClass,
                action = NovaWidgetContract.ACTION_NEXT,
                requestCode = 4012
            )
        )
    }

    private fun createActionPendingIntent(
        context: Context,
        providerClass: Class<*>,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, providerClass).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
