package com.example.ai.tools

import org.json.JSONObject

data class Tool(
    val name: String,
    val description: String,
    val schema: String // JSON schema or description of parameters
)

interface ToolExecutor {
    suspend fun execute(toolName: String, parameters: JSONObject): ToolResult
}

sealed class ToolResult {
    data class Success(val message: String, val deepLink: String? = null) : ToolResult()
    data class Error(val error: String) : ToolResult()
    data class RequiresConfirmation(val prompt: String, val pendingAction: () -> Unit) : ToolResult()
}
