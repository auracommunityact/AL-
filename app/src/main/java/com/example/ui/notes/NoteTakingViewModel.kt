package com.example.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteTakingViewModel(private val noteDao: NoteDao) : ViewModel() {
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes = _notes.asStateFlow()

    fun loadNotes(url: String) {
        viewModelScope.launch {
            _notes.value = noteDao.getNotesForUrl(url)
        }
    }

    fun saveNote(content: String, url: String) {
        viewModelScope.launch {
            noteDao.insertNote(NoteEntity(content = content, relatedUrl = url))
            loadNotes(url)
        }
    }
}
