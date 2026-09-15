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

                var downloadUrl = book.pdfUrl
                if (downloadUrl.contains("drive.google.com/file/d/")) {
                    val parts = downloadUrl.split("/")
                    val idIndex = parts.indexOf("d") + 1
                    if (idIndex < parts.size) {
                        downloadUrl = "https://drive.google.com/uc?export=download&id=${parts[idIndex]}"
                    }
                }

                val url = URL(downloadUrl)
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
                                onProgress(((total * 90) / fileLength).toInt())
                            }
                            output.write(data, 0, count)
                        }
                    }
                }
                
                // Download Cover Image
                val coverFileName = "book_cover_${book.id}.jpg"
                val coverFile = File(context.filesDir, coverFileName)
                var localCoverPath = book.coverImage
                try {
                    val coverUrl = URL(book.coverImage)
                    val coverConnection = coverUrl.openConnection()
                    coverConnection.connect()
                    coverUrl.openStream().use { input ->
                        coverFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    localCoverPath = "file://" + coverFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                onProgress(100)

                val offlineBook = OfflineBook(
                    id = book.id,
                    bookName = book.bookName,
                    className = book.className,
                    subject = book.subject,
                    coverImage = localCoverPath,
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
                
                val coverPath = if (book.coverImage.startsWith("file://")) book.coverImage.removePrefix("file://") else book.coverImage
                val coverFile = File(coverPath)
                if (coverFile.exists() && coverPath.startsWith(context.filesDir.absolutePath)) {
                    coverFile.delete()
                }
                
                offlineBookDao.deleteOfflineBookById(bookId)
            }
        }
    }
}
