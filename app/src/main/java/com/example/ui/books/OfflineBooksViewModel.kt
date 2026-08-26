package com.example.ui.books

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.OfflineBook
import com.example.data.models.Book
import com.example.data.repository.OfflineBookManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OfflineBooksViewModel(application: Application) : AndroidViewModel(application) {
    private val offlineBookManager = OfflineBookManager(application)

    private val _offlineBooks = MutableStateFlow<List<OfflineBook>>(emptyList())
    val offlineBooks: StateFlow<List<OfflineBook>> = _offlineBooks.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    init {
        viewModelScope.launch {
            offlineBookManager.getOfflineBooks().collect { books ->
                _offlineBooks.value = books
            }
        }
    }

    fun downloadBook(book: Book) {
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                put(book.id, 0)
            }
            offlineBookManager.downloadBook(
                book = book,
                onProgress = { progress ->
                    _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                        put(book.id, progress)
                    }
                }
            )
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                remove(book.id)
            }
        }
    }

    fun deleteOfflineBook(bookId: String) {
        viewModelScope.launch {
            offlineBookManager.deleteBook(bookId)
        }
    }
}
