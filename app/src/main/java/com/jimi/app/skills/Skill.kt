package com.jimi.app.skills

/**
 * A Skill is a reusable capability that Jimi can learn, improve, and call.
 * Skills can be built-in or AI-generated from examples.
 */
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val category: String,          // e.g. "phone", "browser", "research", "game", "network", "coding"
    val version: Int = 1,
    val isAiGenerated: Boolean = false,
    val triggerPhrases: List<String> = emptyList(),
    val steps: List<SkillStep> = emptyList(),
    val successCount: Int = 0,
    val failCount: Int = 0
)

data class SkillStep(
    val tool: String,
    val args: Map<String, Any?> = emptyMap(),
    val description: String = ""
)

/**
 * Registry of all skills (built-in + learned).
 * Later this will support saving/loading AI-generated skills.
 */
object SkillRegistry {

    private val skills = mutableListOf<Skill>()

    init {
        // Built-in starter skills
        register(
            Skill(
                id = "game_sentinel",
                name = "Game Sentinel",
                description = "Monitor and help with games: detect screen state, suggest moves, or automate simple actions.",
                category = "game",
                triggerPhrases = listOf("game help", "play this game", "watch the game", "game sentinel")
            )
        )
        register(
            Skill(
                id = "network_radar",
                name = "Network Radar",
                description = "Check network status, speed, diagnose connection issues, and report signal quality.",
                category = "network",
                triggerPhrases = listOf("network", "wifi status", "internet speed", "connection", "network radar")
            )
        )
        register(
            Skill(
                id = "research_agent",
                name = "Research Agent",
                description = "Search the web, read pages, summarize information, and answer research questions.",
                category = "research",
                triggerPhrases = listOf("research", "search for", "look up", "find information", "what is")
            )
        )
        register(
            Skill(
                id = "coding_assistant",
                name = "Coding Assistant",
                description = "Help write, explain, debug, and improve code. Can work with GitHub later.",
                category = "coding",
                triggerPhrases = listOf("code", "write code", "debug", "programming", "github")
            )
        )
    }

    fun register(skill: Skill) {
        skills.removeAll { it.id == skill.id }
        skills.add(skill)
    }

    fun findByTrigger(text: String): Skill? {
        val lower = text.lowercase()
        return skills.firstOrNull { skill ->
            skill.triggerPhrases.any { phrase -> lower.contains(phrase) }
        }
    }

    fun all(): List<Skill> = skills.toList()

    fun byCategory(category: String): List<Skill> =
        skills.filter { it.category.equals(category, ignoreCase = true) }
}
