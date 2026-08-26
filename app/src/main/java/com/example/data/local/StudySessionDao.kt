package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY dateMillis ASC")
    fun getAllSessions(): Flow<List<StudySession>>
    
    @Query("SELECT * FROM study_sessions WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY dateMillis ASC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): StudySession?
    
    @Query("SELECT * FROM study_sessions WHERE alarmEnabled = 1 AND completedStatus = 'PENDING' AND dateMillis >= :now ORDER BY dateMillis ASC")
    suspend fun getUpcomingAlarmSessions(now: Long): List<StudySession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Update
    suspend fun updateSession(session: StudySession)

    @Delete
    suspend fun deleteSession(session: StudySession)
}
