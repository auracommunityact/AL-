package com.example.data.repository

import com.example.data.local.StudySession
import com.example.data.local.StudySessionDao
import kotlinx.coroutines.flow.Flow

class PlannerRepository(private val dao: StudySessionDao) {

    val allSessions: Flow<List<StudySession>> = dao.getAllSessions()

    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudySession>> {
        return dao.getSessionsForDay(startOfDay, endOfDay)
    }

    suspend fun getSessionById(id: Long): StudySession? {
        return dao.getSessionById(id)
    }
    
    suspend fun getUpcomingAlarmSessions(now: Long): List<StudySession> {
        return dao.getUpcomingAlarmSessions(now)
    }

    suspend fun insertSession(session: StudySession): Long {
        return dao.insertSession(session)
    }

    suspend fun updateSession(session: StudySession) {
        dao.updateSession(session)
    }

    suspend fun deleteSession(session: StudySession) {
        dao.deleteSession(session)
    }
}
