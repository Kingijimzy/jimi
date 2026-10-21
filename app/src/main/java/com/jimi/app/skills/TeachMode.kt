package com.jimi.app.skills

import android.content.Context

/**
 * Teach-a-task mode (Grok Bot style).
 * User performs actions; we capture steps into a macro.
 * Accessibility can also auto-record click_text from screen taps.
 */
object TeachMode {

    data class Session(
        val name: String,
        val steps: MutableList<MacroStore.MacroStep> = mutableListOf(),
        val startedAt: Long = System.currentTimeMillis()
    )

    @Volatile
    private var session: Session? = null

    @Volatile
    private var appContext: Context? = null

    fun bind(context: Context) {
        appContext = context.applicationContext
    }

    fun isRecording(): Boolean = session != null

    fun start(name: String): String {
        val clean = name.trim().ifEmpty { "macro_${System.currentTimeMillis()}" }
        session = Session(clean)
        return "Teaching started for \"$clean\". Perform taps on screen (or say open/tap commands), then: stop teaching."
    }

    fun addStep(step: MacroStore.MacroStep): Boolean {
        val s = session ?: return false
        val last = s.steps.lastOrNull()
        if (last != null && last.tool == step.tool && last.args == step.args) {
            return false
        }
        s.steps.add(step)
        return true
    }

    fun recordOpenApp(name: String) =
        addStep(MacroStore.MacroStep("open_app", mapOf("name" to name), 1500))

    fun recordClickText(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty() || t.length > 40) return false
        return addStep(MacroStore.MacroStep("click_text", mapOf("text" to t), 900))
    }

    fun recordWait(ms: Long = 1000) =
        addStep(MacroStore.MacroStep("wait", mapOf("ms" to ms), 200))

    fun recordHome() =
        addStep(MacroStore.MacroStep("home", emptyMap(), 600))

    fun recordBack() =
        addStep(MacroStore.MacroStep("back", emptyMap(), 600))

    fun stopAndSave(): String {
        val s = session ?: return "Not currently teaching."
        val ctx = appContext
        session = null
        if (s.steps.isEmpty()) return "No steps recorded. Teaching cancelled."
        if (ctx == null) return "Internal error: no context to save."
        val ok = MacroStore.save(ctx, s.name, s.steps)
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

    fun stepCount(): Int = session?.steps?.size ?: 0
}
