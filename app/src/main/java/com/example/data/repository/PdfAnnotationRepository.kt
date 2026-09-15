package com.example.data.repository

import com.example.data.local.PdfAnnotation
import com.example.data.local.PdfAnnotationDao
import kotlinx.coroutines.flow.Flow

class PdfAnnotationRepository(private val dao: PdfAnnotationDao) {
    fun getAnnotationsForBook(bookId: String): Flow<List<PdfAnnotation>> {
        return dao.getAnnotationsForBook(bookId)
    }

    suspend fun insertAnnotation(annotation: PdfAnnotation) {
        dao.insertAnnotation(annotation)
    }

    suspend fun deleteAnnotationById(id: String) {
        dao.deleteAnnotationById(id)
    }
}
