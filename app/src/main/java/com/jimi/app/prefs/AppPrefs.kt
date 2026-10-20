package com.jimi.app.prefs

import android.content.Context

/**
 * Simple app preferences (TTS, confirmations, etc.).
 */
class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("jimi_prefs", Context.MODE_PRIVATE)

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("tts_enabled", true)
        set(value) = prefs.edit().putBoolean("tts_enabled", value).apply()

    var confirmSensitive: Boolean
        get() = prefs.getBoolean("confirm_sensitive", true)
        set(value) = prefs.edit().putBoolean("confirm_sensitive", value).apply()
}
