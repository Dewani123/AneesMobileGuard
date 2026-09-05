package com.anees.mobileguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationReporter {
    fun publishOnce(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: android.location.Location? = null
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                val l = lm.getLastKnownLocation(provider)
                if (l != null && (best == null || l.time > best!!.time)) best = l
            } catch (_: SecurityException) { }
        }
        best?.let { FirebaseDeviceBridge.publishLocation(context, it.latitude, it.longitude, it.accuracy) }
    }
}
