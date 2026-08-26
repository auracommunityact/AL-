package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculator_history")
data class CalculatorHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expression: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis()
)
