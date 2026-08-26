package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.repository.AuraRepository
import com.example.data.repository.notifications.SupabaseNotification
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }

    // Form inputs
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var redirectLinkInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Announcement") }
    var priorityInput by remember { mutableStateOf("Normal") }
    var targetTypeInput by remember { mutableStateOf("All") } // All, Class, User
    var targetValueInput by remember { mutableStateOf("") }
    var actionButtonTextInput by remember { mutableStateOf("") }

    var isSending by remember { mutableStateOf(false) }

    // Sent History
    var sentHistory by remember { mutableStateOf<List<SupabaseNotification>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(true) }

    val supabase = remember { com.example.data.supabase.SupabaseService.client }

    fun refreshHistory() {
        coroutineScope.launch {
            isLoadingHistory = true
            try {
                val list = withContext(Dispatchers.IO) {
                    supabase.from("notifications").select().decodeList<SupabaseNotification>()
                }
                sentHistory = list.sortedByDescending { it.created_at }
            } catch (e: Exception) {
                // Ignore silent error
            } finally {
                isLoadingHistory = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Notification") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Notification Composer
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Compose Announcement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Notification Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Description / Message *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                ImagePickerSection(
                    title = "Notification Photo (Optional)",
                    selectedImageUri = selectedImageUri,
                    onImageSelected = { selectedImageUri = it }
                )

                OutlinedTextField(
                    value = redirectLinkInput,
                    onValueChange = { redirectLinkInput = it },
                    label = { Text("Redirect Link / deepLink (e.g. books, main?tab=videos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. books, study_planner, notifications") }
                )

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Category (e.g. Announcement, Event, Quiz)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Normal", "High").forEach { priority ->
                        FilterChip(
                            selected = priorityInput == priority,
                            onClick = { priorityInput = priority },
                            label = { Text(priority) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Target Audience", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Class", "User").forEach { type ->
                        FilterChip(
                            selected = targetTypeInput == type,
                            onClick = { targetTypeInput = type },
                            label = { Text(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (targetTypeInput != "All") {
                    OutlinedTextField(
                        value = targetValueInput,
                        onValueChange = { targetValueInput = it },
                        label = { Text(if (targetTypeInput == "Class") "Class Name (e.g. 10th-A)" else "User ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = actionButtonTextInput,
                    onValueChange = { actionButtonTextInput = it },
                    label = { Text("Action Button Text (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. View Details, Open Link") }
                )

                Button(
                    onClick = {
                        if (titleInput.isBlank() || descInput.isBlank()) {
                            Toast.makeText(context, "Title and Description are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            isSending = true
                            try {
                                var finalImageUrl: String? = null
                                
                                if (selectedImageUri != null) {
                                    val bytes = com.example.utils.StorageUtils.compressImage(context, selectedImageUri!!)
                                    if (bytes != null) {
                                        val fileName = "notification_${UUID.randomUUID()}.jpg"
                                        val uploadedUrl = repository.uploadImage(bytes, fileName, "notifications")
                                        if (uploadedUrl.isNotEmpty()) {
                                            finalImageUrl = uploadedUrl
                                        }
                                    }
                                }

                                val notification = SupabaseNotification(
                                    id = UUID.randomUUID().toString(),
                                    title = titleInput,
                                    description = descInput,
                                    image_url = finalImageUrl,
                                    category = categoryInput.ifBlank { "Announcement" },
                                    deep_link = redirectLinkInput.ifBlank { null },
                                    created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                                    priority = priorityInput,
                                    target_type = targetTypeInput,
                                    target_value = targetValueInput.ifBlank { null },
                                    action_button_text = actionButtonTextInput.ifBlank { null }
                                )
                                repository.addNotification(notification)
                                Toast.makeText(context, "Notification sent successfully!", Toast.LENGTH_SHORT).show()

                                // Clear inputs
                                titleInput = ""
                                descInput = ""
                                selectedImageUri = null
                                redirectLinkInput = ""
                                categoryInput = "Announcement"
                                targetValueInput = ""
                                actionButtonTextInput = ""

                                refreshHistory()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error sending: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send to All Users")
                    }
                }
            }

            // Vertical Divider for wide screen layouts
            VerticalDivider(modifier = Modifier.fillMaxHeight())

            // Right Column: Sent Notifications History
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Notification History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (sentHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No previously sent notifications.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sentHistory, key = { it.id }) { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (notif.image_url != null) {
                                        AsyncImage(
                                            model = notif.image_url,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Notifications,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = notif.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        val idValue: Any = notif.id.toLongOrNull() ?: notif.id
                                                        supabase.from("notifications").delete {
                                                            filter { eq("id", idValue) }
                                                        }
                                                    }
                                                    Toast.makeText(context, "Deleted from server", Toast.LENGTH_SHORT).show()
                                                    AuraRepository.notifyNotificationsChanged()
                                                    refreshHistory()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
