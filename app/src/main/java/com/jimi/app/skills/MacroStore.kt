package com.jimi.app.skills

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Grok Bot inspired "teach-a-task":
 * User shows a sequence once → Jimi saves it as a macro → replay forever with zero AI cost.
 *
 * Perfect for:
 * - FC Mobile daily rewards
 * - Claim objectives
 * - Dismiss popups
 * - Any repeated phone flow
 */
object MacroStore {

    data class MacroStep(
        val tool: String,           // open_app, tap, swipe, wait, click_text, key...
        val args: Map<String, Any?>,
        val delayMs: Long = 600
    )

    data class Macro(
        val name: String,
        val steps: List<MacroStep>,
        val created: Long = System.currentTimeMillis(),
        val runCount: Int = 0
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences("jimi_macros", Context.MODE_PRIVATE)

    fun save(context: Context, name: String, steps: List<MacroStep>): Boolean {
        if (name.isBlank() || steps.isEmpty()) return false
        val arr = JSONArray()
        steps.forEach { step ->
            val argsObj = JSONObject()
            step.args.forEach { (k, v) -> argsObj.put(k, v) }
            arr.put(
                JSONObject()
                    .put("tool", step.tool)
                    .put("args", argsObj)
                    .put("delayMs", step.delayMs)
            )
        }
        val root = JSONObject()
            .put("name", name.trim())
            .put("steps", arr)
            .put("created", System.currentTimeMillis())
            .put("runCount", 0)

        val all = loadAllRaw(context)
        // Replace if same name
        var replaced = false
        for (i in 0 until all.length()) {
            if (all.getJSONObject(i).optString("name").equals(name, ignoreCase = true)) {
                all.put(i, root)
                replaced = true
                break
            }
        }
        if (!replaced) all.put(root)
        prefs(context).edit().putString("macros", all.toString()).apply()
        return true
    }

    fun list(context: Context): List<String> {
        val all = loadAllRaw(context)
        val names = mutableListOf<String>()
        for (i in 0 until all.length()) {
            names.add(all.getJSONObject(i).optString("name"))
        }
        return names
    }

    fun find(context: Context, name: String): Macro? {
        val all = loadAllRaw(context)
        for (i in 0 until all.length()) {
            val o = all.getJSONObject(i)
            if (o.optString("name").equals(name, ignoreCase = true)) {
                return parseMacro(o)
            }
        }
        // Fuzzy
        for (i in 0 until all.length()) {
            val o = all.getJSONObject(i)
            if (o.optString("name").lowercase().contains(name.lowercase())) {
                return parseMacro(o)
            }
        }
        return null
    }

    fun delete(context: Context, name: String): Boolean {
        val all = loadAllRaw(context)
        for (i in 0 until all.length()) {
            if (all.getJSONObject(i).optString("name").equals(name, ignoreCase = true)) {
                all.remove(i)
                prefs(context).edit().putString("macros", all.toString()).apply()
                return true
            }
        }
        return false
    }

    private fun loadAllRaw(context: Context): JSONArray {
        val raw = prefs(context).getString("macros", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    private fun parseMacro(o: JSONObject): Macro {
        val stepsArr = o.optJSONArray("steps") ?: JSONArray()
        val steps = mutableListOf<MacroStep>()
        for (i in 0 until stepsArr.length()) {
            val s = stepsArr.getJSONObject(i)
            val argsObj = s.optJSONObject("args") ?: JSONObject()
            val args = mutableMapOf<String, Any?>()
            argsObj.keys().forEach { key -> args[key] = argsObj.get(key) }
            steps.add(
                MacroStep(
                    tool = s.optString("tool"),
                    args = args,
                    delayMs = s.optLong("delayMs", 600)
                )
            )
        }
        return Macro(
            name = o.optString("name"),
            steps = steps,
            created = o.optLong("created"),
            runCount = o.optInt("runCount")
        )
    }
}
