package com.example.ui.admin

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.models.Quiz
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageQuizzesScreen(navController: NavController, repository: AuraRepository = AuraRepository()) {
    var quizzes by remember { mutableStateOf<List<Quiz>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadQuizzes() {
        scope.launch {
            isLoading = true
            quizzes = repository.getQuizzes()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadQuizzes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Quizzes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("admin_add_edit_quiz/new") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Quiz")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (quizzes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No quizzes found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(quizzes) { quiz ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                                if (quiz.associatedId.isNotEmpty()) {
                                    Text("Assoc ID: ${quiz.associatedId}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { navController.navigate("admin_add_edit_quiz/${quiz.id}") }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        repository.deleteQuiz(quiz.id)
                                        loadQuizzes()
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
