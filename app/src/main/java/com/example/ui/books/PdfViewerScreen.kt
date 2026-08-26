package com.example.ui.books
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.local.PdfAnnotation
import com.example.data.local.PdfBookmark
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
enum class AnnotationTool {
    NONE, PEN, HIGHLIGHTER, UNDERLINE, PENCIL, MARKER, STRAIGHT_LINE, RECTANGLE, CIRCLE, ARROW, TEXT, STICKY_NOTE, ERASER
}
enum class ReadingMode {
    LIGHT, NIGHT, SEPIA
}
enum class ViewMode {
    HORIZONTAL, VERTICAL
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    navController: NavController,
    pdfUrl: String,
    bookId: String = "default_book",
    initialPageArg: Int = -1,
    viewModel: PdfViewerViewModel = viewModel()
) {
    val progress by viewModel.downloadProgress.collectAsState()
    val pageCount by viewModel.pdfPageCount.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val initialPage by viewModel.initialPage.collectAsState()
    val pagerState = rememberPagerState(pageCount = { pageCount })
    LaunchedEffect(initialPage, pageCount) {
        if (pageCount > 0) {
            val targetPage = if (initialPageArg >= 0) initialPageArg else initialPage
            if (targetPage in 0 until pageCount) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateProgress(bookId, pagerState.currentPage)
    }
    var currentTool by remember { mutableStateOf(AnnotationTool.NONE) }
    var selectedColor by remember { mutableStateOf(Color(0xFFE53935)) }
    var strokeWidth by remember { mutableStateOf(5f) }
    val colors = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835), 
        Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFFF48FB1), Color(0xFF212121),
        Color.White
    )
    val undoStack = remember { mutableStateListOf<PdfAnnotation>() }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }
    var summaryText by remember { mutableStateOf("") }
    var isUIVisible by remember { mutableStateOf(true) }
    var readingMode by remember { mutableStateOf(ReadingMode.LIGHT) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val startTime = remember { System.currentTimeMillis() }
    val handleBack = {
        val timeSpentMillis = System.currentTimeMillis() - startTime
        val minutes = (timeSpentMillis / 1000) / 60
        val seconds = (timeSpentMillis / 1000) % 60
        val timeString = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        android.widget.Toast.makeText(context, "Time spent reading: $timeString", android.widget.Toast.LENGTH_SHORT).show()
        navController.popBackStack()
    }
    BackHandler {
        handleBack()
    }
    LaunchedEffect(pdfUrl, bookId) {
        viewModel.loadPdf(pdfUrl, bookId)
    }
    // Background color based on mode
    val bgColor = when(readingMode) {
        ReadingMode.LIGHT -> Color(0xFFF5F5F5)
        ReadingMode.NIGHT -> Color(0xFF121212)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
    }
    val topBarColor = when(readingMode) {
        ReadingMode.LIGHT -> Color.White.copy(alpha = 0.95f)
        ReadingMode.NIGHT -> Color(0xFF1E1E1E).copy(alpha = 0.95f)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8).copy(alpha = 0.95f)
    }
    val contentColor = when(readingMode) {
        ReadingMode.LIGHT -> Color.Black
        ReadingMode.NIGHT -> Color.White
        ReadingMode.SEPIA -> Color(0xFF5B4636)
    }
    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = { Text("Bookmarks & TOC", fontWeight = FontWeight.Bold) },
            text = {
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks added yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bookmarks.size) { index ->
                            val bookmark = bookmarks[index]
                            Card(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(bookmark.pageNumber)
                                        showBookmarksDialog = false
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Page ${bookmark.pageNumber + 1}", fontWeight = FontWeight.SemiBold)
                                    }
                                    IconButton(onClick = { viewModel.toggleBookmark(bookId, bookmark.pageNumber) }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove Bookmark", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    if (showSummaryDialog) {
        ModalBottomSheet(
            onDismissRequest = { if (!isSummarizing) showSummaryDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AI Assistant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                if (isSummarizing) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Analyzing page content...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    SelectionContainer {
                        Text(summaryText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showSummaryDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Reader Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                // Reading Mode
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeOption(ReadingMode.LIGHT, readingMode, "Light", Color(0xFFF5F5F5), Color.Black) { readingMode = it }
                        ThemeOption(ReadingMode.NIGHT, readingMode, "Dark", Color(0xFF1E1E1E), Color.White) { readingMode = it }
                        ThemeOption(ReadingMode.SEPIA, readingMode, "Sepia", Color(0xFFF4ECD8), Color(0xFF5B4636)) { readingMode = it }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    Scaffold(
        containerColor = bgColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (progress == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            } else if (progress == -1f) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Text("Failed to load document", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { viewModel.loadPdf(pdfUrl, bookId) }) {
                        Text("Try Again")
                    }
                }
            } else if (progress!! < 1f) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = { progress!! }, modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparing Document... ${(progress!! * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = contentColor)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (currentTool == AnnotationTool.NONE) {
                                        isUIVisible = !isUIVisible
                                    }
                                }
                            )
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = currentTool == AnnotationTool.NONE
                    ) { page ->
                        PdfPage(
                            pageIndex = page,
                            viewModel = viewModel,
                            annotations = annotations.filter { it.pageNumber == page },
                            currentTool = currentTool,
                            selectedColor = selectedColor,
                            strokeWidth = strokeWidth,
                            bookId = bookId,
                            readingMode = readingMode
                        )
                    }
                }
            }
            // Top App Bar - Animated visibility
            AnimatedVisibility(
                visible = isUIVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = topBarColor,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { handleBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = contentColor)
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                "Document",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (pageCount > 0) {
                                Text(
                                    "${pagerState.currentPage + 1} of $pageCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                        IconButton(onClick = { /* Search */ }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = contentColor)
                        }
                        val currentPageIndex by remember { derivedStateOf { pagerState.currentPage } }
                        val isBookmarked = bookmarks.any { it.pageNumber == currentPageIndex }
                        IconButton(onClick = { viewModel.toggleBookmark(bookId, currentPageIndex) }) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else contentColor
                            )
                        }
                        IconButton(onClick = { showBookmarksDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "TOC", tint = contentColor)
                        }
                        var showMoreMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = contentColor)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Display Settings") },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                    onClick = { 
                                        showMoreMenu = false
                                        showSettingsSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("View Complete Book Summary") },
                                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        val encodedUrl = java.net.URLEncoder.encode(pdfUrl, "UTF-8")
                                        navController.navigate("book_summary?url=$encodedUrl&bookId=$bookId")
                                    }
                                )
                            }
                        }
                    }
                }
            }
            // Floating AI Button
            AnimatedVisibility(
                visible = isUIVisible && currentTool == AnnotationTool.NONE,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                FloatingActionButton(
                    onClick = {
                        showSummaryDialog = true
                        isSummarizing = true
                        summaryText = ""
                        coroutineScope.launch {
                            val pageIndex = pagerState.currentPage
                            summaryText = viewModel.summarizePage(pageIndex, 1080)
                            isSummarizing = false
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Assistant")
                }
            }
            // Bottom Controls Container
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page Navigation
                var showPageJumpDialog by remember { mutableStateOf(false) }
                AnimatedVisibility(
                    visible = isUIVisible && currentTool == AnnotationTool.NONE,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(0.9f)
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = topBarColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                    enabled = pagerState.currentPage > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Page", tint = if (pagerState.currentPage > 0) contentColor else contentColor.copy(alpha = 0.3f))
                                }
                                Text(
                                    text = "Page ${pagerState.currentPage + 1} of $pageCount",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showPageJumpDialog = true }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                    enabled = pagerState.currentPage < pageCount - 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Page", tint = if (pagerState.currentPage < pageCount - 1) contentColor else contentColor.copy(alpha = 0.3f))
                                }
                            }
                            Slider(
                                value = pagerState.currentPage.toFloat(),
                                onValueChange = { 
                                    coroutineScope.launch { 
                                        pagerState.scrollToPage(it.toInt()) 
                                    } 
                                },
                                valueRange = 0f..maxOf(0f, (pageCount - 1).toFloat()),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
                if (showPageJumpDialog) {
                    var jumpPageText by remember { mutableStateOf((pagerState.currentPage + 1).toString()) }
                    AlertDialog(
                        onDismissRequest = { showPageJumpDialog = false },
                        title = { Text("Jump to Page") },
                        text = {
                            OutlinedTextField(
                                value = jumpPageText,
                                onValueChange = { jumpPageText = it.filter { char -> char.isDigit() } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Page Number") }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val page = jumpPageText.toIntOrNull()
                                if (page != null && page in 1..pageCount) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page - 1)
                                    }
                                }
                                showPageJumpDialog = false
                            }) {
                                Text("Go")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPageJumpDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                // Bottom Floating Toolbar
                AnimatedVisibility(
                    visible = isUIVisible || currentTool != AnnotationTool.NONE,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = topBarColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Color & Stroke controls (only visible if drawing tool is selected)
                        AnimatedVisibility(visible = currentTool == AnnotationTool.PEN || currentTool == AnnotationTool.HIGHLIGHTER) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Thickness", style = MaterialTheme.typography.labelMedium, color = contentColor)
                                    Slider(
                                        value = strokeWidth,
                                        onValueChange = { strokeWidth = it },
                                        valueRange = 1f..30f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                                    )
                                }
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(colors) { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    2.dp,
                                                    if (selectedColor == c) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                                .clickable { selectedColor = c }
                                        )
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = contentColor.copy(alpha = 0.1f))
                            }
                        }
                        // Main tools
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentTool != AnnotationTool.NONE) {
                                IconButton(onClick = { currentTool = AnnotationTool.NONE }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close Tool", tint = contentColor)
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = contentColor.copy(alpha = 0.2f))
                            }
                            ToolButton(Icons.Outlined.Create, "Pen", currentTool == AnnotationTool.PEN, contentColor) { currentTool = AnnotationTool.PEN }
                            ToolButton(Icons.Outlined.FormatColorFill, "Highlighter", currentTool == AnnotationTool.HIGHLIGHTER, contentColor) { currentTool = AnnotationTool.HIGHLIGHTER }
                            ToolButton(Icons.Outlined.ChangeHistory, "Shapes", currentTool == AnnotationTool.RECTANGLE, contentColor) { currentTool = AnnotationTool.RECTANGLE }
                            ToolButton(Icons.Outlined.Notes, "Text", currentTool == AnnotationTool.TEXT, contentColor) { currentTool = AnnotationTool.TEXT }
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = contentColor.copy(alpha = 0.2f))
                            IconButton(
                                onClick = {
                                    if (currentTool == AnnotationTool.ERASER) currentTool = AnnotationTool.NONE
                                    else currentTool = AnnotationTool.ERASER
                                }
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eraser", tint = if (currentTool == AnnotationTool.ERASER) MaterialTheme.colorScheme.primary else contentColor)
                            }
                            IconButton(
                                onClick = {
                                    val last = annotations.maxByOrNull { it.timestamp }
                                    if (last != null) {
                                        undoStack.add(last)
                                        viewModel.deleteAnnotation(last.id)
                                    }
                                },
                                enabled = annotations.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (annotations.isNotEmpty()) contentColor else contentColor.copy(alpha = 0.3f))
                            }
                            IconButton(
                                onClick = {
                                    val toRedo = undoStack.lastOrNull()
                                    if (toRedo != null) {
                                        undoStack.remove(toRedo)
                                        viewModel.addAnnotation(toRedo)
                                    }
                                },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (undoStack.isNotEmpty()) contentColor else contentColor.copy(alpha = 0.3f))
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
fun ThemeOption(
    mode: ReadingMode,
    currentMode: ReadingMode,
    label: String,
    bgColor: Color,
    textColor: Color,
    onClick: (ReadingMode) -> Unit
) {
    val selected = mode == currentMode
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick(mode) }) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = textColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isSelected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape) else Modifier
    ) {
        Icon(icon, contentDescription = description, tint = if (isSelected) MaterialTheme.colorScheme.primary else tint)
    }
}
@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}
@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color) {
    Box(modifier = modifier.width(1.dp).background(color))
}
@Composable
fun PdfPage(
    pageIndex: Int,
    viewModel: PdfViewerViewModel,
    annotations: List<PdfAnnotation>,
    currentTool: AnnotationTool,
    selectedColor: Color,
    strokeWidth: Float,
    bookId: String,
    readingMode: ReadingMode
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var width by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val gson = remember { Gson() }
    // Zoom state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(width) {
        if (width > 0 && bitmap == null) {
            coroutineScope.launch(Dispatchers.IO) {
                bitmap = viewModel.renderPage(pageIndex, width)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { width = it.width }
            .clip(androidx.compose.ui.graphics.RectangleShape)
    ) {
        if (bitmap != null) {
            // Apply reading mode filters
            val colorFilter = when (readingMode) {
                ReadingMode.NIGHT -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix(
                        floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
                ReadingMode.SEPIA -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix(
                        floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
                else -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        if (currentTool == AnnotationTool.NONE) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page $pageIndex",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    colorFilter = colorFilter
                )
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(currentTool, scale) {
                            if (currentTool != AnnotationTool.NONE && scale == 1f) {
                                detectDragGestures(
                                    onDragStart = { currentPath = listOf(it) },
                                    onDragEnd = {
                                        if (currentPath.isNotEmpty()) {
                                            if (currentTool == AnnotationTool.ERASER) {
                                                val eraserRect = Rect(currentPath.first(), currentPath.last()).inflate(40f)
                                                annotations.forEach { ann ->
                                                    try {
                                                        val typeToken = object : TypeToken<List<Offset>>() {}.type
                                                        val points: List<Offset> = gson.fromJson(ann.coordinates, typeToken)
                                                        if (points.any { p -> eraserRect.contains(p) }) {
                                                            viewModel.deleteAnnotation(ann.id)
                                                        }
                                                    } catch (e: Exception) { }
                                                }
                                            } else {
                                                val annotation = PdfAnnotation(
                                                    bookId = bookId,
                                                    userId = "current_user",
                                                    pageNumber = pageIndex,
                                                    type = currentTool.name,
                                                    color = selectedColor.value.toLong(),
                                                    strokeWidth = if (currentTool == AnnotationTool.HIGHLIGHTER) 20f else strokeWidth,
                                                    coordinates = gson.toJson(currentPath)
                                                )
                                                viewModel.addAnnotation(annotation)
                                            }
                                            currentPath = emptyList()
                                        }
                                    },
                                    onDragCancel = { currentPath = emptyList() },
                                    onDrag = { change, _ ->
                                        currentPath = currentPath + change.position
                                    }
                                )
                            }
                        }
                ) {
                    // Draw saved annotations
                    annotations.forEach { ann ->
                        try {
                            val typeToken = object : TypeToken<List<Offset>>() {}.type
                            val points: List<Offset> = gson.fromJson(ann.coordinates, typeToken)
                            if (points.isNotEmpty()) {
                                val alpha = if (ann.type == AnnotationTool.HIGHLIGHTER.name) 0.4f else 1f
                                val color = Color(ann.color.toULong()).copy(alpha = alpha)
                                if (ann.type == AnnotationTool.RECTANGLE.name) {
                                    val start = points.first()
                                    val end = points.last()
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                                        size = androidx.compose.ui.geometry.Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)),
                                        style = Stroke(width = ann.strokeWidth)
                                    )
                                } else if (ann.type == AnnotationTool.UNDERLINE.name) {
                                    drawLine(
                                        color = color,
                                        start = Offset(points.first().x, points.first().y + 10f),
                                        end = Offset(points.last().x, points.first().y + 10f),
                                        strokeWidth = ann.strokeWidth
                                    )
                                } else {
                                    val path = Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        for (i in 1 until points.size) { lineTo(points[i].x, points[i].y) }
                                    }
                                    drawPath(
                                        path = path,
                                        color = color,
                                        style = Stroke(width = ann.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        } catch (e: Exception) { }
                    }
                    // Draw current path
                    if (currentPath.isNotEmpty() && currentTool != AnnotationTool.ERASER) {
                        val alpha = if (currentTool == AnnotationTool.HIGHLIGHTER) 0.4f else 1f
                        val sWidth = if (currentTool == AnnotationTool.HIGHLIGHTER) 20f else strokeWidth
                        val drawColor = selectedColor.copy(alpha = alpha)
                        if (currentTool == AnnotationTool.RECTANGLE) {
                            val start = currentPath.first()
                            val end = currentPath.last()
                            drawRect(
                                color = drawColor,
                                topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                                size = androidx.compose.ui.geometry.Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)),
                                style = Stroke(width = sWidth)
                            )
                        } else if (currentTool == AnnotationTool.UNDERLINE) {
                            drawLine(
                                color = drawColor,
                                start = Offset(currentPath.first().x, currentPath.first().y + 10f),
                                end = Offset(currentPath.last().x, currentPath.first().y + 10f),
                                strokeWidth = sWidth
                            )
                        } else {
                            val path = Path().apply {
                                moveTo(currentPath.first().x, currentPath.first().y)
                                for (i in 1 until currentPath.size) { lineTo(currentPath[i].x, currentPath[i].y) }
                            }
                            drawPath(
                                path = path,
                                color = drawColor,
                                style = Stroke(width = sWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
