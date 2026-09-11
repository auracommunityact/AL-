package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.models.AppUpdateConfig
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppUpdatesScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var latestVersionName by remember { mutableStateOf("1.0.0") }
    var latestVersionCode by remember { mutableStateOf("1") }
    var minimumSupportedVersionCode by remember { mutableStateOf("1") }
    var updateTitle by remember { mutableStateOf("New Update Available") }
    var updateMessage by remember { mutableStateOf("A new version of the app is available. Please update to enjoy the latest features and bug fixes.") }
    var changelog by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("https://aura-learning.en.uptodown.com/android") }
    var forceUpdate by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val config = SupabaseService.client.postgrest["app_updates"]
                .select { 
                    order("latest_version_code", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<AppUpdateConfig>()

            if (config != null) {
                latestVersionName = config.latestVersionName
                latestVersionCode = config.latestVersionCode.toString()
                minimumSupportedVersionCode = config.minimumSupportedVersionCode.toString()
                updateTitle = config.updateTitle
                updateMessage = config.updateMessage
                changelog = config.changelog ?: ""
                downloadUrl = config.downloadUrl
                forceUpdate = config.forceUpdate
                isActive = config.isActive
            }
        } catch (e: Exception) {
            // Ignored, might be empty table
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Updates Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                if (successMessage != null) {
                    Text(successMessage!!, color = MaterialTheme.colorScheme.primary)
                }

                OutlinedTextField(
                    value = latestVersionName,
                    onValueChange = { latestVersionName = it },
                    label = { Text("Latest Version Name (e.g. 1.0.6)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = latestVersionCode,
                    onValueChange = { latestVersionCode = it.filter { char -> char.isDigit() } },
                    label = { Text("Latest Version Code (e.g. 6)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minimumSupportedVersionCode,
                    onValueChange = { minimumSupportedVersionCode = it.filter { char -> char.isDigit() } },
                    label = { Text("Minimum Supported Version Code (e.g. 1)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = updateTitle,
                    onValueChange = { updateTitle = it },
                    label = { Text("Update Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = updateMessage,
                    onValueChange = { updateMessage = it },
                    label = { Text("Update Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = changelog,
                    onValueChange = { changelog = it },
                    label = { Text("Changelog") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    label = { Text("Uptodown Download URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = forceUpdate, onCheckedChange = { forceUpdate = it })
                    Text("Force Update (Ignore minimum version)")
                }

                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Enable this Update")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val newConfig = AppUpdateConfig(
                                    latestVersionName = latestVersionName,
                                    latestVersionCode = latestVersionCode.toIntOrNull() ?: 1,
                                    minimumSupportedVersionCode = minimumSupportedVersionCode.toIntOrNull() ?: 1,
                                    updateTitle = updateTitle,
                                    updateMessage = updateMessage,
                                    changelog = changelog,
                                    downloadUrl = downloadUrl,
                                    forceUpdate = forceUpdate,
                                    isActive = isActive
                                )
                                SupabaseService.client.postgrest["app_updates"].insert(newConfig)
                                successMessage = "Update configuration saved successfully!"
                            } catch (e: Exception) {
                                errorMessage = "Failed to save: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Update Configuration")
                }
            }
        }
    }
}
