package com.example.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Flashcard
import com.example.data.models.FlashcardDeck
import com.example.data.models.Note
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(private val repository: AuraRepository) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _decks = MutableStateFlow<List<FlashcardDeck>>(emptyList())
    val decks: StateFlow<List<FlashcardDeck>> = _decks.asStateFlow()

    fun loadStudyData(userId: String) {
        viewModelScope.launch {
            _notes.value = repository.getNotesByUser(userId)
            _decks.value = repository.getFlashcardDecksByUser(userId)
        }
    }

    fun addNote(note: Note, userId: String) {
        viewModelScope.launch {
            repository.addNote(note)
            loadStudyData(userId)
        }
    }

    fun deleteNote(noteId: String, userId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            loadStudyData(userId)
        }
    }

    fun addDeck(deck: FlashcardDeck, userId: String) {
        viewModelScope.launch {
            repository.addFlashcardDeck(deck)
            loadStudyData(userId)
        }
    }

    fun deleteDeck(deckId: String, userId: String) {
        viewModelScope.launch {
            repository.deleteFlashcardDeck(deckId)
            loadStudyData(userId)
        }
    }

    suspend fun getFlashcards(deckId: String): List<Flashcard> {
        return repository.getFlashcardsByDeck(deckId)
    }

    fun addFlashcard(card: Flashcard) {
        viewModelScope.launch {
            repository.addFlashcard(card)
        }
    }
}
