package com.example.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNoteButton(onNoteClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onNoteClick,
        modifier = modifier.padding(16.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(Icons.Filled.Edit, contentDescription = "Add Note")
    }
}

@Composable
fun NoteDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take a Note") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your takeaway...") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text); onDismiss() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
