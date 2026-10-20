package com.jimi.app.tools

import com.jimi.app.service.JimiAccessibilityService

/**
 * Easy access to screen control for Agent and skills.
 */
object ScreenTools {

    fun isReady(): Boolean = JimiAccessibilityService.isEnabled()

    fun statusMessage(): String {
        return if (isReady()) {
            "Screen control is on. I can tap, swipe, and read the screen."
        } else {
            "Screen control is off. Enable Jimi in Settings → Accessibility to unlock full control."
        }
    }

    fun goHome(): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        return if (svc.goHome()) "Went home." else "Could not go home."
    }

    fun goBack(): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        return if (svc.goBack()) "Went back." else "Could not go back."
    }

    fun clickText(text: String): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        return if (svc.clickByText(text)) "Tapped \"$text\"." else "Could not find \"$text\" on screen."
    }

    fun screenSummary(): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        return svc.screenTextSummary()
    }

    suspend fun tap(x: Float, y: Float): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        return if (svc.tap(x, y)) "Tapped at $x, $y." else "Tap failed."
    }
}
