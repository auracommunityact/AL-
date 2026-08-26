package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteReaderDao {
    @Query("SELECT * FROM cached_websites ORDER BY timestamp DESC")
    fun getAllCachedWebsites(): Flow<List<WebsiteReaderEntity>>

    @Query("SELECT * FROM cached_websites WHERE url = :url LIMIT 1")
    suspend fun getWebsiteByUrl(url: String): WebsiteReaderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsite(website: WebsiteReaderEntity)

    @Query("DELETE FROM cached_websites WHERE url = :url")
    suspend fun deleteWebsite(url: String)

    @Query("DELETE FROM cached_websites")
    suspend fun clearCache()

    // Chat History queries
    @Query("SELECT * FROM website_chat_history WHERE url = :url ORDER BY timestamp ASC")
    fun getChatHistoryByUrl(url: String): Flow<List<WebsiteChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: WebsiteChatEntity)

    @Query("DELETE FROM website_chat_history WHERE url = :url")
    suspend fun deleteChatHistoryByUrl(url: String)
}
