package com.jimi.app.skills

import android.content.Context
import com.jimi.app.tools.PhoneTools

/**
 * Research Agent — lightweight research helper.
 * Opens a web search for the query; full page-reading comes later with browser automation.
 */
class ResearchAgent(private val context: Context) {

    private val phone = PhoneTools(context)

    fun research(query: String): String {
        val q = query.trim()
        if (q.isEmpty()) return "What should I research?"
        phone.webSearch(q)
        return "Searching for: $q. I opened results in your browser."
    }

    fun help(): String {
        return "Research Agent can search the web for you. Say: research quantum computing, or look up FC Mobile tips."
    }
}
