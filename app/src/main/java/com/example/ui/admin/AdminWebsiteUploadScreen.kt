package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
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
import com.example.data.models.Website
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWebsiteUploadScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { com.example.data.repository.AuraRepository() }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Website") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImagePickerSection(
                title = "Website Logo",
                selectedImageUri = selectedImageUri,
                onImageSelected = { selectedImageUri = it }
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Website Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Website URL (e.g. https://labs.google)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank() || description.isBlank() || url.isBlank() || selectedImageUri == null) {
                        Toast.makeText(context, "Please fill all fields and select a logo", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isUploading = true
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            // Upload Image
                            var logoUrl = ""
                            selectedImageUri?.let { uri ->
                                val bytes = com.example.utils.StorageUtils.compressImage(context, uri)
                                if (bytes != null) {
                                    val fileName = "website_${System.currentTimeMillis()}.jpg"
                                    logoUrl = repository.uploadCoverImage(bytes, fileName)
                                }
                            }

                            if (logoUrl.isEmpty()) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                    isUploading = false
                                }
                                return@launch
                            }

                            val website = Website(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                logo = logoUrl,
                                url = url,
                                createdAt = System.currentTimeMillis()
                            )

                            com.example.data.repository.AuraRepository().addWebsite(website)
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Website uploaded successfully", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error: ${e.message}. Note: You might need to create 'websites' table in Supabase.", Toast.LENGTH_LONG).show()
                                isUploading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Upload Website")
                }
            }
        }
    }
}
