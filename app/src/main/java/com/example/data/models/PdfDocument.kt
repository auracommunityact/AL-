package com.example.data.models

import android.net.Uri

data class PdfDocument(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val path: String
)
