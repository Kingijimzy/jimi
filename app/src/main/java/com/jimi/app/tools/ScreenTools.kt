package com.jimi.app.tools

import com.jimi.app.service.JimiAccessibilityService
import kotlinx.coroutines.runBlocking

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

    /**
     * Relative swipe from screen center.
     * direction: up | down | left | right
     */
    fun swipe(direction: String, distanceFraction: Float = 0.35f): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        val metrics = try {
            val dm = android.content.res.Resources.getSystem().displayMetrics
            dm.widthPixels to dm.heightPixels
        } catch (_: Exception) {
            1080 to 1920
        }
        val w = metrics.first.toFloat()
        val h = metrics.second.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val distX = w * distanceFraction
        val distY = h * distanceFraction

        val (x1, y1, x2, y2) = when (direction.lowercase()) {
            "up" -> listOf(cx, cy + distY / 2, cx, cy - distY / 2)
            "down" -> listOf(cx, cy - distY / 2, cx, cy + distY / 2)
            "left" -> listOf(cx + distX / 2, cy, cx - distX / 2, cy)
            "right" -> listOf(cx - distX / 2, cy, cx + distX / 2, cy)
            else -> return "Swipe direction must be up, down, left, or right."
        }

        return try {
            val ok = runBlocking { svc.swipe(x1, y1, x2, y2, 320) }
            if (ok) "Swiped $direction." else "Swipe failed."
        } catch (e: Exception) {
            "Swipe error: ${e.message}"
        }
    }

    suspend fun tap(x: Float, y: Float): String {
        val svc = JimiAccessibilityService.instance ?: return statusMessage()
        return if (svc.tap(x, y)) "Tapped at $x, $y." else "Tap failed."
    }
}
