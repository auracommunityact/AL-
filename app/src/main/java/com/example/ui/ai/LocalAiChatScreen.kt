package com.example.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ai.AuraLocalAITutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiMessage(val text: String, val isUser: Boolean, val isStreaming: Boolean = false, val sourceInfo: String? = null)

class LocalAiViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()
    
    private val _isOffline = MutableStateFlow(true) // Local-first architecture
    val isOffline = _isOffline.asStateFlow()

    private var generationJob: Job? = null

    init {
        _messages.value = listOf(
            AiMessage(
                "Hello! I am Aura AI, your personal learning assistant. I am currently running in **Local Offline Mode**.\n\nHow can I help you with your studies today?",
                isUser = false
            )
        )
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return
        
        val userMsg = AiMessage(query, isUser = true)
        _messages.value = _messages.value + userMsg
        
        val initialBotMsg = AiMessage("", isUser = false, isStreaming = true)
        _messages.value = _messages.value + initialBotMsg
        
        _isGenerating.value = true
        
        generationJob = viewModelScope.launch {
            try {
                // Determine if we need to retrieve local knowledge
                val responseFlow = AuraLocalAITutor.generateChatResponse(query, useRag = true)
                var currentResponse = ""
                
                responseFlow.collect { chunk ->
                    currentResponse += chunk
                    // Update the last message
                    val currentList = _messages.value.toMutableList()
                    val lastIndex = currentList.size - 1
                    currentList[lastIndex] = currentList[lastIndex].copy(text = currentResponse)
                    _messages.value = currentList
                }
                
                // Finalize
                val finalList = _messages.value.toMutableList()
                val lastIndex = finalList.size - 1
                finalList[lastIndex] = finalList[lastIndex].copy(isStreaming = false, sourceInfo = "Aura Learning Knowledge Base")
                _messages.value = finalList
                
            } catch (e: Exception) {
                 val currentList = _messages.value.toMutableList()
                 val lastIndex = currentList.size - 1
                 currentList[lastIndex] = currentList[lastIndex].copy(text = "An error occurred: ${e.message}", isStreaming = false)
                 _messages.value = currentList
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        val finalList = _messages.value.toMutableList()
        val lastIndex = finalList.size - 1
        if (finalList.isNotEmpty() && finalList[lastIndex].isStreaming) {
            finalList[lastIndex] = finalList[lastIndex].copy(isStreaming = false)
            _messages.value = finalList
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAiChatScreen(navController: NavController, rootNavController: NavController, viewModel: LocalAiViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Aura AI", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOffline) Icons.Default.CloudOff else Icons.Default.AutoAwesome,
                                contentDescription = "Status",
                                modifier = Modifier.size(14.dp),
                                tint = if (isOffline) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isOffline) "Local Offline Mode" else "Cloud Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOffline) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { rootNavController.navigate("aura_ai_settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Aura AI...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isGenerating) {
                        FloatingActionButton(onClick = { viewModel.stopGeneration() }, containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Stop")
                        }
                    } else {
                        FloatingActionButton(
                            onClick = { 
                                viewModel.sendMessage(inputQuery)
                                inputQuery = ""
                            },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }
    }
}

@Composable
fun MessageBubble(message: AiMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(shape)
                .background(bgColor)
                .padding(16.dp)
        ) {
            // Very simple Markdown simulation for MVP, replacing with plain text 
            // In a real app we would use a Markdown renderer library like richtext-compose
            Text(
                text = message.text.replace("**", ""), 
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (message.isStreaming) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }
            
            if (message.sourceInfo != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Source: ${message.sourceInfo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
