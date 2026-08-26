package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pdf_annotations")
data class PdfAnnotation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val userId: String,
    val pageNumber: Int,
    val type: String, // "UNDERLINE", "HIGHLIGHT", "PEN"
    val color: Long,
    val strokeWidth: Float,
    val coordinates: String, // JSON representation of points or rects
    val timestamp: Long = System.currentTimeMillis()
)
