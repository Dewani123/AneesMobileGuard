package com.anees.mobileguard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinSetupActivity : AppCompatActivity() {

    private lateinit var pinManager: PinManager
    private lateinit var pinInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        pinManager = PinManager(this)
        pinInput = findViewById(R.id.editNewPin)
        confirmInput = findViewById(R.id.editConfirmPin)
        errorText = findViewById(R.id.textPinError)

        findViewById<Button>(R.id.buttonSavePin).setOnClickListener { onSaveClicked() }
    }

    private fun onSaveClicked() {
        val pin = pinInput.text.toString()
        val confirm = confirmInput.text.toString()

        if (pin.length < 4) {
            showError(getString(R.string.pin_too_short))
            return
        }
        if (pin != confirm) {
            showError(getString(R.string.pin_mismatch))
            return
        }

        pinManager.setPin(pin)
        Toast.makeText(this, getString(R.string.pin_saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = TextView.VISIBLE
    }
}
