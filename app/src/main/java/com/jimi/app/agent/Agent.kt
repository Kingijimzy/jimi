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

                // 2. Skill trigger check
                val skill = SkillRegistry.findByTrigger(lower)
                if (skill != null) {
                    onMessage("Activating skill: ${skill.name}. ${skill.description}")
                    // Full skill execution will be expanded later
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
        when {
            lower in listOf("hi", "hello", "hey", "hi jimi", "hello jimi", "hey jimi") -> {
                onMessage("Hello. I'm Jimi, your personal assistant. How can I help you today?")
                return true
            }
            lower.contains("who are you") || lower.contains("what are you") || lower == "about" -> {
                onMessage("I'm Jimi — a hybrid AI assistant on your phone. I combine local intelligence with free and paid AI models. I can control the device, browse, research, code, and learn new skills.")
                return true
            }
            lower == "time" || lower.contains("what time") || lower.contains("what's the time") -> {
                onMessage(phoneTools.currentTime())
                return true
            }
            lower.contains("battery") -> {
                onMessage(phoneTools.batteryStatus())
                return true
            }
            lower.contains("device info") || lower == "about phone" -> {
                onMessage(phoneTools.deviceInfo())
                return true
            }
            lower.startsWith("remember ") -> {
                val fact = original.removePrefix("remember ").removePrefix("Remember ").trim()
                val m = memory.add("fact", fact)
                onMessage(if (m != null) "Got it. I'll remember that." else "I couldn't save that.")
                return true
            }
            lower == "what do you remember" || lower == "my memories" || lower == "show memories" -> {
                val items = memory.all().take(8)
                onMessage(
                    if (items.isEmpty()) "I don't have any long-term memories yet."
                    else "Here's what I remember:\n" + items.joinToString("\n") { "• ${it.text}" }
                )
                return true
            }
            lower == "help" || lower == "what can you do" -> {
                onMessage(
                    "I can answer questions, remember things, check time and battery, " +
                    "and soon control your phone, browse the web, research, code, and run skills " +
                    "like Game Sentinel and Network Radar. Add free API keys in Settings to unlock full intelligence."
                )
                return true
            }
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
