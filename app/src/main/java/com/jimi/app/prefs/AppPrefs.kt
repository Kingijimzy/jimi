package com.jimi.app.prefs

import android.content.Context

/**
 * Simple app preferences (TTS, confirmations, onboarding).
 */
class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("jimi_prefs", Context.MODE_PRIVATE)

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("tts_enabled", true)
        set(value) = prefs.edit().putBoolean("tts_enabled", value).apply()

    var confirmSensitive: Boolean
        get() = prefs.getBoolean("confirm_sensitive", true)
        set(value) = prefs.edit().putBoolean("confirm_sensitive", value).apply()

    /** Paid providers are never used unless the user explicitly enables them. */
    var allowPaidProviders: Boolean
        get() = prefs.getBoolean("allow_paid_providers", false)
        set(value) = prefs.edit().putBoolean("allow_paid_providers", value).apply()

    var onboardingDismissed: Boolean
        get() = prefs.getBoolean("onboarding_dismissed", false)
        set(value) = prefs.edit().putBoolean("onboarding_dismissed", value).apply()
}
