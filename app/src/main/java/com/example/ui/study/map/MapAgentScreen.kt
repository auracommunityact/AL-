package com.example.ui.study.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAgentScreen(navController: NavController, viewModel: MapAgentViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val mapAction by viewModel.mapAction.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(mapAction) {
        mapAction?.let { action ->
            val uri = when (action) {
                is MapAction.OpenMapSearch -> Uri.parse("geo:0,0?q=${Uri.encode(action.query)}")
                is MapAction.OpenMapDirections -> {
                    if (action.origin != null) {
                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(action.origin)}&destination=${Uri.encode(action.destination)}")
                    } else {
                        Uri.parse("google.navigation:q=${Uri.encode(action.destination)}")
                    }
                }
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            // fallback if google.navigation is not supported
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to browser
                val fallbackUri = when (action) {
                    is MapAction.OpenMapSearch -> Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(action.query)}")
                    is MapAction.OpenMapDirections -> Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(action.destination)}")
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
            }
            viewModel.clearMapAction()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map Agent") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask for places or directions...") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    text = message.text,
                    color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
