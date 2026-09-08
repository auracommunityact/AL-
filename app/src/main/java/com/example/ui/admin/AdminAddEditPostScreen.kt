package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Post
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.utils.StorageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditPostScreen(
    navController: NavController,
    postId: String? = null,
    repository: AuraRepository = remember { AuraRepository() }
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var youtubeUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("draft") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(postId != null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    LaunchedEffect(postId) {
        if (postId != null) {
            isFetching = true
            try {
                val post = repository.getPosts().find { it.id == postId }
                if (post != null) {
                    title = post.title
                    description = post.description
                    youtubeUrl = post.youtube_url ?: ""
                    status = post.status
                    imageUrl = post.image_url
                } else {
                    Toast.makeText(context, "Post not found", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching post", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } finally {
                isFetching = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (postId == null) "Add Post" else "Edit Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isFetching || isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (isLoading) "Saving..." else "Loading...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Required)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Required)") },
                    placeholder = { Text("Enter post content.") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
                
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = { youtubeUrl = it },
                    label = { Text("YouTube Video URL (Optional)") },
                    placeholder = { Text("e.g. https://youtu.be/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = status == "draft",
                            onClick = { status = "draft" }
                        )
                        Text("Draft", modifier = Modifier.padding(end = 16.dp))
                        RadioButton(
                            selected = status == "published",
                            onClick = { status = "published" }
                        )
                        Text("Published")
                    }
                }
                
                Text("Post Picture (Optional)", style = MaterialTheme.typography.titleSmall)

                if (selectedImageUri != null || imageUrl != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        AsyncImage(
                            model = selectedImageUri ?: imageUrl,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                selectedImageUri = null
                                imageUrl = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove Image", tint = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Picture")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank()) {
                            Toast.makeText(context, "Title and Description are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                var finalImageUrl = imageUrl
                                if (selectedImageUri != null) {
                                    val imageBytes = StorageUtils.compressImage(context, selectedImageUri!!)
                                    if (imageBytes != null) {
                                        val fileName = "post_${UUID.randomUUID()}.jpg"
                                        finalImageUrl = repository.uploadPostImage(imageBytes, fileName)
                                        
                                        // Delete old image if replacing
                                        if (imageUrl != null && imageUrl != finalImageUrl) {
                                            repository.deletePostImage(imageUrl!!)
                                        }
                                    }
                                }

                                val finalYoutubeUrl = youtubeUrl.takeIf { it.isNotBlank() }
                                var extractedVideoId: String? = null
                                if (finalYoutubeUrl != null) {
                                    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F|shorts\\/)[^#\\&\\?\\n]*"
                                    val compiledPattern = java.util.regex.Pattern.compile(pattern)
                                    val matcher = compiledPattern.matcher(finalYoutubeUrl)
                                    if (matcher.find()) {
                                        extractedVideoId = matcher.group()
                                    }
                                }

                                val post = Post(
                                    id = postId ?: UUID.randomUUID().toString(),
                                    title = title,
                                    description = description,
                                    image_url = finalImageUrl,
                                    youtube_url = finalYoutubeUrl,
                                    youtube_video_id = extractedVideoId,
                                    status = status
                                )

                                if (postId == null) {
                                    repository.addPost(post)
                                    Toast.makeText(context, "Post created successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    repository.updatePost(post)
                                    Toast.makeText(context, "Post updated successfully", Toast.LENGTH_SHORT).show()
                                }
                                AuraRepository.notifyPostsChanged()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error saving post: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && description.isNotBlank()
                ) {
                    Text(if (postId == null) "Create Post" else "Update Post")
                }
            }
        }
    }
}
