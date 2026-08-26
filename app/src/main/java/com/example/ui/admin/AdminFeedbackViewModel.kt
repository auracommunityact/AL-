package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Feedback
import com.example.data.models.FeedbackConstants
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminFeedbackViewModel(private val repository: AuraRepository = AuraRepository()) : ViewModel() {
    private val _allFeedback = MutableStateFlow<List<Feedback>>(emptyList())
    val allFeedback = _allFeedback.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    val filteredFeedback = combine(
        _allFeedback, _searchQuery, _selectedCategory, _selectedStatus
    ) { feedback, query, category, status ->
        feedback.filter { f ->
            (query.isBlank() || f.subject.contains(query, ignoreCase = true) || 
             f.description.contains(query, ignoreCase = true) || 
             f.userName.contains(query, ignoreCase = true) || 
             f.userEmail.contains(query, ignoreCase = true)) &&
            (category == null || f.category == category) &&
            (status == null || f.status == status)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics = _allFeedback.map { list ->
        FeedbackAnalytics(
            totalFeedback = list.size,
            bugReports = list.count { it.category == "Bug Report" },
            featureRequests = list.count { it.category == "Feature Request" },
            suggestions = list.count { it.category == "Improvement Suggestion" },
            completed = list.count { it.status == "Completed" },
            pending = list.count { it.status == "New" || it.status == "Under Review" },
            mostRequestedFeature = list.filter { it.category == "Feature Request" }
                .groupBy { it.requestedFeatureName }
                .maxByOrNull { it.value.size }?.key ?: "None",
            featureRequestCounts = list.filter { it.category == "Feature Request" && it.requestedFeatureName != null }
                .groupBy { it.requestedFeatureName!! }
                .mapValues { it.value.size }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedbackAnalytics())

    init {
        fetchFeedback()
    }

    fun fetchFeedback() {
        viewModelScope.launch {
            _isLoading.value = true
            _allFeedback.value = repository.getAllFeedback()
            _isLoading.value = false
        }
    }

    fun updateStatus(feedbackId: String, status: String) {
        viewModelScope.launch {
            val success = repository.updateFeedbackStatus(feedbackId, status)
            if (success) {
                fetchFeedback()
            }
        }
    }

    fun deleteFeedback(feedbackId: String) {
        viewModelScope.launch {
            val success = repository.deleteFeedback(feedbackId)
            if (success) {
                fetchFeedback()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterChange(category: String?) {
        _selectedCategory.value = category
    }

    fun onStatusFilterChange(status: String?) {
        _selectedStatus.value = status
    }
}

data class FeedbackAnalytics(
    val totalFeedback: Int = 0,
    val bugReports: Int = 0,
    val featureRequests: Int = 0,
    val suggestions: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val mostRequestedFeature: String = "None",
    val featureRequestCounts: Map<String, Int> = emptyMap()
)
