package com.anees.mobileguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class FindMyPhoneActivity : AppCompatActivity() {
    private lateinit var locationText: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_my_phone)

        findViewById<TextView>(R.id.textDeviceId).text = DeviceIdentity.getId(this)
        locationText = findViewById(R.id.textLocation)

        findViewById<Button>(R.id.buttonRefreshLocation).setOnClickListener { loadLocation() }
        findViewById<Button>(R.id.buttonOpenMap).setOnClickListener { openMap() }
        findViewById<Button>(R.id.buttonShareId).setOnClickListener { shareId() }

        loadLocation()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun loadLocation() {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }

        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && (best == null || loc.time > best!!.time)) best = loc
            } catch (_: SecurityException) { }
        }

        locationText.text = if (best != null) {
            String.format(Locale.US, "%.6f, %.6f", best!!.latitude, best!!.longitude)
        } else {
            getString(R.string.location_not_available)
        }
    }

    private fun openMap() {
        if (!hasLocationPermission()) {
            loadLocation()
            return
        }
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && (best == null || loc.time > best!!.time)) best = loc
            } catch (_: SecurityException) { }
        }
        if (best == null) return
        val uri = Uri.parse("geo:${best!!.latitude},${best!!.longitude}?q=${best!!.latitude},${best!!.longitude}(Anees%20Mobile%20Guard)")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun shareId() {
        val id = DeviceIdentity.getId(this)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Anees Mobile Guard Device ID: $id")
        }, getString(R.string.share_device_id)))
    }
}
