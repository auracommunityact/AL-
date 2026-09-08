package com.example.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ai.AuraLocalAITutor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalStudyScheduleScreen(navController: NavController) {
    var selectedGrade by remember { mutableStateOf("Class 10") }
    val grades = listOf("Class 8", "Class 9", "Class 10", "Class 11", "Class 12")
    
    val availableSubjects = listOf("Mathematics", "Science", "Social Science", "English", "Hindi", "Physics", "Chemistry", "Biology")
    val selectedSubjects = remember { mutableStateListOf("Mathematics", "Science", "English") }
    
    var generatedSchedule by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aura AI Scheduler", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Icon(Icons.Filled.OfflineBolt, contentDescription = "Offline", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("OFFLINE", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Generate a personalized weekly study schedule tailored to your class and subjects using Aura Local AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Grade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                grades.take(3).forEach { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade },
                        label = { Text(grade) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                grades.drop(3).forEach { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade },
                        label = { Text(grade) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableSubjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubjects.contains(subject),
                        onClick = { 
                            if (selectedSubjects.contains(subject)) {
                                if (selectedSubjects.size > 1) selectedSubjects.remove(subject)
                            } else {
                                selectedSubjects.add(subject)
                            }
                        },
                        label = { Text(subject) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (!isGenerating) {
                        isGenerating = true
                        generatedSchedule = ""
                        scope.launch {
                            AuraLocalAITutor.generateStudySchedule(selectedGrade, selectedSubjects.toList())
                                .collect { chunk ->
                                    generatedSchedule += chunk
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isGenerating
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGenerating) "Generating Schedule..." else "Generate Study Schedule")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedVisibility(visible = generatedSchedule.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RenderScheduleMarkdown(generatedSchedule)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun RenderScheduleMarkdown(text: String) {
    val lines = text.split("\n")
    Column {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("## ")) {
                Text(
                    text = trimmed.substring(3),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            } else if (trimmed.startsWith("### ")) {
                Text(
                    text = trimmed.substring(4),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            } else if (trimmed.startsWith("- ")) {
                Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                    Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        text = buildAnnotatedString {
                            val content = trimmed.substring(2)
                            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                            var lastIndex = 0
                            boldRegex.findAll(content).forEach { matchResult ->
                                append(content.substring(lastIndex, matchResult.range.first))
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1])
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            append(content.substring(lastIndex))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (trimmed.startsWith("---")) {
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        val content = trimmed
                        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                        var lastIndex = 0
                        boldRegex.findAll(content).forEach { matchResult ->
                            append(content.substring(lastIndex, matchResult.range.first))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(matchResult.groupValues[1])
                            }
                            lastIndex = matchResult.range.last + 1
                        }
                        append(content.substring(lastIndex))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
