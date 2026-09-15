package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_websites")
data class WebsiteReaderEntity(
    @PrimaryKey val url: String,
    val title: String,
    val domain: String,
    val faviconUrl: String?,
    val extractedText: String,
    val headingsAndParagraphsJson: String, // Stored as a serialized JSON string for rich reading mode
    val aiSummary: String?, // Precompiled multi-section AI summary
    val detectedLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "website_chat_history")
data class WebsiteChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
