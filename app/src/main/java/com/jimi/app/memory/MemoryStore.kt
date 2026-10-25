package com.jimi.app.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple long-term memory store.
 * User can inspect, edit, and delete memories later in Settings.
 * Never stores passwords, OTPs, or highly sensitive data by default.
 */
class MemoryStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("jimi_memory", Context.MODE_PRIVATE)

    data class Memory(
        val id: Long,
        val category: String,   // preference, project, fact, task, decision
        val text: String,
        val created: Long,
        val updated: Long
    )

    fun add(category: String, text: String): Memory? {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 500) return null
        // Basic safety: reject obvious secrets
        val lower = clean.lowercase()
        if (listOf("password", "otp", "pin", "card number", "cvv").any { lower.contains(it) }) {
            return null
        }

        val id = System.currentTimeMillis()
        val now = id
        val obj = JSONObject()
            .put("id", id)
            .put("category", category)
            .put("text", clean)
            .put("created", now)
            .put("updated", now)

        val arr = loadArray()
        arr.put(obj)
        // Keep only last 80 memories for low-end devices
        while (arr.length() > 80) arr.remove(0)
        saveArray(arr)
        return Memory(id, category, clean, now, now)
    }

    fun all(): List<Memory> {
        val arr = loadArray()
        val list = mutableListOf<Memory>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Memory(
                    o.getLong("id"),
                    o.optString("category", "fact"),
                    o.optString("text"),
                    o.optLong("created"),
                    o.optLong("updated")
                )
            )
        }
        return list.sortedByDescending { it.updated }
    }

    fun search(query: String): List<Memory> {
        val q = query.lowercase()
        return all().filter {
            it.text.lowercase().contains(q) || it.category.lowercase().contains(q)
        }.take(12)
    }

    fun update(id: Long, category: String, text: String): Memory? {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 500) return null
        val lower = clean.lowercase()
        if (listOf("password", "otp", "pin", "card number", "cvv").any { lower.contains(it) }) return null
        val arr = loadArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optLong("id") == id) {
                val now = System.currentTimeMillis()
                o.put("category", category)
                o.put("text", clean)
                o.put("updated", now)
                saveArray(arr)
                return Memory(id, category, clean, o.optLong("created"), now)
            }
        }
        return null
    }

    fun delete(id: Long): Boolean {
        val arr = loadArray()
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).getLong("id") == id) {
                arr.remove(i)
                saveArray(arr)
                return true
            }
        }
        return false
    }

    fun promptBlock(): String {
        val items = all().take(12)
        if (items.isEmpty()) return "(no long-term memories yet)"
        return items.joinToString("\n") { "- [${it.category}] ${it.text}" }
    }

    private fun loadArray(): JSONArray {
        val raw = prefs.getString("memories", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    private fun saveArray(arr: JSONArray) {
        prefs.edit().putString("memories", arr.toString()).apply()
    }
}
