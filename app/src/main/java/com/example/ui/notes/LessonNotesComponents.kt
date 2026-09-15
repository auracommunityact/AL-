package com.example.ui.notes

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LessonNoteSection(
    lessonId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LessonNotesViewModel = viewModel(
        factory = LessonNotesViewModelFactory(context.applicationContext as Application)
    )

    val currentNote by viewModel.currentNote.collectAsState()
    var editableNote by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(lessonId) {
        viewModel.loadNoteForLesson(lessonId)
    }

    LaunchedEffect(currentNote) {
        if (!isEditing) {
            editableNote = currentNote
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Study Reminders & Summary",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editableNote,
                onValueChange = { 
                    editableNote = it
                    isEditing = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = { Text("Write your notes or takeaways here...") },
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isEditing) {
                    TextButton(
                        onClick = {
                            editableNote = currentNote
                            isEditing = false
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                }
                Button(
                    onClick = {
                        viewModel.saveNoteForLesson(lessonId, editableNote)
                        isEditing = false
                    },
                    enabled = isEditing
                ) {
                    Text("Save Note")
                }
            }
        }
    }
}
