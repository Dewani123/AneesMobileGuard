package com.anees.mobileguard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RemoteRescueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_rescue)
        val status = findViewById<TextView>(R.id.remoteStatus)
        val deviceId = findViewById<TextView>(R.id.deviceId)
        val refresh = { status.text = if (RemoteRescueManager.isLostMode(this)) "LOST MODE: ACTIVE" else "LOST MODE: READY" }
        deviceId.text = "Device ID: ${RemoteRescueManager.deviceId(this)}\nPairing Code: ${FirebaseDeviceBridge.pairingSecret(this)}"
        findViewById<Button>(R.id.activateLost).setOnClickListener { RemoteRescueManager.setLostMode(this, true); refresh() }
        findViewById<Button>(R.id.deactivateLost).setOnClickListener { RemoteRescueManager.setLostMode(this, false); refresh() }
        findViewById<Button>(R.id.playAlarmRemote).setOnClickListener { androidx.core.content.ContextCompat.startForegroundService(this, android.content.Intent(this, GuardService::class.java).setAction(GuardService.ACTION_TRIGGER_ALARM)); refresh() }

        val target = findViewById<EditText>(R.id.remoteDeviceId)
        val code = findViewById<EditText>(R.id.remotePairingCode)
        findViewById<Button>(R.id.remoteActivate).setOnClickListener { send(target.text.toString(), code.text.toString(), "ACTIVATE_LOST_MODE") }
        findViewById<Button>(R.id.remoteLocate).setOnClickListener { send(target.text.toString(), code.text.toString(), "REQUEST_LOCATION") }
        findViewById<Button>(R.id.remoteAlarm).setOnClickListener { send(target.text.toString(), code.text.toString(), "PLAY_ALARM") }
        findViewById<Button>(R.id.remoteStatusButton).setOnClickListener { send(target.text.toString(), code.text.toString(), "REQUEST_DEVICE_STATUS") }
        findViewById<Button>(R.id.remoteDeactivate).setOnClickListener { send(target.text.toString(), code.text.toString(), "DEACTIVATE_LOST_MODE") }
        refresh()
    }

    private fun send(deviceId: String, pairingSecret: String, command: String) {
        if (deviceId.isBlank() || pairingSecret.isBlank()) {
            Toast.makeText(this, "Enter Device ID and Pairing Code", Toast.LENGTH_SHORT).show(); return
        }
        try {
            val data = hashMapOf<String, Any>("deviceId" to deviceId.trim(), "pairingSecret" to pairingSecret.trim(), "command" to command)
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("sendRemoteCommand").call(data)
                .addOnSuccessListener { Toast.makeText(this, "Remote command sent", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { Toast.makeText(this, "Remote command failed: ${it.message}", Toast.LENGTH_LONG).show() }
        } catch (t: Throwable) {
            Toast.makeText(this, "Firebase is not configured yet", Toast.LENGTH_LONG).show()
        }
    }
}
