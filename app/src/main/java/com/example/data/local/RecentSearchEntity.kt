package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
