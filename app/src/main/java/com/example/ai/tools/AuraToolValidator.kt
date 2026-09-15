package com.example.ai.tools

import org.json.JSONObject

class AuraToolValidator(private val registry: AuraToolRegistry) {
    
    fun validate(toolName: String, parameters: JSONObject): ValidationResult {
        val tool = registry.tools.find { it.name == toolName }
            ?: return ValidationResult.Invalid("Tool '$toolName' is not registered or not permitted.")

        // Basic parameter validation could go here depending on tool schema
        // For example, checking if 'query' is present when schema requires it
        if (tool.schema.contains("required") && tool.schema.contains("query")) {
            if (!parameters.has("query") || parameters.optString("query").isBlank()) {
                return ValidationResult.Invalid("Missing required parameter: 'query' for tool '$toolName'")
            }
        }

        return ValidationResult.Valid(tool)
    }

    sealed class ValidationResult {
        data class Valid(val tool: Tool) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
