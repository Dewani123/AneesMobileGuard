package com.anees.mobileguard

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.content.ContextCompat

class AmgFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FirebaseDeviceBridge.registerDevice(this)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val command = message.data["command"] ?: return
        when (command) {
            "ACTIVATE_LOST_MODE" -> RemoteRescueManager.setLostMode(this, true)
            "DEACTIVATE_LOST_MODE" -> RemoteRescueManager.setLostMode(this, false)
            "PLAY_ALARM" -> ContextCompat.startForegroundService(
                this, Intent(this, GuardService::class.java).setAction(GuardService.ACTION_TRIGGER_ALARM)
            )
            "REQUEST_LOCATION" -> LocationReporter.publishOnce(this)
            "REQUEST_DEVICE_STATUS" -> FirebaseDeviceBridge.registerDevice(this)
        }
    }
}
