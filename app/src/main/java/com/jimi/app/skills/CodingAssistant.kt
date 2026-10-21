package com.jimi.app.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jimi.app.tools.PhoneTools

/**
 * Coding Assistant — lightweight coding helper.
 * Opens GitHub / search for code questions; full AI coding uses the hybrid router.
 */
class CodingAssistant(private val context: Context) {

    private val phone = PhoneTools(context)

    fun help(): String {
        return "Coding Assistant can search code topics and open GitHub. " +
                "Say: code help coroutines, or open github, or search github for compose accessibility."
    }

    fun openGithub(): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened GitHub."
        } catch (e: Exception) {
            "Could not open GitHub: ${e.message}"
        }
    }

    fun searchGithub(query: String): String {
        val q = query.trim()
        if (q.isEmpty()) return "What should I search on GitHub?"
        return try {
            val url = "https://github.com/search?q=${Uri.encode(q)}&type=repositories"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching GitHub for: $q"
        } catch (e: Exception) {
            "Could not open GitHub search: ${e.message}"
        }
    }

    fun searchCodeTopic(query: String): String {
        val q = query.trim()
        if (q.isEmpty()) return help()
        phone.webSearch("$q programming example")
        return "Opened search for: $q programming example"
    }
}
