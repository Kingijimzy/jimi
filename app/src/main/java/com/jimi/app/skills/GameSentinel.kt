package com.jimi.app.skills

import android.content.Context
import com.jimi.app.service.JimiAccessibilityService
import com.jimi.app.tools.PhoneTools

/**
 * Game Sentinel — help with mobile games like FC Mobile.
 *
 * Realistic scope on a phone:
 * - Open the game
 * - Claim daily rewards / simple UI flows via macros
 * - Describe the screen (when vision is available)
 * - Record a sequence once and replay it (Grok Bot style teach-a-task)
 *
 * Full autonomous "play the match" requires vision + fast control and
 * is best done with recorded macros + occasional AI decisions.
 */
class GameSentinel(private val context: Context) {

    private val phone = PhoneTools(context)

    data class GameProfile(
        val name: String,
        val packageHints: List<String>,
        val commonActions: List<String>
    )

    private val profiles = listOf(
        GameProfile(
            name = "FC Mobile",
            packageHints = listOf("ea.gp.fifamobile", "fc", "fifa", "ea sports fc"),
            commonActions = listOf(
                "Claim daily rewards",
                "Open squad / team management",
                "Collect objectives",
                "Dismiss popups",
                "Go to home / lobby"
            )
        ),
        GameProfile(
            name = "Generic mobile game",
            packageHints = listOf("game"),
            commonActions = listOf(
                "Claim daily reward",
                "Close popups",
                "Collect resources"
            )
        )
    )

    fun findProfile(query: String): GameProfile? {
        val q = query.lowercase()
        return profiles.firstOrNull { p ->
            p.name.lowercase().contains(q) ||
                    p.packageHints.any { q.contains(it) } ||
                    q.contains("fc mobile") || q.contains("fifa")
        } ?: profiles.last()
    }

    /** Open the game if possible. */
    fun openGame(gameName: String = "FC Mobile"): String {
        val profile = findProfile(gameName)
        // Try common names
        val candidates = listOf("FC Mobile", "EA Sports FC", "FIFA", profile.name)
        for (name in candidates) {
            val result = phone.openApp(name)
            if (!result.contains("couldn't find")) return result
        }
        return "I couldn't find FC Mobile installed. Please install it or tell me the exact app name."
    }

    /** High-level help text for the user. */
    fun help(gameName: String = "FC Mobile"): String {
        val profile = findProfile(gameName)
        return buildString {
            append("Game Sentinel is ready for ${profile.name}.\n")
            append("I can:\n")
            append("• Open the game\n")
            append("• Help claim daily rewards (via taught macros)\n")
            append("• Record a sequence you show me once, then replay it\n")
            append("• Describe the screen when vision is available\n")
            append("\nSay: \"Open FC Mobile\" or \"Teach me daily rewards for FC Mobile\".")
        }
    }

    fun status(): String {
        val svc = JimiAccessibilityService.instance
        return if (svc != null) {
            "Accessibility is on. Game Sentinel can control the screen."
        } else {
            "Accessibility is off. Enable it in Settings so I can tap and swipe inside games."
        }
    }
}
