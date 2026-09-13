package com.example.ai.vision

data class VisionResponse(
    val success: Boolean,
    val answer: String?,
    val error: String? = null
)
