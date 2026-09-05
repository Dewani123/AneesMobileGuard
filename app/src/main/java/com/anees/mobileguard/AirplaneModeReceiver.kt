package com.anees.mobileguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Best-effort local alert when Airplane Mode is enabled while local Guard Mode is armed. */
class AirplaneModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_AIRPLANE_MODE_CHANGED) return
        if (!GuardSettings(context).guardModeOn) return
        val enabled = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        if (!enabled) return
        SmartSecurityEvents.add(context, "✈️ Airplane Mode enabled while Guard Mode was active")
        ContextCompat.startForegroundService(
            context,
            Intent(context, GuardService::class.java).setAction(GuardService.ACTION_TRIGGER_AIRPLANE_ALARM)
        )
    }
}
