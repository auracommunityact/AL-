package com.example.ui.admin

import android.widget.Toast
import android.net.Uri
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
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import android.provider.OpenableColumns
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import com.example.data.models.Book
import com.example.data.models.Video
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch
import java.util.UUID

fun extractYoutubeVideoId(url: String): String {
    if (url.isEmpty()) return ""
    if (url.length == 11) return url // likely already an ID
    val patterns = listOf(
        "v=",
        "be/",
        "embed/"
    )
    for (pattern in patterns) {
        val index = url.indexOf(pattern)
        if (index != -1) {
            val start = index + pattern.length
            val end = url.indexOfAny(charArrayOf('&', '?'), start)
            return if (end != -1) url.substring(start, end) else url.substring(start)
        }
    }
    return url
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentUploadScreen(navController: NavController, isVideo: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }
    var isUploading by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Science") }
    var className by remember { mutableStateOf("10th") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var contentUrl by remember { mutableStateOf("") } // PDF URL or YouTube URL
    var teacher by remember { mutableStateOf("") } // For videos

    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showClassDropdown by remember { mutableStateOf(false) }

    val classes = listOf("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th", "11th", "12th")
    val subjects = listOf("Mathematics", "Science", "English", "Hindi", "Social Studies", "Computer Science")

    var showPreviewDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isVideo) "Upload Video" else "Upload Book") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
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
            
            ImagePickerSection(
                title = if (isVideo) "Thumbnail Image" else "Cover Image",
                selectedImageUri = selectedImageUri,
                onImageSelected = { selectedImageUri = it },
                existingImageUrl = thumbnailUrl
            )
            
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
                                "No PDF file selected",
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
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = { showPreviewDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Filled.Preview, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Preview Mode")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
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
                            isUploading = true
                            try {
                                var finalImageUrl = thumbnailUrl
                                if (selectedImageUri != null) {
                                    val bytes = com.example.utils.StorageUtils.compressImage(context, selectedImageUri!!)
                                    if (bytes != null) {
                                        val fileName = "thumb_${UUID.randomUUID()}.jpg"
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
                                            isUploading = false
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
                                    val video = Video(
                                        title = title,
                                        description = description,
                                        className = className,
                                        subject = subject,
                                        thumbnail = finalImageUrl.ifEmpty { "https://images.unsplash.com/photo-1596496050827-8299e0220de1?auto=format&fit=crop&w=300&q=80" },
                                        videoUrl = finalVideoUrl,
                                        youtubeVideoId = videoId,
                                        chapter = title,
                                        partNumber = 1,
                                        teacher = teacher.ifEmpty { "Aura Teacher" },
                                        duration = "15:00",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.addVideo(video)
                                    Toast.makeText(context, "Video uploaded successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    val book = Book(
                                        bookName = title,
                                        className = className,
                                        subject = subject,
                                        coverImage = finalImageUrl.ifEmpty { "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=400&auto=format&fit=crop" },
                                        pdfUrl = finalContentUrl,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.addBook(book)
                                    Toast.makeText(context, "Book uploaded successfully", Toast.LENGTH_SHORT).show()
                                }
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Finalize Upload")
                    }
                }
            }
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("App Preview") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("This is how it will appear to users in the app:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isVideo) {
                        // Simulate VideoCard
                        PreviewVideoCard(
                            Video(
                                title = title.ifEmpty { "Sample Video Title" },
                                subject = subject.ifEmpty { "Sample Subject" },
                                teacher = teacher.ifEmpty { "Sample Teacher" },
                                thumbnail = thumbnailUrl
                            )
                        )
                    } else {
                        // Simulate Book Card
                        PreviewBookCard(
                            Book(
                                bookName = title.ifEmpty { "Sample Book Title" },
                                subject = subject.ifEmpty { "Sample Subject" },
                                className = className.ifEmpty { "Sample Class" },
                                coverImage = thumbnailUrl
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Looks Good")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PreviewVideoCard(video: Video) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = video.thumbnail.ifEmpty { "https://images.unsplash.com/photo-1596496050827-8299e0220de1?auto=format&fit=crop&w=300&q=80" },
                    contentDescription = video.title,
                    modifier = Modifier.height(200.dp).fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                if (video.title.contains("lesson 1", ignoreCase = true)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart)
                    ) {
                        Text(
                            text = "Lesson 1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Video",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(video.description.ifEmpty { "This is a sample description for the preview mode to show how the text will wrap and appear." }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Grade ${video.className}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun PreviewBookCard(book: Book) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = book.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=400&auto=format&fit=crop" },
                    contentDescription = book.bookName,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(book.bookName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("${book.className} • ${book.subject}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
