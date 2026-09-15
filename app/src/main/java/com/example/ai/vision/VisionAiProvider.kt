package com.example.ai.vision

interface VisionAiProvider {
    suspend fun analyzeImage(request: VisionRequest, conversationId: String? = null): VisionResponse
}
