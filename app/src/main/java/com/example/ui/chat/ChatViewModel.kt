package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repository.ChatRepository
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val auraRepository = com.example.data.repository.AuraRepository()
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.example.data.models.User>>(emptyList())
    val searchResults: StateFlow<List<com.example.data.models.User>> = _searchResults.asStateFlow()
    
    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()
    
    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userPresences = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val userPresences: StateFlow<Map<String, UserPresence>> = _userPresences.asStateFlow()

    private val _typingStatuses = MutableStateFlow<Map<String, TypingStatus>>(emptyMap())
    val typingStatuses: StateFlow<Map<String, TypingStatus>> = _typingStatuses.asStateFlow()

    private val _reactions = MutableStateFlow<Map<String, List<MessageReaction>>>(emptyMap())
    val reactions: StateFlow<Map<String, List<MessageReaction>>> = _reactions.asStateFlow()

    private val _reads = MutableStateFlow<Map<String, List<MessageRead>>>(emptyMap())
    val reads: StateFlow<Map<String, List<MessageRead>>> = _reads.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _replyToMessage = MutableStateFlow<Message?>(null)
    val replyToMessage: StateFlow<Message?> = _replyToMessage.asStateFlow()

    private var messagesJob: Job? = null
    private var reactionsJob: Job? = null
    private var readsJob: Job? = null
    private var typingJob: Job? = null
    private var presenceJob: Job? = null

    val currentUserId: String?
        get() = SupabaseService.client.auth.currentUserOrNull()?.id

    init {
        updatePresence(true)
        observePresence()
    }

    private fun updatePresence(isOnline: Boolean) {
        viewModelScope.launch {
            try {
                repository.updatePresence(isOnline)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observePresence() {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            try {
                repository.subscribeToPresence().collect { presence ->
                    val current = _userPresences.value.toMutableMap()
                    current[presence.userId] = presence
                    _userPresences.value = current
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updatePresence(false)
        cancelAllJobs()
    }

    private fun cancelAllJobs() {
        messagesJob?.cancel()
        reactionsJob?.cancel()
        readsJob?.cancel()
        typingJob?.cancel()
        presenceJob?.cancel()
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = auraRepository.searchUsers(query)
                val blocked = repository.getBlockedUsers().map { it.blockedUserId }.toSet()
                _searchResults.value = results.filter { it.id != currentUserId && !blocked.contains(it.id) }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startConversation(otherUserId: String, otherUserName: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.getOrCreateConversation(otherUserId, otherUserName)
                onComplete(id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
        
    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val convos = repository.getConversations()
                _conversations.value = convos
                _blockedUsers.value = repository.getBlockedUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMessages(conversationId: String) {
        messagesJob?.cancel()
        reactionsJob?.cancel()
        readsJob?.cancel()
        typingJob?.cancel()
        
        viewModelScope.launch {
            _isLoading.value = true
            _replyToMessage.value = null
            try {
                val convo = repository.getConversation(conversationId)
                _currentConversation.value = convo

                val msgs = repository.getMessages(conversationId)
                _currentMessages.value = msgs
                
                val reactionsMap = mutableMapOf<String, List<MessageReaction>>()
                val readsMap = mutableMapOf<String, List<MessageRead>>()
                
                msgs.forEach { msg ->
                    try {
                        reactionsMap[msg.id] = repository.getReactions(msg.id)
                    } catch (e: Exception) {
                        reactionsMap[msg.id] = emptyList()
                    }
                    try {
                        readsMap[msg.id] = repository.getReads(msg.id)
                    } catch (e: Exception) {
                        readsMap[msg.id] = emptyList()
                    }
                }
                _reactions.value = reactionsMap
                _reads.value = readsMap

                msgs.filter { it.senderId != currentUserId }.forEach { msg ->
                    try {
                        repository.markMessageAsRead(msg.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                messagesJob = viewModelScope.launch {
                    try {
                        repository.subscribeToMessageUpdates(conversationId).collect { update ->
                            when (update) {
                                is MessageUpdate.Insert -> {
                                    if (!_currentMessages.value.any { it.id == update.message.id }) {
                                        _currentMessages.value = _currentMessages.value + update.message
                                        if (update.message.senderId != currentUserId) {
                                            repository.markMessageAsRead(update.message.id)
                                        }
                                    }
                                }
                                is MessageUpdate.Update -> {
                                    _currentMessages.value = _currentMessages.value.map {
                                        if (it.id == update.message.id) update.message else it
                                    }
                                }
                                is MessageUpdate.Delete -> {
                                    _currentMessages.value = _currentMessages.value.filter { it.id != update.messageId }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                reactionsJob = viewModelScope.launch {
                    try {
                        repository.subscribeToReactions(conversationId).collect { newReaction ->
                            val current = _reactions.value.toMutableMap()
                            val list = current[newReaction.messageId]?.toMutableList() ?: mutableListOf()
                            if (!list.any { it.userId == newReaction.userId && it.reaction == newReaction.reaction }) {
                                list.add(newReaction)
                                current[newReaction.messageId] = list
                                _reactions.value = current
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                readsJob = viewModelScope.launch {
                    try {
                        repository.subscribeToReads(conversationId).collect { newRead ->
                            val current = _reads.value.toMutableMap()
                            val list = current[newRead.messageId]?.toMutableList() ?: mutableListOf()
                            if (!list.any { it.userId == newRead.userId }) {
                                list.add(newRead)
                                current[newRead.messageId] = list
                                _reads.value = current
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                typingJob = viewModelScope.launch {
                    try {
                        repository.subscribeToTyping(conversationId).collect { status ->
                            val current = _typingStatuses.value.toMutableMap()
                            if (status.status == "none") {
                                current.remove(status.userId)
                            } else {
                                current[status.userId] = status
                            }
                            _typingStatuses.value = current
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(conversationId: String, text: String, type: String = "text") {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val replyId = _replyToMessage.value?.id
                val msg = Message(
                    conversationId = conversationId,
                    senderId = userId,
                    text = text,
                    type = type,
                    replyToId = replyId
                )
                repository.sendMessage(msg)
                repository.updateConversationLastMessage(conversationId, text)
                _replyToMessage.value = null
                setTypingStatus(conversationId, "none")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTypingStatus(conversationId: String, status: String) {
        viewModelScope.launch {
            try {
                repository.setTypingStatus(conversationId, status)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            try {
                repository.addReaction(messageId, reaction)
                val current = _reactions.value.toMutableMap()
                val list = current[messageId]?.toMutableList() ?: mutableListOf()
                val userId = currentUserId ?: return@launch
                if (!list.any { it.userId == userId && it.reaction == reaction }) {
                    list.add(MessageReaction(messageId, userId, reaction))
                    current[messageId] = list
                    _reactions.value = current
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            try {
                repository.removeReaction(messageId, reaction)
                val current = _reactions.value.toMutableMap()
                val list = current[messageId]?.toMutableList() ?: return@launch
                val userId = currentUserId ?: return@launch
                list.removeAll { it.userId == userId && it.reaction == reaction }
                current[messageId] = list
                _reactions.value = current
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(messageId, forEveryone)
                if (!forEveryone) {
                    _currentMessages.value = _currentMessages.value.filter { it.id != messageId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            try {
                repository.editMessage(messageId, newText)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectMessageToReply(message: Message?) {
        _replyToMessage.value = message
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                repository.blockUser(targetUserId)
                loadConversations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                repository.unblockUser(targetUserId)
                loadConversations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reportUser(targetUserId: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.reportUser(targetUserId, reason)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadAndSendAttachment(conversationId: String, fileName: String, bytes: ByteArray, type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            setTypingStatus(conversationId, "uploading")
            try {
                val url = repository.uploadAttachment(fileName, bytes)
                if (url.isNotEmpty()) {
                    val userId = currentUserId ?: return@launch
                    val msg = Message(
                        conversationId = conversationId,
                        senderId = userId,
                        text = "Shared an attachment: $fileName",
                        type = type,
                        attachmentUrl = url
                    )
                    repository.sendMessage(msg)
                    repository.updateConversationLastMessage(conversationId, "Shared an attachment")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                setTypingStatus(conversationId, "none")
            }
        }
    }
}
