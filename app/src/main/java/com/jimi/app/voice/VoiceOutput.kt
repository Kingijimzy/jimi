package com.jimi.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Text-to-speech for Jimi replies.
 */
class VoiceOutput(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var enabled: Boolean = true

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(1.02f)
            tts?.setPitch(1.0f)
        }
    }

    fun speak(text: String) {
        if (!enabled || !ready) return
        val clean = text.trim()
        if (clean.isEmpty()) return
        // Avoid reading long tool dumps
        val clipped = if (clean.length > 400) clean.take(400) + "..." else clean
        tts?.speak(clipped, TextToSpeech.QUEUE_FLUSH, null, "jimi_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        ready = false
    }
}
