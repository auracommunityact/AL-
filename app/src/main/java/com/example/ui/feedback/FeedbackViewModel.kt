package com.example.ui.feedback

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FeedbackViewModel(private val repository: AuraRepository = AuraRepository()) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AppFeature>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isFeatureRequested = MutableStateFlow(false)
    val isFeatureRequested = _isFeatureRequested.asStateFlow()

    private val _submissionStatus = MutableStateFlow<SubmissionStatus>(SubmissionStatus.Idle)
    val submissionStatus = _submissionStatus.asStateFlow()

    private val _featureRequests = MutableStateFlow<List<Feedback>>(emptyList())
    val featureRequests = _featureRequests.asStateFlow()

    init {
        setupSearch()
        fetchFeatureRequests()
    }

    fun fetchFeatureRequests() {
        viewModelScope.launch {
            val allFeedback = repository.getAllFeedback()
            val requests = allFeedback
                .filter { it.category == "Feature Request" && it.status != "Rejected" && it.status != "Completed" }
                .sortedByDescending { it.upvotes }
            _featureRequests.value = requests
        }
    }

    fun toggleUpvote(feedbackId: String, userId: String) {
        viewModelScope.launch {
            // Optimistically update UI
            val currentList = _featureRequests.value
            _featureRequests.value = currentList.map {
                if (it.id == feedbackId) {
                    val isUpvoted = it.upvotedBy.contains(userId)
                    val newUpvotedBy = it.upvotedBy.toMutableList()
                    if (isUpvoted) newUpvotedBy.remove(userId) else newUpvotedBy.add(userId)
                    it.copy(
                        upvotedBy = newUpvotedBy,
                        upvotes = newUpvotedBy.size
                    )
                } else it
            }.sortedByDescending { it.upvotes }

            val success = repository.toggleUpvoteFeedback(feedbackId, userId)
            if (!success) {
                // Revert if failed
                fetchFeatureRequests()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                    } else {
                        val matches = AppFeatures.FEATURES.filter { 
                            it.name.contains(query, ignoreCase = true) || 
                            it.description.contains(query, ignoreCase = true)
                        }
                        _searchResults.value = matches
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun submitFeedback(
        user: User,
        category: String,
        subject: String,
        description: String,
        requestedFeatureName: String? = null,
        screenshotBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _submissionStatus.value = SubmissionStatus.Loading
            
            var screenshotUrl: String? = null
            if (screenshotBytes != null) {
                screenshotUrl = repository.uploadFeedbackScreenshot(
                    screenshotBytes, 
                    "feedback_${UUID.randomUUID()}.jpg"
                )
            }

            val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
            val appVersion = "1.0.0" // Ideally fetched from BuildConfig

            val feedback = Feedback(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userName = user.name,
                userEmail = user.email,
                category = category,
                subject = subject,
                description = description,
                requestedFeatureName = requestedFeatureName,
                screenshotUrl = screenshotUrl,
                deviceInfo = deviceInfo,
                appVersion = appVersion
            )

            val success = repository.submitFeedback(feedback)
            if (success) {
                _submissionStatus.value = SubmissionStatus.Success
            } else {
                _submissionStatus.value = SubmissionStatus.Error("Failed to submit feedback. Please try again.")
            }
        }
    }

    fun resetSubmissionStatus() {
        _submissionStatus.value = SubmissionStatus.Idle
    }
}

sealed class SubmissionStatus {
    object Idle : SubmissionStatus()
    object Loading : SubmissionStatus()
    object Success : SubmissionStatus()
    data class Error(val message: String) : SubmissionStatus()
}
