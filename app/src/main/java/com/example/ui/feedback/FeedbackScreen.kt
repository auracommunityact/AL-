package com.example.ui.feedback

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.AppFeature
import com.example.data.models.FeedbackConstants
import com.example.ui.ViewModelFactory
import com.example.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: FeedbackViewModel = viewModel(factory = ViewModelFactory)
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val submissionStatus by viewModel.submissionStatus.collectAsState()
    val featureRequests by viewModel.featureRequests.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(FeedbackConstants.CATEGORIES[0]) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var requestedFeatureName by remember { mutableStateOf("") }
    var whyNeeded by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val primaryBlue = Color(0xFF1A237E)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        screenshotUri = uri
    }

    if (submissionStatus is SubmissionStatus.Success) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSubmissionStatus(); showForm = false; navController.popBackStack() },
            confirmButton = {
                Button(onClick = { viewModel.resetSubmissionStatus(); showForm = false; navController.popBackStack() }) {
                    Text("OK")
                }
            },
            title = { Text("Success") },
            text = { Text("Thank you! Your feedback has been submitted successfully.") },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(48.dp)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback & Suggestions", fontWeight = FontWeight.Bold) },
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
                .background(Color.White)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search features or suggest new ones...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            if (searchQuery.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                "✅ Available in Aura Learning",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(searchResults) { feature ->
                            FeatureSuggestionCard(feature, primaryBlue) {
                                navController.navigate(feature.route)
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "This feature is currently not available.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { 
                                        requestedFeatureName = searchQuery
                                        selectedCategory = "Feature Request"
                                        showForm = true 
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Request this Feature", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Default Feedback Options
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "We value your feedback",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue
                        )
                        Text(
                            "Help us improve Aura Learning by sharing your thoughts, reporting bugs, or suggesting new features.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    item {
                        FeedbackCategoryGrid(primaryBlue) { category ->
                            selectedCategory = category
                            showForm = true
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Top Feature Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (featureRequests.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = primaryBlue)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("No feature requests yet. Be the first to suggest one!", color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(featureRequests) { request ->
                            val isUpvoted = currentUser?.let { request.upvotedBy.contains(it.id) } ?: false
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = request.requestedFeatureName ?: request.subject,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryBlue
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = request.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 2
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(enabled = currentUser != null) {
                                            currentUser?.let { user ->
                                                viewModel.toggleUpvote(request.id, user.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (isUpvoted) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                            contentDescription = "Upvote",
                                            tint = if (isUpvoted) primaryBlue else Color.Gray
                                        )
                                        Text(
                                            text = request.upvotes.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isUpvoted) primaryBlue else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        ModalBottomSheet(
            onDismissRequest = { showForm = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            FeedbackForm(
                category = selectedCategory,
                requestedFeatureName = requestedFeatureName,
                onCategoryChange = { selectedCategory = it },
                subject = subject,
                onSubjectChange = { subject = it },
                description = description,
                onDescriptionChange = { description = it },
                whyNeeded = whyNeeded,
                onWhyNeededChange = { whyNeeded = it },
                screenshotUri = screenshotUri,
                onPickScreenshot = { imagePicker.launch("image/*") },
                onRemoveScreenshot = { screenshotUri = null },
                onSubmit = {
                    currentUser?.let { user ->
                        val finalDescription = if (selectedCategory == "Feature Request") {
                            "$description\n\nWhy I need this: $whyNeeded"
                        } else description

                        val screenshotBytes = screenshotUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }

                        viewModel.submitFeedback(
                            user = user,
                            category = selectedCategory,
                            subject = if (selectedCategory == "Feature Request") "Feature Request: $requestedFeatureName" else subject,
                            description = finalDescription,
                            requestedFeatureName = if (selectedCategory == "Feature Request") requestedFeatureName else null,
                            screenshotBytes = screenshotBytes
                        )
                    }
                },
                isSubmitting = submissionStatus is SubmissionStatus.Loading,
                primaryBlue = primaryBlue
            )
        }
    }
}

@Composable
fun FeatureSuggestionCard(feature: AppFeature, primaryBlue: Color, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primaryBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = primaryBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(feature.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(feature.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("Open Feature")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FeedbackCategoryGrid(primaryBlue: Color, onSelect: (String) -> Unit) {
    val items = listOf(
        Pair("Bug Report", Icons.Default.BugReport),
        Pair("App Feedback", Icons.Default.Feedback),
        Pair("Improvement", Icons.Default.TrendingUp),
        Pair("Feature Request", Icons.Default.AddCircle),
        Pair("UI/UX Feedback", Icons.Default.Palette),
        Pair("Other", Icons.Default.MoreHoriz)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select a category", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        
        // Grid layout manually using Rows
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    CategoryItem(
                        title = item.first,
                        icon = item.second,
                        primaryBlue = primaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(item.first) }
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primaryBlue: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackForm(
    category: String,
    requestedFeatureName: String,
    onCategoryChange: (String) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    whyNeeded: String,
    onWhyNeededChange: (String) -> Unit,
    screenshotUri: Uri?,
    onPickScreenshot: () -> Unit,
    onRemoveScreenshot: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    primaryBlue: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = if (category == "Feature Request") "Request a New Feature" else "Submit Feedback",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = primaryBlue
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Category Dropdown
        Text("Category", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                FeedbackConstants.CATEGORIES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onCategoryChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (category == "Feature Request") {
            Text("Feature Name", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = requestedFeatureName,
                onValueChange = { /* handled in outer state if needed, but it's pre-filled */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                readOnly = true // Pre-filled from search
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Why do you need this feature?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = whyNeeded,
                onValueChange = onWhyNeededChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                placeholder = { Text("Describe how this feature would help you...") }
            )
        } else {
            Text("Subject", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Brief summary of your feedback") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Description", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 4,
            placeholder = { Text("Provide more details...") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Screenshot
        Text("Optional Screenshot", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        if (screenshotUri != null) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = screenshotUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onRemoveScreenshot,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onPickScreenshot() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = primaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Screenshot", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
            enabled = !isSubmitting && (description.isNotBlank() && (category == "Feature Request" || subject.isNotBlank()))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Submit Feedback", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
