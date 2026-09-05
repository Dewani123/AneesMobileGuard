package com.anees.mobileguard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * The full-screen alarm warning. Shown on top of everything, including the
 * lock screen, using the same official APIs alarm-clock apps use
 * (setShowWhenLocked / setTurnScreenOn on API 27+, window flags below that).
 * Importantly, this does NOT unlock the device — the keyguard remains active
 * underneath this screen, so no security is bypassed.
 *
 * The only way off this screen is entering the correct PIN.
 */
class AlarmActivity : AppCompatActivity() {

    private val stopAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            finish()
        }
        // Wrong PIN or cancelled entry: this screen stays up and the alarm keeps ringing.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContentView(R.layout.activity_alarm)

        findViewById<Button>(R.id.buttonEnterPin).setOnClickListener {
            val intent = Intent(this, PinEntryActivity::class.java)
            intent.putExtra(PinEntryActivity.EXTRA_PURPOSE, PinEntryActivity.PURPOSE_STOP_ALARM)
            stopAlarmLauncher.launch(intent)
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally does nothing: the alarm must not be dismissible without
        // the correct PIN, per the app's security requirements.
    }
}
