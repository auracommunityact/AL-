package com.example.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String = "", 
    val title: String = "",
    val description: String = "",
    val image_url: String? = null,
    val youtube_url: String? = null,
    val youtube_video_id: String? = null,
    val status: String = "draft", // "draft" or "published"
    val created_at: String? = null,
    val updated_at: String? = null
)
