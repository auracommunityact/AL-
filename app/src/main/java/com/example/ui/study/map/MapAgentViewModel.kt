package com.example.ui.study.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val isLoading: Boolean = false)

sealed class MapAction {
    data class OpenMapSearch(val query: String) : MapAction()
    data class OpenMapDirections(val destination: String, val origin: String?) : MapAction()
}

class MapAgentViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hi! I'm your Map Agent. I can help you find places, routes, and directions. What are you looking for?", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _mapAction = MutableStateFlow<MapAction?>(null)
    val mapAction: StateFlow<MapAction?> = _mapAction.asStateFlow()

    fun clearMapAction() {
        _mapAction.value = null
    }

    fun sendMessage(text: String) {
        val userMsg = ChatMessage(text, isUser = true)
        val loadingMsg = ChatMessage("", isUser = false, isLoading = true)
        _messages.value = _messages.value + userMsg + loadingMsg

        viewModelScope.launch {
            try {
                val response = generateContent(text)
                
                _messages.value = _messages.value.dropLast(1) // Remove loading
                
                if (response.text != null) {
                    _messages.value = _messages.value + ChatMessage(response.text, isUser = false)
                }
                
                if (response.action != null) {
                    _mapAction.value = response.action
                }
                
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1)
                _messages.value = _messages.value + ChatMessage("Error: ${e.message}", isUser = false)
            }
        }
    }

    private suspend fun generateContent(prompt: String): AgentResponse {
        val lowerPrompt = prompt.lowercase()
        var responseText: String? = null
        var action: MapAction? = null

        if (lowerPrompt.contains("directions to") || lowerPrompt.contains("route to") || lowerPrompt.contains("navigate to")) {
            val keyword = listOf("directions to", "route to", "navigate to").find { lowerPrompt.contains(it) }!!
            val afterKeyword = prompt.substring(lowerPrompt.indexOf(keyword) + keyword.length).trim()
            
            // Check for "from"
            val fromIndex = afterKeyword.indexOf(" from ", ignoreCase = true)
            if (fromIndex != -1) {
                val destination = afterKeyword.substring(0, fromIndex).trim()
                val origin = afterKeyword.substring(fromIndex + 6).trim()
                action = MapAction.OpenMapDirections(destination, origin)
                responseText = "Getting directions to $destination from $origin..."
            } else {
                val destination = afterKeyword
                action = MapAction.OpenMapDirections(destination, null)
                responseText = "Getting directions to $destination..."
            }
        } else if (lowerPrompt.contains("where is") || lowerPrompt.contains("find") || lowerPrompt.contains("search for") || lowerPrompt.contains("show me")) {
            val keyword = listOf("where is", "search for", "show me", "find").find { lowerPrompt.contains(it) }!!
            val destination = prompt.substring(lowerPrompt.indexOf(keyword) + keyword.length).trim().removeSuffix("?")
            action = MapAction.OpenMapSearch(destination)
            responseText = "Searching for $destination..."
        } else {
            responseText = "I'm a simple Map Agent. Try asking me:\n- 'directions to [place]'\n- 'directions to [place] from [place]'\n- 'where is [place]'\n- 'search for [place]'"
        }

        // Add a slight delay to simulate processing
        delay(600)
        
        return AgentResponse(responseText, action)
    }
    
    data class AgentResponse(val text: String?, val action: MapAction?)
}
