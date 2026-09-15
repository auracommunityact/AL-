package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculatorHistoryDao {
    @Query("SELECT * FROM calculator_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalculatorHistoryEntity>>

    @Query("SELECT * FROM calculator_history WHERE expression LIKE :searchQuery OR answer LIKE :searchQuery ORDER BY timestamp DESC")
    fun searchHistory(searchQuery: String): Flow<List<CalculatorHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: CalculatorHistoryEntity)

    @Query("DELETE FROM calculator_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM calculator_history")
    suspend fun deleteAllHistory()
}
