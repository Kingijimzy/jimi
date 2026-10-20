package com.jimi.app.tools

import android.content.Context
import com.jimi.app.skills.NetworkRadar
import org.json.JSONObject

/**
 * Registry of tools the Agent can call.
 * Format is simple JSON so free models can follow it reliably.
 */
class ToolRegistry(private val context: Context) {

    private val phone = PhoneTools(context)
    private val network = NetworkRadar(context)

    data class ToolResult(val ok: Boolean, val message: String)

    fun catalogForPrompt(): String {
        return """
Available tools (reply with a single line TOOL: name {json} when you need one):
- open_app {"name":"WhatsApp"}
- open_url {"url":"https://..."}
- web_search {"query":"..."}
- open_settings {"page":"wifi|bluetooth|display|sound|battery|apps|location|accessibility|main"}
- click_text {"text":"Claim"}
- go_home {}
- go_back {}
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
                    else ToolResult(true, phone.openApp(n))
                }
                "open_url" -> {
                    val u = args.optString("url", "")
                    if (u.isBlank()) ToolResult(false, "Missing url")
                    else ToolResult(true, phone.openUrl(u))
                }
                "web_search" -> {
                    val q = args.optString("query", "")
                    if (q.isBlank()) ToolResult(false, "Missing query")
                    else ToolResult(true, phone.webSearch(q))
                }
                "open_settings" -> {
                    ToolResult(true, phone.openSettings(args.optString("page", "main")))
                }
                "click_text" -> {
                    val t = args.optString("text", "")
                    if (t.isBlank()) ToolResult(false, "Missing text")
                    else ToolResult(true, ScreenTools.clickText(t))
                }
                "go_home" -> ToolResult(true, ScreenTools.goHome())
                "go_back" -> ToolResult(true, ScreenTools.goBack())
                "network_status" -> ToolResult(true, network.status())
                "battery" -> ToolResult(true, phone.batteryStatus())
                "time" -> ToolResult(true, phone.currentTime())
                "flashlight" -> {
                    val on = args.optBoolean("on", true)
                    ToolResult(true, phone.flashlight(on))
                }
                "volume" -> {
                    val action = args.optString("action", "up")
                    val pct = args.optInt("percent", 50)
                    ToolResult(true, phone.setVolume(action, pct))
                }
                "read_screen" -> ToolResult(true, ScreenTools.screenSummary())
                else -> ToolResult(false, "Unknown tool: $name")
            }
        } catch (e: Exception) {
            ToolResult(false, "Tool error: ${e.message}")
        }
    }
}
