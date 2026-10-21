package com.jimi.app.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jimi.app.tools.PhoneTools
import com.jimi.app.tools.ScreenTools

/**
 * Browser Automation — open sites, YouTube, Gmail, and simple in-page actions when Accessibility is on.
 */
class BrowserAutomation(private val context: Context) {

    private val phone = PhoneTools(context)

    fun help(): String {
        return "Browser automation: open YouTube, open Gmail, open Chrome, go to a URL, " +
                "search YouTube for ..., or with Accessibility: scroll down / tap text on page."
    }

    fun openYoutube(query: String? = null): String {
        return try {
            if (!query.isNullOrBlank()) {
                val url = "https://www.youtube.com/results?search_query=" + Uri.encode(query.trim())
                phone.openUrl(url)
            } else {
                // Prefer app, fall back to web
                val app = phone.openApp("YouTube")
                if (app.contains("couldn't find", true)) phone.openUrl("https://www.youtube.com")
                else app
            }
        } catch (e: Exception) {
            "Could not open YouTube: ${e.message}"
        }
    }

    fun openGmail(): String {
        val app = phone.openApp("Gmail")
        if (!app.contains("couldn't find", true)) return app
        // mailto scheme opens email app chooser
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened email app."
        } catch (_: Exception) {
            phone.openUrl("https://mail.google.com")
        }
    }

    fun openChrome(): String = phone.openApp("Chrome").let {
        if (it.contains("couldn't find", true)) phone.openUrl("https://www.google.com") else it
    }

    fun goTo(url: String): String = phone.openUrl(url)

    fun scrollDown(): String {
        if (!ScreenTools.isReady()) return ScreenTools.statusMessage()
        return ScreenTools.swipe(direction = "down")
    }

    fun scrollUp(): String {
        if (!ScreenTools.isReady()) return ScreenTools.statusMessage()
        return ScreenTools.swipe(direction = "up")
    }
}
