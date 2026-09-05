package com.example.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LessonNotesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LessonNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = LessonNotesDataStore(application)

    private val _currentNote = MutableStateFlow("")
    val currentNote: StateFlow<String> = _currentNote

    fun loadNoteForLesson(lessonId: String) {
        viewModelScope.launch {
            dataStore.getNoteForLesson(lessonId).collect { note ->
                _currentNote.value = note
            }
        }
    }

    fun saveNoteForLesson(lessonId: String, note: String) {
        viewModelScope.launch {
            dataStore.saveNoteForLesson(lessonId, note)
            // It will automatically update via collect if we keep listening, but for simplicity:
            _currentNote.value = note
        }
    }
}

class LessonNotesViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LessonNotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LessonNotesViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
