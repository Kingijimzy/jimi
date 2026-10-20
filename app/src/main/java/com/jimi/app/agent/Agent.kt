package com.jimi.app.agent

import android.content.Context
import com.jimi.app.memory.MemoryStore
import com.jimi.app.skills.GameSentinel
import com.jimi.app.skills.MacroRunner
import com.jimi.app.skills.MacroStore
import com.jimi.app.skills.NetworkRadar
import com.jimi.app.skills.SkillRegistry
import com.jimi.app.tools.PhoneTools
import com.jimi.app.tools.ScreenTools
import com.jimi.app.tools.ToolRegistry
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class Agent(
    private val context: Context,
    private val onMessage: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val router = ModelRouter(context)
    private val phoneTools = PhoneTools(context)
    private val memory = MemoryStore(context)
    private val gameSentinel = GameSentinel(context)
    private val networkRadar = NetworkRadar(context)
    private val macroRunner = MacroRunner(context)
    private val tools = ToolRegistry(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var stopped = false

    init {
        macroRunner.seedFcDailyExample(context)
    }

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

                // Macro execution
                val runMacro = Regex("^(run|do|play|start) (macro |routine )?(.+)$").find(lower)
                if (runMacro != null) {
                    val name = runMacro.groupValues[3].trim()
                    if (MacroStore.find(context, name) != null || name.contains("fc") || name.contains("daily")) {
                        OrbStateManager.acting()
                        onStatus("Running macro...")
                        val result = macroRunner.run(name) { onStatus(it) }
                        onMessage(result)
                        return@launch
                    }
                }

                // Instant local (no API)
                if (handleLocal(lower, userGoal)) return@launch

                val skill = SkillRegistry.findByTrigger(lower)
                if (skill != null) {
                    onMessage("Activating skill: ${skill.name}. ${skill.description}")
                }

                // AI + tool loop (limited steps to protect free tier)
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", buildSystemPrompt()))
                    .put(JSONObject().put("role", "user").put("content", userGoal))

                var steps = 0
                val maxToolSteps = 4

                while (!stopped && steps < maxToolSteps) {
                    steps++
                    OrbStateManager.thinking()
                    onStatus(if (steps == 1) "Asking AI..." else "Continuing (step $steps)...")

                    val raw = router.chat(messages, maxTokens = 400)
                    if (stopped) break

                    val toolResult = tools.tryParseAndRun(raw)
                    if (toolResult != null) {
                        OrbStateManager.acting()
                        onStatus("Acting...")
                        onMessage(toolResult.message)
                        messages.put(JSONObject().put("role", "assistant").put("content", raw))
                        messages.put(
                            JSONObject().put("role", "user")
                                .put("content", "Tool result: ${toolResult.message}. Continue if needed, or finish with a short spoken reply.")
                        )
                        // If tool failed, let model respond once more
                        if (!toolResult.ok && steps >= maxToolSteps - 1) break
                        continue
                    }

                    // Plain language answer
                    OrbStateManager.speaking()
                    // Strip any accidental TOOL lines from spoken reply
                    val spoken = raw.lines()
                        .filterNot { it.trim().startsWith("TOOL:", ignoreCase = true) }
                        .joinToString("\n")
                        .trim()
                        .ifEmpty { raw }
                    onMessage(spoken)
                    break
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

    private fun handleLocal(lower: String, original: String): Boolean {
        if (lower in listOf("hi", "hello", "hey", "hi jimi", "hello jimi", "hey jimi")) {
            onMessage("Hello. I'm Jimi, your personal assistant. How can I help you today?")
            return true
        }
        if (lower.contains("who are you") || lower.contains("what are you") || lower == "about") {
            onMessage("I'm Jimi — hybrid AI on your phone. Device control, FC Mobile, macros, network checks, research, and more.")
            return true
        }

        if (lower == "time" || lower.contains("what time") || lower.contains("what's the time")) {
            onMessage(phoneTools.currentTime()); return true
        }
        if (lower.contains("battery") && !lower.contains("settings")) {
            onMessage(phoneTools.batteryStatus()); return true
        }
        if (lower.contains("device info") || lower == "about phone") {
            onMessage(phoneTools.deviceInfo()); return true
        }

        if (lower.contains("network") || lower.contains("wifi status") || lower.contains("internet status") ||
            lower == "wifi" || lower.contains("network radar") || lower.contains("connection status")) {
            onMessage(if (lower.contains("help")) networkRadar.help() else networkRadar.status())
            return true
        }

        if (lower in listOf("go home", "home screen", "press home")) {
            OrbStateManager.acting(); onMessage(ScreenTools.goHome()); return true
        }
        if (lower in listOf("go back", "press back", "back")) {
            OrbStateManager.acting(); onMessage(ScreenTools.goBack()); return true
        }
        if (lower.startsWith("tap ") || lower.startsWith("click ")) {
            val text = original.substringAfter(" ").trim()
            if (text.isNotBlank()) {
                OrbStateManager.acting(); onMessage(ScreenTools.clickText(text)); return true
            }
        }
        if (lower in listOf("read screen", "what is on screen", "screen text", "what's on screen")) {
            onMessage(ScreenTools.screenSummary()); return true
        }
        if (lower.contains("accessibility status") || lower == "screen control") {
            onMessage(ScreenTools.statusMessage()); return true
        }

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

        if (lower.contains("fc mobile") || lower.contains("fifa") || lower.contains("game sentinel") ||
            lower.contains("play fc") || lower.contains("open fc")) {
            if (lower.contains("open") || lower.contains("launch") || lower.contains("start") || lower.contains("play")) {
                OrbStateManager.acting()
                onMessage(gameSentinel.openGame("FC Mobile"))
                return true
            }
            onMessage(gameSentinel.help("FC Mobile") + "\n\n" + gameSentinel.status())
            return true
        }

        if (lower in listOf("list macros", "my macros", "show macros", "list routines")) {
            val names = MacroStore.list(context)
            onMessage(
                if (names.isEmpty()) "No macros yet. Sample 'fc daily' is created on first launch."
                else "Your macros: " + names.joinToString(", ")
            )
            return true
        }
        if (lower.startsWith("delete macro ") || lower.startsWith("remove macro ")) {
            val name = original.substringAfter("macro ").trim()
            onMessage(if (MacroStore.delete(context, name)) "Deleted macro \"$name\"." else "Macro not found.")
            return true
        }

        val openAppMatch = Regex("^(open|launch|start) (.+)$").find(lower)
        if (openAppMatch != null) {
            val name = openAppMatch.groupValues[2].trim()
            if (!name.contains("settings") && !name.startsWith("macro")) {
                OrbStateManager.acting()
                onMessage(phoneTools.openApp(name))
                return true
            }
        }

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

        val searchMatch = Regex("^(search|google|look up|search for|google for) (.+)$").find(lower)
        if (searchMatch != null) {
            OrbStateManager.acting()
            onMessage(phoneTools.webSearch(searchMatch.groupValues[2].trim()))
            return true
        }

        if (lower.startsWith("open http") || lower.startsWith("open www") || lower.startsWith("go to ")) {
            OrbStateManager.acting()
            onMessage(phoneTools.openUrl(original.substringAfter(" ").trim()))
            return true
        }

        if (lower in listOf("volume up", "turn it up", "louder")) {
            onMessage(phoneTools.setVolume("up")); return true
        }
        if (lower in listOf("volume down", "turn it down", "quieter")) {
            onMessage(phoneTools.setVolume("down")); return true
        }
        if (lower == "mute" || lower == "mute volume") {
            onMessage(phoneTools.setVolume("mute")); return true
        }
        if (lower == "unmute") {
            onMessage(phoneTools.setVolume("unmute")); return true
        }
        val volSet = Regex("^(set )?volume (to )?(\\d{1,3})$").find(lower)
        if (volSet != null) {
            onMessage(phoneTools.setVolume("set", volSet.groupValues[3].toInt()))
            return true
        }

        if (lower.contains("flashlight") || lower.contains("torch")) {
            onMessage(phoneTools.flashlight(!lower.contains("off")))
            return true
        }

        val dialMatch = Regex("^(call|dial|phone) ([0-9 +\\-()]{5,})$").find(lower)
        if (dialMatch != null) {
            OrbStateManager.acting()
            onMessage(phoneTools.dial(dialMatch.groupValues[2]))
            return true
        }

        if (lower == "help" || lower == "what can you do") {
            onMessage(
                "I can open apps, search, check network, control the screen, help with FC Mobile, " +
                "run macros, remember things, and use AI tools. Enable Accessibility for full control."
            )
            return true
        }

        return false
    }

    private fun buildSystemPrompt(): String {
        val mem = memory.promptBlock()
        return """
You are Jimi, a calm, sharp, loyal personal AI on the user's Android phone.
Speak in short natural sentences. No markdown unless asked.
Prefer safety. Ask before irreversible actions.

${tools.catalogForPrompt()}

Time: ${phoneTools.currentTime()}
Device: ${phoneTools.deviceInfo()}
Screen control: ${if (ScreenTools.isReady()) "on" else "off"}

Memory:
$mem
""".trim()
    }
}
