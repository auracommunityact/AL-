with open("app/src/main/java/com/example/ai/tools/AuraToolRegistry.kt", "w") as f:
    f.write(r"""package com.example.ai.tools

import org.json.JSONObject

class AuraToolRegistry {
    val tools = listOf(
        // === APP TOOLS ===
        Tool("open_aura_learning", "Opens the Aura Learning home page.", "{}"),
        Tool("open_aura_play", "Opens the Aura Play home page.", "{}"),
        Tool("open_aura_community", "Opens the Aura Community ACT home page.", "{}"),
        Tool("open_app", "Opens an external Android app by package name.", "{\"package\": \"string (required)\"}"),
        
        // === AURA LEARNING TOOLS ===
        Tool("open_learning_home", "Opens the Aura Learning home dashboard.", "{}"),
        Tool("open_learning_books", "Opens the learning books section. Can filter by subject/class.", "{\"subject\": \"string (optional)\", \"class\": \"string (optional)\"}"),
        Tool("search_learning_books", "Searches for learning books using a query.", "{\"query\": \"string (required)\", \"class\": \"string (optional)\", \"subject\": \"string (optional)\"}"),
        Tool("open_learning_book", "Opens a specific learning book by ID or title.", "{\"book_id\": \"string (required)\"}"),
        Tool("open_learning_videos", "Opens the learning videos section. Can filter by subject/class.", "{\"subject\": \"string (optional)\", \"class\": \"string (optional)\"}"),
        Tool("search_learning_videos", "Searches for learning videos using a query.", "{\"query\": \"string (required)\", \"class\": \"string (optional)\", \"subject\": \"string (optional)\"}"),
        Tool("open_learning_video", "Opens a specific learning video by ID or title.", "{\"video_id\": \"string (required)\"}"),
        Tool("open_question_papers", "Opens the Question Papers tab.", "{\"class\": \"string (optional)\", \"subject\": \"string (optional)\"}"),
        Tool("search_question_papers", "Searches for question papers using a query.", "{\"query\": \"string (required)\"}"),
        Tool("open_question_paper", "Opens a specific question paper by ID.", "{\"paper_id\": \"string (required)\"}"),
        Tool("search_learning_content", "Performs a global search across all Aura Learning content.", "{\"query\": \"string (required)\"}"),

        // === COMMUNITY TOOLS ===
        Tool("open_community_home", "Opens the Aura Community Home feed.", "{}"),
        Tool("open_community_profile", "Opens the user's Community Profile.", "{}"),
        Tool("open_community_messages", "Opens the Community Messages/Chat.", "{}"),
        Tool("open_community_settings", "Opens the Community Settings.", "{}"),

        // === AURA PLAY TOOLS ===
        Tool("open_play_home", "Opens the Aura Play dashboard.", "{}"),
        Tool("search_games", "Searches for games in Aura Play.", "{\"query\": \"string (required)\"}"),
        Tool("open_game", "Opens a specific game by ID or name.", "{\"game_id\": \"string (required)\"}"),

        // === ANDROID TOOLS ===
        Tool("open_camera", "Opens the Android device camera.", "{}"),
        Tool("open_gallery", "Opens the Android device image gallery.", "{}"),
        Tool("open_browser", "Opens the web browser with a specific URL.", "{\"url\": \"string (required)\"}"),
        Tool("open_settings", "Opens the Android system settings.", "{}"),
        Tool("open_app_settings", "Opens the application settings for this app.", "{}"),
        Tool("share_text", "Shares text using the Android share sheet.", "{\"text\": \"string (required)\"}"),
        Tool("copy_to_clipboard", "Copies text to the device clipboard.", "{\"text\": \"string (required)\"}"),
        Tool("create_local_note", "Creates a local offline note.", "{\"title\": \"string (required)\", \"content\": \"string (required)\"}"),
        Tool("start_timer", "Starts a local timer.", "{\"duration_seconds\": \"integer (required)\", \"label\": \"string (optional)\"}"),
        Tool("get_local_time", "Gets the current device local time.", "{}"),

        // === AI TOOLS ===
        Tool("analyze_image", "Analyzes the currently attached image.", "{\"prompt\": \"string (optional)\"}"),
        Tool("analyze_audio", "Analyzes the recently recorded audio.", "{\"prompt\": \"string (optional)\"}"),
        Tool("summarize_text", "Summarizes provided text.", "{\"text\": \"string (required)\"}"),
        Tool("translate_text", "Translates text to a target language.", "{\"text\": \"string (required)\", \"target_language\": \"string (required)\"}"),
        Tool("explain_topic", "Provides an educational explanation of a topic.", "{\"topic\": \"string (required)\", \"depth\": \"string (optional)\"}"),
        Tool("generate_study_plan", "Generates a study plan.", "{\"subject\": \"string (required)\", \"days\": \"integer (required)\"}")
    )

    fun getToolPrompts(): String {
        return tools.joinToString("\n") { 
            "- ${it.name}: ${it.description} | params: ${it.schema}" 
        }
    }
}
""")
