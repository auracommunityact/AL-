package com.example.ai.vision

import android.net.Uri

data class VisionRequest(
    val text: String?,
    val imageUri: Uri?
)
