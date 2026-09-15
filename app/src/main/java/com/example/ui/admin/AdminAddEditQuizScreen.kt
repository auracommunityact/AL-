package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.models.Quiz
import com.example.data.models.QuizQuestion
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditQuizScreen(
    navController: NavController,
    quizId: String,
    repository: AuraRepository = AuraRepository()
) {
    val isEditing = quizId != "new"
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(isEditing) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var associatedId by remember { mutableStateOf("") }

    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }

    LaunchedEffect(quizId) {
        if (isEditing) {
            val quizzes = repository.getQuizzes()
            val quiz = quizzes.find { it.id == quizId }
            if (quiz != null) {
                title = quiz.title
                description = quiz.description
                className = quiz.className
                subject = quiz.subject
                associatedId = quiz.associatedId
                questions = repository.getQuizQuestions(quizId)
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Quiz" else "Add Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Quiz Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = associatedId,
                        onValueChange = { associatedId = it },
                        label = { Text("Associated ID (Video/Book ID)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Questions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(questions) { index, question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Q${index + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    questions = questions.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            OutlinedTextField(
                                value = question.questionText,
                                onValueChange = { newText ->
                                    questions = questions.toMutableList().apply {
                                        this[index] = question.copy(questionText = newText)
                                    }
                                },
                                label = { Text("Question Text") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Options
                            for (i in 0 until 4) {
                                val optionText = question.options.getOrNull(i) ?: ""
                                OutlinedTextField(
                                    value = optionText,
                                    onValueChange = { newOpt ->
                                        val newOptions = question.options.toMutableList()
                                        while (newOptions.size <= i) newOptions.add("")
                                        newOptions[i] = newOpt
                                        questions = questions.toMutableList().apply {
                                            this[index] = question.copy(options = newOptions)
                                        }
                                    },
                                    label = { Text("Option ${i + 1}") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = (question.correctOptionIndex + 1).toString(),
                                onValueChange = { newIdxStr ->
                                    val newIdx = newIdxStr.toIntOrNull()?.let { it - 1 } ?: 0
                                    questions = questions.toMutableList().apply {
                                        this[index] = question.copy(correctOptionIndex = newIdx)
                                    }
                                },
                                label = { Text("Correct Option (1-4)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = question.explanation,
                                onValueChange = { newExp ->
                                    questions = questions.toMutableList().apply {
                                        this[index] = question.copy(explanation = newExp)
                                    }
                                },
                                label = { Text("Explanation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            questions = questions + QuizQuestion(order = questions.size, options = listOf("", "", "", ""))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Question")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Question")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val q = Quiz(
                                    id = if (isEditing) quizId else "",
                                    title = title,
                                    description = description,
                                    className = className,
                                    subject = subject,
                                    associatedId = associatedId
                                )
                                val savedQuizId = repository.addQuiz(q)
                                if (savedQuizId.isNotEmpty()) {
                                    repository.saveQuizQuestions(savedQuizId, questions)
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Save Quiz")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
