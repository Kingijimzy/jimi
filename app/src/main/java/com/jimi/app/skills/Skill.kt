package com.jimi.app.skills

/**
 * A Skill is a reusable capability that Jimi can learn, improve, and call.
 */
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
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

object SkillRegistry {

    private val skills = mutableListOf<Skill>()

    init {
        register(
            Skill(
                id = "game_sentinel",
                name = "Game Sentinel",
                description = "Help with games like FC Mobile: open game, macros, daily rewards.",
                category = "game",
                triggerPhrases = listOf("game help", "play this game", "game sentinel", "fc mobile")
            )
        )
        register(
            Skill(
                id = "network_radar",
                name = "Network Radar",
                description = "Check network status and connection quality.",
                category = "network",
                triggerPhrases = listOf("network", "wifi status", "network radar")
            )
        )
        register(
            Skill(
                id = "research_agent",
                name = "Research Agent",
                description = "Search the web and open research results.",
                category = "research",
                triggerPhrases = listOf("research", "look up", "find information")
            )
        )
        register(
            Skill(
                id = "coding_assistant",
                name = "Coding Assistant",
                description = "Code help and GitHub search.",
                category = "coding",
                triggerPhrases = listOf("code help", "coding", "github")
            )
        )
        register(
            Skill(
                id = "browser_automation",
                name = "Browser Automation",
                description = "Open YouTube, Gmail, Chrome, URLs, and scroll pages when Accessibility is on.",
                category = "browser",
                triggerPhrases = listOf("youtube", "gmail", "browser", "scroll")
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
