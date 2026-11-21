package com.novamusic.player.update

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.novamusic.player.NovaMusicApplication
import com.novamusic.player.push.BroadcastDeviceRegistrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class NovaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(
            TAG,
            "FCM onNewToken triggered (${token.length} chars): ${BroadcastDeviceRegistrar.maskToken(token)}"
        )
        val app = applicationContext as? NovaMusicApplication
        if (app == null) {
            Log.w(TAG, "onNewToken skipped: NovaMusicApplication unavailable")
            return
        }
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                app.container.fcmUpdatePushManager.onNewToken(token)
            } ?: Log.w(TAG, "onNewToken backend sync timed out after ${OPERATION_TIMEOUT_MS}ms")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(
            TAG,
            "FCM onMessageReceived id=${message.messageId ?: "none"} from=${message.from ?: "unknown"} dataKeys=${message.data.keys.joinToString(",")}"
        )
        val app = applicationContext as? NovaMusicApplication ?: return
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                app.container.fcmUpdatePushManager.handleRemoteMessage(message)
            }
        }
    }

    companion object {
        private const val TAG = "NovaFcmService"
        private const val OPERATION_TIMEOUT_MS = 8_000L
    }
}
