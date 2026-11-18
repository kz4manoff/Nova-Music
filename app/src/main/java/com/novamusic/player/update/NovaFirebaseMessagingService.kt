package com.novamusic.player.update

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.novamusic.player.NovaMusicApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class NovaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? NovaMusicApplication ?: return
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                app.container.fcmUpdatePushManager.onNewToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val app = applicationContext as? NovaMusicApplication ?: return
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                app.container.fcmUpdatePushManager.handleRemoteMessage(message)
            }
        }
    }

    companion object {
        private const val OPERATION_TIMEOUT_MS = 8_000L
    }
}
