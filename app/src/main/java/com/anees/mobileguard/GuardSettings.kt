package com.anees.mobileguard

import android.content.Context

/** Thin typed wrapper around SharedPreferences for every user-facing setting. */
class GuardSettings(context: Context) {
    private val prefs = context.getSharedPreferences("anees_guard_settings", Context.MODE_PRIVATE)

    var guardModeOn: Boolean
        get() = prefs.getBoolean("guard_on", false)
        set(value) = prefs.edit().putBoolean("guard_on", value).apply()

    /** Content URI (as String) of the chosen alarm sound, or null = system default alarm. */
    var alarmSoundUri: String?
        get() = prefs.getString("alarm_sound_uri", null)
        set(value) = prefs.edit().putString("alarm_sound_uri", value).apply()

    /** 0-100 */
    var alarmVolume: Int
        get() = prefs.getInt("alarm_volume", 90)
        set(value) = prefs.edit().putInt("alarm_volume", value).apply()

    var voiceWarningEnabled: Boolean
        get() = prefs.getBoolean("voice_warning_enabled", true)
        set(value) = prefs.edit().putBoolean("voice_warning_enabled", value).apply()

    /** BCP-47 language tag, e.g. "en-US", "ar-SA". */
    var voiceLanguageTag: String
        get() = prefs.getString("voice_language_tag", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("voice_language_tag", value).apply()

    /** 1 (least sensitive) .. 10 (most sensitive) */
    var motionSensitivity: Int
        get() = prefs.getInt("motion_sensitivity", 5)
        set(value) = prefs.edit().putInt("motion_sensitivity", value).apply()

    var triggerOnScreenOn: Boolean
        get() = prefs.getBoolean("trigger_on_screen_on", false)
        set(value) = prefs.edit().putBoolean("trigger_on_screen_on", value).apply()

    var triggerOnMotion: Boolean
        get() = false
        set(value) = prefs.edit().putBoolean("trigger_on_motion", value).apply()

    /** If true AND guardModeOn was true at reboot, Guard Mode resumes automatically. */
    var autoRestartOnBoot: Boolean
        get() = prefs.getBoolean("auto_restart_on_boot", false)
        set(value) = prefs.edit().putBoolean("auto_restart_on_boot", value).apply()

    /** Seconds after arming before sensors become active, so the owner can place the phone. */
    var armingDelaySeconds: Int
        get() = prefs.getInt("arming_delay_seconds", 8)
        set(value) = prefs.edit().putInt("arming_delay_seconds", value.coerceIn(3, 30)).apply()

    /** User-editable TTS warning spoken during an alarm. */
    var voiceWarningText: String
        get() = prefs.getString("voice_warning_text", "Anees, someone is touching your mobile.")
            ?: "Anees, someone is touching your mobile."
        set(value) = prefs.edit().putString("voice_warning_text", value).apply()

    var flashOnAlarm: Boolean
        get() = prefs.getBoolean("flash_on_alarm", true)
        set(value) = prefs.edit().putBoolean("flash_on_alarm", value).apply()

    var vibrationOnAlarm: Boolean
        get() = prefs.getBoolean("vibration_on_alarm", true)
        set(value) = prefs.edit().putBoolean("vibration_on_alarm", value).apply()

    var lastKnownLatitude: Float
        get() = prefs.getFloat("last_latitude", Float.NaN)
        set(value) = prefs.edit().putFloat("last_latitude", value).apply()

    var lastKnownLongitude: Float
        get() = prefs.getFloat("last_longitude", Float.NaN)
        set(value) = prefs.edit().putFloat("last_longitude", value).apply()
    /** Enables Smartwatch/Bluetooth Range Guard. */
    var bluetoothRangeGuardEnabled: Boolean
        get() = prefs.getBoolean("bluetooth_range_guard_enabled", false)
        set(value) = prefs.edit().putBoolean("bluetooth_range_guard_enabled", value).apply()

    /** Optional MAC address of the selected smartwatch/Bluetooth device. */
    var bluetoothGuardDeviceAddress: String?
        get() = prefs.getString("bluetooth_guard_device_address", null)
        set(value) = prefs.edit().putString("bluetooth_guard_device_address", value).apply()

    /** Smart Anti-Theft sensitivity alias: 1 (low) .. 10 (high). */
    var smartSensitivity: Int
        get() = motionSensitivity
        set(value) { motionSensitivity = value.coerceIn(1, 10) }

    /** Whether charger removal should trigger the alarm while Guard Mode is active. */
    var triggerOnChargerRemoval: Boolean
        get() = false
        set(value) = prefs.edit().putBoolean("trigger_on_charger_removal", value).apply()

}

