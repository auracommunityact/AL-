package com.example.data.repository.pdf

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.models.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PdfScannerRepository(private val context: Context) {

    fun scanForPdfs(): Flow<List<PdfDocument>> = flow {
        val pdfList = mutableListOf<PdfDocument>()
        
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val path = cursor.getString(dataColumn) ?: ""

                    val contentUri: Uri = android.content.ContentUris.withAppendedId(
                        collection,
                        id
                    )

                    pdfList.add(
                        PdfDocument(
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateModified = dateModified * 1000L, // convert to ms
                            path = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PdfScannerRepository", "Error scanning for PDFs: ${e.message}")
        }

        emit(pdfList)
    }.flowOn(Dispatchers.IO)
}
