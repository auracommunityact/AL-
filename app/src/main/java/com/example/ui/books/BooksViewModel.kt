package com.example.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Book
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BooksViewModel(private val repository: AuraRepository) : ViewModel() {
    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _selectedClass = MutableStateFlow<String?>(null)
    val selectedClass: StateFlow<String?> = _selectedClass.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchBooks()
        viewModelScope.launch {
            AuraRepository.booksUpdateTrigger.collect {
                fetchBooks()
            }
        }
    }

    fun fetchBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val fetched = repository.getBooks()
                _allBooks.value = fetched
                if (fetched.isEmpty()) {
                    _errorMessage.value = "No books available"
                }
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to load books: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilters(className: String?, subject: String?) {
        _selectedClass.value = className
        _selectedSubject.value = subject
        applyFilters()
    }

    fun clearFilters() {
        _selectedClass.value = null
        _selectedSubject.value = null
        applyFilters()
    }

    private fun applyFilters() {
        val cls = _selectedClass.value
        val sub = _selectedSubject.value
        var filtered = _allBooks.value

        if (!cls.isNullOrBlank() && !cls.equals("All Grades", ignoreCase = true)) {
            val targetDigits = cls.filter { it.isDigit() }
            filtered = filtered.filter { book ->
                val bookClass = book.className.trim()
                val bookDigits = bookClass.filter { it.isDigit() }
                
                bookClass.equals(cls, ignoreCase = true) ||
                (targetDigits.isNotEmpty() && bookDigits == targetDigits) ||
                bookClass.contains(cls, ignoreCase = true) ||
                cls.contains(bookClass, ignoreCase = true)
            }
        }

        if (!sub.isNullOrBlank() && !sub.equals("Ebooks", ignoreCase = true) && !sub.equals("All", ignoreCase = true)) {
            val mappedSubject = when (sub) {
                "SST" -> "Social Studies"
                "Computer" -> "Computer Science"
                else -> sub
            }
            filtered = filtered.filter { book ->
                val bSub = book.subject.lowercase().trim()
                val targetSub = mappedSubject.lowercase().trim()
                bSub.contains(targetSub) || targetSub.contains(bSub) || matchesSubjectCategory(bSub, targetSub)
            }
        }

        _books.value = filtered
    }

    private fun matchesSubjectCategory(bookSub: String, cat: String): Boolean {
        if (cat == "social science" && (bookSub.contains("social") || bookSub.contains("studies") || bookSub.contains("history") || bookSub.contains("geography") || bookSub.contains("sst"))) return true
        if (cat == "mathematics" && (bookSub.contains("math") || bookSub.contains("maths"))) return true
        return false
    }
}
