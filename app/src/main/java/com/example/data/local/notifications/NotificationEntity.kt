package com.example.data.local.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val category: String,
    val deepLink: String?,
    val timestamp: Long,
    val isRead: Boolean,
    val priority: String = "Normal",
    val actionButtonText: String? = null
)
