package com.anees.mobileguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var settings: GuardSettings
    private lateinit var pinManager: PinManager
    private lateinit var statusText: TextView
    private lateinit var subStatusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var batteryText: TextView
    private lateinit var locationText: TextView
    private lateinit var eventsText: TextView

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshDashboard() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = GuardSettings(this); pinManager = PinManager(this)
        statusText = findViewById(R.id.textStatus); subStatusText = findViewById(R.id.textSubStatus)
        toggleButton = findViewById(R.id.buttonToggleGuard); batteryText = findViewById(R.id.tvBattery)
        locationText = findViewById(R.id.tvLocation); eventsText = findViewById(R.id.tvEvents)
        findViewById<Button>(R.id.buttonSettings).setOnClickListener { openSettings() }
        findViewById<Button>(R.id.buttonFindPhone).setOnClickListener { findMyPhone() }
        findViewById<Button>(R.id.buttonRemoteRescue).setOnClickListener { startActivity(Intent(this, RemoteRescueActivity::class.java)) }
        findViewById<Button>(R.id.buttonAiAssistant).setOnClickListener { startActivity(Intent(this, AiSecurityAssistantActivity::class.java)) }
        toggleButton.setOnClickListener { onToggleClicked() }
        findViewById<Button>(R.id.btnTest).setOnClickListener { testAlarm() }
        maybeRequestNotificationPermission(); requestLocationIfNeeded(); FirebaseDeviceBridge.registerDevice(this); refreshDashboard()
    }

    override fun onResume() { super.onResume(); refreshDashboard() }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun onToggleClicked() {
        if (!pinManager.isPinSet()) { startActivity(Intent(this, PinSetupActivity::class.java)); return }
        if (settings.guardModeOn) {
            startActivity(Intent(this, PinEntryActivity::class.java).putExtra(PinEntryActivity.EXTRA_PURPOSE, PinEntryActivity.PURPOSE_DISARM))
        } else armGuardMode()
    }

    private fun armGuardMode() {
        settings.guardModeOn = true
        ContextCompat.startForegroundService(this, Intent(this, GuardService::class.java).setAction(GuardService.ACTION_START_GUARDING))
        SmartSecurityEvents.add(this, "🛡️ Protection armed")
        FirebaseDeviceBridge.registerDevice(this)
        refreshDashboard()
    }

    private fun findMyPhone() { startActivity(Intent(this, FindMyPhoneActivity::class.java)) }

    private fun openSettings() {
        if (!pinManager.isPinSet()) { startActivity(Intent(this, PinSetupActivity::class.java)); return }
        startActivity(Intent(this, PinEntryActivity::class.java).putExtra(PinEntryActivity.EXTRA_PURPOSE, PinEntryActivity.PURPOSE_OPEN_SETTINGS))
    }

    private fun testAlarm() {
        SmartSecurityEvents.add(this, "🔔 Security alarm test")
        ContextCompat.startForegroundService(this, Intent(this, GuardService::class.java).setAction(GuardService.ACTION_TRIGGER_ALARM))
    }

    private fun refreshDashboard() {
        val active = settings.guardModeOn || RemoteRescueManager.isLostMode(this)
        statusText.text = if (active) "PROTECTION ACTIVE" else "PROTECTION OFF"
        statusText.setTextColor(if (active) 0xFF2E8B57.toInt() else 0xFFFF6B6B.toInt())
        subStatusText.text = if (RemoteRescueManager.isLostMode(this)) "Lost Mode is active" else if (settings.guardModeOn) "Your device is being monitored" else "Your device is not being monitored"
        toggleButton.text = if (settings.guardModeOn) "TURN GUARD MODE OFF" else "TURN GUARD MODE ON"
        val bi = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryText.text = "Battery: ${if (level >= 0) "$level%" else "--"}"
        locationText.text = "Location: ${lastLocationText()}"
        eventsText.text = SmartSecurityEvents.get(this).ifBlank { "No security events yet." }
    }

    private fun lastLocationText(): String {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return "Permission needed"
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: android.location.Location? = null
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) try { val l=lm.getLastKnownLocation(p); if(l!=null && (best==null || l.time>best!!.time)) best=l } catch(_:SecurityException){}
        return if(best==null) "Unavailable" else String.format(Locale.US,"%.4f, %.4f",best!!.latitude,best!!.longitude)
    }
}
