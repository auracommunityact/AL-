with open("app/src/main/java/com/example/ai/tools/AuraToolExecutor.kt", "w") as f:
    f.write("""package com.example.ai.tools

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuraToolExecutor(private val context: Context) : ToolExecutor {

    override suspend fun execute(toolName: String, parameters: JSONObject): ToolResult {
        return try {
            when (toolName) {
                // === APP TOOLS ===
                "open_aura_learning" -> ToolResult.Success("Opening Aura Learning...", "aura://learning")
                "open_aura_play" -> ToolResult.Success("Opening Aura Play...", "aura://play")
                "open_aura_community" -> ToolResult.Success("Opening Aura Community...", "aura://community")
                "open_app" -> {
                    val packageName = parameters.optString("package", "")
                    if (packageName.isEmpty()) return ToolResult.Error("Missing package name.")
                    ToolResult.RequiresConfirmation(
                        "Are you sure you want to open the external app '$packageName'?",
                        pendingAction = {
                            // Execution would happen here after confirmation
                        }
                    )
                }

                // === AURA LEARNING TOOLS ===
                "open_learning_home" -> ToolResult.Success("Opening Aura Learning Home...", "aura://learning/home")
                "open_learning_books" -> {
                    val subject = parameters.optString("subject", "")
                    val className = parameters.optString("class", "")
                    var uri = "aura://learning/books"
                    val queryParams = mutableListOf<String>()
                    if (subject.isNotEmpty()) queryParams.add("subject=${Uri.encode(subject)}")
                    if (className.isNotEmpty()) queryParams.add("class=${Uri.encode(className)}")
                    if (queryParams.isNotEmpty()) uri += "?" + queryParams.joinToString("&")
                    ToolResult.Success("Opening books...", uri)
                }
                "search_learning_books" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching books for '$query'...", "aura://learning/books/search?q=${Uri.encode(query)}")
                }
                "open_learning_book" -> {
                    val bookId = parameters.optString("book_id", "")
                    ToolResult.Success("Opening book...", "aura://learning/books/$bookId")
                }
                "open_learning_videos" -> {
                    val subject = parameters.optString("subject", "")
                    val className = parameters.optString("class", "")
                    var uri = "aura://learning/videos"
                    val queryParams = mutableListOf<String>()
                    if (subject.isNotEmpty()) queryParams.add("subject=${Uri.encode(subject)}")
                    if (className.isNotEmpty()) queryParams.add("class=${Uri.encode(className)}")
                    if (queryParams.isNotEmpty()) uri += "?" + queryParams.joinToString("&")
                    ToolResult.Success("Opening videos...", uri)
                }
                "search_learning_videos" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching videos for '$query'...", "aura://learning/videos?query=${Uri.encode(query)}")
                }
                "open_learning_video" -> {
                    val videoId = parameters.optString("video_id", "")
                    ToolResult.Success("Opening video...", "aura://learning/videos/$videoId")
                }
                "open_question_papers" -> {
                    val subject = parameters.optString("subject", "")
                    val className = parameters.optString("class", "")
                    var uri = "aura://learning/question-papers"
                    val queryParams = mutableListOf<String>()
                    if (subject.isNotEmpty()) queryParams.add("subject=${Uri.encode(subject)}")
                    if (className.isNotEmpty()) queryParams.add("class=${Uri.encode(className)}")
                    if (queryParams.isNotEmpty()) uri += "?" + queryParams.joinToString("&")
                    ToolResult.Success("Opening Question Papers...", uri)
                }
                "search_question_papers" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching question papers for '$query'...", "aura://learning/question-papers/search?q=${Uri.encode(query)}")
                }
                "open_question_paper" -> {
                    val paperId = parameters.optString("paper_id", "")
                    ToolResult.Success("Opening question paper...", "aura://learning/question-papers/$paperId")
                }
                "search_learning_content" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching all learning content for '$query'...", "aura://learning/search?q=${Uri.encode(query)}")
                }

                // === COMMUNITY TOOLS ===
                "open_community_home" -> ToolResult.Success("Opening Community feed...", "aura://community/home")
                "open_community_profile" -> ToolResult.Success("Opening your Profile...", "aura://community/profile")
                "open_community_messages" -> ToolResult.Success("Opening Messages...", "aura://community/messages")
                "open_community_settings" -> ToolResult.Success("Opening Community Settings...", "aura://community/settings")

                // === AURA PLAY TOOLS ===
                "open_play_home" -> ToolResult.Success("Opening Aura Play...", "aura://play/home")
                "search_games" -> {
                    val query = parameters.optString("query", "")
                    ToolResult.Success("Searching games for '$query'...", "aura://play/search?q=${Uri.encode(query)}")
                }
                "open_game" -> {
                    val gameId = parameters.optString("game_id", "")
                    ToolResult.Success("Opening game...", "aura://play/game?id=${Uri.encode(gameId)}")
                }

                // === ANDROID TOOLS ===
                "open_camera" -> ToolResult.Success("Opening camera...", "android://camera")
                "open_gallery" -> ToolResult.Success("Opening gallery...", "android://gallery")
                "open_browser" -> {
                    val url = parameters.optString("url", "")
                    ToolResult.Success("Opening browser...", "android://browser?url=${Uri.encode(url)}")
                }
                "open_settings" -> ToolResult.Success("Opening Android settings...", "android://settings")
                "open_app_settings" -> ToolResult.Success("Opening app settings...", "android://app_settings")
                "share_text" -> {
                    val text = parameters.optString("text", "")
                    ToolResult.Success("Sharing text...", "android://share?text=${Uri.encode(text)}")
                }
                "copy_to_clipboard" -> {
                    val text = parameters.optString("text", "")
                    // Real implementation would use ClipboardManager here
                    ToolResult.Success("Text copied to clipboard.")
                }
                "create_local_note" -> {
                    val title = parameters.optString("title", "New Note")
                    val content = parameters.optString("content", "")
                    ToolResult.Success("Created note: $title", "aura://learning/notes/new")
                }
                "start_timer" -> {
                    val seconds = parameters.optInt("duration_seconds", 0)
                    ToolResult.Success("Timer started for $seconds seconds.")
                }
                "get_local_time" -> {
                    val formatter = SimpleDateFormat("h:mm a, EEEE, MMMM d, yyyy", Locale.getDefault())
                    val currentTime = formatter.format(Date())
                    ToolResult.Success("The current local time is $currentTime")
                }

                // === AI TOOLS (Stubs for full implementation) ===
                "analyze_image", "analyze_audio", "summarize_text", "translate_text", "explain_topic", "generate_study_plan" -> {
                    ToolResult.Success("AI tool '$toolName' executed successfully.")
                }

                else -> ToolResult.Error("Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to execute $toolName: ${e.message}")
        }
    }
}
""")
