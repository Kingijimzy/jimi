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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hybrid intelligence router.
 * Gemini uses native Google AI Studio API.
 * Groq / OpenRouter / DeepSeek use OpenAI-compatible chat completions.
 */
class ModelRouter(private val context: Context) {

    enum class Tier { FREE, PAID }
    enum class ApiStyle { OPENAI, GEMINI_NATIVE }

    data class Provider(
        val id: String,
        val name: String,
        val baseUrl: String,
        val models: List<String>,
        val tier: Tier,
        val style: ApiStyle = ApiStyle.OPENAI,
        val hasVision: Boolean = false
    )

    private val providers = listOf(
        Provider(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
            tier = Tier.FREE
        ),
        Provider(
            id = "gemini",
            name = "Gemini (Google AI Studio)",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            models = listOf(
                "gemini-2.0-flash-lite",
                "gemini-2.0-flash",
                "gemini-1.5-flash",
                "gemini-1.5-flash-latest"
            ),
            tier = Tier.FREE,
            style = ApiStyle.GEMINI_NATIVE,
            hasVision = true
        ),
        Provider(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            models = listOf(
                "google/gemma-2-9b-it:free",
                "meta-llama/llama-3.2-3b-instruct:free",
                "qwen/qwen-2.5-7b-instruct:free",
                "mistralai/mistral-7b-instruct:free"
            ),
            tier = Tier.FREE
        ),
        Provider(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            models = listOf("deepseek-chat"),
            tier = Tier.PAID
        )
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun getKey(providerId: String): String {
        val prefs = context.getSharedPreferences("jimi_keys", Context.MODE_PRIVATE)
        return prefs.getString("key_$providerId", "")?.trim().orEmpty()
    }

    fun setKey(providerId: String, key: String) {
        context.getSharedPreferences("jimi_keys", Context.MODE_PRIVATE)
            .edit()
            .putString("key_$providerId", key.trim())
            .apply()
    }

    fun availableProviders(): List<Provider> =
        providers.filter { getKey(it.id).isNotEmpty() }

    fun bestProvider(needVision: Boolean = false): Provider? {
        val available = availableProviders()
        if (available.isEmpty()) return null
        val free = available.filter { it.tier == Tier.FREE && (!needVision || it.hasVision) }
        if (free.isNotEmpty()) {
            return free.firstOrNull { it.id == "gemini" }
                ?: free.firstOrNull { it.id == "groq" }
                ?: free.first()
        }
        return available.firstOrNull { it.tier == Tier.PAID && (!needVision || it.hasVision) }
    }

    suspend fun chat(
        messages: JSONArray,
        needVision: Boolean = false,
        maxTokens: Int = 500
    ): String = withContext(Dispatchers.IO) {
        val primary = bestProvider(needVision)
            ?: return@withContext "No AI provider configured. Open Settings and paste a free key from Google AI Studio (Gemini) or Groq."

        // Use one primary provider. Escalate to another configured free provider only for HTTP 429.
        val candidates = buildList {
            add(primary)
            addAll(availableProviders().filter {
                it.id != primary.id && it.tier == Tier.FREE && (!needVision || it.hasVision)
            })
        }.distinctBy { it.id }
        var rateLimitMessage: String? = null
        for (provider in candidates) {
            val key = getKey(provider.id)
            if (key.isEmpty()) continue
            val result = when (provider.style) {
                ApiStyle.GEMINI_NATIVE -> chatGemini(provider, key, messages, maxTokens)
                ApiStyle.OPENAI -> chatOpenAiStyle(provider, key, messages, maxTokens)
            }
            if (result.startsWith("RATE_LIMITED:")) {
                rateLimitMessage = result.removePrefix("RATE_LIMITED:").trim()
                continue
            }
            return@withContext result
        }
        rateLimitMessage ?: "No configured provider could complete this request."
    }

    private fun chatGemini(
        provider: Provider,
        key: String,
        messages: JSONArray,
        maxTokens: Int
    ): String {
        val contents = JSONArray()
        var systemText = ""
        for (i in 0 until messages.length()) {
            val m = messages.getJSONObject(i)
            val role = m.optString("role")
            val content = m.optString("content")
            when (role) {
                "system" -> systemText = content
                "user" -> contents.put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", content)))
                )
                "assistant" -> contents.put(
                    JSONObject()
                        .put("role", "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", content)))
                )
            }
        }
        if (contents.length() == 0) return "Nothing to send to Gemini."

        var lastError = ""
        for (model in provider.models) {
            val body = JSONObject()
                .put("contents", contents)
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("maxOutputTokens", maxTokens)
                        .put("temperature", 0.4)
                )
            if (systemText.isNotBlank()) {
                body.put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemText))
                    )
                )
            }

            val url =
                "${provider.baseUrl}/models/$model:generateContent?key=${java.net.URLEncoder.encode(key, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()

            try {
                val (code, raw) = execute(request)
                if (code in 200..299) return extractGemini(raw)

                lastError = geminiError(code, raw, model)
                when {
                    code == 400 && raw.contains("API key", true) ->
                        return "Gemini rejected the API key. Get a free key at aistudio.google.com/apikey and paste it under Gemini in Settings."
                    code == 429 ->
                        return "RATE_LIMITED: Gemini HTTP 429 rate limit. Trying the next configured free provider."
                    code == 401 || code == 403 ->
                        return "Gemini key invalid or blocked. Create a new key in Google AI Studio."
                    code == 404 || raw.contains("NOT_FOUND", true) ->
                        return "Gemini model $model was not found. No alternate model was tried."
                    else -> return lastError
                }
            } catch (e: Exception) {
                return "Could not reach Gemini: ${e.message}"
            }
        }
        return lastError.ifEmpty { "Gemini failed for all models." }
    }

    private fun chatOpenAiStyle(
        provider: Provider,
        key: String,
        messages: JSONArray,
        maxTokens: Int
    ): String {
        var lastError = ""
        for (model in provider.models) {
            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("max_tokens", maxTokens)
                .put("temperature", 0.4)

            val builder = Request.Builder()
                .url("${provider.baseUrl}/chat/completions")
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))

            if (provider.id == "openrouter") {
                builder.addHeader("HTTP-Referer", "https://github.com/Kingijimzy/jimi")
                builder.addHeader("X-Title", "Jimi")
            }

            try {
                val (code, raw) = execute(builder.build())
                if (code in 200..299) return extractOpenAi(raw)

                lastError = when (code) {
                    401, 403 -> "${provider.name} rejected the key. Check Settings."
                    402 -> "${provider.name} wants payment for model $model."
                    429 -> "RATE_LIMITED: ${provider.name} HTTP 429 rate limit. Trying the next configured free provider."
                    else -> "${provider.name} ($model) error $code: ${raw.take(180)}"
                }

                // Do not rotate models on ordinary failures. Provider fallback is only for HTTP 429.
                return lastError
            } catch (e: Exception) {
                return "Could not reach ${provider.name}: ${e.message}"
            }
        }
        return if (provider.id == "openrouter") {
            "OpenRouter free models unavailable ($lastError). Prefer Gemini or Groq free keys."
        } else {
            lastError.ifEmpty { "${provider.name} failed." }
        }
    }

    private val requestCounterLock = Any()

    fun requestCountToday(): Int {
        val prefs = context.getSharedPreferences("jimi_request_budget", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return if (prefs.getString("day", "") == today) prefs.getInt("count", 0) else 0
    }

    private fun recordRequest() {
        val prefs = context.getSharedPreferences("jimi_request_budget", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        synchronized(requestCounterLock) {
            val oldDay = prefs.getString("day", "")
            val oldCount = if (oldDay == today) prefs.getInt("count", 0) else 0
            prefs.edit().putString("day", today).putInt("count", oldCount + 1).apply()
        }
    }

    private fun execute(request: Request): Pair<Int, String> {
        recordRequest()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            return response.code to raw
        }
    }

    private fun geminiError(code: Int, raw: String, model: String): String {
        return when (code) {
            401, 403 -> "Gemini key invalid or blocked."
            429 -> "Gemini rate limit on $model."
            else -> "Gemini ($model) error $code: ${raw.take(200)}"
        }
    }

    private fun extractGemini(body: String): String {
        return try {
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates") ?: return "Empty Gemini response."
            if (candidates.length() == 0) return "Empty Gemini response."
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) return "(no content)"
            parts.getJSONObject(0).optString("text", "").trim().ifEmpty { "(no content)" }
        } catch (_: Exception) {
            "Failed to parse Gemini response."
        }
    }

    private fun extractOpenAi(body: String): String {
        return try {
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices") ?: return "Empty response from model."
            if (choices.length() == 0) return "Empty response from model."
            val message = choices.getJSONObject(0).optJSONObject("message")
            message?.optString("content", "")?.trim()?.ifEmpty { "(no content)" }
                ?: "(no content)"
        } catch (_: Exception) {
            "Failed to parse model response."
        }
    }
}
