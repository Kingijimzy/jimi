package com.jimi.app.tools

import android.content.Context
import com.jimi.app.skills.NetworkRadar
import org.json.JSONObject

class ToolRegistry(private val context: Context) {

    private val phone = PhoneTools(context)
    private val network = NetworkRadar(context)

    data class ToolResult(val ok: Boolean, val message: String)

    private fun fromActionMessage(message: String): ToolResult {
        val lower = message.lowercase()
        val failureMarkers = listOf("couldn't", "could not", "failed", "failure", "not found", "not enabled", "not available", "unavailable", "accessibility is off", "screen may have changed", "error:")
        return ToolResult(failureMarkers.none { lower.contains(it) }, message)
    }

    fun catalogForPrompt(): String {
        return """
Available tools (reply with a single line TOOL: name {json} when you need one):
- open_app {"name":"WhatsApp"}
- open_url {"url":"https://..."}
- web_search {"query":"..."}
- open_settings {"page":"wifi|bluetooth|display|sound|battery|apps|location|accessibility|main"}
- click_text {"text":"Claim"}
- swipe {"direction":"up|down|left|right"}
- go_home {}
- go_back {}
- read_screen {}
- network_status {}
- battery {}
- time {}
- flashlight {"on":true}
- volume {"action":"up|down|mute|unmute|set","percent":50}
Only use a tool when the user wants an action. Otherwise just answer.
""".trim()
    }

    fun tryParseAndRun(reply: String): ToolResult? {
        val line = reply.lines().firstOrNull { it.trim().startsWith("TOOL:", ignoreCase = true) }
            ?: return null
        val rest = line.substringAfter(":").trim()
        val name = rest.substringBefore(" ").trim().lowercase()
        val jsonPart = rest.substringAfter(" ", "{}").trim().ifEmpty { "{}" }
        val args = try {
            JSONObject(if (jsonPart.startsWith("{")) jsonPart else "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        return run(name, args)
    }

    fun run(name: String, args: JSONObject): ToolResult {
        return try {
            when (name) {
                "open_app" -> {
                    val n = args.optString("name", "")
                    if (n.isBlank()) ToolResult(false, "Missing app name")
                    else fromActionMessage(phone.openApp(n))
                }
                "open_url" -> {
                    val u = args.optString("url", "")
                    if (u.isBlank()) ToolResult(false, "Missing url")
                    else fromActionMessage(phone.openUrl(u))
                }
                "web_search" -> {
                    val q = args.optString("query", "")
                    if (q.isBlank()) ToolResult(false, "Missing query")
                    else fromActionMessage(phone.webSearch(q))
                }
                "open_settings" -> {
                    fromActionMessage(phone.openSettings(args.optString("page", "main")))
                }
                "click_text" -> {
                    val t = args.optString("text", "")
                    if (t.isBlank()) ToolResult(false, "Missing text")
                    else fromActionMessage(ScreenTools.clickText(t))
                }
                "swipe" -> {
                    val d = args.optString("direction", "down")
                    fromActionMessage(ScreenTools.swipe(d))
                }
                "go_home" -> fromActionMessage(ScreenTools.goHome())
                "go_back" -> fromActionMessage(ScreenTools.goBack())
                "read_screen" -> fromActionMessage(ScreenTools.screenSummary())
                "network_status" -> fromActionMessage(network.status())
                "battery" -> fromActionMessage(phone.batteryStatus())
                "time" -> fromActionMessage(phone.currentTime())
                "flashlight" -> {
                    val on = args.optBoolean("on", true)
                    fromActionMessage(phone.flashlight(on))
                }
                "volume" -> {
                    val action = args.optString("action", "up")
                    val pct = args.optInt("percent", 50)
                    fromActionMessage(phone.setVolume(action, pct))
                }
                else -> ToolResult(false, "Unknown tool: $name")
            }
        } catch (e: Exception) {
            ToolResult(false, "Tool error: ${e.message}")
        }
    }
}
