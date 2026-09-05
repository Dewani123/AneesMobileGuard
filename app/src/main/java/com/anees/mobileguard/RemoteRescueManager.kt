package com.anees.mobileguard

import android.content.Context
import android.content.Intent

object RemoteRescueManager {
    private const val PREFS = "remote_rescue"
    private const val LOST = "lost_mode"
    fun deviceId(context: Context) = DeviceIdentity.getId(context)
    fun isLostMode(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(LOST, false)
    fun setLostMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(LOST, enabled).apply()
        SmartSecurityEvents.add(context, if (enabled) "🚨 Remote Lost Mode activated" else "Remote Lost Mode deactivated")
        FirebaseDeviceBridge.registerDevice(context)
        val i = Intent(context, GuardService::class.java).setAction(if (enabled) GuardService.ACTION_REMOTE_LOST_MODE else GuardService.ACTION_STOP_LOST_MODE)
        androidx.core.content.ContextCompat.startForegroundService(context, i)
    }
}
