package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NotificationPreferences
import com.example.data.local.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {
    val booksEnabled = context.dataStore.data.map { it[NotificationPreferences.BOOKS_ENABLED] ?: true }
    val videosEnabled = context.dataStore.data.map { it[NotificationPreferences.VIDEOS_ENABLED] ?: true }
    val resourcesEnabled = context.dataStore.data.map { it[NotificationPreferences.RESOURCES_ENABLED] ?: true }

    fun toggleBooks(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[NotificationPreferences.BOOKS_ENABLED] = enabled }
        }
    }

    fun toggleVideos(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[NotificationPreferences.VIDEOS_ENABLED] = enabled }
        }
    }

    fun toggleResources(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[NotificationPreferences.RESOURCES_ENABLED] = enabled }
        }
    }
}
