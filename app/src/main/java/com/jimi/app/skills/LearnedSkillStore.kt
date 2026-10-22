package com.jimi.app.skills

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persist user-taught / AI-generated skills so they evolve across launches.
 * Skills are created from successful macros and can gain success/fail counts.
 */
object LearnedSkillStore {

    private const val PREFS = "jimi_learned_skills"
    private const val KEY = "skills_json"

    fun all(context: Context): List<Skill> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> parse(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, skill: Skill) {
        val list = all(context).toMutableList()
        list.removeAll { it.id == skill.id }
        list.add(skill)
        write(context, list)
        SkillRegistry.register(skill)
    }

    fun recordSuccess(context: Context, id: String) {
        val list = all(context).map {
            if (it.id == id) it.copy(successCount = it.successCount + 1, version = it.version + 1)
            else it
        }
        write(context, list)
        list.find { it.id == id }?.let { SkillRegistry.register(it) }
    }

    fun recordFail(context: Context, id: String) {
        val list = all(context).map {
            if (it.id == id) it.copy(failCount = it.failCount + 1)
            else it
        }
        write(context, list)
        list.find { it.id == id }?.let { SkillRegistry.register(it) }
    }

    /** Promote a successful macro into an evolving skill. */
    fun fromMacro(context: Context, macroName: String): Skill? {
        val macro = MacroStore.find(context, macroName) ?: return null
        val id = "learned_" + macroName.lowercase().replace(Regex("[^a-z0-9]+"), "_")
        val skill = Skill(
            id = id,
            name = macroName.replaceFirstChar { it.uppercase() },
            description = "Learned routine: $macroName (${macro.steps.size} steps). Improves with use.",
            category = "learned",
            version = 1,
            isAiGenerated = true,
            triggerPhrases = listOf(macroName.lowercase(), "run $macroName", "do $macroName"),
            steps = macro.steps.map {
                SkillStep(tool = it.tool, args = it.args, description = it.tool)
            },
            successCount = 1
        )
        save(context, skill)
        return skill
    }

    fun loadIntoRegistry(context: Context) {
        all(context).forEach { SkillRegistry.register(it) }
    }

    private fun write(context: Context, list: List<Skill>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("description", s.description)
                    .put("category", s.category)
                    .put("version", s.version)
                    .put("isAiGenerated", s.isAiGenerated)
                    .put("triggerPhrases", JSONArray(s.triggerPhrases))
                    .put("successCount", s.successCount)
                    .put("failCount", s.failCount)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    private fun parse(o: JSONObject): Skill? {
        return try {
            val phrases = o.optJSONArray("triggerPhrases") ?: JSONArray()
            val phraseList = (0 until phrases.length()).map { phrases.getString(it) }
            Skill(
                id = o.getString("id"),
                name = o.getString("name"),
                description = o.optString("description"),
                category = o.optString("category", "learned"),
                version = o.optInt("version", 1),
                isAiGenerated = o.optBoolean("isAiGenerated", true),
                triggerPhrases = phraseList,
                successCount = o.optInt("successCount", 0),
                failCount = o.optInt("failCount", 0)
            )
        } catch (_: Exception) {
            null
        }
    }
}
