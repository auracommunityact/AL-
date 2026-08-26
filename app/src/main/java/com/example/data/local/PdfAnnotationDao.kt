package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfAnnotationDao {
    @Query("SELECT * FROM pdf_annotations WHERE bookId = :bookId")
    fun getAnnotationsForBook(bookId: String): Flow<List<PdfAnnotation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PdfAnnotation)

    @Delete
    suspend fun deleteAnnotation(annotation: PdfAnnotation)
    
    @Query("DELETE FROM pdf_annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: String)
}
