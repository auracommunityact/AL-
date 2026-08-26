package com.example.ui.pdf.viewmodels

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.pdf.models.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfToolViewModel : ViewModel() {
    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    val pdfFiles = _pdfFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun scanForPdfs(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _pdfFiles.value = withContext(Dispatchers.IO) {
                val files = mutableListOf<PdfFile>()
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )
                val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
                val selectionArgs = arrayOf("application/pdf")
                
                try {
                    context.contentResolver.query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        selection,
                        selectionArgs,
                        "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                        while (cursor.moveToNext()) {
                            val path = cursor.getString(pathCol)
                            if (path != null && File(path).exists()) {
                                files.add(
                                    PdfFile(
                                        cursor.getLong(idCol),
                                        cursor.getString(nameCol) ?: "Unnamed PDF",
                                        path,
                                        cursor.getLong(sizeCol),
                                        cursor.getLong(dateCol)
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fallback direct folder scanning to make file access 100% reliable
                try {
                    scanDirectoriesDirectly(files)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Sort merged files by date modified descending
                files.sortByDescending { it.dateModified }
                files
            }
            _isLoading.value = false
        }
    }

    private fun scanDirectoriesDirectly(files: MutableList<PdfFile>) {
        val root = Environment.getExternalStorageDirectory() ?: return
        val dirsToScan = listOf(
            File(root, Environment.DIRECTORY_DOWNLOADS),
            File(root, Environment.DIRECTORY_DOCUMENTS),
            File(root, "PDF"),
            root
        )
        
        val visitedPaths = files.map { it.path }.toMutableSet()
        
        for (dir in dirsToScan) {
            if (dir.exists() && dir.isDirectory) {
                scanDirRecursive(dir, files, visitedPaths, depth = 0)
            }
        }
    }

    private fun scanDirRecursive(dir: File, files: MutableList<PdfFile>, visited: MutableSet<String>, depth: Int) {
        if (depth > 4) return // Guard against deep hierarchies
        val list = dir.listFiles() ?: return
        for (file in list) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".") && file.name != "Android") {
                    scanDirRecursive(file, files, visited, depth + 1)
                }
            } else if (file.isFile && file.name.lowercase().endsWith(".pdf")) {
                val path = file.absolutePath
                if (!visited.contains(path)) {
                    visited.add(path)
                    files.add(
                        PdfFile(
                            id = file.hashCode().toLong(),
                            name = file.name,
                            path = path,
                            size = file.length(),
                            dateModified = file.lastModified() / 1000
                        )
                    )
                }
            }
        }
    }
}
