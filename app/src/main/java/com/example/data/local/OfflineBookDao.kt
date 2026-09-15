package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineBookDao {
    @Query("SELECT * FROM offline_books ORDER BY downloadedAt DESC")
    fun getAllOfflineBooks(): Flow<List<OfflineBook>>

    @Query("SELECT * FROM offline_books WHERE id = :bookId")
    suspend fun getOfflineBook(bookId: String): OfflineBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineBook(offlineBook: OfflineBook)

    @Delete
    suspend fun deleteOfflineBook(offlineBook: OfflineBook)

    @Query("DELETE FROM offline_books WHERE id = :bookId")
    suspend fun deleteOfflineBookById(bookId: String)
}
