package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ResultAnalysisDao {
    @Query("SELECT * FROM result_analysis_cache WHERE imageUri = :uri")
    suspend fun getResultByUri(uri: String): ResultAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ResultAnalysisEntity)

    @Query("DELETE FROM result_analysis_cache")
    suspend fun clearCache()
}
