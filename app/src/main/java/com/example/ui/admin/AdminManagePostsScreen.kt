package com.example.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.models.Post
import com.example.data.repository.AuraRepository
import com.example.ui.components.PostCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagePostsScreen(
    navController: NavController,
    repository: AuraRepository = remember { AuraRepository() }
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadPosts() {
        coroutineScope.launch {
            isLoading = true
            posts = repository.getPosts()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Posts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("admin_add_edit_post") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Post")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No posts found. Add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            PostCard(post = post)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { navController.navigate("admin_add_edit_post?postId=${post.id}") }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { postToDelete = post }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val post = postToDelete!!
                        coroutineScope.launch {
                            try {
                                if (!post.image_url.isNullOrBlank()) {
                                    repository.deletePostImage(post.image_url)
                                }
                                repository.deletePost(post.id)
                                AuraRepository.notifyPostsChanged()
                                loadPosts()
                                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error deleting post: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                postToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
