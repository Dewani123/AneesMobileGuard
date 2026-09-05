package com.anees.mobileguard

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles creating, verifying and changing the Guard PIN.
 *
 * The PIN itself is NEVER stored. Only a random salt plus a SHA-256 hash of
 * (salt + PIN) is saved in this app's private SharedPreferences, which Android
 * sandboxes so no other app can read it without root. There is no way to
 * recover the original PIN from what is stored on disk.
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash)
            .putInt(KEY_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val correct = hashPin(pin, salt) == storedHash
        if (correct) resetAttempts() else registerFailedAttempt()
        return correct
    }

    private fun registerFailedAttempt() {
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockoutDuration(attempts))
        }
        editor.apply()
    }

    private fun resetAttempts() {
        prefs.edit().putInt(KEY_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
    }

    /** Milliseconds remaining before another PIN attempt is allowed (0 = not locked out). */
    fun lockoutRemainingMillis(): Long {
        val remaining = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L) - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    // Escalating lockout: 30s, 60s, 120s, 240s ... capped at 30 minutes,
    // to slow down repeated PIN guessing without permanently locking the owner out.
    private fun lockoutDuration(attempts: Int): Long {
        val extra = (attempts - MAX_ATTEMPTS_BEFORE_LOCKOUT).coerceAtMost(6)
        val multiplier = 1L shl extra
        return (30_000L * multiplier).coerceAtMost(30 * 60_000L)
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "anees_guard_secure_prefs"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 3
    }
}
