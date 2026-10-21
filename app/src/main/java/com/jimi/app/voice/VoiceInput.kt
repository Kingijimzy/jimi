package com.jimi.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * On-device speech to text.
 * SpeechRecognizer MUST be created and used on the main thread (Xiaomi/Redmi is strict).
 */
class VoiceInput(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListening: () -> Unit = {},
    private val onDone: () -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        mainHandler.post {
            startOnMain()
        }
    }

    private fun startOnMain() {
        if (!isAvailable()) {
            onError("Speech recognition not available. Install Google app / speech services.")
            return
        }
        stopOnMain()
        try {
            val sr = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            recognizer = sr
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListening()
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Mic audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Voice client error — try again"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow Microphone for Jimi in app permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Voice needs internet (Google speech)"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy — wait a second"
                        SpeechRecognizer.ERROR_SERVER -> "Speech server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                        10 -> "Too many voice requests — wait a moment" // ERROR_TOO_MANY_REQUESTS on newer APIs
                        11 -> "Language not supported"
                        12 -> "Server disconnected"
                        else -> "Voice error ($error). Check mic permission and Google speech."
                    }
                    onError(msg)
                    onDone()
                }

                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull()?.trim().orEmpty()
                    if (text.isNotEmpty()) onResult(text) else onError("Didn't catch that.")
                    onDone()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            sr.startListening(intent)
        } catch (e: Exception) {
            onError("Could not start voice: ${e.message}")
            onDone()
        }
    }

    fun stop() {
        mainHandler.post { stopOnMain() }
    }

    private fun stopOnMain() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }
}
