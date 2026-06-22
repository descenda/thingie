package org.cycb.canvas.utils

import android.util.Patterns

object LinkParser {
    fun extractLinks(text: String): List<String> {
        val links = mutableListOf<String>()
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            links.add(matcher.group())
        }
        return links
    }
}
