package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

object NotificationPreferences {
    val BOOKS_ENABLED = booleanPreferencesKey("books_enabled")
    val VIDEOS_ENABLED = booleanPreferencesKey("videos_enabled")
    val RESOURCES_ENABLED = booleanPreferencesKey("resources_enabled")
}
