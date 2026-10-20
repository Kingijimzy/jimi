package com.jimi.app.skills

import android.content.Context

/**
 * Teach-a-task mode (Grok Bot style).
 * User performs actions; we capture steps into a macro.
 *
 * Current stage: API + storage ready.
 * Full automatic capture from Accessibility events comes next.
 */
class TeachMode(private val context: Context) {

    data class Session(
        val name: String,
        val steps: MutableList<MacroStore.MacroStep> = mutableListOf(),
        val startedAt: Long = System.currentTimeMillis()
    )

    @Volatile
    private var session: Session? = null

    fun isRecording(): Boolean = session != null

    fun start(name: String): String {
        val clean = name.trim().ifEmpty { "macro_${System.currentTimeMillis()}" }
        session = Session(clean)
        return "Teaching started for \"$clean\". Perform the steps, then say: stop teaching."
    }

    fun addStep(step: MacroStore.MacroStep): Boolean {
        val s = session ?: return false
        s.steps.add(step)
        return true
    }

    /** Convenience helpers while recording. */
    fun recordOpenApp(name: String) =
        addStep(MacroStore.MacroStep("open_app", mapOf("name" to name), 1500))

    fun recordClickText(text: String) =
        addStep(MacroStore.MacroStep("click_text", mapOf("text" to text), 900))

    fun recordWait(ms: Long = 1000) =
        addStep(MacroStore.MacroStep("wait", mapOf("ms" to ms), 200))

    fun recordHome() =
        addStep(MacroStore.MacroStep("home", emptyMap(), 600))

    fun recordBack() =
        addStep(MacroStore.MacroStep("back", emptyMap(), 600))

    fun stopAndSave(): String {
        val s = session ?: return "Not currently teaching."
        session = null
        if (s.steps.isEmpty()) return "No steps recorded. Teaching cancelled."
        val ok = MacroStore.save(context, s.name, s.steps)
        return if (ok) {
            "Saved macro \"${s.name}\" with ${s.steps.size} steps. Say: run macro ${s.name}"
        } else {
            "Failed to save macro."
        }
    }

    fun cancel(): String {
        session = null
        return "Teaching cancelled."
    }

    fun status(): String {
        val s = session ?: return "Not teaching."
        return "Teaching \"${s.name}\" — ${s.steps.size} steps so far."
    }
}
