package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.User
import com.example.data.models.UserPresence
import com.example.data.repository.AuraRepository
import com.example.data.repository.ChatRepository
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserSearchViewModel(
    private val repository: AuraRepository = AuraRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userPresences = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val userPresences: StateFlow<Map<String, UserPresence>> = _userPresences.asStateFlow()

    init {
        setupSearch()
        observePresence()
    }

    private fun observePresence() {
        viewModelScope.launch {
            try {
                chatRepository.subscribeToPresence().collect { presence ->
                    val current = _userPresences.value.toMutableMap()
                    current[presence.userId] = presence
                    _userPresences.value = current
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .onEach { query ->
                    if (query.length >= 2) {
                        _isLoading.value = true
                    }
                }
                .collect { query ->
                    if (query.length >= 2) {
                        try {
                            val currentUserId = SupabaseService.client.auth.currentUserOrNull()?.id
                            val results = repository.searchUsers(query)
                            val blocked = chatRepository.getBlockedUsers().map { it.blockedUserId }.toSet()
                            _searchResults.value = results.filter { 
                                it.id != currentUserId && 
                                !blocked.contains(it.id) &&
                                it.accountStatus != "Suspended" &&
                                it.accountStatus != "Deleted"
                            }
                        } catch (e: Exception) {
                            _searchResults.value = emptyList()
                        } finally {
                            _isLoading.value = false
                        }
                    } else {
                        _searchResults.value = emptyList()
                        _isLoading.value = false
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun startConversation(otherUserId: String, otherUserName: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = chatRepository.getOrCreateConversation(otherUserId, otherUserName)
                onComplete(id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

