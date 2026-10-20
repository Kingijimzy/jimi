package com.jimi.app.agent

import android.content.Context
import com.jimi.app.memory.MemoryStore
import com.jimi.app.skills.SkillRegistry
import com.jimi.app.tools.PhoneTools
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * The brain of Jimi.
 * Local-first → Free APIs → Paid.
 */
class Agent(
    private val context: Context,
    private val onMessage: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val router = ModelRouter(context)
    private val phoneTools = PhoneTools(context)
    private val memory = MemoryStore(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var stopped = false

    fun stop() {
        stopped = true
    }

    fun run(userGoal: String) {
        stopped = false
        scope.launch {
            try {
                OrbStateManager.thinking()
                onStatus("Thinking...")

                val lower = userGoal.lowercase().trim()

                // 1. Instant local handling (no API cost)
                if (handleLocal(lower, userGoal)) {
                    OrbStateManager.idle()
                    onStatus("Ready")
                    return@launch
                }

                // 2. Skill trigger
                val skill = SkillRegistry.findByTrigger(lower)
                if (skill != null) {
                    onMessage("Activating skill: ${skill.name}. ${skill.description}")
                }

                // 3. Hybrid AI call
                val system = buildSystemPrompt()
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", userGoal))

                OrbStateManager.thinking()
                onStatus("Asking AI...")

                val raw = router.chat(messages, maxTokens = 450)
                if (stopped) return@launch

                OrbStateManager.speaking()
                onMessage(raw)

            } catch (e: Exception) {
                OrbStateManager.error()
                onMessage("Something went wrong: ${e.message}")
            } finally {
                OrbStateManager.idle()
                onStatus("Ready")
            }
        }
    }

    private fun handleLocal(lower: String, original: String): Boolean {
        // Greetings
        if (lower in listOf("hi", "hello", "hey", "hi jimi", "hello jimi", "hey jimi")) {
            onMessage("Hello. I'm Jimi, your personal assistant. How can I help you today?")
            return true
        }
        if (lower.contains("who are you") || lower.contains("what are you") || lower == "about") {
            onMessage("I'm Jimi — a hybrid AI assistant on your phone. I combine local intelligence with free and paid AI models. I can control the device, browse, research, code, and learn new skills.")
            return true
        }

        // Time / battery / device
        if (lower == "time" || lower.contains("what time") || lower.contains("what's the time")) {
            onMessage(phoneTools.currentTime())
            return true
        }
        if (lower.contains("battery")) {
            onMessage(phoneTools.batteryStatus())
            return true
        }
        if (lower.contains("device info") || lower == "about phone") {
            onMessage(phoneTools.deviceInfo())
            return true
        }

        // Memory
        if (lower.startsWith("remember ")) {
            val fact = original.removePrefix("remember ").removePrefix("Remember ").trim()
            val m = memory.add("fact", fact)
            onMessage(if (m != null) "Got it. I'll remember that." else "I couldn't save that.")
            return true
        }
        if (lower in listOf("what do you remember", "my memories", "show memories")) {
            val items = memory.all().take(8)
            onMessage(
                if (items.isEmpty()) "I don't have any long-term memories yet."
                else "Here's what I remember:\n" + items.joinToString("\n") { "• ${it.text}" }
            )
            return true
        }

        // Open app
        val openAppMatch = Regex("^(open|launch|start) (.+)$").find(lower)
        if (openAppMatch != null) {
            val name = openAppMatch.groupValues[2].trim()
            if (name !in listOf("settings", "wifi settings", "bluetooth settings")) {
                OrbStateManager.acting()
                onMessage(phoneTools.openApp(name))
                return true
            }
        }

        // Settings pages
        val settingsPages = listOf("wifi", "bluetooth", "display", "sound", "battery", "apps", "location", "accessibility")
        for (page in settingsPages) {
            if (lower.contains(page) && lower.contains("settings")) {
                OrbStateManager.acting()
                onMessage(phoneTools.openSettings(page))
                return true
            }
        }
        if (lower == "open settings" || lower == "settings") {
            OrbStateManager.acting()
            onMessage(phoneTools.openSettings("main"))
            return true
        }

        // Web search
        val searchMatch = Regex("^(search|google|look up|search for|google for) (.+)$").find(lower)
        if (searchMatch != null) {
            OrbStateManager.acting()
            onMessage(phoneTools.webSearch(searchMatch.groupValues[2].trim()))
            return true
        }

        // Open URL
        if (lower.startsWith("open http") || lower.startsWith("open www") || lower.startsWith("go to ")) {
            val url = original.substringAfter(" ").trim()
            OrbStateManager.acting()
            onMessage(phoneTools.openUrl(url))
            return true
        }

        // Volume
        if (lower in listOf("volume up", "turn it up", "louder")) {
            onMessage(phoneTools.setVolume("up"))
            return true
        }
        if (lower in listOf("volume down", "turn it down", "quieter")) {
            onMessage(phoneTools.setVolume("down"))
            return true
        }
        if (lower == "mute" || lower == "mute volume") {
            onMessage(phoneTools.setVolume("mute"))
            return true
        }
        if (lower == "unmute") {
            onMessage(phoneTools.setVolume("unmute"))
            return true
        }
        val volSet = Regex("^(set )?volume (to )?(\\d{1,3})$").find(lower)
        if (volSet != null) {
            onMessage(phoneTools.setVolume("set", volSet.groupValues[3].toInt()))
            return true
        }

        // Flashlight
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val off = lower.contains("off")
            onMessage(phoneTools.flashlight(!off))
            return true
        }

        // Dial
        val dialMatch = Regex("^(call|dial|phone) ([0-9 +\\-()]{5,})$").find(lower)
        if (dialMatch != null) {
            OrbStateManager.acting()
            onMessage(phoneTools.dial(dialMatch.groupValues[2]))
            return true
        }

        // Help
        if (lower == "help" || lower == "what can you do") {
            onMessage(
                "I can open apps, search the web, control volume and flashlight, " +
                "open settings, dial numbers, remember things, check time and battery, " +
                "and answer questions with AI. Add free API keys in Settings for full intelligence. " +
                "More control and skills are being added."
            )
            return true
        }

        return false
    }

    private fun buildSystemPrompt(): String {
        val mem = memory.promptBlock()
        return """
You are Jimi, a calm, sharp, and loyal personal AI assistant living on the user's Android phone.
Speak in short, natural spoken sentences. No markdown, no bullet lists unless the user asks for them.
You can help with reasoning, research, coding ideas, and planning phone tasks.
Prefer the safest path. Ask for confirmation before anything irreversible.
Current time: ${phoneTools.currentTime()}
Device: ${phoneTools.deviceInfo()}

Long-term memory:
$mem
""".trim()
    }
}
