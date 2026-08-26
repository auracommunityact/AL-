package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "exam_date_sheets")
data class ExamDateSheetEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val examDate: String, // e.g. "2026-07-15"
    val examDay: String,  // e.g. "Wednesday"
    val examTime: String, // e.g. "10:00 AM"
    val grade: String,    // e.g. "10th"
    val timestamp: Long   // epoch millisecond of the exam start
)

@Dao
interface ExamDateSheetDao {
    @Query("SELECT * FROM exam_date_sheets ORDER BY timestamp ASC")
    fun getAllExamsFlow(): Flow<List<ExamDateSheetEntity>>

    @Query("SELECT * FROM exam_date_sheets ORDER BY timestamp ASC")
    suspend fun getAllExams(): List<ExamDateSheetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamDateSheetEntity)

    @Update
    suspend fun updateExam(exam: ExamDateSheetEntity)

    @Query("DELETE FROM exam_date_sheets WHERE id = :id")
    suspend fun deleteExamById(id: String)

    @Query("DELETE FROM exam_date_sheets")
    suspend fun deleteAllExams()
}
