package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedBookDao {
    @Query("SELECT * FROM cached_books ORDER BY cachedAt DESC")
    suspend fun getAllCachedBooks(): List<CachedBookEntity>

    @Query("SELECT * FROM cached_books WHERE id = :bookId")
    suspend fun getCachedBook(bookId: String): CachedBookEntity?

    @Query("SELECT * FROM cached_books WHERE className = :className")
    suspend fun getCachedBooksByClass(className: String): List<CachedBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedBooks(books: List<CachedBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedBook(book: CachedBookEntity)
}
