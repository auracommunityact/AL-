import re

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModel.kt", "r") as f:
    content = f.read()

# Add import
content = content.replace("import com.example.ai.tools.AuraToolRegistry", "import com.example.ai.tools.AuraToolRegistry\nimport com.example.ai.tools.AuraToolValidator")

# Update constructor
content = content.replace(
    "class AuraAiViewModel(\n    private val memoryDao: AiMemoryDao,\n    private val llmEngine: LlmInferenceEngine,\n    private val toolRegistry: AuraToolRegistry,\n    private val toolExecutor: AuraToolExecutor\n) : ViewModel()",
    "class AuraAiViewModel(\n    private val memoryDao: AiMemoryDao,\n    private val llmEngine: LlmInferenceEngine,\n    private val toolRegistry: AuraToolRegistry,\n    private val toolValidator: AuraToolValidator,\n    private val toolExecutor: AuraToolExecutor\n) : ViewModel()"
)

# Update handleSlashCommand
slash_replacement = """        val validationResult = toolValidator.validate(toolName, parameters)
        if (validationResult is AuraToolValidator.ValidationResult.Invalid) {
            val errorMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "model",
                content = "Tool Validation Failed: ${validationResult.reason}",
                timestamp = System.currentTimeMillis()
            )
            memoryDao.insertMessage(errorMsg)
            return
        }

        val toolMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "tool",
            content = "Executing: $toolName...",
            timestamp = System.currentTimeMillis()
        )
        memoryDao.insertMessage(toolMsg)

        val result = toolExecutor.execute(toolName, parameters)"""

content = content.replace("""        val toolMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "tool",
            content = "Executing: $toolName...",
            timestamp = System.currentTimeMillis()
        )
        memoryDao.insertMessage(toolMsg)

        val result = toolExecutor.execute(toolName, parameters)""", slash_replacement)

# Update handleModelResponse
model_replacement = """                    // Validate tool
                    val validationResult = toolValidator.validate(toolName, parameters)
                    if (validationResult is AuraToolValidator.ValidationResult.Invalid) {
                        val errorMsg = ChatMessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            role = "model",
                            content = "Tool Validation Failed: ${validationResult.reason}",
                            timestamp = System.currentTimeMillis()
                        )
                        memoryDao.insertMessage(errorMsg)
                        return
                    }

                    // Add tool execution visual indicator message
                    val toolMsg = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = "tool",
                        content = "Executing: $toolName...",
                        timestamp = System.currentTimeMillis()
                    )
                    memoryDao.insertMessage(toolMsg)

                    // Execute tool
                    val result = toolExecutor.execute(toolName, parameters)"""

content = content.replace("""                    // Add tool execution visual indicator message
                    val toolMsg = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = "tool",
                        content = "Executing: $toolName...",
                        timestamp = System.currentTimeMillis()
                    )
                    memoryDao.insertMessage(toolMsg)

                    // Execute tool
                    val result = toolExecutor.execute(toolName, parameters)""", model_replacement)

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModel.kt", "w") as f:
    f.write(content)
