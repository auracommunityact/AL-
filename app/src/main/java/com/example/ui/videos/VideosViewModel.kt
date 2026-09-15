package com.example.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Video
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideosViewModel(private val repository: AuraRepository) : ViewModel() {
    private val _allVideos = MutableStateFlow<List<Video>>(emptyList())
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _selectedClass = MutableStateFlow<String?>(null)
    val selectedClass: StateFlow<String?> = _selectedClass.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchVideos()
        viewModelScope.launch {
            AuraRepository.videosUpdateTrigger.collect {
                fetchVideos()
            }
        }
    }

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _allVideos.value = repository.getVideos()
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun applyFilters() {
        val cls = _selectedClass.value
        val sub = _selectedSubject.value
        var filtered = _allVideos.value

        if (cls != null) {
            filtered = filtered.filter { it.className.equals(cls, ignoreCase = true) }
        }
        if (sub != null && sub.isNotEmpty()) {
            val mappedSubject = when (sub) {
                "SST" -> "Social Studies"
                "Computer" -> "Computer Science"
                else -> sub
            }
            filtered = filtered.filter { it.subject.equals(mappedSubject, ignoreCase = true) }
        }

        _videos.value = filtered
    }
}
