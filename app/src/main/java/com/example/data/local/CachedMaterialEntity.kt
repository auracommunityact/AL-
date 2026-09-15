package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_materials")
data class CachedMaterialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "question_paper", "note", "syllabus", "resource", "video"
    val category: String,
    val contentUrl: String,
    val description: String,
    val cachedAt: Long
)
