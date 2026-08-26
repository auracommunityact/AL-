package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel) {
    val booksEnabled by viewModel.booksEnabled.collectAsState(initial = true)
    val videosEnabled by viewModel.videosEnabled.collectAsState(initial = true)
    val resourcesEnabled by viewModel.resourcesEnabled.collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Notification Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Books Notifications", modifier = Modifier.weight(1f))
                Switch(checked = booksEnabled, onCheckedChange = { viewModel.toggleBooks(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Video Lessons Notifications", modifier = Modifier.weight(1f))
                Switch(checked = videosEnabled, onCheckedChange = { viewModel.toggleVideos(it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Resources Notifications", modifier = Modifier.weight(1f))
                Switch(checked = resourcesEnabled, onCheckedChange = { viewModel.toggleResources(it) })
            }
        }
    }
}
