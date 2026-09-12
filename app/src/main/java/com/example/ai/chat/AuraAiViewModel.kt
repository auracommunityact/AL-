package com.example.ai.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.memory.AiMemoryDao
import com.example.ai.memory.ChatMessageEntity
import com.example.ai.model.AiConfig
import com.example.ai.model.LlmInferenceEngine
import com.example.ai.tools.AuraToolExecutor
import com.example.ai.tools.AuraToolRegistry
import com.example.ai.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class AuraAiViewModel(
    private val memoryDao: AiMemoryDao,
    private val llmEngine: LlmInferenceEngine,
    private val toolRegistry: AuraToolRegistry,
    private val toolExecutor: AuraToolExecutor
) : ViewModel() {

    private val conversationId = "default_conversation"
    
    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _navigationEvent = MutableStateFlow<String?>(null)
    val navigationEvent: StateFlow<String?> = _navigationEvent.asStateFlow()

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.getMessagesForConversation(conversationId).collect {
                _messages.value = it
            }
        }
    }
    
    fun initializeEngine(context: Context) {
        try {
            llmEngine.initialize(context, AiConfig.MODEL_FILE_NAME)
            _engineError.value = null
        } catch (e: Exception) {
            _engineError.value = e.message
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = text,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertMessage(userMessage)
            _isTyping.value = true

            // Slash Command Parser for direct tool execution
            if (text.startsWith("/")) {
                handleSlashCommand(text.substring(1).trim())
                _isTyping.value = false
                return@launch
            }

            if (_engineError.value != null) {
                // If model is missing, emit error message directly to chat
                val errorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "model",
                    content = "System Error: ${_engineError.value}",
                    timestamp = System.currentTimeMillis()
                )
                memoryDao.insertMessage(errorMsg)
                _isTyping.value = false
                return@launch
            }

            try {
                // Construct prompt
                val prompt = buildPrompt(text)
                
                // Get inference response
                var fullResponse = ""
                llmEngine.generateResponse(prompt).collect { chunk ->
                    fullResponse += chunk
                }

                // Parse and handle response (Check if tool call)
                handleModelResponse(fullResponse)
                
            } catch (e: Exception) {
                val errorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "model",
                    content = "Error during inference: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                memoryDao.insertMessage(errorMsg)
            } finally {
                _isTyping.value = false
            }
        }
    }

    private suspend fun handleSlashCommand(commandText: String) {
        val lowerCmd = commandText.lowercase()
        var toolName = ""
        var parameters = JSONObject()

        if (lowerCmd.startsWith("open aura learning")) {
            toolName = "open_aura_learning"
        } else if (lowerCmd.startsWith("open hindi books")) {
            toolName = "open_learning_books"
            parameters.put("subject", "Hindi")
        } else if (lowerCmd.startsWith("open learning books") || lowerCmd.startsWith("open books")) {
            toolName = "open_learning_books"
        } else if (lowerCmd.startsWith("open question papers")) {
            toolName = "open_question_papers"
        } else if (lowerCmd.startsWith("open aura play")) {
            toolName = "open_aura_play"
        } else if (lowerCmd.startsWith("open aura community")) {
            toolName = "open_aura_community"
        } else if (lowerCmd.startsWith("search books ")) {
            toolName = "search_learning_books"
            parameters.put("query", commandText.removePrefix("search books ").trim())
        } else if (lowerCmd.startsWith("search videos ")) {
            toolName = "search_learning_videos"
            parameters.put("query", commandText.removePrefix("search videos ").trim())
        } else if (lowerCmd.startsWith("open camera")) {
            toolName = "open_camera"
        } else {
            val errorMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "model",
                content = "Unrecognized slash command: /$commandText",
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

        val result = toolExecutor.execute(toolName, parameters)
        when (result) {
            is ToolResult.Success -> {
                result.deepLink?.let { link ->
                    _navigationEvent.value = link
                }
                val successMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "model",
                    content = result.message,
                    timestamp = System.currentTimeMillis()
                )
                memoryDao.insertMessage(successMsg)
            }
            is ToolResult.Error -> {
                val errorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "model",
                    content = "Failed to execute command: ${result.error}",
                    timestamp = System.currentTimeMillis()
                )
                memoryDao.insertMessage(errorMsg)
            }
            is ToolResult.RequiresConfirmation -> {
                val confirmMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "model",
                    content = "Action requires confirmation: ${result.prompt}",
                    timestamp = System.currentTimeMillis()
                )
                memoryDao.insertMessage(confirmMsg)
            }
        }
    }

    private fun buildPrompt(userText: String): String {
        val toolsPrompt = toolRegistry.getToolPrompts()
        return "${AiConfig.SYSTEM_PROMPT}\n\nAvailable Tools:\n$toolsPrompt\n\nUser: $userText\nAura:"
    }

    private suspend fun handleModelResponse(response: String) {
        val content = response.trim()
        
        // Very basic JSON detection for tool calls
        if (content.startsWith("{") && content.contains("\"tool\"")) {
            try {
                val json = JSONObject(content)
                val type = json.optString("type")
                if (type == "tool_call") {
                    val toolName = json.getString("tool")
                    val parameters = json.optJSONObject("parameters") ?: JSONObject()
                    
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
                    val result = toolExecutor.execute(toolName, parameters)
                    when (result) {
                        is ToolResult.Success -> {
                            result.deepLink?.let { link ->
                                _navigationEvent.value = link
                            }
                            val successMsg = ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                role = "model",
                                content = result.message,
                                timestamp = System.currentTimeMillis()
                            )
                            memoryDao.insertMessage(successMsg)
                        }
                        is ToolResult.Error -> {
                            val errorMsg = ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                role = "model",
                                content = "Failed to execute command: ${result.error}",
                                timestamp = System.currentTimeMillis()
                            )
                            memoryDao.insertMessage(errorMsg)
                        }
                        is ToolResult.RequiresConfirmation -> {
                            val confirmMsg = ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                role = "model",
                                content = "Action requires confirmation: ${result.prompt}",
                                timestamp = System.currentTimeMillis()
                            )
                            memoryDao.insertMessage(confirmMsg)
                        }
                    }
                    return
                }
            } catch (e: Exception) {
                // Not valid JSON, fallback to normal text
            }
        }
        
        // Normal text response
        val modelMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "model",
            content = content,
            timestamp = System.currentTimeMillis()
        )
        memoryDao.insertMessage(modelMessage)
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        llmEngine.close()
    }
}
