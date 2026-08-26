package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.models.QuestionPaper
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditQuestionPaperScreen(navController: NavController, questionPaperId: String) {
    val context = LocalContext.current
    val repository = remember { AuraRepository() }
    val scope = rememberCoroutineScope()

    var paper by remember { mutableStateOf<QuestionPaper?>(null) }
    var title by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var board by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var sectionsFromDb by remember { mutableStateOf<List<com.example.data.models.QuestionPaperSection>>(emptyList()) }
    var expandedSection by remember { mutableStateOf(false) }

    LaunchedEffect(questionPaperId) {
        scope.launch {
            sectionsFromDb = repository.getQuestionPaperSections().filter { it.isActive }
            val all = repository.getQuestionPapers()
            val found = all.find { it.id == questionPaperId }
            if (found != null) {
                paper = found
                title = found.title
                className = found.className
                subject = found.subject
                board = found.board
                year = found.year
                totalPages = found.totalPages.toString()
                fileSize = found.fileSize
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Question Paper") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (paper == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedSection,
                            onExpandedChange = { expandedSection = !expandedSection }
                        ) {
                            OutlinedTextField(
                                value = className,
                                onValueChange = { className = it },
                                label = { Text("Section/Class") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSection) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expandedSection,
                                onDismissRequest = { expandedSection = false }
                            ) {
                                sectionsFromDb.forEach { section ->
                                    DropdownMenuItem(
                                        text = { Text(section.name) },
                                        onClick = {
                                            className = section.name
                                            expandedSection = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = board,
                        onValueChange = { board = it },
                        label = { Text("Board") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = totalPages,
                        onValueChange = { totalPages = it },
                        label = { Text("Total Pages") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fileSize,
                        onValueChange = { fileSize = it },
                        label = { Text("File Size") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val updated = paper!!.copy(
                                    title = title,
                                    className = className,
                                    subject = subject,
                                    board = board,
                                    year = year,
                                    totalPages = totalPages.toIntOrNull() ?: 0,
                                    fileSize = fileSize
                                )
                                repository.updateQuestionPaper(updated)
                                Toast.makeText(context, "Question Paper updated!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
