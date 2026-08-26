package com.example.ui.quiz

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ui.auth.AuthViewModel
import com.example.utils.AdsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    navController: NavController,
    lessonId: String,
    viewModel: QuizViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val quiz by viewModel.quiz.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(lessonId) {
        viewModel.loadQuizByAssociatedId(lessonId)
    }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }

    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz?.title ?: "Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            } else if (quizCompleted) {
                LaunchedEffect(Unit) {
                    currentUser?.id?.let { uid ->
                        quiz?.id?.let { qid ->
                            viewModel.submitQuizResult(uid, qid, score, questions.size)
                        }
                    }
                }
                QuizResultScreen(
                    score = score,
                    totalQuestions = questions.size,
                    onDone = {
                        AdsManager.showInterstitial(context) {
                            navController.popBackStack()
                        }
                    }
                )
            } else if (currentQuestion != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Indicator
                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Question ${currentQuestionIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Question Text
                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Options
                    currentQuestion.options.forEachIndexed { index, option ->
                        val isSelected = selectedOptionIndex == index
                        val isCorrect = index == currentQuestion.correctOptionIndex
                        
                        val backgroundColor = when {
                            !showFeedback -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            showFeedback && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            showFeedback && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        
                        val borderColor = when {
                            !showFeedback -> if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            showFeedback && isCorrect -> Color(0xFF4CAF50)
                            showFeedback && isSelected && !isCorrect -> Color(0xFFF44336)
                            else -> Color.Transparent
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !showFeedback) {
                                    selectedOptionIndex = index
                                },
                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                            border = CardDefaults.outlinedCardBorder(true).copy(width = 2.dp, brush = androidx.compose.ui.graphics.SolidColor(borderColor))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                if (showFeedback) {
                                    if (isCorrect) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = Color(0xFF4CAF50))
                                    } else if (isSelected) {
                                        Icon(Icons.Filled.Cancel, contentDescription = "Incorrect", tint = Color(0xFFF44336))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    AnimatedVisibility(visible = showFeedback && currentQuestion.explanation.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(currentQuestion.explanation, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            if (!showFeedback) {
                                showFeedback = true
                                if (selectedOptionIndex == currentQuestion.correctOptionIndex) {
                                    score++
                                }
                            } else {
                                if (currentQuestionIndex < questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedOptionIndex = null
                                    showFeedback = false
                                } else {
                                    quizCompleted = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedOptionIndex != null
                    ) {
                        Text(if (!showFeedback) "Check Answer" else if (currentQuestionIndex < questions.size - 1) "Next Question" else "Finish Quiz")
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(score: Int, totalQuestions: Int, onDone: () -> Unit) {
    val percentage = (score.toFloat() / totalQuestions) * 100
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = if (percentage >= 50) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Quiz Completed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You scored $score out of $totalQuestions",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text("Done")
        }
    }
}
