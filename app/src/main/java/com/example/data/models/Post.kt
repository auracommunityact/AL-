package com.example.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String = "", // UUID handled by Supabase or client
    val title: String = "",
    val description: String = "",
    val image_url: String? = null,
    val created_at: String? = null
)
