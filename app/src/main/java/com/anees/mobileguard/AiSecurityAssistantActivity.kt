package com.anees.mobileguard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Privacy-first local Security Assistant.
 * It answers security questions from on-device app state only.
 * It does not read contacts, SMS, photos, files, microphone, clipboard, or browser data.
 */
class AiSecurityAssistantActivity : AppCompatActivity() {
    private lateinit var settings: GuardSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_security_assistant)
        settings = GuardSettings(this)

        val input = findViewById<EditText>(R.id.aiInput)
        val answer = findViewById<TextView>(R.id.aiAnswer)
        val ask = findViewById<Button>(R.id.aiAsk)

        ask.setOnClickListener {
            val question = input.text.toString().trim()
            answer.text = respond(question)
        }

        findViewById<Button>(R.id.aiStatus).setOnClickListener {
            answer.text = respond("is my phone secure")
        }
        findViewById<Button>(R.id.aiLost).setOnClickListener {
            answer.text = respond("how do I use lost mode")
        }
        findViewById<Button>(R.id.aiPrivacy).setOnClickListener {
            answer.text = respond("privacy")
        }
    }

    private fun respond(raw: String): String {
        val q = raw.lowercase()
        val lost = RemoteRescueManager.isLostMode(this)
        val guard = settings.guardModeOn
        val events = SmartSecurityEvents.get(this)
        val battery = getBatteryPercent()
        val location = if (events.contains("location", ignoreCase = true)) "A recent location event is available." else "No location event is recorded yet."

        return when {
            q.isBlank() -> "Ask me about protection status, Lost Mode, suspicious activity, location, battery, or privacy."
            q.contains("secure") || q.contains("status") || q.contains("safe") ->
                if (lost) "Your phone is in Lost Mode. Remote Rescue protection is active."
                else if (guard) "Protection is ACTIVE. Guard Mode is monitoring the device. Battery: $battery%."
                else "Protection is currently OFF. Turn Guard Mode ON when you want local anti-theft monitoring."
            q.contains("lost") || q.contains("rescue") ->
                "Lost Mode is ${if (lost) "ACTIVE" else "OFF"}. Open Remote Rescue to pair the device and manage authorized remote commands."
            q.contains("alarm") ->
                "The security alarm can be tested from the main dashboard. If Guard Mode detects configured suspicious activity, the alarm service can respond."
            q.contains("movement") || q.contains("motion") || q.contains("touch") ->
                "Movement protection is ${if (guard) "active while Guard Mode is ON" else "available after you arm Guard Mode"}."
            q.contains("location") || q.contains("where") -> location + " Location access is used only for anti-theft features after Android permission is granted."
            q.contains("battery") -> "Current battery level: $battery%."
            q.contains("event") || q.contains("suspicious") || q.contains("activity") ->
                if (events.isBlank()) "No security events are recorded yet." else "Latest security events:\n$events"
            q.contains("privacy") || q.contains("personal") || q.contains("data") ->
                "Privacy mode: the assistant uses only Mobile Guard security state stored on this device. It does not access contacts, SMS, photos, files, microphone recordings, clipboard, passwords, or browser history."
            q.contains("help") || q.contains("how") ->
                "Try: 'Is my phone secure?', 'How does Lost Mode work?', 'Show suspicious activity', 'Where is my last location?', or 'What data do you use?'"
            else -> "I can help with Mobile Guard security status, alarms, movement detection, Lost Mode, location, battery, events, and privacy."
        }
    }

    private fun getBatteryPercent(): Int {
        val intent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return if (level >= 0) level else 0
    }
}
