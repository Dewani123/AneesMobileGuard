package com.anees.mobileguard

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.security.MessageDigest

/**
 * Privacy-first Firebase bridge. It stores only anti-theft device telemetry:
 * device ID, FCM token, app version, online/armed/lost state and optional last location.
 * It never reads contacts, SMS, photos, files, call logs or microphone data.
 */
object FirebaseDeviceBridge {
    private const val TAG = "AMG-Firebase"
    private const val COLLECTION = "devices"
    private const val PREFS = "firebase_device"
    private const val PAIRING = "pairing_secret"

    fun registerDevice(context: Context) {
        try {
            val auth = FirebaseAuth.getInstance()
            val afterAuth: () -> Unit = {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    val id = DeviceIdentity.getId(context)
                    val data = hashMapOf<String, Any>(
                        "deviceId" to id,
                        "ownerUid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                        "pairingSecretHash" to hashPairingSecret(pairingSecret(context)),
                        "fcmToken" to token,
                        "appVersion" to "2.0",
                        "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "batteryPercent" to currentBattery(context),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "guardMode" to GuardSettings(context).guardModeOn,
                        "lostMode" to RemoteRescueManager.isLostMode(context)
                    )
                    FirebaseFirestore.getInstance().collection(COLLECTION).document(id)
                        .set(data, com.google.firebase.firestore.SetOptions.merge())
                        .addOnFailureListener { Log.w(TAG, "Device registration failed", it) }
                }.addOnFailureListener { Log.w(TAG, "FCM token unavailable", it) }
            }
            if (auth.currentUser == null) {
                auth.signInAnonymously().addOnSuccessListener { afterAuth() }
                    .addOnFailureListener { Log.w(TAG, "Firebase auth unavailable", it) }
            } else afterAuth()
        } catch (t: Throwable) {
            // Firebase is optional until the developer supplies google-services.json.
            Log.i(TAG, "Firebase not configured yet")
        }
    }

    fun publishLocation(context: Context, latitude: Double, longitude: Double, accuracy: Float?) {
        try {
            val id = DeviceIdentity.getId(context)
            val data = hashMapOf<String, Any>(
                "lat" to latitude,
                "lng" to longitude,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            if (accuracy != null) data["accuracyM"] = accuracy
            FirebaseFirestore.getInstance().collection(COLLECTION).document(id)
                .set(mapOf("lastLocation" to data), com.google.firebase.firestore.SetOptions.merge())
        } catch (t: Throwable) {
            Log.i(TAG, "Location sync skipped")
        }
    }

    private fun currentBattery(context: Context): Int {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return level
    }

    fun pairingSecret(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(PAIRING, null)
        if (existing != null) return existing
        val secret = java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
        prefs.edit().putString(PAIRING, secret).apply()
        return secret
    }

    fun hashPairingSecret(secret: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
