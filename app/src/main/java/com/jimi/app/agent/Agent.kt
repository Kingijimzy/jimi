package com.jimi.app.agent

import android.content.Context
import com.jimi.app.tools.PhoneTools
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * The brain of Jimi.
 * Receives user goals, reasons, calls tools, and reports progress.
 */
class Agent(
    private val context: Context,
    private val onMessage: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val router = ModelRouter(context)
    private val phoneTools = PhoneTools(context)
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

                // 1. Very simple local classification first (will expand)
                val lower = userGoal.lowercase().trim()
                if (handleLocal(lower)) {
                    OrbStateManager.idle()
                    return@launch
                }

                // 2. Full agent loop (hybrid)
                val system = buildSystemPrompt()
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", userGoal))

                var steps = 0
                val maxSteps = 12  // Keep free tiers alive longer

                while (!stopped && steps < maxSteps) {
                    steps++
                    OrbStateManager.thinking()
                    onStatus("Reasoning (step $steps)...")

                    val raw = router.chat(messages)
                    if (stopped) break

                    // For now just speak the response (tool calling will be added next)
                    OrbStateManager.speaking()
                    onMessage(raw)
                    break // temporary until full tool loop is ready
                }

                if (steps >= maxSteps) {
                    onMessage("I reached my step limit for this free session. Tell me to continue if needed.")
                }
            } catch (e: Exception) {
                OrbStateManager.error()
                onMessage("Something went wrong: ${e.message}")
            } finally {
                OrbStateManager.idle()
                onStatus("Ready")
            }
        }
    }

    private fun handleLocal(text: String): Boolean {
        // Instant local responses (no API)
        when {
            text in listOf("hi", "hello", "hey", "hi jimi", "hello jimi") -> {
                onMessage("Hello. I'm Jimi, your personal assistant. How can I help you today?")
                return true
            }
            text.contains("who are you") || text.contains("what are you") -> {
                onMessage("I'm Jimi — a hybrid AI assistant living on your phone. I can control the device, browse, research, code, and learn new skills over time.")
                return true
            }
            text == "time" || text.contains("what time") -> {
                onMessage(phoneTools.currentTime())
                return true
            }
            text.contains("battery") -> {
                onMessage(phoneTools.batteryStatus())
                return true
            }
        }
        return false
    }

    private fun buildSystemPrompt(): String {
        return """
You are Jimi, a calm, sharp, and loyal personal AI assistant living on the user's Android phone.
You speak in short, natural sentences. No markdown, no lists unless asked.
You can control the phone, browse the web, research, write code, and learn new skills.
Always prefer the safest path. Ask for confirmation before irreversible actions.
Current time: ${phoneTools.currentTime()}
""".trim()
    }
}
