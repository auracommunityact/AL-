package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedVideoDao {
    @Query("SELECT * FROM cached_videos ORDER BY videoOrder ASC, cachedAt DESC")
    suspend fun getAllCachedVideos(): List<CachedVideoEntity>

    @Query("SELECT * FROM cached_videos WHERE id = :videoId")
    suspend fun getCachedVideo(videoId: String): CachedVideoEntity?

    @Query("SELECT * FROM cached_videos WHERE className = :className ORDER BY videoOrder ASC, cachedAt DESC")
    suspend fun getCachedVideosByClass(className: String): List<CachedVideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedVideos(videos: List<CachedVideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedVideo(video: CachedVideoEntity)
}
