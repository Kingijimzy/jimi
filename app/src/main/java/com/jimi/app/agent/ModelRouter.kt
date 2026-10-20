package com.jimi.app.agent

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hybrid intelligence router.
 * Order of preference:
 * 1. Local rules / macros (unlimited)
 * 2. Free APIs (Groq, Gemini Flash-Lite, etc.)
 * 3. Paid APIs (DeepSeek later)
 *
 * Designed so most everyday tasks finish before free rate limits are hit.
 */
class ModelRouter(private val context: Context) {

    enum class Tier { LOCAL, FREE, PAID }

    data class Provider(
        val id: String,
        val name: String,
        val baseUrl: String,
        val model: String,
        val tier: Tier,
        val hasVision: Boolean = false
    )

    private val providers = listOf(
        // Free tier - fast first
        Provider("groq", "Groq", "https://api.groq.com/openai/v1",
            "llama-3.3-70b-versatile", Tier.FREE),
        Provider("gemini", "Gemini Flash-Lite", "https://generativelanguage.googleapis.com/v1beta/openai",
            "gemini-flash-lite-latest", Tier.FREE, hasVision = true),
        Provider("openrouter", "OpenRouter Free", "https://openrouter.ai/api/v1",
            "meta-llama/llama-3.3-70b-instruct:free", Tier.FREE),
        // Paid - ready for later
        Provider("deepseek", "DeepSeek", "https://api.deepseek.com",
            "deepseek-chat", Tier.PAID)
    )

    // Simple key storage (will move to encrypted prefs later)
    fun getKey(providerId: String): String {
        val prefs = context.getSharedPreferences("jimi_keys", Context.MODE_PRIVATE)
        return prefs.getString("key_$providerId", "") ?: ""
    }

    fun setKey(providerId: String, key: String) {
        context.getSharedPreferences("jimi_keys", Context.MODE_PRIVATE)
            .edit()
            .putString("key_$providerId", key.trim())
            .apply()
    }

    fun availableProviders(): List<Provider> {
        return providers.filter { p ->
            when (p.tier) {
                Tier.LOCAL -> true
                Tier.FREE, Tier.PAID -> getKey(p.id).isNotEmpty()
            }
        }
    }

    /**
     * Returns the best provider for the current request.
     * Prefer free/fast providers first.
     */
    fun bestProvider(needVision: Boolean = false): Provider? {
        val available = availableProviders()
        if (available.isEmpty()) return null

        // Prefer free first, then paid
        val free = available.filter { it.tier == Tier.FREE && (!needVision || it.hasVision) }
        if (free.isNotEmpty()) return free.first()

        val paid = available.filter { it.tier == Tier.PAID && (!needVision || it.hasVision) }
        return paid.firstOrNull()
    }

    // Placeholder for actual chat call - will be expanded
    suspend fun chat(
        messages: JSONArray,
        needVision: Boolean = false,
        maxTokens: Int = 600
    ): String {
        val provider = bestProvider(needVision)
            ?: return "No AI provider configured yet. Please add a free API key in Settings."

        // TODO: Real HTTP call with OkHttp + rate limit handling
        // For now return a clear message so UI works
        return "[Jimi using ${provider.name}] Intelligence layer is connecting. Full hybrid routing coming next."
    }
}
