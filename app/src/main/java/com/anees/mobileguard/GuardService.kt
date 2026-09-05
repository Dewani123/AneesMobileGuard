package com.anees.mobileguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.bluetooth.BluetoothDevice
import android.media.AudioManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.content.pm.PackageManager
import android.os.Vibrator
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The heart of Guard Mode.
 *
 * WHAT ANDROID OFFICIALLY ALLOWS (and what this service relies on):
 *  - A foreground service with a visible notification can run continuously,
 *    including while the screen is off, as long as the user has armed it.
 *  - Apps CANNOT receive raw touch/tap events from other apps or the lock
 *    screen (no accessibility-style global touch interception is used here —
 *    that would require an Accessibility Service, which Play Store policy
 *    reserves for genuine accessibility purposes, not surveillance/anti-theft).
 *  - What IS available and used here instead:
 *      1) The accelerometer, to detect the phone being picked up, tilted or
 *         moved while Guard Mode is armed.
 *      2) The ACTION_SCREEN_ON broadcast, to detect the screen being woken
 *         (e.g. by the power button) while Guard Mode is armed.
 *  - A full-screen alarm Activity can legitimately be shown over the lock
 *    screen using setShowWhenLocked()/setTurnScreenOn(), the same official
 *    mechanism alarm-clock and phone-call apps use. It does NOT unlock the
 *    device — the phone stays locked underneath the alarm screen.
 */
class GuardService : Service(), SensorEventListener {

    private lateinit var settings: GuardSettings
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var powerReceiver: BroadcastReceiver? = null

    private var mediaPlayer: MediaPlayer? = null
    private var ttsHelper: TtsHelper? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var alarmActive = false
    private var lastAlertTime = 0L
    private var armingUntil = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recoverySync = object : Runnable {
        override fun run() {
            if (RemoteRescueManager.isLostMode(this@GuardService)) {
                FirebaseDeviceBridge.registerDevice(this@GuardService)
                LocationReporter.publishOnce(this@GuardService)
                mainHandler.postDelayed(this, 5 * 60 * 1000L)
            }
        }
    }
    private var cameraManager: CameraManager? = null
    private var flashCameraId: String? = null

