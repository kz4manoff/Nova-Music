package com.novamusic.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.novamusic.player.NovaMusicApplication
import com.novamusic.player.service.MusicService
import com.novamusic.player.service.PlaybackSessionContract

/**
 * Shared AppWidgetProvider base for NOVA MUSIC Liquid Glass Home Screen Widgets.
 * Supports responsive resizing (Medium <-> Large) and reliable background/cold-start
 * transport controls through Media3 MediaController -> MusicService.
 */
abstract class BaseNovaWidgetProvider(
    private val defaultSize: NovaWidgetSize
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val coordinator = (context.applicationContext as NovaMusicApplication)
            .container
            .widgetCoordinator
        coordinator.updateProviderWidgets(
            providerClass = javaClass,
            appWidgetIds = appWidgetIds,
            defaultSize = defaultSize
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val coordinator = (context.applicationContext as NovaMusicApplication)
            .container
            .widgetCoordinator
        coordinator.updateProviderWidgets(
            providerClass = javaClass,
            appWidgetIds = intArrayOf(appWidgetId),
            defaultSize = defaultSize
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            NovaWidgetContract.ACTION_PLAY_PAUSE -> {
                dispatchPlaybackCommand(context, PlaybackSessionContract.ACTION_WIDGET_PLAY_PAUSE)
                return
            }

            NovaWidgetContract.ACTION_NEXT -> {
                dispatchPlaybackCommand(context, PlaybackSessionContract.ACTION_WIDGET_NEXT)
                return
            }

            NovaWidgetContract.ACTION_PREVIOUS -> {
                dispatchPlaybackCommand(context, PlaybackSessionContract.ACTION_WIDGET_PREVIOUS)
                return
            }

            NovaWidgetContract.ACTION_REFRESH -> {
                val coordinator = (context.applicationContext as NovaMusicApplication)
                    .container
                    .widgetCoordinator
                coordinator.refreshAll(forceFullUpdate = true)
                return
            }
        }
        super.onReceive(context, intent)
    }

    private fun dispatchPlaybackCommand(context: Context, sessionAction: String) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        val sessionToken = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

        controllerFuture.addListener({
            var controller: MediaController? = null
            try {
                controller = controllerFuture.get()
                val commandFuture = controller.sendCustomCommand(
                    SessionCommand(sessionAction, Bundle.EMPTY),
                    Bundle.EMPTY
                )
                commandFuture.addListener({
                    try {
                        commandFuture.get()
                    } catch (_: Throwable) {
                    } finally {
                        runCatching { MediaController.releaseFuture(controllerFuture) }
                        pendingResult.finish()
                    }
                }, MoreExecutors.directExecutor())
            } catch (_: Throwable) {
                runCatching {
                    if (controller != null) {
                        MediaController.releaseFuture(controllerFuture)
                    } else {
                        controllerFuture.cancel(false)
                    }
                }
                pendingResult.finish()
            }
        }, MoreExecutors.directExecutor())
    }
}

class NovaMusicMediumWidgetProvider : BaseNovaWidgetProvider(NovaWidgetSize.MEDIUM)

class NovaMusicLargeWidgetProvider : BaseNovaWidgetProvider(NovaWidgetSize.LARGE)
