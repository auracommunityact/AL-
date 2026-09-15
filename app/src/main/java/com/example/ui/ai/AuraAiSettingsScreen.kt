package com.example.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraAiSettingsScreen(rootNavController: NavController) {
    var isModelDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aura AI Setup") },
                navigationIcon = {
                    IconButton(onClick = { rootNavController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Local Model Configuration", style = MaterialTheme.typography.titleLarge)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model: Gemma-2B-Instruct-Q4 (GGUF)", fontWeight = FontWeight.Bold)
                    Text("Version: v1.2")
                    Text("Size: ~1.4 GB")
                    Spacer(Modifier.height(8.dp))
                    
                    if (isModelDownloaded) {
                        Text("Status: Installed \u2705", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { isModelDownloaded = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Model")
                        }
                    } else if (isDownloading) {
                        Text("Status: Downloading...")
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        // Simulating download
                        LaunchedEffect(Unit) {
                            while (progress < 1f) {
                                kotlinx.coroutines.delay(100)
                                progress += 0.05f
                            }
                            isDownloading = false
                            isModelDownloaded = true
                        }
                    } else {
                        Text("Status: Not Installed \u26A0\uFE0F")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { 
                            isDownloading = true 
                            progress = 0f
                        }) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download Model")
                        }
                    }
                }
            }
            
            Divider()
            
            Text("RAG & Memory Settings", style = MaterialTheme.typography.titleMedium)
            
            var useRag by remember { mutableStateOf(true) }
            var useMemory by remember { mutableStateOf(true) }
            
            ListItem(
                headlineContent = { Text("Use Aura Learning Knowledge Base") },
                supportingContent = { Text("Searches local books & videos to answer questions.") },
                trailingContent = {
                    Switch(checked = useRag, onCheckedChange = { useRag = it })
                }
            )
            
            ListItem(
                headlineContent = { Text("Conversation Memory") },
                supportingContent = { Text("Remember previous messages in the chat.") },
                trailingContent = {
                    Switch(checked = useMemory, onCheckedChange = { useMemory = it })
                }
            )
        }
    }
}
