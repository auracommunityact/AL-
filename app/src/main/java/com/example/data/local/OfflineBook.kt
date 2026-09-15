package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_books")
data class OfflineBook(
    @PrimaryKey val id: String,
    val bookName: String,
    val className: String,
    val subject: String,
    val coverImage: String,
    val localPdfPath: String,
    val downloadedAt: Long
)
