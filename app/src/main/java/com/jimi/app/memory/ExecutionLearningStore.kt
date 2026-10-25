package com.jimi.app.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small, privacy-conscious execution journal used to avoid repeating failed actions.
 * It stores tool names and normalized failure categories, never raw tool arguments or screen text.
 */
class ExecutionLearningStore(context: Context) {
    private val prefs = context.getSharedPreferences("jimi_execution_learning", Context.MODE_PRIVATE)

    data class Entry(
        val tool: String,
        val success: Boolean,
        val outcome: String,
        val at: Long
    )

    fun record(toolName: String, success: Boolean, message: String) {
        val tool = toolName.trim().lowercase().take(40).ifBlank { "unknown_tool" }
        val outcome = if (success) "accepted" else classifyFailure(message)
        val arr = load()
        arr.put(
            JSONObject()
                .put("tool", tool)
                .put("success", success)
                .put("outcome", outcome)
                .put("at", System.currentTimeMillis())
        )
        while (arr.length() > 30) arr.remove(0)
        prefs.edit().putString("entries", arr.toString()).apply()
    }

    fun recentFailures(limit: Int = 5): List<Entry> =
        all().filterNot { it.success }.takeLast(limit).reversed()

    fun promptBlock(): String {
        val failures = recentFailures()
        if (failures.isEmpty()) return "(no recorded tool failures)"
        return failures.joinToString("\n") {
            "- ${it.tool}: ${it.outcome}; do not repeat the same action unless the screen, target, or permissions have changed."
        }
    }

    fun all(): List<Entry> {
        val arr = load()
        return (0 until arr.length()).mapNotNull { i ->
            try {
                val o = arr.getJSONObject(i)
                Entry(
                    tool = o.optString("tool", "unknown_tool"),
                    success = o.optBoolean("success", false),
                    outcome = o.optString("outcome", "unknown"),
                    at = o.optLong("at")
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun clear() {
        prefs.edit().remove("entries").apply()
    }

    private fun classifyFailure(message: String): String {
        val text = message.lowercase()
        return when {
            "accessibility" in text -> "accessibility_unavailable"
            "permission" in text -> "permission_missing"
            "not found" in text || "couldn't find" in text -> "target_not_found"
            "timeout" in text || "timed out" in text -> "timeout"
            "rate limit" in text || "429" in text -> "provider_rate_limited"
            "unknown tool" in text -> "unsupported_tool"
            else -> "action_failed"
        }
    }

    private fun load(): JSONArray {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }
}
