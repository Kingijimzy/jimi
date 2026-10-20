package com.jimi.app.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Hybrid intelligence router.
 * Order: Local rules → Free APIs → Paid (DeepSeek).
 * Real HTTP calls with basic rate-limit awareness.
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
        Provider("groq", "Groq", "https://api.groq.com/openai/v1",
            "llama-3.3-70b-versatile", Tier.FREE),
        Provider("gemini", "Gemini Flash-Lite", "https://generativelanguage.googleapis.com/v1beta/openai",
            "gemini-2.0-flash-lite", Tier.FREE, hasVision = true),
        Provider("openrouter", "OpenRouter Free", "https://openrouter.ai/api/v1",
            "meta-llama/llama-3.3-70b-instruct:free", Tier.FREE),
        Provider("deepseek", "DeepSeek", "https://api.deepseek.com",
            "deepseek-chat", Tier.PAID)
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

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
            getKey(p.id).isNotEmpty()
        }
    }

    fun bestProvider(needVision: Boolean = false): Provider? {
        val available = availableProviders()
        if (available.isEmpty()) return null

        val free = available.filter { it.tier == Tier.FREE && (!needVision || it.hasVision) }
        if (free.isNotEmpty()) return free.first()

        return available.filter { it.tier == Tier.PAID && (!needVision || it.hasVision) }.firstOrNull()
    }

    suspend fun chat(
        messages: JSONArray,
        needVision: Boolean = false,
        maxTokens: Int = 500
    ): String = withContext(Dispatchers.IO) {
        val provider = bestProvider(needVision)
            ?: return@withContext "No AI provider configured yet. Open Settings and add a free API key (Groq or Gemini recommended)."

        val key = getKey(provider.id)
        val body = JSONObject()
            .put("model", provider.model)
            .put("messages", messages)
            .put("max_tokens", maxTokens)
            .put("temperature", 0.4)

        val request = Request.Builder()
            .url("${provider.baseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext when (response.code) {
                        401, 403 -> "${provider.name} rejected the key. Please check it in Settings."
                        429 -> "${provider.name} rate limit reached. Try again in a minute or add another free key."
                        else -> "${provider.name} error (${response.code}): ${raw.take(180)}"
                    }
                }
                extractContent(raw)
            }
        } catch (e: Exception) {
            "Could not reach ${provider.name}: ${e.message}"
        }
    }

    private fun extractContent(body: String): String {
        return try {
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices") ?: return "Empty response from model."
            if (choices.length() == 0) return "Empty response from model."
            val message = choices.getJSONObject(0).optJSONObject("message")
            message?.optString("content", "")?.trim()?.ifEmpty { "(no content)" }
                ?: "(no content)"
        } catch (e: Exception) {
            "Failed to parse model response."
        }
    }
}
