package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Upload
import android.provider.OpenableColumns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Book
import com.example.data.models.Video
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditContentScreen(navController: NavController, id: String, isVideo: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Form fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Science") }
    var className by remember { mutableStateOf("10th") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var contentUrl by remember { mutableStateOf("") } // PDF URL or YouTube URL
    var teacher by remember { mutableStateOf("") } // For videos

    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showClassDropdown by remember { mutableStateOf(false) }

    val classes = listOf("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th", "11th", "12th")
    val subjects = listOf("Mathematics", "Science", "English", "Hindi", "Social Studies", "Computer Science")

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
        if (uri != null) {
            val name = getFileName(context, uri) ?: "Selected PDF"
            selectedPdfName = name
            if (title.isBlank()) {
                title = name.substringBeforeLast(".")
            }
        }
    }

    // Load initial content
    LaunchedEffect(id) {
        coroutineScope.launch {
            isLoading = true
            try {
                if (isVideo) {
                    val video = repository.getVideoById(id)
                    if (video != null) {
                        title = video.title
                        description = video.description
                        subject = video.subject
                        className = video.className
                        imageUrl = video.thumbnail
                        contentUrl = video.videoUrl
                        teacher = video.teacher
                    } else {
                        Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                } else {
                    val book = repository.getBook(id)
                    if (book != null) {
                        title = book.bookName
                        subject = book.subject
                        className = book.className
                        imageUrl = book.coverImage
                        contentUrl = book.pdfUrl
                    } else {
                        Toast.makeText(context, "Book not found", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isVideo) "Edit Video" else "Edit Book") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isVideo) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showSubjectDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Subject")
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showSubjectDropdown = true }
                        )
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = className,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class / Grade") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showClassDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Class")
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showClassDropdown = true }
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSubjectDropdown,
                        onDismissRequest = { showSubjectDropdown = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    subject = sub
                                    showSubjectDropdown = false
                                }
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showClassDropdown,
                        onDismissRequest = { showClassDropdown = false }
                    ) {
                        classes.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    className = cls
                                    showClassDropdown = false
                                }
                            )
                        }
                    }
                }
                
                if (isVideo) {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Text(if (isVideo) "Thumbnail Image" else "Cover Image", style = MaterialTheme.typography.titleMedium)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Cover Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to select image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                if (!isVideo) {
                    Text("Select PDF Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (selectedPdfUri != null) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedPdfName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Change File")
                                    }
                                    TextButton(
                                        onClick = {
                                            selectedPdfUri = null
                                            selectedPdfName = ""
                                        }
                                    ) {
                                        Text("Clear", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (contentUrl.isNotEmpty()) "Using current online PDF URL" else "No PDF file selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") }
                                ) {
                                    Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose PDF from Device")
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = "OR enter PDF URL manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                OutlinedTextField(
                    value = contentUrl,
                    onValueChange = { contentUrl = it },
                    label = { Text(if (isVideo) "YouTube Video ID or URL" else "PDF Document URL (Optional if file is chosen)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (title.isBlank() || subject.isBlank() || className.isBlank()) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isVideo && selectedPdfUri == null && contentUrl.isBlank()) {
                            Toast.makeText(context, "Please select a PDF file or enter a PDF URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isVideo && contentUrl.isBlank()) {
                            Toast.makeText(context, "Please enter a YouTube video ID or URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                var finalImageUrl = imageUrl
                                if (selectedImageUri != null) {
                                    val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                                    val bytes = inputStream?.readBytes()
                                    inputStream?.close()
                                    if (bytes != null) {
                                        val ext = context.contentResolver.getType(selectedImageUri!!)?.split("/")?.lastOrNull() ?: "jpg"
                                        val fileName = "${UUID.randomUUID()}.$ext"
                                        val uploadedUrl = repository.uploadCoverImage(bytes, fileName)
                                        if (uploadedUrl.isNotEmpty()) {
                                            finalImageUrl = uploadedUrl
                                        } else {
                                            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                var finalContentUrl = contentUrl
                                if (!isVideo && selectedPdfUri != null) {
                                    val inputStream = context.contentResolver.openInputStream(selectedPdfUri!!)
                                    val bytes = inputStream?.readBytes()
                                    inputStream?.close()
                                    if (bytes != null) {
                                        val ext = "pdf"
                                        val fileName = "${selectedPdfName.substringBeforeLast(".")}_${UUID.randomUUID().toString().take(6)}.$ext"
                                        val uploadedUrl = repository.uploadBookPdf(bytes, fileName)
                                        if (uploadedUrl.isNotEmpty()) {
                                            finalContentUrl = uploadedUrl
                                        } else {
                                            Toast.makeText(context, "Failed to upload PDF", Toast.LENGTH_SHORT).show()
                                            isSaving = false
                                            return@launch
                                        }
                                    }
                                }

                                if (isVideo) {
                                    val videoId = extractYoutubeVideoId(finalContentUrl)
                                    val finalVideoUrl = if (finalContentUrl.contains("youtube.com") || finalContentUrl.contains("youtu.be")) {
                                        finalContentUrl
                                    } else {
                                        "https://www.youtube.com/watch?v=$finalContentUrl"
                                    }
                                    val updatedVideo = Video(
                                        id = id,
                                        title = title,
                                        description = description,
                                        className = className,
                                        subject = subject,
                                        thumbnail = finalImageUrl,
                                        videoUrl = finalVideoUrl,
                                        youtubeVideoId = videoId,
                                        chapter = title,
                                        partNumber = 1,
                                        teacher = teacher.ifEmpty { "Aura Teacher" },
                                        duration = "15:00",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.updateVideo(updatedVideo)
                                    Toast.makeText(context, "Video updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updatedBook = Book(
                                        id = id,
                                        bookName = title,
                                        className = className,
                                        subject = subject,
                                        coverImage = finalImageUrl,
                                        pdfUrl = finalContentUrl,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.updateBook(updatedBook)
                                    Toast.makeText(context, "Book updated successfully", Toast.LENGTH_SHORT).show()
                                }
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
