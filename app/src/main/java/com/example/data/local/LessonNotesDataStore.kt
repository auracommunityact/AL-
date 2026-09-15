package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lessonNotesDataStore: DataStore<Preferences> by preferencesDataStore(name = "lesson_notes")

class LessonNotesDataStore(private val context: Context) {

    fun getNoteForLesson(lessonId: String): Flow<String> {
        val key = stringPreferencesKey(lessonId)
        return context.lessonNotesDataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }

    suspend fun saveNoteForLesson(lessonId: String, note: String) {
        val key = stringPreferencesKey(lessonId)
        context.lessonNotesDataStore.edit { preferences ->
            preferences[key] = note
        }
    }
}
