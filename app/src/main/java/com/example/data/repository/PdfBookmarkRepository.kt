package com.example.data.repository

import com.example.data.local.PdfBookmark
import com.example.data.local.PdfBookmarkDao
import kotlinx.coroutines.flow.Flow

class PdfBookmarkRepository(private val dao: PdfBookmarkDao) {
    fun getBookmarksForBook(bookId: String): Flow<List<PdfBookmark>> {
        return dao.getBookmarksForBook(bookId)
    }

    suspend fun insertBookmark(bookmark: PdfBookmark) {
        dao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmarkByPage(bookId: String, pageNumber: Int) {
        dao.deleteBookmarkByPage(bookId, pageNumber)
    }
}
