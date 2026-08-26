package com.example.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.auth.AuthViewModel

data class StudyTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isAi: Boolean = false
)

val allStudyTools = listOf(
    StudyTool("planner", "Study Planner", "Organize your study schedule and goals", Icons.Filled.CalendarMonth),
    StudyTool("countdown", "Exam Countdown", "Track remaining days for your upcoming exams", Icons.Filled.Event),
    StudyTool("pdf_reader", "PDF Reader", "Open and read digital PDF books offline", Icons.Filled.PictureAsPdf),
    StudyTool("translate", "Translate Notes", "Translate study material into multiple languages", Icons.Filled.Translate),
    StudyTool("calculator", "Scientific Calculator", "Perform complex mathematical calculations", Icons.Filled.Calculate),
    StudyTool("result_analysis", "Result Analysis", "Analyze school test and exam results", Icons.Filled.Analytics),
    StudyTool("progress", "Progress Tracker", "Track syllabus and learning statistics", Icons.AutoMirrored.Filled.TrendingUp),
    StudyTool("weekly_report", "Weekly Report", "Generate weekly study performance reports", Icons.Filled.Assessment),
    StudyTool("map_agent", "Map Agent", "Explore geographical places and learn history", Icons.Filled.Map),
    StudyTool("ai_homework", "AI Homework Helper", "Instant answers & step-by-step homework help", Icons.Filled.SmartToy, true),
    StudyTool("ai_doubt", "AI Doubt Solver", "Clear your doubts on any syllabus topic instantly", Icons.Filled.QuestionAnswer, true),
    StudyTool("ai_summarizer", "AI Notes Summarizer", "Summarize chapters into key revision bullet-points", Icons.Filled.Summarize, true),
    StudyTool("ai_essay", "AI Essay Writer", "Write and structure beautiful academic essays", Icons.Filled.Description, true),
    StudyTool("ai_mcq", "AI MCQ Generator", "Generate personalized practice quizzes & MCQs", Icons.AutoMirrored.Filled.ListAlt, true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    rootNavController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) allStudyTools
        else allStudyTools.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Tools", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search premium tools...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTools) { tool ->
                    ToolCard(tool = tool) {
                        when (tool.id) {
                            "planner" -> rootNavController.navigate("study_planner")
                            "countdown" -> navController.navigate("exam_countdown")
                            "pdf_reader" -> rootNavController.navigate("pdf_tool")
                            "map_agent" -> rootNavController.navigate("map_agent")
                            "translate" -> rootNavController.navigate("notes_translate")
                            "calculator" -> rootNavController.navigate("calculator")
                            "result_analysis" -> rootNavController.navigate("result_analysis")
                            "progress" -> rootNavController.navigate("progress")
                            "weekly_report" -> rootNavController.navigate("weekly_report")
                            "ai_homework" -> rootNavController.navigate("ai_chat?prompt=" + android.net.Uri.encode("Hey Gemini AI! I need help with my homework. Can you help me solve it step-by-step and explain the core concepts clearly?"))
                            "ai_doubt" -> rootNavController.navigate("ai_chat?prompt=" + android.net.Uri.encode("Hi Gemini AI! I have a specific doubt in my syllabus. Can you clarify it with clean explanations and examples?"))
                            "ai_summarizer" -> rootNavController.navigate("ai_chat?prompt=" + android.net.Uri.encode("Hello! Can you help me summarize this educational topic or text into concise, high-yield revision notes?"))
                            "ai_essay" -> rootNavController.navigate("ai_chat?prompt=" + android.net.Uri.encode("Hi! Can you guide me in writing or structuring a polished academic essay on my topic?"))
                            "ai_mcq" -> rootNavController.navigate("ai_chat?prompt=" + android.net.Uri.encode("Hey Gemini! Can you generate a set of practice Multiple Choice Questions (MCQs) on my topic with answers and brief explanations?"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: StudyTool, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                modifier = Modifier.size(40.dp),
                tint = if (tool.isAi) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
