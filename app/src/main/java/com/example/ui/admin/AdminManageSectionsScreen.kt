package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.QuestionPaperSection
import com.example.data.repository.AuraRepository
import com.example.utils.StorageUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageSectionsScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { AuraRepository() }
    val scope = rememberCoroutineScope()
    
    var sections by remember { mutableStateOf<List<QuestionPaperSection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSection by remember { mutableStateOf<QuestionPaperSection?>(null) }
    var sectionToDelete by remember { mutableStateOf<QuestionPaperSection?>(null) }
    
    val loadSections = {
        scope.launch {
            isLoading = true
            sections = repository.getQuestionPaperSections()
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        loadSections()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Sections") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadSections() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    editingSection = null
                    showAddDialog = true 
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Section") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (sections.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No sections found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sections) { section ->
                        SectionAdminItem(
                            section = section,
                            onEdit = { 
                                editingSection = section
                                showAddDialog = true 
                            },
                            onDelete = { sectionToDelete = section },
                            onMoveUp = { 
                                val index = sections.indexOf(section)
                                if (index > 0) {
                                    val other = sections[index - 1]
                                    scope.launch {
                                        repository.updateQuestionPaperSection(section.copy(order = other.order))
                                        repository.updateQuestionPaperSection(other.copy(order = section.order))
                                        loadSections()
                                    }
                                }
                            },
                            onMoveDown = {
                                val index = sections.indexOf(section)
                                if (index < sections.size - 1) {
                                    val other = sections[index + 1]
                                    scope.launch {
                                        repository.updateQuestionPaperSection(section.copy(order = other.order))
                                        repository.updateQuestionPaperSection(other.copy(order = section.order))
                                        loadSections()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        SectionDialog(
            section = editingSection,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, order, isActive, imageUri ->
                scope.launch {
                    try {
                        var imageUrl = editingSection?.thumbnail ?: ""
                        if (imageUri != null) {
                            val bytes = StorageUtils.compressImage(context, imageUri)
                            if (bytes != null) {
                                val fileName = "section_${UUID.randomUUID()}.jpg"
                                imageUrl = repository.uploadImage(bytes, fileName, "sections")
                            }
                        }
                        
                        val newSection = QuestionPaperSection(
                            id = editingSection?.id ?: "",
                            name = name,
                            description = desc,
                            thumbnail = imageUrl,
                            order = order,
                            isActive = isActive
                        )
                        
                        if (editingSection == null) {
                            repository.addQuestionPaperSection(newSection)
                        } else {
                            repository.updateQuestionPaperSection(newSection)
                        }
                        
                        showAddDialog = false
                        loadSections()
                        Toast.makeText(context, "Section saved", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
    
    if (sectionToDelete != null) {
        var isChecking by remember { mutableStateOf(false) }
        var papersCount by remember { mutableStateOf(0) }
        
        LaunchedEffect(sectionToDelete) {
            isChecking = true
            val allPapers = repository.getQuestionPapers()
            papersCount = allPapers.count { it.className == sectionToDelete?.name || it.section == sectionToDelete?.name }
            isChecking = false
        }
        
        AlertDialog(
            onDismissRequest = { sectionToDelete = null },
            title = { Text("Delete Section") },
            text = { 
                Column {
                    Text("Are you sure you want to delete '${sectionToDelete?.name}'? This action cannot be undone.")
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(top = 8.dp))
                    } else if (papersCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Warning: This section contains $papersCount question paper(s). Deleting it will leave them without a section.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.deleteQuestionPaperSection(sectionToDelete!!.id)
                                sectionToDelete = null
                                loadSections()
                                Toast.makeText(context, "Section deleted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { sectionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionAdminItem(
    section: QuestionPaperSection,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = section.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(section.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (section.description.isNotEmpty()) {
                    Text(
                        section.description, 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (section.isActive) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            if (section.isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = if (section.isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Order: ${section.order}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Column {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                }
            }
            
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDialog(
    section: QuestionPaperSection?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Boolean, android.net.Uri?) -> Unit
) {
    var name by remember { mutableStateOf(section?.name ?: "") }
    var description by remember { mutableStateOf(section?.description ?: "") }
    var order by remember { mutableStateOf(section?.order?.toString() ?: "0") }
    var isActive by remember { mutableStateOf(section?.isActive ?: true) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (section == null) "Add Section" else "Edit Section") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = order,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) order = it },
                    label = { Text("Display Order") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Is Active")
                }
                
                ImagePickerSection(
                    title = "Thumbnail",
                    selectedImageUri = selectedImageUri,
                    onImageSelected = { selectedImageUri = it },
                    existingImageUrl = section?.thumbnail
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    isSaving = true
                    onConfirm(name, description, order.toIntOrNull() ?: 0, isActive, selectedImageUri)
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}
