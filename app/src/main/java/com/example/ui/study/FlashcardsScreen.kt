package com.example.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.models.Flashcard
import com.example.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    navController: NavController,
    deckId: String
) {
    val studyViewModel: StudyViewModel = viewModel(factory = ViewModelFactory)
    var flashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Simple state to flip card on click
    val flippedCards = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(deckId) {
        flashcards = studyViewModel.getFlashcards(deckId)
    }

    if (showAddDialog) {
        AddFlashcardDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { front, back ->
                val newCard = Flashcard(deckId = deckId, frontText = front, backText = back)
                studyViewModel.addFlashcard(newCard)
                showAddDialog = false
                // refresh
                flashcards = flashcards + newCard // optimistic
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Deck") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Flashcard")
            }
        }
    ) { padding ->
        if (flashcards.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No flashcards yet. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(flashcards) { card ->
                    val isFlipped = flippedCards[card.id] ?: false
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clickable { flippedCards[card.id] = !isFlipped },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFlipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (isFlipped) card.backText else card.frontText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = if (isFlipped) FontWeight.Normal else FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                color = if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = if (isFlipped) "Answer" else "Question",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlashcardDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Flashcard") },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Question)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Answer)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (front.isNotBlank() && back.isNotBlank()) onAdd(front, back) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
