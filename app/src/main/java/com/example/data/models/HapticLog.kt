package com.example.data.models

import kotlinx.serialization.Serializable

@Serializable
data class HapticLog(
    val id: String = "",
    val event_type: String = "",
    val user_email: String = "",
    val details: String = "",
    val created_at: Long = 0L
)