    override fun onCreate() {
        super.onCreate()
        settings = GuardSettings(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        flashCameraId = try {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                chars?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { null }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_GUARDING -> startGuarding()
            ACTION_STOP_GUARDING -> stopGuarding()
            ACTION_STOP_LOST_MODE -> stopLostMode()
            ACTION_TRIGGER_ALARM -> triggerAlarm()
            ACTION_TRIGGER_AIRPLANE_ALARM -> triggerAlarm()
            ACTION_STOP_ALARM -> stopAlarmOnly()
            ACTION_REMOTE_LOST_MODE -> { SmartSecurityEvents.add(this, "🚨 Lost Mode protection started"); startLostMode() }
            else -> if (settings.guardModeOn) startGuarding()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------- Guard lifecycle ----------

    private fun startGuarding() {
        startForeground(NOTIF_ID, buildGuardingNotification())
        // Give the owner time to put the phone down after pressing Guard ON.
        armingUntil = System.currentTimeMillis() + settings.armingDelaySeconds * 1000L
        mainHandler.postDelayed({ registerGuardSensors() }, settings.armingDelaySeconds * 1000L)
    }

    private fun registerGuardSensors() {
        if (!settings.guardModeOn || alarmActive) return
        if (settings.triggerOnMotion && accelerometer != null && settings.guardModeOn) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (settings.triggerOnScreenOn) registerScreenReceiver()
        if (settings.bluetoothRangeGuardEnabled) registerBluetoothReceiver()
        if (settings.triggerOnChargerRemoval && settings.guardModeOn) registerPowerReceiver()
    }

    private fun startLostMode() {
        startForeground(NOTIF_ID, buildLostModeNotification())
        mainHandler.removeCallbacks(recoverySync)
        mainHandler.post(recoverySync)
    }

    private fun stopLostMode() {
        mainHandler.removeCallbacks(recoverySync)
        if (RemoteRescueManager.isLostMode(this)) {
            startForeground(NOTIF_ID, buildLostModeNotification())
        } else if (settings.guardModeOn) {
            startForeground(NOTIF_ID, buildGuardingNotification())
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopGuarding() {
        stopAlarmOnly()
        sensorManager.unregisterListener(this)
        unregisterScreenReceiver()
        unregisterBluetoothReceiver()
        unregisterPowerReceiver()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON && settings.guardModeOn &&
                    System.currentTimeMillis() >= armingUntil) {
                    triggerAlarm()
                }
            }
        }
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            try { unregisterReceiver(it) } catch (e: IllegalArgumentException) { /* already gone */ }
        }
        screenReceiver = null
    }

    // ---------- Bluetooth Smartwatch Range Guard ----------

    private fun registerBluetoothReceiver() {
        if (bluetoothReceiver != null) return

        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return
                if (!settings.guardModeOn || !settings.bluetoothRangeGuardEnabled) return

                val device = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                } catch (_: SecurityException) { null }

                val selected = settings.bluetoothGuardDeviceAddress?.trim()?.uppercase()
                val disconnected = try { device?.address?.uppercase() } catch (_: SecurityException) { null }

                // If a device was selected, only that device's disconnect triggers.
                // If no device is selected, any ACL disconnect can trigger the guard.
                if (selected.isNullOrEmpty() || selected == disconnected) {
                    triggerAlarm()
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(bluetoothReceiver, filter)
            }
        } catch (_: SecurityException) {
            bluetoothReceiver = null
        }
    }

    private fun unregisterBluetoothReceiver() {
        bluetoothReceiver?.let {
            try { unregisterReceiver(it) } catch (_: IllegalArgumentException) { }
        }
        bluetoothReceiver = null
    }

    // ---------- Charger removal detection ----------

    private fun registerPowerReceiver() {
        if (powerReceiver != null) return
        powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!settings.guardModeOn || !settings.triggerOnChargerRemoval) return
                if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
                    SmartSecurityEvents.add(context, "🚨 Charger removed while protection was active")
                    triggerAlarm()
                } else if (intent.action == Intent.ACTION_POWER_CONNECTED) {
                    SmartSecurityEvents.add(context, "🔌 Charger connected")
                }
            }
        }
        try {
            val f = IntentFilter().apply { addAction(Intent.ACTION_POWER_DISCONNECTED); addAction(Intent.ACTION_POWER_CONNECTED) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(powerReceiver, f, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(powerReceiver, f)
        } catch (_: Exception) { powerReceiver = null }
    }

    private fun unregisterPowerReceiver() {
        powerReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        powerReceiver = null
    }

    // ---------- Motion detection ----------

    override fun onSensorChanged(event: SensorEvent) {
        if (!settings.guardModeOn || alarmActive) return
        if (System.currentTimeMillis() < armingUntil) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)

        // Sensitivity 1 (least) .. 10 (most) maps to a movement threshold:
        // higher sensitivity = lower threshold = easier to trigger.
        val threshold = 8.0f - (settings.motionSensitivity * 0.6f)
        val now = System.currentTimeMillis()

        if (delta > threshold && now - lastAlertTime > 1500) {
            lastAlertTime = now
            SmartSecurityEvents.add(this, "🚨 Suspicious movement detected")
            triggerAlarm()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ---------- Alarm ----------

    private fun triggerAlarm() {
        if (alarmActive) return
        alarmActive = true

        acquireWakeLock()
        prepareAlarmAudio()
        playAlarmSound()
        if (settings.voiceWarningEnabled) {
            ttsHelper = TtsHelper(this, settings.voiceLanguageTag)
            ttsHelper?.startRepeating(settings.voiceWarningText, 6000L)
        }
        if (settings.vibrationOnAlarm) startVibration()
        if (settings.flashOnAlarm) setFlash(true)

        startForeground(NOTIF_ID, buildAlarmNotification())
        launchAlarmScreen()
    }

    private fun stopAlarmOnly() {
        if (!alarmActive) {
            return
        }
        alarmActive = false
        mediaPlayer?.let { try { it.stop(); it.release() } catch (e: Exception) { /* ignore */ } }
        mediaPlayer = null
        ttsHelper?.shutdown()
        ttsHelper = null
        if (settings.flashOnAlarm) setFlash(false)
        releaseWakeLock()
        if (RemoteRescueManager.isLostMode(this)) {
            startForeground(NOTIF_ID, buildLostModeNotification())
        } else if (settings.guardModeOn) {
            startForeground(NOTIF_ID, buildGuardingNotification())
        }
    }

    /** Best-effort alarm-channel escalation. Android/OEM/DND policy may still restrict it. */
    private fun prepareAlarmAudio() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Best effort: leave silent/vibrate mode for an owner-triggered anti-theft alarm.
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            } catch (_: SecurityException) { }

            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
        } catch (_: Exception) {
            // Never crash Guard Mode because an OEM blocks an audio operation.
        }
    }

    private fun playAlarmSound() {
        val soundUri: Uri = settings.alarmSoundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getValidRingtoneUri(this)

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@GuardService, soundUri)
                isLooping = true
                val volume = settings.alarmVolume / 100f
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            // If the chosen sound can't be played for any reason, fail safe by
            // doing nothing rather than crashing — the voice warning and the
            // visual alarm screen still fire.
        }
    }

    private fun launchAlarmScreen() {
        val intent = Intent(this, AlarmActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AneesGuard:AlarmWakeLock"
        )
        wakeLock?.acquire(5 * 60 * 1000L) // safety cap: 5 minutes
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }


    private fun startVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 500, 250, 500, 250)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun setFlash(enabled: Boolean) {
        val id = flashCameraId ?: return
        try {
            cameraManager?.setTorchMode(id, enabled)
        } catch (_: Exception) {}
    }

    // ---------- Notifications ----------

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GUARDING, getString(R.string.channel_guarding_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALARM, getString(R.string.channel_alarm_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun buildGuardingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_GUARDING)
            .setContentTitle(getString(R.string.notif_guarding_title))
            .setContentText(getString(R.string.notif_guarding_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildLostModeNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_GUARDING)
            .setContentTitle("Anees Mobile Guard — LOST MODE")
            .setContentText("Recovery protection is active. Location sync is running when available.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildAlarmNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setContentTitle(getString(R.string.notif_alarm_title))
            .setContentText(getString(R.string.notif_alarm_text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        unregisterScreenReceiver()
        unregisterBluetoothReceiver()
        unregisterPowerReceiver()
        mediaPlayer?.release()
        ttsHelper?.shutdown()
        if (settings.flashOnAlarm) setFlash(false)
        try {
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        } catch (_: Exception) {}
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.removeCallbacks(recoverySync)
        releaseWakeLock()
    }

    companion object {
        const val ACTION_REMOTE_LOST_MODE = "com.anees.mobileguard.action.REMOTE_LOST_MODE"
        const val ACTION_START_GUARDING = "com.anees.mobileguard.action.START_GUARDING"
        const val ACTION_STOP_GUARDING = "com.anees.mobileguard.action.STOP_GUARDING"
        const val ACTION_TRIGGER_ALARM = "com.anees.mobileguard.action.TRIGGER_ALARM"
        const val ACTION_TRIGGER_AIRPLANE_ALARM = "com.anees.mobileguard.action.TRIGGER_AIRPLANE_ALARM"
        const val ACTION_STOP_ALARM = "com.anees.mobileguard.action.STOP_ALARM"
        const val ACTION_STOP_LOST_MODE = "com.anees.mobileguard.action.STOP_LOST_MODE"

        private const val CHANNEL_GUARDING = "guarding_channel"
        private const val CHANNEL_ALARM = "alarm_channel"
        private const val NOTIF_ID = 1001
    }
}
