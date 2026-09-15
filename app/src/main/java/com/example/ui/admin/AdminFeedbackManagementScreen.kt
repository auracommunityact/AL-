package com.example.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Feedback
import com.example.data.models.FeedbackConstants
import com.example.ui.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbackManagementScreen(
    navController: NavController,
    viewModel: AdminFeedbackViewModel = viewModel(factory = ViewModelFactory)
) {
    val feedbackList by viewModel.filteredFeedback.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    
    val primaryBlue = Color(0xFF1A237E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export Logic */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FF))
        ) {
            // Search and Filter Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by name, email, subject...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.onCategoryFilterChange(null) },
                                label = { Text("All Categories") }
                            )
                        }
                        items(FeedbackConstants.CATEGORIES) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.onCategoryFilterChange(category) },
                                label = { Text(category) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStatus == null,
                                onClick = { viewModel.onStatusFilterChange(null) },
                                label = { Text("All Status") }
                            )
                        }
                        items(FeedbackConstants.STATUS_OPTIONS) { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { viewModel.onStatusFilterChange(status) },
                                label = { Text(status) }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryBlue)
                }
            } else if (feedbackList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No feedback entries found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(feedbackList) { feedback ->
                        AdminFeedbackCard(
                            feedback = feedback,
                            onStatusChange = { viewModel.updateStatus(feedback.id, it) },
                            onDelete = { viewModel.deleteFeedback(feedback.id) },
                            primaryBlue = primaryBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminFeedbackCard(
    feedback: Feedback,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit,
    primaryBlue: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(feedback.status)
                Text(
                    text = dateFormat.format(Date(feedback.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = feedback.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryBlue
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminFeedbackCardIcon(Icons.Default.Person, contentDescription = null, size = 14.dp, tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${feedback.userName} (${feedback.userEmail})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = primaryBlue.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = feedback.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = feedback.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (feedback.requestedFeatureName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdminFeedbackCardIcon(Icons.Default.Star, contentDescription = null, size = 16.dp, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Requested: ${feedback.requestedFeatureName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (feedback.screenshotUrl != null) {
                    Text("Screenshot:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = feedback.screenshotUrl,
                        contentDescription = "Screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Device", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(feedback.deviceInfo, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Version", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(feedback.appVersion, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Update Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FeedbackConstants.STATUS_OPTIONS) { status ->
                        val isSelected = feedback.status == status
                        Surface(
                            modifier = Modifier.clickable { onStatusChange(status) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) primaryBlue else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(
                                text = status,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (expanded) "Show Less" else "View Details")
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "New" -> Color(0xFF2196F3)
        "Under Review" -> Color(0xFFFF9800)
        "Planned" -> Color(0xFF9C27B0)
        "In Progress" -> Color(0xFF00BCD4)
        "Completed" -> Color(0xFF4CAF50)
        "Rejected" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdminFeedbackCardIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}
