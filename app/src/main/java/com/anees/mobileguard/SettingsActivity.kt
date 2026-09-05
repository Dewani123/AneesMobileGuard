package com.anees.mobileguard

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: GuardSettings
    private lateinit var soundLabel: TextView

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) settings.flashOnAlarm = false
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            settings.alarmSoundUri = uri?.toString()
            updateSoundLabel()
        }
    }

    // Supported voice-warning languages. Actual availability depends on the
    // TTS language packs installed on the device; TtsHelper falls back
    // gracefully if a chosen language isn't installed.
    private val languages = listOf(
        "en-US" to "English",
        "ar-SA" to "Arabic",
        "ur-PK" to "Urdu",
        "fr-FR" to "French"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = GuardSettings(this)

        soundLabel = findViewById(R.id.textAlarmSoundValue)
        updateSoundLabel()
        findViewById<Button>(R.id.buttonPickSound).setOnClickListener { pickAlarmSound() }

        setupSeekBar(R.id.seekVolume, settings.alarmVolume) { settings.alarmVolume = it }
        setupSeekBar(R.id.seekSensitivity, settings.motionSensitivity - 1, max = 9) {
            settings.motionSensitivity = it + 1
        }

        setupSwitch(R.id.switchVoiceWarning, settings.voiceWarningEnabled) { settings.voiceWarningEnabled = it }
        setupSwitch(R.id.switchTriggerMotion, settings.triggerOnMotion) { settings.triggerOnMotion = it }
        setupSwitch(R.id.switchTriggerScreenOn, settings.triggerOnScreenOn) { settings.triggerOnScreenOn = it }
        setupSwitch(R.id.switchAutoRestartBoot, settings.autoRestartOnBoot) { settings.autoRestartOnBoot = it }
        setupSwitch(R.id.switchFlashAlarm, settings.flashOnAlarm) { enabled ->
            if (enabled && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                settings.flashOnAlarm = enabled
            }
        }
        setupSwitch(R.id.switchVibrationAlarm, settings.vibrationOnAlarm) { settings.vibrationOnAlarm = it }
        setupSwitch(R.id.switchBluetoothRangeGuard, settings.bluetoothRangeGuardEnabled) { enabled ->
            settings.bluetoothRangeGuardEnabled = enabled
        }

        setupArmingDelay()
        findViewById<Button>(R.id.buttonVoiceMessage).setOnClickListener { editVoiceMessage() }
        setupLanguageSpinner()

        findViewById<Button>(R.id.buttonChangePin).setOnClickListener {
            startActivity(Intent(this, PinSetupActivity::class.java))
        }

        findViewById<Button>(R.id.buttonTestAlarm).setOnClickListener {
            val serviceIntent = Intent(this, GuardService::class.java)
            serviceIntent.action = GuardService.ACTION_TRIGGER_ALARM
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, R.string.test_alarm_started, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupArmingDelay() {
        val seek = findViewById<SeekBar>(R.id.seekArmingDelay)
        seek.max = 27
        seek.progress = settings.armingDelaySeconds - 3
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) settings.armingDelaySeconds = progress + 3
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun editVoiceMessage() {
        val input = EditText(this)
        input.setText(settings.voiceWarningText)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        AlertDialog.Builder(this)
            .setTitle(R.string.voice_message_dialog_title)
            .setMessage(R.string.voice_message_dialog_subtitle)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save_message) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) settings.voiceWarningText = value
            }.show()
    }

    private fun setupSeekBar(id: Int, initial: Int, max: Int? = null, onChange: (Int) -> Unit) {
        val seekBar = findViewById<SeekBar>(id)
        max?.let { seekBar.max = it }
        seekBar.progress = initial
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChange(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSwitch(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        findViewById<Switch>(id).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        }
    }

    private fun setupLanguageSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerLanguage)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.second }
        )
        val currentIndex = languages.indexOfFirst { it.first == settings.voiceLanguageTag }.coerceAtLeast(0)
        spinner.setSelection(currentIndex)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                settings.voiceLanguageTag = languages[position].first
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun pickAlarmSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        settings.alarmSoundUri?.let {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun updateSoundLabel() {
        val uriString = settings.alarmSoundUri
        soundLabel.text = if (uriString == null) {
            getString(R.string.default_alarm_sound)
        } else {
            try {
                RingtoneManager.getRingtone(this, Uri.parse(uriString))?.getTitle(this)
                    ?: getString(R.string.custom_alarm_sound)
            } catch (e: Exception) {
                getString(R.string.custom_alarm_sound)
            }
        }
    }
}
