package com.example.ui.books

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.data.models.Book
import com.example.ui.ViewModelFactory
import com.example.ui.auth.AuthViewModel
import com.example.utils.AdsManager
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.utils.VoiceSearchHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    rootNavController: NavController,
    viewModel: BooksViewModel = viewModel(factory = ViewModelFactory),
    offlineBooksViewModel: OfflineBooksViewModel = viewModel()
) {
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val offlineBooks by offlineBooksViewModel.offlineBooks.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val voiceHelper = remember { VoiceSearchHelper(context) }
    val speechResult by voiceHelper.speechResult.collectAsState()
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceHelper.startListening()
        }
    }

    LaunchedEffect(speechResult) {
        if (speechResult.isNotEmpty()) {
            val encodedQuery = URLEncoder.encode(speechResult, "UTF-8")
            voiceHelper.clearSpeechResult()
            navController.navigate("global_search?query=$encodedQuery")
        }
    }

    // Sync selected grade with backend filter
    LaunchedEffect(currentUser?.selectedGrade) {
        val grade = currentUser?.selectedGrade ?: "All Grades"
        val initialClass = if (grade == "All Grades") null else {
            val gradeStr = grade.replace("Grade ", "")
            val isNumeric = gradeStr.any { it.isDigit() }
            if (isNumeric && gradeStr.isNotEmpty()) {
                val cleanGrade = gradeStr.filter { it.isDigit() }
                when (cleanGrade) {
                    "1" -> "1st"
                    "2" -> "2nd"
                    "3" -> "3rd"
                    else -> "${cleanGrade}th"
                }
            } else {
                null
            }
        }
        viewModel.setFilters(initialClass, selectedSubject)
    }

    // Categories list based on user request (with Ebooks first, followed by specific subjects)
    val categories = listOf("Ebooks", "Hindi", "English", "Mathematics", "Science", "Social science", "Sanskrit")
    var selectedCategory by remember { mutableStateOf("Ebooks") }

    // Helper to determine if a book subject matches the selected category tab
    fun matchesCategory(bookSubject: String, category: String): Boolean {
        if (category == "Ebooks") return true
        val normSubject = bookSubject.lowercase().trim()
        val normCat = category.lowercase().trim()
        if (normCat == "social science" && (normSubject.contains("social") || normSubject.contains("studies") || normSubject.contains("history") || normSubject.contains("geography"))) return true
        return normSubject.contains(normCat) || normCat.contains(normSubject)
    }

    // Filtered lists containing only database-stored books
    val recentlyReducedBooks = remember(selectedCategory, books) {
        books.filter { matchesCategory(it.subject, selectedCategory) }
    }

    val freePreviewBooks = remember(selectedCategory, books) {
        books.filter { matchesCategory(it.subject, selectedCategory) }
    }

    // Modal Sheet or dialog state for grade/class filtering
    var showGradeFilterDialog by remember { mutableStateOf(false) }
    val classesList = listOf("All Grades", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th", "11th", "12th")

    if (showGradeFilterDialog) {
        AlertDialog(
            onDismissRequest = { showGradeFilterDialog = false },
            title = { Text("Select Class/Grade") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(250.dp).verticalScroll(rememberScrollState())) {
                    classesList.forEach { gradeOption ->
                        TextButton(
                            onClick = {
                                val filterVal = if (gradeOption == "All Grades") null else gradeOption
                                viewModel.setFilters(filterVal, selectedSubject)
                                authViewModel.updateSelectedGrade(gradeOption)
                                showGradeFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = gradeOption,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if ((gradeOption == "All Grades" && selectedClass == null) || (selectedClass == gradeOption)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradeFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Enforce full immersive dark theme container to match image_0.png mockup
    Surface(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        color = Color(0xFF0C0D0E) // Very premium pitch-black background
    ) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.fetchBooks() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Premium Search Bar at the top (matches image_0.png)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search capsule
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                        .background(Color(0xFF202124)) // Sleek dark grey background
                        .clickable { navController.navigate("global_search") }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF9AA0A6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search Books",
                        style = TextStyle(
                            color = Color(0xFF9AA0A6),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Search",
                        tint = Color(0xFF9AA0A6),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                // Custom user profile icon / filter button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(2.dp, CircleShape)
                        .background(Color(0xFF1A73E8), CircleShape)
                        .clickable { showGradeFilterDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser?.email?.take(1)?.uppercase() ?: "A",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Pin Selected Category Course to Home Screen
                var isCoursePinned by remember(selectedCategory) {
                    mutableStateOf(com.example.utils.ShortcutHelper.isShortcutPinned(context, "course_${selectedCategory.lowercase()}"))
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            val courseId = "course_${selectedCategory.lowercase()}"
                            val courseTitle = "$selectedCategory Course"
                            if (isCoursePinned) {
                                com.example.utils.ShortcutHelper.removeShortcut(context, courseId, courseTitle)
                                isCoursePinned = false
                            } else {
                                com.example.utils.ShortcutHelper.pinShortcut(
                                    context = context,
                                    id = courseId,
                                    title = courseTitle,
                                    imageUrl = null,
                                    type = "course",
                                    internalRoute = "books"
                                )
                                isCoursePinned = true
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Pin Course to Home Screen",
                        tint = if (isCoursePinned) MaterialTheme.colorScheme.primary else Color(0xFF9AA0A6)
                    )
                }
            }

            // 2. Horizontal Navigation Menu (Matches categories request exactly!)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Column(
                        modifier = Modifier
                            .clickable {
                                selectedCategory = category
                                // Also update DB filters to match
                                val dbSubject = if (category == "Ebooks") null else category
                                viewModel.setFilters(selectedClass, dbSubject)
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = category,
                            style = TextStyle(
                                color = if (isSelected) Color(0xFF8AB4F8) else Color(0xFF9AA0A6), // Light blue vs light gray
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Underline indicator
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(if (isSelected) 36.dp else 0.dp)
                                .background(Color(0xFF8AB4F8), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            // Separator line under menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0xFF303030))
            )

            // 3. Main Scrollable Content Area with Vertically Stacked Carousels
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                
                // Show offline downloads if any are available
                if (offlineBooks.isNotEmpty()) {
                    item {
                        CarouselHeader(
                            title = "My Saved Books",
                            subtitle = "Available offline",
                            onActionClick = {
                                Toast.makeText(context, "Showing downloads", Toast.LENGTH_SHORT).show()
                            }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(offlineBooks) { offlineBook ->
                                val bookObj = Book(
                                    id = offlineBook.id,
                                    bookName = offlineBook.bookName,
                                    className = offlineBook.className,
                                    subject = offlineBook.subject,
                                    coverImage = offlineBook.coverImage,
                                    pdfUrl = "file://" + offlineBook.localPdfPath
                                )
                                HighFidelityBookCard(
                                    book = bookObj,
                                    onBookClick = {
                                        AdsManager.showInterstitial(context) {
                                            val encodedUrl = URLEncoder.encode(bookObj.pdfUrl, "UTF-8")
                                            rootNavController.navigate("pdf_viewer?url=$encodedUrl")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // First Carousel: "Recently reduced ebooks"
                item {
                    CarouselHeader(
                        title = "Recently reduced ebooks",
                        subtitle = "Our latest offers",
                        onActionClick = {
                            Toast.makeText(context, "More offers coming soon!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    if (recentlyReducedBooks.isEmpty()) {
                        EmptyStatePlaceholder(
                            subjectName = selectedCategory,
                            onResetFilters = if (selectedClass != null || selectedCategory != "Ebooks") {
                                {
                                    viewModel.clearFilters()
                                    authViewModel.updateSelectedGrade("All Grades")
                                    selectedCategory = "Ebooks"
                                }
                            } else null
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(recentlyReducedBooks) { book ->
                                HighFidelityBookCard(
                                    book = book,
                                    onBookClick = {
                                        AdsManager.showInterstitial(context) {
                                            rootNavController.navigate("book_detail/${book.id}")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Native Advanced Ad Placement
                item {
                    com.example.ui.components.NativeAdViewComposable()
                }

                // Second Carousel: "Start your free preview"
                item {
                    CarouselHeader(
                        title = "Start your free preview",
                        subtitle = "Read sample first buy later",
                        onActionClick = {
                            Toast.makeText(context, "Enjoy your free previews!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    if (freePreviewBooks.isEmpty()) {
                        EmptyStatePlaceholder(
                            subjectName = selectedCategory,
                            onResetFilters = if (selectedClass != null || selectedCategory != "Ebooks") {
                                {
                                    viewModel.clearFilters()
                                    authViewModel.updateSelectedGrade("All Grades")
                                    selectedCategory = "Ebooks"
                                }
                            } else null
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(freePreviewBooks) { book ->
                                HighFidelityBookCard(
                                    book = book,
                                    onBookClick = {
                                        AdsManager.showInterstitial(context) {
                                            rootNavController.navigate("book_detail/${book.id}")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Brand Banner
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "AURA PREMIUM LIBRARY",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Read. Learn. Grow.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun CarouselHeader(
    title: String,
    subtitle: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    color = Color(0xFF9AA0A6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF202124), CircleShape)
                .clickable { onActionClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View More",
                tint = Color(0xFFE8EAED),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HighFidelityBookCard(
    book: Book,
    onBookClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable(onClick = onBookClick),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2C2C2C))
        ) {
            AsyncImage(
                model = book.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=300&q=80" },
                contentDescription = book.bookName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = book.bookName,
            style = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = book.className.ifEmpty { "Free book" },
            style = TextStyle(
                color = Color(0xFF9AA0A6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyStatePlaceholder(
    subjectName: String,
    onResetFilters: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1F22))
            .border(1.dp, Color(0xFF2F3033), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "No books found under '$subjectName'",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Check other subject tabs or update your grade level filter.",
                color = Color(0xFF9AA0A6),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            if (onResetFilters != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onResetFilters,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("View All Grades & Books", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
