package com.example.ui.home

import androidx.lifecycle.ViewModel
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.auth.auth
import androidx.lifecycle.viewModelScope
import com.example.data.models.Banner
import com.example.data.models.Book
import com.example.data.models.Video
import com.example.data.models.User
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: AuraRepository) : ViewModel() {
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _announcements = MutableStateFlow<List<com.example.data.models.Announcement>>(emptyList())
    val announcements: StateFlow<List<com.example.data.models.Announcement>> = _announcements.asStateFlow()

    private val _homeSections = MutableStateFlow<List<com.example.data.models.HomeSectionConfig>>(emptyList())
    val homeSections: StateFlow<List<com.example.data.models.HomeSectionConfig>> = _homeSections.asStateFlow()

    private val _recentBooks = MutableStateFlow<List<Book>>(emptyList())
    val recentBooks: StateFlow<List<Book>> = _recentBooks.asStateFlow()

    private val _recentVideos = MutableStateFlow<List<Video>>(emptyList())
    val recentVideos: StateFlow<List<Video>> = _recentVideos.asStateFlow()
    private val _continueWatching = MutableStateFlow<List<Video>>(emptyList())
    val continueWatching: StateFlow<List<Video>> = _continueWatching.asStateFlow()
    private val _continueReading = MutableStateFlow<List<Book>>(emptyList())
    val continueReading: StateFlow<List<Book>> = _continueReading.asStateFlow()

    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    val allBooks: StateFlow<List<Book>> = _allBooks.asStateFlow()

    private val _allVideos = MutableStateFlow<List<Video>>(emptyList())
    val allVideos: StateFlow<List<Video>> = _allVideos.asStateFlow()

    private val _allQuestionPapers = MutableStateFlow<List<com.example.data.models.QuestionPaper>>(emptyList())
    val allQuestionPapers: StateFlow<List<com.example.data.models.QuestionPaper>> = _allQuestionPapers.asStateFlow()

    private val _questionPaperSections = MutableStateFlow<List<com.example.data.models.QuestionPaperSection>>(emptyList())
    val questionPaperSections: StateFlow<List<com.example.data.models.QuestionPaperSection>> = _questionPaperSections.asStateFlow()

    private val _allWebsites = MutableStateFlow<List<com.example.data.models.Website>>(emptyList())
    val allWebsites: StateFlow<List<com.example.data.models.Website>> = _allWebsites.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String>("All Subjects")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeExamSubject = MutableStateFlow<String?>(null)
    val activeExamSubject: StateFlow<String?> = _activeExamSubject.asStateFlow()

    init {
        viewModelScope.launch {
            merge(
                AuraRepository.booksUpdateTrigger,
                AuraRepository.videosUpdateTrigger,
                AuraRepository.sectionsUpdateTrigger,
                AuraRepository.homeConfigUpdateTrigger
            ).collect {
                fetchData()
            }
        }
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
        filterContent(_selectedSubject.value)
    }

    fun setActiveExamSubject(subject: String?) {
        _activeExamSubject.value = subject
        filterContent(_selectedSubject.value)
    }

    private fun filterContent(subject: String) {
        var filteredBooks = _allBooks.value
        var filteredVideos = _allVideos.value

        val activeExam = _activeExamSubject.value
        if (!activeExam.isNullOrBlank()) {
            filteredBooks = filteredBooks.filter { it.subject.equals(activeExam, ignoreCase = true) }
            filteredVideos = filteredVideos.filter { it.subject.equals(activeExam, ignoreCase = true) }
        } else if (subject != "All Subjects" && subject.isNotEmpty()) {
            val mappedSubject = when (subject) {
                "SST" -> "Social Studies"
                "Computer" -> "Computer Science"
                else -> subject
            }
            filteredBooks = filteredBooks.filter { it.subject.equals(mappedSubject, ignoreCase = true) }
            filteredVideos = filteredVideos.filter { it.subject.equals(mappedSubject, ignoreCase = true) }
        }

        _recentBooks.value = filteredBooks.sortedByDescending { it.createdAt }.take(5)
        _recentVideos.value = filteredVideos.sortedByDescending { it.createdAt }.take(5)
    }

    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults: StateFlow<List<User>> = _userSearchResults.asStateFlow()

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _userSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _userSearchResults.value = repository.searchUsers(query)
            } catch (e: Exception) {
                e.printStackTrace()
                _userSearchResults.value = emptyList()
            }
        }
    }

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // 1. Load home sections (has fallback inside repository, always succeeds)
            try {
                _homeSections.value = repository.getHomeSectionConfigs(onlyVisible = true)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading home sections", e)
            }
            
            // 2. Load other dynamic data with safe individual try-catches
            try {
                _banners.value = repository.getBanners()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading banners", e)
            }
            
            try {
                _announcements.value = repository.getAnnouncements()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading announcements", e)
            }
            
            var fetchedBooks = emptyList<com.example.data.models.Book>()
            try {
                fetchedBooks = repository.getBooks()
                _allBooks.value = fetchedBooks
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading books", e)
            }
            
            var fetchedVideos = emptyList<com.example.data.models.Video>()
            try {
                fetchedVideos = repository.getVideos()
                _allVideos.value = fetchedVideos
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading videos", e)
            }
            
            try {
                val fetchedQuestionPapers = repository.getQuestionPapers()
                _allQuestionPapers.value = fetchedQuestionPapers
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading question papers", e)
            }
            
            try {
                val fetchedSections = repository.getQuestionPaperSections()
                _questionPaperSections.value = fetchedSections
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading question paper sections", e)
            }
            
            try {
                val fetchedWebsites = repository.getWebsites()
                _allWebsites.value = fetchedWebsites
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading websites", e)
            }
            
            try {
                filterContent(_selectedSubject.value)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error filtering content", e)
            }
            
            // Load user progress
            try {
                val userId = SupabaseService.client.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    val videoProgress = repository.getVideoProgress(userId)
                    val bookProgress = repository.getBookProgress(userId)
                    
                    val cVideos = videoProgress
                        .sortedByDescending { it.lastWatchedAt }
                        .mapNotNull { vp -> fetchedVideos.find { it.id == vp.videoId } }
                        .take(5)
                    _continueWatching.value = cVideos
                    
                    val cBooks = bookProgress
                        .sortedByDescending { it.lastReadAt }
                        .mapNotNull { bp -> fetchedBooks.find { it.id == bp.bookId } }
                        .take(5)
                    _continueReading.value = cBooks
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading user progress", e)
            }
            
            _isLoading.value = false
        }
    }
}
