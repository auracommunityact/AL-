package com.example.ai.tools

import org.json.JSONObject

class AuraToolRegistry {
    val tools = listOf(
        Tool(
            name = "open_aura_learning",
            description = "Opens the Aura Learning home page.",
            schema = "{}"
        ),
        Tool(
            name = "open_learning_books",
            description = "Opens the learning books section. Can filter by subject.",
            schema = """{"subject": "string (optional)", "class": "string (optional)"}"""
        ),
        Tool(
            name = "search_learning_books",
            description = "Searches for learning books using a query.",
            schema = """{"query": "string (required)", "class": "string (optional)", "subject": "string (optional)"}"""
        ),
        Tool(
            name = "search_learning_videos",
            description = "Searches for learning videos using a query.",
            schema = """{"query": "string (required)", "class": "string (optional)", "subject": "string (optional)"}"""
        ),
        Tool(
            name = "open_question_papers",
            description = "Opens the Question Papers tab.",
            schema = "{}"
        ),
        Tool(
            name = "open_aura_play",
            description = "Opens the Aura Play home page.",
            schema = "{}"
        ),
        Tool(
            name = "open_aura_community",
            description = "Opens the Aura Community ACT home page.",
            schema = "{}"
        ),
        Tool(
            name = "open_camera",
            description = "Opens the Android device camera.",
            schema = "{}"
        )
    )

    fun getToolPrompts(): String {
        return tools.joinToString("\n") { 
            "- ${it.name}: ${it.description} | params: ${it.schema}" 
        }
    }
}
