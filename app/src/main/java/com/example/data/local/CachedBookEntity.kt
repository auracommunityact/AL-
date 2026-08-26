package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.models.Book

@Entity(tableName = "cached_books")
data class CachedBookEntity(
    @PrimaryKey val id: String,
    val bookName: String,
    val className: String,
    val subject: String,
    val description: String,
    val coverImage: String,
    val pdfUrl: String,
    val cachedAt: Long
) {
    fun toBook() = Book(
        id = id,
        bookName = bookName,
        className = className,
        subject = subject,
        description = description,
        coverImage = coverImage,
        pdfUrl = pdfUrl,
        createdAt = cachedAt
    )

    companion object {
        fun fromBook(book: Book) = CachedBookEntity(
            id = book.id,
            bookName = book.bookName,
            className = book.className,
            subject = book.subject,
            description = book.description,
            coverImage = book.coverImage,
            pdfUrl = book.pdfUrl,
            cachedAt = if (book.createdAt > 0L) book.createdAt else System.currentTimeMillis()
        )
    }
}
