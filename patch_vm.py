import re

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModel.kt", "r") as f:
    content = f.read()

# I need to update sendMessage to use visionRepository
# First, let's replace the whole sendMessage(text: String, imageUri: Uri?) function block

old_block = """    fun sendMessage(text: String, imageUri: Uri?) {
        if (text.isBlank() && imageUri == null) return

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = if (text.isNotBlank()) text else "[Image attachment]",
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri?.toString()
        )

        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertMessage(userMessage)
            _isTyping.value = true

            // Slash Command Parser for direct tool execution
            if (text.isNotBlank() && text.startsWith("/")) {
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
    }"""

new_block = """    fun sendMessage(text: String, imageUri: Uri?) {
        if (text.isBlank() && imageUri == null) return

        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = if (text.isNotBlank()) text else "[Image attachment]",
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri?.toString()
        )

        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.insertMessage(userMessage)
            _isTyping.value = true

            // Slash Command Parser for direct tool execution
            if (text.isNotBlank() && text.startsWith("/")) {
                handleSlashCommand(text.substring(1).trim())
                _isTyping.value = false
                return@launch
            }

            if (imageUri != null) {
                try {
                    val visionReq = VisionRequest(text, imageUri)
                    val response = visionRepository.analyze(visionReq, conversationId)
                    
                    if (response.success) {
                        clearSelectedImage()
                        val modelMessage = ChatMessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            role = "model",
                            content = response.answer ?: "I analyzed the image.",
                            timestamp = System.currentTimeMillis()
                        )
                        memoryDao.insertMessage(modelMessage)
                    } else {
                        val errorMsg = ChatMessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            role = "model",
                            content = "Error: ${response.error ?: "Unknown Vision AI Error"}",
                            timestamp = System.currentTimeMillis()
                        )
                        memoryDao.insertMessage(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = "model",
                        content = "Error during vision inference: ${e.message}",
                        timestamp = System.currentTimeMillis()
                    )
                    memoryDao.insertMessage(errorMsg)
                } finally {
                    _isTyping.value = false
                }
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
    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
else:
    print("Old block not found!")

with open("app/src/main/java/com/example/ai/chat/AuraAiViewModel.kt", "w") as f:
    f.write(content)
