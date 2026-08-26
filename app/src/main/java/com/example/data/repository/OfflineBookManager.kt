package com.example.data.repository

import android.content.Context
import com.example.data.local.OfflineBook
import com.example.data.local.PlannerDatabase
import com.example.data.models.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class OfflineBookManager(private val context: Context) {
    private val offlineBookDao = PlannerDatabase.getDatabase(context).offlineBookDao()

    fun getOfflineBooks(): Flow<List<OfflineBook>> = offlineBookDao.getAllOfflineBooks()

    suspend fun getOfflineBook(bookId: String): OfflineBook? = offlineBookDao.getOfflineBook(bookId)

    suspend fun downloadBook(book: Book, onProgress: (Int) -> Unit = {}): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "book_${book.id}.pdf"
                val file = File(context.filesDir, fileName)

                val url = URL(book.pdfUrl)
                val connection = url.openConnection()
                connection.connect()

                val fileLength = connection.contentLength

                url.openStream().use { input ->
                    file.outputStream().use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count.toLong()
                            if (fileLength > 0) {
                                onProgress((total * 100 / fileLength).toInt())
                            }
                            output.write(data, 0, count)
                        }
                    }
                }

                val offlineBook = OfflineBook(
                    id = book.id,
                    bookName = book.bookName,
                    className = book.className,
                    subject = book.subject,
                    coverImage = book.coverImage,
                    localPdfPath = file.absolutePath,
                    downloadedAt = System.currentTimeMillis()
                )
                offlineBookDao.insertOfflineBook(offlineBook)

                Result.success(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun deleteBook(bookId: String) {
        withContext(Dispatchers.IO) {
            val book = offlineBookDao.getOfflineBook(bookId)
            if (book != null) {
                val file = File(book.localPdfPath)
                if (file.exists()) {
                    file.delete()
                }
                offlineBookDao.deleteOfflineBookById(bookId)
            }
        }
    }
}
