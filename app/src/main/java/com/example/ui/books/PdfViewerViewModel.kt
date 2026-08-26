package com.example.ui.books

import android.app.Application
import com.example.data.models.BookProgress
import com.example.data.repository.AuraRepository
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.auth.auth
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PdfAnnotation
import com.example.data.local.PdfBookmark
import com.example.data.local.PlannerDatabase
import com.example.data.repository.PdfAnnotationRepository
import com.example.data.repository.PdfBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.data.repository.TranslationService

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}


class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: PdfAnnotationRepository
    private val bookmarkRepo: PdfBookmarkRepository
    private val translationService = TranslationService()
    private val auraRepo = AuraRepository()

    init {
        val db = PlannerDatabase.getDatabase(application)
        repo = PdfAnnotationRepository(db.pdfAnnotationDao())
        bookmarkRepo = PdfBookmarkRepository(db.pdfBookmarkDao())
    }

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

        private val _initialPage = MutableStateFlow(0)
    val initialPage: StateFlow<Int> = _initialPage

    private val _pdfPageCount = MutableStateFlow(0)
    val pdfPageCount: StateFlow<Int> = _pdfPageCount

    private val _annotations = MutableStateFlow<List<PdfAnnotation>>(emptyList())
    val annotations: StateFlow<List<PdfAnnotation>> = _annotations

    private val _bookmarks = MutableStateFlow<List<PdfBookmark>>(emptyList())
    val bookmarks: StateFlow<List<PdfBookmark>> = _bookmarks

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    fun updateProgress(bookId: String, pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = SupabaseService.client.auth.currentSessionOrNull()?.user?.id ?: return@launch
            val progress = BookProgress(
                userId = userId,
                bookId = bookId,
                lastPage = pageIndex
            )
            auraRepo.updateBookProgress(progress)
        }
    }

    fun loadPdf(url: String, bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collect annotations
                launch {
                    repo.getAnnotationsForBook(bookId).collect {
                        _annotations.value = it
                    }
                }
                
                // Collect bookmarks
                launch {
                    bookmarkRepo.getBookmarksForBook(bookId).collect {
                        _bookmarks.value = it
                    }
                }

                                val userId = SupabaseService.client.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    val progressList = auraRepo.getBookProgress(userId)
                    val progress = progressList.find { it.bookId == bookId }
                    if (progress != null) {
                        _initialPage.value = progress.lastPage
                    }
                }

                val file: File
                if (url.startsWith("/") || url.startsWith("file://")) {
                    val path = url.removePrefix("file://")
                    file = File(path)
                    _downloadProgress.value = 1f
                } else {
                    file = File(getApplication<Application>().cacheDir, "book_$bookId.pdf")
                    if (!file.exists()) {
                        _downloadProgress.value = 0f
                        val downloadUrl = if (url.contains("drive.google.com/file/d/")) {
                            val parts = url.split("/")
                            val idIndex = parts.indexOf("d") + 1
                            if (idIndex > 0 && idIndex < parts.size) {
                                "https://drive.google.com/uc?export=download&id=${parts[idIndex]}"
                            } else url
                        } else url

                        val client = OkHttpClient()
                        val request = Request.Builder().url(downloadUrl).build()
                        val response = client.newCall(request).execute()
                        
                        if (!response.isSuccessful) throw Exception("Failed to download PDF")
                        
                        val body = response.body ?: throw Exception("Empty body")
                        val contentLength = body.contentLength()
                        val inputStream = body.byteStream()
                        val outputStream = FileOutputStream(file)
                        
                        var bytesCopied = 0L
                        val buffer = ByteArray(8 * 1024)
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            if (contentLength > 0) {
                                _downloadProgress.value = bytesCopied.toFloat() / contentLength
                            }
                            bytes = inputStream.read(buffer)
                        }
                        outputStream.close()
                        inputStream.close()
                    }
                    _downloadProgress.value = 1f
                }
                
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                _pdfPageCount.value = pdfRenderer!!.pageCount
                
            } catch (e: Exception) {
                Log.e("PdfViewerViewModel", "Error loading PDF", e)
                _downloadProgress.value = -1f // Error state
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pdfRenderer!!.pageCount) return@withContext null
        
        try {
            val page = pdfRenderer!!.openPage(pageIndex)
            val height = (width.toFloat() / page.width * page.height).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // Fill white background
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            Log.e("PdfViewerViewModel", "Error rendering page $pageIndex", e)
            null
        }
    }

    fun addAnnotation(annotation: PdfAnnotation) {
        viewModelScope.launch {
            repo.insertAnnotation(annotation)
        }
    }

    fun deleteAnnotation(id: String) {
        viewModelScope.launch {
            repo.deleteAnnotationById(id)
        }
    }

    fun toggleBookmark(bookId: String, pageNumber: Int) {
        viewModelScope.launch {
            val isBookmarked = _bookmarks.value.any { it.pageNumber == pageNumber }
            if (isBookmarked) {
                bookmarkRepo.deleteBookmarkByPage(bookId, pageNumber)
            } else {
                bookmarkRepo.insertBookmark(com.example.data.local.PdfBookmark(bookId = bookId, pageNumber = pageNumber))
                // Store progress to Supabase
                updateProgress(bookId, pageNumber)
            }
        }
    }

    suspend fun summarizePage(pageIndex: Int, width: Int): String {
        val bitmap = renderPage(pageIndex, width) ?: return "Failed to render page for summarization."
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image).await()
            val extractedText = visionText.text
            if (extractedText.isBlank()) {
                return "No readable text found on this page."
            }
            translationService.summarizeNotes(extractedText)
        } catch (e: Exception) {
            "Error extracting text: ${e.message}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}
