package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AuraRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.books.BooksViewModel
import com.example.ui.home.HomeViewModel
import com.example.ui.videos.VideosViewModel
import com.example.ui.profile.MyLibraryViewModel

object ViewModelFactory : ViewModelProvider.Factory {
    private val repository = AuraRepository()

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(BooksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BooksViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(VideosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideosViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.study.StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.study.StudyViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.videos.VideoPlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.videos.VideoPlayerViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(MyLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyLibraryViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.chat.ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.chat.ChatViewModel() as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.gamification.LeaderboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.gamification.LeaderboardViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.search.UserSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.search.UserSearchViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.feedback.FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.feedback.FeedbackViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(com.example.ui.admin.AdminFeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.ui.admin.AdminFeedbackViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
