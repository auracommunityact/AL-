package com.example.ui.books

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Book
import com.example.data.repository.AuraRepository
import com.example.ui.auth.AuthViewModel
import com.example.utils.HapticHelper
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    navController: NavController,
    bookId: String,
    authViewModel: AuthViewModel,
    offlineBooksViewModel: OfflineBooksViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }

    var book by remember { mutableStateOf<Book?>(null) }
    var relatedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    val currentBookId = book?.id ?: bookId
    var isShortcutPinned by remember(currentBookId) { mutableStateOf(com.example.utils.ShortcutHelper.isShortcutPinned(context, "book_$currentBookId")) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val offlineBooks by offlineBooksViewModel.offlineBooks.collectAsState()
    val downloadProgress by offlineBooksViewModel.downloadProgress.collectAsState()

    // Helper to generate slug and match book
    fun String.toSlug(): String {
        return this.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    fun isBookMatch(book: Book, query: String): Boolean {
        val q = query.lowercase().trim()
        if (book.id.lowercase() == q) return true
        
        val nameSlug = book.bookName.toSlug()
        if (nameSlug == q) return true
        
        val queryNormalized = q
            .replace("math-", "mathematics-")
            .replace("sst-", "social-studies-")
            .replace("computer-", "computer-science-")
            
        val nameSlugNormalized = nameSlug
            .replace("mathematics-", "math-")
            .replace("social-studies-", "sst-")
            .replace("computer-science-", "computer-")
            
        if (nameSlug == queryNormalized || nameSlugNormalized == q) return true
        
        return false
    }

    // Fetch book details and related books
    fun loadBookDetails() {
        coroutineScope.launch {
            isLoading = true
            hasError = false
            try {
                var fetchedBook = repository.getBook(bookId)
                val allBooks = repository.getBooks()
                if (fetchedBook == null) {
                    fetchedBook = allBooks.find { isBookMatch(it, bookId) }
                }
                book = fetchedBook
                
                if (fetchedBook != null) {
                    // Filter related books: same subject or class, excluding current book
                    relatedBooks = allBooks.filter { 
                        it.id != fetchedBook.id && 
                        (it.subject.equals(fetchedBook.subject, ignoreCase = true) || 
                         it.className.equals(fetchedBook.className, ignoreCase = true))
                    }.take(6)
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(bookId) {
        loadBookDetails()
    }

    val isSaved = currentUser?.savedBooks?.contains(currentBookId) == true
    val isDownloaded = offlineBooks.any { it.id == currentBookId }
    val currentProgress = downloadProgress[currentBookId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (book != null && !isLoading) {
                        Text(
                            text = book!!.bookName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text("Book Details", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (book != null && !isLoading) {
                        val currentBook = book!!
                        // Share Button
                        IconButton(
                            onClick = {
                                com.example.utils.ShareHelper.shareContent(
                                    context = context,
                                    title = currentBook.bookName,
                                    contentType = "book",
                                    id = currentBook.id
                                )
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))

                        // Add to Home Screen Button
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (isShortcutPinned) {
                                        com.example.utils.ShortcutHelper.removeShortcut(context, "book_$currentBookId", currentBook.bookName)
                                        isShortcutPinned = false
                                    } else {
                                        com.example.utils.ShortcutHelper.pinShortcut(
                                            context = context,
                                            id = "book_$currentBookId",
                                            title = currentBook.bookName,
                                            imageUrl = currentBook.coverImage,
                                            type = "book",
                                            internalRoute = "book_detail/$currentBookId"
                                        )
                                        isShortcutPinned = true
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Add to Home Screen",
                                tint = if (isShortcutPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Save Bookmark Button
                        IconButton(
                            onClick = {
                                authViewModel.toggleSaveBook(currentBookId)
                                val stateStr = if (isSaved) "Unsaved" else "Saved"
                                HapticHelper.triggerAndLog(
                                    context = context,
                                    eventType = "Bookmark Book",
                                    details = "$stateStr book ID: $currentBookId (${book?.bookName ?: "Unknown"})",
                                    userEmail = currentUser?.email
                                )
                                val message = if (isSaved) "Removed from Learning" else "Saved to Learning"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (isSaved) "Remove from Learning" else "Save to Learning",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (book != null && !isLoading) {
                val currentBook = book!!
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download button
                        OutlinedButton(
                            onClick = {
                                if (isDownloaded) {
                                    offlineBooksViewModel.deleteOfflineBook(currentBook.id)
                                    Toast.makeText(context, "Removed offline download", Toast.LENGTH_SHORT).show()
                                } else if (currentProgress == null) {
                                    offlineBooksViewModel.downloadBook(currentBook)
                                    Toast.makeText(context, "Starting offline download...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                1.5.dp, 
                                if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (currentProgress != null) {
                                CircularProgressIndicator(
                                    progress = { currentProgress / 100f },
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${currentProgress}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isDownloaded) "Delete Offline" else "Save Offline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Read Now button
                        Button(
                            onClick = {
                                HapticHelper.triggerAndLog(
                                    context = context,
                                    eventType = "Read Now Click",
                                    details = "Started reading book ID: ${currentBook.id} (${currentBook.bookName})",
                                    userEmail = currentUser?.email
                                )
                                val localFile = offlineBooks.find { it.id == currentBook.id }
                                val urlToOpen = if (localFile != null) "file://" + localFile.localPdfPath else currentBook.pdfUrl
                                if (urlToOpen.isNotEmpty()) {
                                    com.example.utils.AdsManager.showInterstitial(context) {
                                        val encodedUrl = URLEncoder.encode(urlToOpen, "UTF-8")
                                        navController.navigate("pdf_viewer?url=$encodedUrl")
                                    }
                                } else {
                                    Toast.makeText(context, "Syllabus book PDF path is not ready yet.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding)) {
                BookDetailShimmer()
            }
        } else if (hasError || book == null) {
            Box(modifier = Modifier.padding(padding)) {
                BookDetailError(
                    onRetry = { loadBookDetails() },
                    onBack = { navController.popBackStack() }
                )
            }
        } else {
            val currentBook = book!!
            
            // Build deterministic detail cards (Google Play Books style)
            val ratingValue = remember(currentBook.id) {
                String.format("%.1f", 4.3 + (currentBook.id.hashCode() % 6) * 0.1)
            }
            val sizeValue = remember(currentBook.id) {
                when (Math.abs(currentBook.id.hashCode() % 4)) {
                    0 -> "8.4 MB"
                    1 -> "11.2 MB"
                    2 -> "14.8 MB"
                    else -> "6.5 MB"
                }
            }
            val pagesValue = remember(currentBook.id) {
                when (Math.abs(currentBook.id.hashCode() % 4)) {
                    0 -> "164"
                    1 -> "210"
                    2 -> "288"
                    else -> "312"
                }
            }
            val langValue = remember(currentBook.subject) {
                if (currentBook.subject.contains("Hindi", ignoreCase = true) || 
                    currentBook.subject.contains("Sanskrit", ignoreCase = true)) "Hindi" else "English"
            }
            val publisherValue = remember(currentBook.className) {
                if (currentBook.className.contains("Book", ignoreCase = true)) "NCERT" else "Aura EdTech"
            }
            val boardValue = remember(currentBook.className) {
                "CBSE"
            }

            val detailItems = listOf(
                BookDetailInfo(ratingValue + " ★", "Rating", Icons.Filled.Star),
                BookDetailInfo(sizeValue, "Size", Icons.Filled.InsertDriveFile),
                BookDetailInfo(pagesValue + " pgs", "Pages", Icons.Filled.ImportContacts),
                BookDetailInfo(langValue, "Language", Icons.Filled.Language),
                BookDetailInfo(publisherValue, "Publisher", Icons.Filled.Business),
                BookDetailInfo(boardValue, "Board", Icons.Filled.School)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Cover Section with subtle immersive background gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .width(170.dp)
                                .height(240.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(currentBook.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=300&q=80" })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = currentBook.bookName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(currentBook.subject, fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = null
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Class ${currentBook.className}", fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title and rating summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentBook.bookName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$ratingValue • Syllabus Book",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Book detailed specifications (Horizontal play books style row)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        detailItems.forEach { item ->
                            Card(
                                modifier = Modifier.width(96.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Expandable About Section
                    var isDescExpanded by remember { mutableStateOf(false) }
                    val fullDescription = "This official Class ${currentBook.className} textbook is professionally curated to cover the comprehensive syllabus of ${currentBook.subject}. This premium resource features detailed chapter summaries, visual diagrams, step-by-step model answers, and board-level exercises designed to support academic excellence and preparation. Highly suitable for both structured classroom learning and self-paced home studying."

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "About this Book",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fullDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize()
                        )
                        TextButton(
                            onClick = { isDescExpanded = !isDescExpanded },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = if (isDescExpanded) "Read Less" else "Read More",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Related Books
                    if (relatedBooks.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Related Books",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                relatedBooks.forEach { relBook ->
                                    Card(
                                        modifier = Modifier
                                            .width(105.dp)
                                            .clickable {
                                                navController.navigate("book_detail/${relBook.id}")
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                    .data(relBook.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=300&q=80" })
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = relBook.bookName,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = relBook.bookName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = relBook.subject,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // ✨ AI Book Summary Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                val localFile = offlineBooks.find { it.id == currentBook.id }
                                val urlToOpen = if (localFile != null) "file://" + localFile.localPdfPath else currentBook.pdfUrl
                                if (urlToOpen.isNotEmpty()) {
                                    val encodedUrl = URLEncoder.encode(urlToOpen, "UTF-8")
                                    navController.navigate("book_summary?url=$encodedUrl&bookId=${currentBook.id}")
                                } else {
                                    Toast.makeText(context, "Syllabus book PDF path is not ready yet.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "AI Book Summary",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "NEW",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Generate chapters outline, metadata, learning outcomes & overall book insights.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Summary",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gemini AI Doubt Solver Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                val encodedPrompt = URLEncoder.encode(
                                    "Hi Gemini AI! I am reading the textbook '${currentBook.bookName}' for ${currentBook.subject} (Class ${currentBook.className}). Can you help me study this book and solve standard questions from it?",
                                    "UTF-8"
                                )
                                navController.navigate("ai_chat?prompt=$encodedPrompt")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Gemini AI Assistant",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "PRO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Get instant step-by-step explanations, answers to questions & homework help.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Discuss with AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

data class BookDetailInfo(
    val value: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ShimmerPlaceholder(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(10f, 10f),
        end = androidx.compose.ui.geometry.Offset(translateAnim.value, translateAnim.value)
    )
    
    Box(
        modifier = modifier
            .background(brush, RoundedCornerShape(8.dp))
    )
}

@Composable
fun BookDetailShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Cover placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            ShimmerPlaceholder(
                modifier = Modifier
                    .width(170.dp)
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title placeholder
            ShimmerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(28.dp)
            )
            
            // Info chips row placeholder
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerPlaceholder(modifier = Modifier.width(100.dp).height(32.dp))
                ShimmerPlaceholder(modifier = Modifier.width(100.dp).height(32.dp))
            }
            
            // Details Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .width(96.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
            
            // Description paragraph
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(16.dp))
                ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(16.dp))
                ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
            }
        }
    }
}

@Composable
fun BookDetailError(onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error icon",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Book not available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "There was a problem loading the book details. Please check your connection or retry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Go Back")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
