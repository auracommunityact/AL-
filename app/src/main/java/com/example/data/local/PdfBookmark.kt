package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pdf_bookmarks")
data class PdfBookmark(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val pageNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)
