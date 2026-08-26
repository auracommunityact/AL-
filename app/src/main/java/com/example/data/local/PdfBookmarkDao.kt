package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfBookmarkDao {
    @Query("SELECT * FROM pdf_bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookmarksForBook(bookId: String): Flow<List<PdfBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PdfBookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: PdfBookmark)
    
    @Query("DELETE FROM pdf_bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkByPage(bookId: String, pageNumber: Int)
}
