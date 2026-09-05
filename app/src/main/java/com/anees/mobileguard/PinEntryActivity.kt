package com.anees.mobileguard

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinEntryActivity : AppCompatActivity() {

    private lateinit var pinManager: PinManager
    private lateinit var settings: GuardSettings
    private lateinit var pinInput: EditText
    private lateinit var messageText: TextView
    private lateinit var confirmButton: Button

    private var purpose: String = PURPOSE_DISARM
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_entry)

        pinManager = PinManager(this)
        settings = GuardSettings(this)
        pinInput = findViewById(R.id.editPin)
        messageText = findViewById(R.id.textMessage)
        confirmButton = findViewById(R.id.buttonConfirmPin)

        purpose = intent.getStringExtra(EXTRA_PURPOSE) ?: PURPOSE_DISARM
        messageText.text = when (purpose) {
            PURPOSE_STOP_ALARM -> getString(R.string.enter_pin_stop_alarm)
            PURPOSE_OPEN_SETTINGS -> getString(R.string.enter_pin_settings)
            else -> getString(R.string.enter_pin_disarm)
        }

        confirmButton.setOnClickListener { onConfirm() }
        updateLockoutState()
    }

    private fun onConfirm() {
        if (pinManager.lockoutRemainingMillis() > 0) {
            Toast.makeText(this, getString(R.string.locked_out_wait), Toast.LENGTH_SHORT).show()
            return
        }

        val enteredPin = pinInput.text.toString()
        if (pinManager.verifyPin(enteredPin)) {
            onCorrectPin()
        } else {
            pinInput.text.clear()
            Toast.makeText(this, getString(R.string.incorrect_pin), Toast.LENGTH_SHORT).show()
            updateLockoutState()
        }
    }

    private fun onCorrectPin() {
        when (purpose) {
            PURPOSE_DISARM -> {
                settings.guardModeOn = false
                val serviceIntent = Intent(this, GuardService::class.java)
                serviceIntent.action = GuardService.ACTION_STOP_GUARDING
                startService(serviceIntent)
                finish()
            }
            PURPOSE_STOP_ALARM -> {
                val serviceIntent = Intent(this, GuardService::class.java)
                serviceIntent.action = GuardService.ACTION_STOP_ALARM
                startService(serviceIntent)
                // Also disarm Guard Mode after a real alarm, so the phone doesn't
                // immediately re-trigger while the owner is still handling it.
                settings.guardModeOn = false
                setResult(RESULT_OK)
                finish()
            }
            PURPOSE_OPEN_SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                finish()
            }
        }
    }

    private fun updateLockoutState() {
        val remaining = pinManager.lockoutRemainingMillis()
        countDownTimer?.cancel()
        if (remaining > 0) {
            confirmButton.isEnabled = false
            countDownTimer = object : CountDownTimer(remaining, 1000) {
                override fun onTick(msUntilFinished: Long) {
                    val seconds = (msUntilFinished / 1000) + 1
                    messageText.text = getString(R.string.locked_out_countdown, seconds)
                }
                override fun onFinish() {
                    confirmButton.isEnabled = true
                    messageText.text = getString(R.string.enter_pin_generic)
                }
            }.start()
        } else {
            confirmButton.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // While stopping a live alarm, the back button must not provide an
        // escape route around the PIN check.
        if (purpose == PURPOSE_STOP_ALARM) return
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_PURPOSE = "extra_purpose"
        const val PURPOSE_DISARM = "disarm"
        const val PURPOSE_STOP_ALARM = "stop_alarm"
        const val PURPOSE_OPEN_SETTINGS = "open_settings"
    }
}
