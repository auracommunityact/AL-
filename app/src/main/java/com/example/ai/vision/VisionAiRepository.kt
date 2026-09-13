package com.example.ai.vision

class VisionAiRepository(
    private val remoteProvider: VisionAiProvider
) {
    suspend fun analyze(request: VisionRequest, conversationId: String? = null): VisionResponse {
        return remoteProvider.analyzeImage(request, conversationId)
    }
}
