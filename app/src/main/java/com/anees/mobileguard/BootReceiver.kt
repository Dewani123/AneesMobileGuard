package com.anees.mobileguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Resumes Guard Mode after a reboot, but ONLY if the user has both:
 *   (a) left Guard Mode armed, and
 *   (b) explicitly enabled "Resume after restart" in Settings.
 * Nothing happens automatically otherwise.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = GuardSettings(context)
        if (settings.guardModeOn && settings.autoRestartOnBoot) {
            val serviceIntent = Intent(context, GuardService::class.java)
            serviceIntent.action = GuardService.ACTION_START_GUARDING
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
