package com.anees.mobileguard

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmartSecurityEvents {
    private const val PREFS = "smart_security_events"
    private const val KEY = "events"
    private const val MAX = 40

    fun add(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val old = prefs.getString(KEY, "") ?: ""
        val next = ("$stamp  $message\n$old").lineSequence().take(MAX).joinToString("\n")
        prefs.edit().putString(KEY, next).apply()
    }

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
}
