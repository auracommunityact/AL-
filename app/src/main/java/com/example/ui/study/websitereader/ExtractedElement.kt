package com.example.ui.study.websitereader

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedElement(
    val type: String, // "h1", "h2", "h3", "p", "li", "table", "link"
    val text: String,
    val linkUrl: String? = null
)
