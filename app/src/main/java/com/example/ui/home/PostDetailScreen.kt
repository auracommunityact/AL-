package com.example.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.repository.AuraRepository
import com.example.ui.components.PostCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    repository: AuraRepository = remember { AuraRepository() }
) {
    var post by remember { mutableStateOf<com.example.data.models.Post?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(postId) {
        val posts = repository.getPosts()
        post = posts.find { it.id == postId }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Details") },
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
        } else if (post == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Post not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                PostCard(post = post!!)
            }
        }
    }
}
