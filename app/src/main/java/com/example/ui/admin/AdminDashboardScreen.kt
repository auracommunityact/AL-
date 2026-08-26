package com.example.ui.admin

import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Feedback
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
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
import com.example.data.models.Book
import com.example.data.models.Video
import com.example.data.models.QuestionPaper
import com.example.data.repository.AuraRepository
import com.example.data.repository.notifications.SupabaseNotification
import com.example.data.supabase.SupabaseService
import com.example.ui.auth.AuthViewModel
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController, authViewModel: AuthViewModel) {
    if (!authViewModel.isAdmin) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }

    var booksList by remember { mutableStateOf<List<Book>>(emptyList()) }
    var videosList by remember { mutableStateOf<List<Video>>(emptyList()) }
    var questionPapersList by remember { mutableStateOf<List<QuestionPaper>>(emptyList()) }
    var websitesList by remember { mutableStateOf<List<com.example.data.models.Website>>(emptyList()) }
    var notificationsList by remember { mutableStateOf<List<SupabaseNotification>>(emptyList()) }
    var hapticLogsList by remember { mutableStateOf<List<com.example.data.models.HapticLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    fun loadAllContent() {
        coroutineScope.launch {
            isLoading = true
            try {
                booksList = repository.getBooks()
                videosList = repository.getVideos()
                questionPapersList = repository.getQuestionPapers()
                websitesList = repository.getWebsites()
                notificationsList = SupabaseService.client.from("notifications")
                    .select().decodeList<SupabaseNotification>()
                    .sortedByDescending { it.created_at }
                try {
                    hapticLogsList = SupabaseService.client.from("haptic_logs")
                        .select().decodeList<com.example.data.models.HapticLog>()
                        .sortedByDescending { it.created_at }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(mContext, "Error loading admin content: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAllContent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadAllContent() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Horizontal Quick Actions Row/Grid to preserve space
            Text(
                text = "Quick Management Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Grid Layout or horizontal scroll row of buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionCard(
                    title = "Upload Book",
                    icon = Icons.Filled.Book,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_upload/book")
                }
                CompactActionCard(
                    title = "Upload Video",
                    icon = Icons.Filled.VideoLibrary,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_upload/video")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionCard(
                    title = "New QuestionPaper",
                    icon = Icons.Filled.School,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_upload_questionPaper")
                }
                CompactActionCard(
                    title = "Notification",
                    icon = Icons.Filled.Notifications,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_notifications")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionCard(
                    title = "Manage Exam Result Portals",
                    icon = Icons.Filled.Language,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_manage_exams")
                }
                CompactActionCard(
                    title = "Upload Websites",
                    icon = Icons.Filled.Web,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_upload_websites")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionCard(
                    title = "Home UI Settings",
                    icon = Icons.Default.Palette,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_home_customization")
                }
                CompactActionCard(
                    title = "Manage Sections",
                    icon = Icons.Default.Category,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_manage_sections")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionCard(
                    title = "Manage Quizzes",
                    icon = androidx.compose.material.icons.Icons.Filled.School,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_manage_quizzes")
                }
                CompactActionCard(
                    title = "Manage Users",
                    icon = androidx.compose.material.icons.Icons.Filled.Person,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_users")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionCard(
                    title = "Feedback Management",
                    icon = androidx.compose.material.icons.Icons.Filled.Feedback,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_feedback_management")
                }
                CompactActionCard(
                    title = "Feedback Analytics",
                    icon = androidx.compose.material.icons.Icons.Filled.BarChart,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("admin_feedback_analytics")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab row to switch between lists
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Books (${booksList.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Videos (${videosList.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("QuestionPapers (${questionPapersList.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Alerts (${notificationsList.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Websites (${websitesList.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Haptic Logs (${hapticLogsList.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // List of selected content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Books
                            if (booksList.isEmpty()) {
                                item { EmptyStateView("No books found") }
                            } else {
                                items(booksList) { book ->
                                    BookAdminItem(book = book, onDelete = {
                                        coroutineScope.launch {
                                            try {
                                                repository.deleteBook(book.id)
                                                Toast.makeText(mContext, "Book deleted", Toast.LENGTH_SHORT).show()
                                                loadAllContent()
                                            } catch (e: Exception) {
                                                Toast.makeText(mContext, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }, onEdit = {
                                        navController.navigate("admin_edit_book/${book.id}")
                                    })
                                }
                            }
                        }
                        1 -> { // Videos
                            if (videosList.isEmpty()) {
                                item { EmptyStateView("No videos found") }
                            } else {
                                items(videosList) { video ->
                                    VideoAdminItem(video = video, onDelete = {
                                        coroutineScope.launch {
                                            try {
                                                repository.deleteVideo(video.id)
                                                Toast.makeText(mContext, "Video deleted", Toast.LENGTH_SHORT).show()
                                                loadAllContent()
                                            } catch (e: Exception) {
                                                Toast.makeText(mContext, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }, onEdit = {
                                        navController.navigate("admin_edit_video/${video.id}")
                                    })
                                }
                            }
                        }
                        2 -> { // QuestionPapers
                            if (questionPapersList.isEmpty()) {
                                item { EmptyStateView("No questionPapers found") }
                            } else {
                                items(questionPapersList) { course ->
                                    QuestionPaperAdminItem(course = course, onDelete = {
                                        coroutineScope.launch {
                                            try {
                                                repository.deleteQuestionPaper(course.id)
                                                Toast.makeText(mContext, "QuestionPaper deleted", Toast.LENGTH_SHORT).show()
                                                loadAllContent()
                                            } catch (e: Exception) {
                                                Toast.makeText(mContext, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }, onEdit = {
                                        navController.navigate("admin_edit_questionPaper/${course.id}")
                                    })
                                }
                            }
                        }
                        3 -> { // Notifications
                            if (notificationsList.isEmpty()) {
                                item { EmptyStateView("No notifications found") }
                            } else {
                                items(notificationsList) { notification ->
                                    NotificationAdminItem(notification = notification, onDelete = {
                                        coroutineScope.launch {
                                            try {
                                                val idValue: Any = notification.id.toLongOrNull() ?: notification.id
                                                SupabaseService.client.from("notifications").delete {
                                                    filter { eq("id", idValue) }
                                                }
                                                Toast.makeText(mContext, "Notification deleted", Toast.LENGTH_SHORT).show()
                                                loadAllContent()
                                            } catch (e: Exception) {
                                                Toast.makeText(mContext, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    })
                                }
                            }
                        }
                        4 -> { // Websites
                            if (websitesList.isEmpty()) {
                                item { EmptyStateView("No websites found") }
                            } else {
                                items(websitesList) { website ->
                                    WebsiteAdminItem(website = website, onDelete = {
                                        coroutineScope.launch {
                                            try {
                                                repository.deleteWebsite(website.id)
                                                Toast.makeText(mContext, "Website deleted", Toast.LENGTH_SHORT).show()
                                                loadAllContent()
                                            } catch (e: Exception) {
                                                Toast.makeText(mContext, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }, onEdit = {
                                        navController.navigate("admin_edit_website/${website.id}")
                                    })
                                }
                            }
                        }
                        5 -> { // Haptic Logs
                            if (hapticLogsList.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        EmptyStateView("No haptic logs found")
                                        Text(
                                            "Make sure you have created the 'haptic_logs' table in Supabase.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Logged interactions stored in Supabase",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        SupabaseService.client.from("haptic_logs").delete {
                                                            filter { neq("id", "") }
                                                        }
                                                        Toast.makeText(mContext, "All logs cleared", Toast.LENGTH_SHORT).show()
                                                        loadAllContent()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(mContext, "Failed to clear: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear Logs")
                                        }
                                    }
                                }
                                items(hapticLogsList) { log ->
                                    HapticLogAdminItem(log = log, onDelete = {
                                        coroutineScope.launch {
                                            try {
                                                SupabaseService.client.from("haptic_logs").delete {
                                                    filter { eq("id", log.id) }
                                                }
                                                Toast.makeText(mContext, "Log deleted", Toast.LENGTH_SHORT).show()
                                                loadAllContent()
                                            } catch (e: Exception) {
                                                Toast.makeText(mContext, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BookAdminItem(book: Book, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.coverImage,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp, 70.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.bookName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Subject: ${book.subject}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Grade: ${book.className}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete '${book.bookName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VideoAdminItem(video: Video, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp, 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Teacher: ${video.teacher}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${video.subject} • Class ${video.className}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete '${video.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuestionPaperAdminItem(course: QuestionPaper, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = course.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp, 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.description, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Subject: ${course.subject}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = {
                com.example.utils.ShareHelper.shareContent(
                    context = context,
                    title = course.title,
                    contentType = "questionPaper",
                    id = course.id
                )
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete QuestionPaper") },
            text = { Text("Are you sure you want to delete '${course.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotificationAdminItem(notification: SupabaseNotification, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val formattedDate = remember(notification.created_at) {
        try {
            // Parses "2026-07-08T12:00:00Z"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(notification.created_at)
            val outputFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            if (date != null) outputFormat.format(date) else notification.created_at
        } catch (e: Exception) {
            notification.created_at
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(notification.description, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tag: ${notification.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(formattedDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Notification") },
            text = { Text("Are you sure you want to delete '${notification.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyStateView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun WebsiteAdminItem(website: com.example.data.models.Website, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = website.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(website.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(website.description, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(website.url, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Website") },
            text = { Text("Are you sure you want to delete '${website.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HapticLogAdminItem(log: com.example.data.models.HapticLog, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val formattedDate = remember(log.created_at) {
        try {
            val date = java.util.Date(log.created_at)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            log.created_at.toString()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (log.event_type) {
                            "Bookmark Book" -> MaterialTheme.colorScheme.primaryContainer
                            "Read Now Click" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (log.event_type) {
                        "Bookmark Book" -> Icons.Default.Bookmark
                        "Read Now Click" -> Icons.AutoMirrored.Filled.MenuBook
                        else -> Icons.Default.Palette
                    },
                    contentDescription = null,
                    tint = when (log.event_type) {
                        "Bookmark Book" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "Read Now Click" -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.event_type,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.details,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.user_email,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete LogEntry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Log Entry") },
            text = { Text("Are you sure you want to delete this log entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
