package com.jimi.app.agent

import android.content.Context
import com.jimi.app.memory.MemoryStore
import com.jimi.app.memory.TaskStore
import com.jimi.app.prefs.AppPrefs
import com.jimi.app.skills.GameSentinel
import com.jimi.app.skills.MacroRunner
import com.jimi.app.skills.MacroStore
import com.jimi.app.skills.NetworkRadar
import com.jimi.app.skills.ResearchAgent
import com.jimi.app.skills.SkillRegistry
import com.jimi.app.skills.TeachMode
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
    private val tasks = TaskStore(context)
    private val prefs = AppPrefs(context)
    private val gameSentinel = GameSentinel(context)
    private val networkRadar = NetworkRadar(context)
    private val researchAgent = ResearchAgent(context)
    private val macroRunner = MacroRunner(context)
    private val teachMode = TeachMode(context)
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

                val runMacro = Regex("^(run|do|play|start) (macro |routine )?(.+)$").find(lower)
                if (runMacro != null) {
                    val name = runMacro.groupValues[3].trim()
                    if (name != "teaching" && (MacroStore.find(context, name) != null || name.contains("fc") || name.contains("daily"))) {
                        OrbStateManager.acting()
                        onStatus("Running macro...")
                        onMessage(macroRunner.run(name) { onStatus(it) })
                        return@launch
                    }
                }

                if (handleLocal(lower, userGoal)) return@launch

                val skill = SkillRegistry.findByTrigger(lower)
                if (skill != null && skill.id == "research_agent") {
                    // Handled below via research regex; keep soft notice only for other skills
                } else if (skill != null) {
                    onMessage("Activating skill: ${skill.name}. ${skill.description}")
                }

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
                        if (!toolResult.ok && steps >= maxToolSteps - 1) break
                        continue
                    }

                    OrbStateManager.speaking()
                    val spoken = raw.lines()
                        .filterNot { it.trim().startsWith("TOOL:", ignoreCase = true) }
                        .joinToString("\n").trim().ifEmpty { raw }
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
            onMessage("I'm Jimi — hybrid AI on your phone. Device control, FC Mobile, teach-a-task macros, research, tasks, network, and more.")
            return true
        }

        if (lower.startsWith("teach ") || lower.startsWith("start teaching ") || lower == "start teaching") {
            val name = when {
                lower.startsWith("start teaching ") -> original.substringAfter("teaching ").trim()
                lower.startsWith("teach ") -> original.substringAfter(" ").trim()
                else -> "custom"
            }.ifEmpty { "custom" }
            onMessage(teachMode.start(name))
            return true
        }
        if (lower in listOf("stop teaching", "finish teaching", "save teaching", "end teaching")) {
            onMessage(teachMode.stopAndSave())
            return true
        }
        if (lower == "cancel teaching") {
            onMessage(teachMode.cancel())
            return true
        }
        if (lower == "teaching status") {
            onMessage(teachMode.status())
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

        // Research
        val researchMatch = Regex("^(research|look up|find information on|find info on) (.+)$").find(lower)
        if (researchMatch != null) {
            OrbStateManager.acting()
            onMessage(researchAgent.research(researchMatch.groupValues[2].trim()))
            return true
        }
        if (lower == "research help" || lower == "research agent") {
            onMessage(researchAgent.help())
            return true
        }

        // Skills list
        if (lower in listOf("list skills", "my skills", "show skills", "skills")) {
            val all = SkillRegistry.all()
            onMessage(
                "Skills:\n" + all.joinToString("\n") { "• ${it.name} — ${it.description.take(60)}" }
            )
            return true
        }

        if (lower.startsWith("add task ") || lower.startsWith("todo ") || lower.startsWith("remind me to ")) {
            val text = when {
                lower.startsWith("add task ") -> original.substringAfter(" ").substringAfter(" ").trim()
                lower.startsWith("todo ") -> original.substringAfter(" ").trim()
                else -> original.substringAfter("to ").trim()
            }
            val t = tasks.add(text)
            onMessage(if (t != null) "Added task: ${t.text}" else "Could not add that task.")
            return true
        }
        if (lower in listOf("my tasks", "list tasks", "show tasks", "tasks")) {
            val open = tasks.list(false)
            onMessage(
                if (open.isEmpty()) "No open tasks."
                else "Open tasks:\n" + open.joinToString("\n") { "• ${it.text}" }
            )
            return true
        }
        if (lower.startsWith("done task ") || lower.startsWith("complete task ") || lower.startsWith("finish task ")) {
            val q = original.substringAfter("task ").trim()
            onMessage(if (tasks.complete(q)) "Marked done." else "I couldn't find that task.")
            return true
        }

        if (lower.contains("network") || lower.contains("wifi status") || lower.contains("internet status") ||
            lower == "wifi" || lower.contains("network radar") || lower.contains("connection status")) {
            onMessage(if (lower.contains("help")) networkRadar.help() else networkRadar.status())
            return true
        }

        if (lower in listOf("go home", "home screen", "press home")) {
            if (teachMode.isRecording()) teachMode.recordHome()
            OrbStateManager.acting(); onMessage(ScreenTools.goHome()); return true
        }
        if (lower in listOf("go back", "press back", "back")) {
            if (teachMode.isRecording()) teachMode.recordBack()
            OrbStateManager.acting(); onMessage(ScreenTools.goBack()); return true
        }
        if (lower.startsWith("tap ") || lower.startsWith("click ")) {
            val text = original.substringAfter(" ").trim()
            if (text.isNotBlank()) {
                if (teachMode.isRecording()) teachMode.recordClickText(text)
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
                if (teachMode.isRecording()) teachMode.recordOpenApp("FC Mobile")
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
                if (names.isEmpty()) "No macros yet. Sample 'fc daily' is created on first launch. Or say: teach daily rewards"
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
            if (!name.contains("settings") && !name.startsWith("macro") && name != "teaching") {
                if (teachMode.isRecording()) teachMode.recordOpenApp(name)
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
            val number = dialMatch.groupValues[2]
            if (prefs.confirmSensitive) {
                PendingActionBus.request(
                    PendingAction(
                        title = "Confirm call",
                        message = "Open dialer with $number?",
                        action = { phoneTools.dial(number) }
                    )
                )
                onMessage("Confirm to open the dialer with $number.")
            } else {
                OrbStateManager.acting()
                onMessage(phoneTools.dial(number))
            }
            return true
        }

        if (lower == "help" || lower == "what can you do") {
            onMessage(
                "I can open apps, research, network, screen control, FC Mobile, " +
                "teach macros, tasks, memory, voice, and AI. Say: list skills. Enable Accessibility for full control."
            )
            return true
        }

        return false
    }

    private fun buildSystemPrompt(): String {
        return """
You are Jimi, a calm, sharp, loyal personal AI on the user's Android phone.
Speak in short natural sentences. No markdown unless asked.
Prefer safety. Ask before irreversible actions.

${tools.catalogForPrompt()}

Time: ${phoneTools.currentTime()}
Device: ${phoneTools.deviceInfo()}
Screen control: ${if (ScreenTools.isReady()) "on" else "off"}
Teaching: ${if (teachMode.isRecording()) teachMode.status() else "off"}

Memory:
${memory.promptBlock()}

Open tasks:
${tasks.promptBlock()}
""".trim()
    }
}
