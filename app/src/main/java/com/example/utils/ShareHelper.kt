package com.example.utils

import android.content.Context
import android.content.Intent

object ShareHelper {
    fun toSlug(str: String): String {
        return str.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim { it == '-' }
    }

    fun shareContent(context: Context, title: String, contentType: String, id: String) {
        val shareUrl = "https://aura.auralearning.workers.dev/$contentType/$id"
        
        val shareMessage = "📚 Check this out on Aura Learning!\n\nTitle: $title\n\n$shareUrl"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share ${contentType.replaceFirstChar { it.uppercase() }}")
        context.startActivity(shareIntent)
    }
}
