package com.anees.mobileguard

import android.content.Context
import java.util.UUID

object DeviceIdentity {
    private const val PREFS = "find_my_phone"
    private const val KEY_ID = "device_id"

    fun getId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ID, null)
        if (existing != null) return existing

        val id = "AMG-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        prefs.edit().putString(KEY_ID, id).apply()
        return id
    }
}
