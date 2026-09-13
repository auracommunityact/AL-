package com.example.ai.model

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import com.google.mediapipe.tasks.genai.llminference.LlmInference

interface LlmInferenceEngine {
    fun initialize(context: Context, modelPath: String)
    suspend fun generateResponse(prompt: String): Flow<String>
    fun close()
}

/**
 * Integration point for LiteRT-LM (MediaPipe LLM Inference API).
 * This class abstracts the initialization and inference of the on-device Gemma 2B model.
 */
class LiteRtLlmEngine : LlmInferenceEngine {
    
    private var isInitialized = false
    private var llmInference: LlmInference? = null

    override fun initialize(context: Context, modelPath: String) {
        val modelFile = File(context.filesDir, modelPath)
        
        // This is the strict integration point. If the physical model file does not exist,
        // we throw an exception rather than providing fake mock answers. 
        if (!modelFile.exists()) {
            throw IllegalStateException("Model file not found at ${modelFile.absolutePath}. Please download the ${AiConfig.MODEL_NAME} model to enable offline AI.")
        }
        
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(AiConfig.MAX_TOKENS)
            .setTemperature(AiConfig.TEMPERATURE)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
        
        isInitialized = true
    }

    override suspend fun generateResponse(prompt: String): Flow<String> = flow {
        if (!isInitialized || llmInference == null) {
            throw IllegalStateException("LiteRT LLM Engine is not initialized.")
        }
        
        val result = llmInference?.generateResponse(prompt)
        emit(result ?: "")
    }

    override fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
