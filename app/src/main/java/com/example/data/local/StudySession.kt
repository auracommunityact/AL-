package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val topic: String,
    val dateMillis: Long, // Start time of the study session in absolute epoch milliseconds
    val durationMins: Int,
    val color: Long, // ARGB color
    val notes: String,
    val repeatType: String, // "ONE_TIME", "DAILY", "WEEKDAYS", "WEEKENDS", "WEEKLY"
    val alarmEnabled: Boolean,
    val alarmOffsetMins: Int, // e.g. 5 mins before
    val alarmDurationMins: Int,
    val alarmSoundUri: String,
    val completedStatus: String // "PENDING", "COMPLETED", "PARTIALLY", "SKIPPED"
)
