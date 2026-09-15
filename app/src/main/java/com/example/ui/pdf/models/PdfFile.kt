package com.example.ui.pdf.models

data class PdfFile(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val dateModified: Long
)
