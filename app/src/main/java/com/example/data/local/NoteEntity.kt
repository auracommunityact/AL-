package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedUrl: String? = null // To link to a specific video/PDF
)
