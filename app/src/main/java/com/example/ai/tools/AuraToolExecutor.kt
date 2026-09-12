package com.example.ai.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

class AuraToolExecutor(private val context: Context) : ToolExecutor {

    override suspend fun execute(toolName: String, parameters: JSONObject): ToolResult {
        return try {
            when (toolName) {
                "open_aura_learning" -> {
                    ToolResult.Success("Opening Aura Learning...", "aura://learning")
                }
                "open_learning_books" -> {
                    val subject = parameters.optString("subject", "")
                    val className = parameters.optString("class", "")
                    var uri = "aura://learning/books"
                    val queryParams = mutableListOf<String>()
                    if (subject.isNotEmpty()) queryParams.add("subject=${Uri.encode(subject)}")
                    if (className.isNotEmpty()) queryParams.add("class=${Uri.encode(className)}")
                    
                    if (queryParams.isNotEmpty()) {
                        uri += "?" + queryParams.joinToString("&")
                    }
                    ToolResult.Success("Opening books...", uri)
                }
                "search_learning_books" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching books for '$query'...", "aura://learning/books/search?q=${Uri.encode(query)}")
                }
                "search_learning_videos" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching videos for '$query'...", "aura://learning/videos?query=${Uri.encode(query)}")
                }
                "open_question_papers" -> {
                    ToolResult.Success("Opening Question Papers...", "aura://learning/question-papers")
                }
                "open_aura_play" -> {
                    ToolResult.Success("Opening Aura Play...", "aura://play")
                }
                "open_aura_community" -> {
                    ToolResult.Success("Opening Aura Community...", "aura://community")
                }
                "open_camera" -> {
                    // Safe Android Action
                    ToolResult.Success("Opening camera...", "android://camera")
                }
                else -> ToolResult.Error("Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to execute $toolName: ${e.message}")
        }
    }
}
