package com.jimi.app.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight task list so Jimi can track things to do.
 */
class TaskStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("jimi_tasks", Context.MODE_PRIVATE)

    data class Task(
        val id: Long,
        val text: String,
        val done: Boolean,
        val created: Long
    )

    fun add(text: String): Task? {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 300) return null
        val id = System.currentTimeMillis()
        val obj = JSONObject()
            .put("id", id)
            .put("text", clean)
            .put("done", false)
            .put("created", id)
        val arr = load()
        arr.put(obj)
        while (arr.length() > 40) arr.remove(0)
        save(arr)
        return Task(id, clean, false, id)
    }

    fun list(includeDone: Boolean = false): List<Task> {
        val arr = load()
        val out = mutableListOf<Task>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val done = o.optBoolean("done", false)
            if (!includeDone && done) continue
            out.add(
                Task(
                    o.getLong("id"),
                    o.optString("text"),
                    done,
                    o.optLong("created")
                )
            )
        }
        return out.sortedByDescending { it.created }
    }

    fun complete(query: String): Boolean {
        val arr = load()
        val q = query.lowercase()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!o.optBoolean("done") && o.optString("text").lowercase().contains(q)) {
                o.put("done", true)
                save(arr)
                return true
            }
        }
        return false
    }

    fun clearDone(): Int {
        val arr = load()
        var removed = 0
        for (i in arr.length() - 1 downTo 0) {
            if (arr.getJSONObject(i).optBoolean("done")) {
                arr.remove(i)
                removed++
            }
        }
        save(arr)
        return removed
    }

    fun promptBlock(): String {
        val open = list(false).take(8)
        if (open.isEmpty()) return "(no open tasks)"
        return open.joinToString("\n") { "- ${it.text}" }
    }

    private fun load(): JSONArray {
        val raw = prefs.getString("tasks", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    private fun save(arr: JSONArray) {
        prefs.edit().putString("tasks", arr.toString()).apply()
    }
}
