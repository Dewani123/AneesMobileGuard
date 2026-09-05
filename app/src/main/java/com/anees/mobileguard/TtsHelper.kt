package com.anees.mobileguard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Wraps Android's built-in, on-device TextToSpeech engine to repeat the
 * spoken warning while the alarm is active. This only SPEAKS using the
 * device's TTS engine — it never records audio and never sends anything
 * off the device.
 */
class TtsHelper(context: Context, languageTag: String) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.forLanguageTag(languageTag)
                val result = tts?.setLanguage(locale)
                ready = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                // Fall back silently to the engine's default language if the
                // requested one isn't installed — speech still occurs.
                if (!ready) {
                    tts?.setLanguage(Locale.getDefault())
                    ready = true
                }
            }
        }
    }

    private fun speakOnce(text: String) {
        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "anees_warning")
        }
    }

    /** Speaks [text] immediately, then again every [intervalMs] until [stopRepeating] is called. */
    fun startRepeating(text: String, intervalMs: Long) {
        stopRepeating()
        val runnable = object : Runnable {
            override fun run() {
                speakOnce(text)
                handler.postDelayed(this, intervalMs)
            }
        }
        repeatRunnable = runnable
        handler.post(runnable)
    }

    fun stopRepeating() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }

    fun shutdown() {
        stopRepeating()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
