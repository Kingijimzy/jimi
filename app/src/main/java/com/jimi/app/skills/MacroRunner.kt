package com.jimi.app.skills

import android.content.Context
import com.jimi.app.service.JimiAccessibilityService
import com.jimi.app.tools.PhoneTools
import kotlinx.coroutines.delay

/**
 * Executes saved macros (Grok Bot style teach-a-task).
 * Successful runs are promoted into evolving learned skills.
 */
class MacroRunner(private val context: Context) {

    private val phone = PhoneTools(context)

    suspend fun run(name: String, onProgress: (String) -> Unit = {}): String {
        val macro = MacroStore.find(context, name)
            ?: return "Macro \"$name\" not found. Say 'list macros' to see saved ones."

        val svc = JimiAccessibilityService.instance
        if (svc == null) {
            return "Accessibility is off. Enable Jimi in Accessibility settings so macros can control the screen."
        }

        onProgress("Running \"${macro.name}\" (${macro.steps.size} steps)...")

        for ((index, step) in macro.steps.withIndex()) {
            onProgress("Step ${index + 1}/${macro.steps.size}: ${step.tool}")
            val ok = executeStep(svc, step)
            if (!ok) {
                val skillId = "learned_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_")
                LearnedSkillStore.recordFail(context, skillId)
                return "Macro stopped at step ${index + 1} (${step.tool}). Screen may have changed."
            }
            delay(step.delayMs.coerceIn(200, 5000))
        }

        // Evolve: promote macro → learned skill + bump success count
        val skill = LearnedSkillStore.fromMacro(context, name)
        val extra = if (skill != null) {
            " Skill \"${skill.name}\" v${skill.version} learned (success ${skill.successCount})."
        } else ""
        return "Macro \"${macro.name}\" finished successfully.$extra"
    }

    suspend fun runSkill(skill: Skill, onProgress: (String) -> Unit = {}): String {
        if (skill.steps.isEmpty()) return "Skill \"${skill.name}\" has no saved steps and cannot run yet."
        val svc = JimiAccessibilityService.instance
            ?: return "Accessibility is off. Enable Jimi in Accessibility settings before running \"${skill.name}\"."
        onProgress("Running learned skill \"${skill.name}\" (${skill.steps.size} steps)...")
        for ((index, step) in skill.steps.withIndex()) {
            onProgress("Step ${index + 1}/${skill.steps.size}: ${step.description.ifBlank { step.tool }}")
            val ok = executeStep(svc, MacroStore.MacroStep(step.tool, step.args, 600))
            if (!ok) {
                LearnedSkillStore.recordFail(context, skill.id)
                return "Skill \"${skill.name}\" stopped at step ${index + 1} (${step.tool}). The action was not confirmed; inspect the screen before retrying."
            }
            delay(600)
        }
        LearnedSkillStore.recordSuccess(context, skill.id)
        return "Skill \"${skill.name}\" completed its saved steps. This confirms the actions were accepted, not that every external app reached its final goal."
    }

    private suspend fun executeStep(
        svc: JimiAccessibilityService,
        step: MacroStore.MacroStep
    ): Boolean {
        return when (step.tool.lowercase()) {
            "open_app" -> {
                val name = step.args["name"]?.toString() ?: return false
                actionMessageSucceeded(phone.openApp(name))
            }
            "open_url" -> {
                val url = step.args["url"]?.toString() ?: return false
                actionMessageSucceeded(phone.openUrl(url))
            }
            "tap" -> {
                val x = (step.args["x"] as? Number)?.toFloat() ?: return false
                val y = (step.args["y"] as? Number)?.toFloat() ?: return false
                svc.tap(x, y)
            }
            "swipe" -> {
                val x1 = (step.args["x1"] as? Number)?.toFloat() ?: return false
                val y1 = (step.args["y1"] as? Number)?.toFloat() ?: return false
                val x2 = (step.args["x2"] as? Number)?.toFloat() ?: return false
                val y2 = (step.args["y2"] as? Number)?.toFloat() ?: return false
                svc.swipe(x1, y1, x2, y2)
            }
            "click_text" -> {
                val text = step.args["text"]?.toString() ?: return false
                svc.clickByText(text)
            }
            "type" -> {
                val text = step.args["text"]?.toString() ?: return false
                svc.typeText(text)
            }
            "back" -> svc.goBack()
            "home" -> svc.goHome()
            "wait" -> {
                val ms = (step.args["ms"] as? Number)?.toLong() ?: 1000L
                delay(ms.coerceIn(100, 10000))
                true
            }
            else -> true
        }
    }

    private fun actionMessageSucceeded(message: String): Boolean {
        val lower = message.lowercase()
        val failureMarkers = listOf("couldn't", "could not", "failed", "failure", "not found", "not available", "unavailable", "error:")
        return failureMarkers.none { lower.contains(it) }
    }

    fun seedFcDailyExample(context: Context) {
        if (MacroStore.find(context, "fc daily") != null) return
        MacroStore.save(
            context,
            "fc daily",
            listOf(
                MacroStore.MacroStep("open_app", mapOf("name" to "FC Mobile"), 2500),
                MacroStore.MacroStep("wait", mapOf("ms" to 3000), 500),
                MacroStore.MacroStep("click_text", mapOf("text" to "Claim"), 1200),
                MacroStore.MacroStep("click_text", mapOf("text" to "Collect"), 1200),
                MacroStore.MacroStep("click_text", mapOf("text" to "OK"), 800),
                MacroStore.MacroStep("click_text", mapOf("text" to "Close"), 800)
            )
        )
    }
}
