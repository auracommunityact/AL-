package com.example.ui.books

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.net.URLEncoder

// Brand Colors (Navy Blue & White Theme)
private val NavyPrimary = Color(0xFF0F2C59)
private val NavyLight = Color(0xFF1E3E62)
private val AccentBlue = Color(0xFF3B82F6)
private val BackgroundWhite = Color(0xFFF8F9FA)
private val CardBorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSummaryScreen(
    navController: NavController,
    pdfUrl: String,
    bookId: String,
    viewModel: BookSummaryViewModel = viewModel()
) {
    val uiState by viewModel.summaryState.collectAsState()
    val progressStatus by viewModel.progressStatus.collectAsState()
    val progressPercentage by viewModel.progressPercentage.collectAsState()
    val readingPage by viewModel.readingPage.collectAsState()

    LaunchedEffect(pdfUrl, bookId) {
        viewModel.getSummary(pdfUrl, bookId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Aura AI Book Analysis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NavyPrimary
                        )
                        if (uiState is SummaryUiState.Success) {
                            val success = uiState as SummaryUiState.Success
                            val badgeText = if (success.isCached) "Cached Offline" else if (success.isOffline) "Offline Local Processing" else "AI Active Engine"
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundWhite)
        ) {
            when (val state = uiState) {
                is SummaryUiState.Idle -> { }
                is SummaryUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.linearGradient(listOf(NavyPrimary, NavyLight)),
                                    RoundedCornerShape(24.dp)
                                )
                                .shadow(8.dp, RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "Analyzing Textbook Structure",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        LinearProgressIndicator(
                            progress = { progressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = AccentBlue,
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = progressStatus,
                                style = MaterialTheme.typography.labelLarge,
                                color = NavyLight,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(progressPercentage * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                    }
                }
                is SummaryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(72.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Analysis Interrupted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { viewModel.getSummary(pdfUrl, bookId) },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume Analysis")
                        }
                    }
                }
                is SummaryUiState.Success -> {
                    SummaryDashboard(
                        data = state.data,
                        pdfUrl = pdfUrl,
                        navController = navController,
                        readingPage = readingPage
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryDashboard(
    data: BookSummaryData,
    pdfUrl: String,
    navController: NavController,
    readingPage: Int
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📊 Overview", "📖 Chapters", "⚡ Quick Revision")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = NavyPrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedTab == index) NavyPrimary else Color.Gray
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> OverviewTab(data = data, readingPage = readingPage)
                1 -> ChaptersTab(data = data, pdfUrl = pdfUrl, navController = navController)
                2 -> QuickRevisionTab(data = data)
            }
        }
    }
}

@Composable
fun OverviewTab(data: BookSummaryData, readingPage: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Grid Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📈 Book Analytics & Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Current Chapter calculation
                val activePage = if (readingPage > 0) readingPage else 1
                val activeChapter = data.chapters.find { activePage in it.startPage..it.endPage }
                val currentChapterName = activeChapter?.chapterName ?: "Introduction"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(
                        title = "Total Pages",
                        value = "${data.metadata.totalPages}",
                        icon = Icons.Filled.Pages,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "Total Chapters",
                        value = "${data.chapters.size}",
                        icon = Icons.Filled.List,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(
                        title = "Current Page",
                        value = if (readingPage > 0) "Page $readingPage" else "Not Started",
                        icon = Icons.Filled.MenuBook,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "Current Chapter",
                        value = currentChapterName,
                        icon = Icons.Filled.Bookmark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reading Progress bar
                val progress = if (data.metadata.totalPages > 0) {
                    activePage.toFloat() / data.metadata.totalPages
                } else 0f

                Text(
                    text = "Reading Journey Progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NavyPrimary,
                        trackColor = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
            }
        }

        // Complete Book Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📘 Complete Book Synthesis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = data.finalSummary.completeBookSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155),
                    lineHeight = 22.sp
                )
            }
        }

        // Top 20 Key Points Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎯 Top 20 High-Yield Key Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                data.finalSummary.top20KeyPoints.forEachIndexed { idx, point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(22.dp)
                                .background(NavyPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Important Topics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📌 Major Syllabus Topics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                data.finalSummary.importantTopics.forEach { topic ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }

        // Exam Prep and Quick Revision Notes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✍️ Exam Preparation Strategy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = data.finalSummary.examPreparationNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "⚡ Quick Revision Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = data.finalSummary.quickRevisionNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF334155),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }
    }
}

@Composable
fun ChaptersTab(data: BookSummaryData, pdfUrl: String, navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📖 Curriculum Chapter Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(data.chapters) { chapter ->
            var isExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BoxBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NavyPrimary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${chapter.chapterNumber}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapter.chapterName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Page Range: ${chapter.startPage} - ${chapter.endPage}",
                                style = MaterialTheme.typography.labelLarge,
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = chapter.shortSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val encoded = URLEncoder.encode(pdfUrl, "UTF-8")
                                navController.navigate("pdf_viewer?url=$encoded&page=${chapter.startPage - 1}")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Chapter", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            border = ButtonDefaults.outlinedButtonBorder().copy(width = 1.dp)
                        ) {
                            Text(if (isExpanded) "Hide Study Guide" else "Show Study Guide", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            HorizontalDivider(color = CardBorderColor)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Detailed analysis",
                                style = MaterialTheme.typography.titleSmall,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = chapter.detailedSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF475569),
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Expandable study panels
                            ExpandableItem(title = "🎯 Chapter Key Takeaways", items = chapter.keyPoints)
                            ExpandableItem(title = "🔑 Technical Terms & Definitions", items = chapter.importantDefinitions)
                            
                            if (chapter.importantFormulas.isNotEmpty()) {
                                ExpandableItem(title = "🧮 Scientific Formulas", items = chapter.importantFormulas)
                            }
                            if (chapter.importantDates.isNotEmpty()) {
                                ExpandableItem(title = "📅 Historical Dates & Events", items = chapter.importantDates)
                            }
                            if (chapter.importantNames.isNotEmpty()) {
                                ExpandableItem(title = "👤 Core Contributors & Figures", items = chapter.importantNames)
                            }

                            // FAQ items
                            if (chapter.frequentlyAskedQuestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "❓ Frequently Asked Questions",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NavyLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                chapter.frequentlyAskedQuestions.forEach { faq ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Q: ${faq.question}",
                                                fontWeight = FontWeight.Bold,
                                                color = NavyPrimary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "A: ${faq.answer}",
                                                color = Color(0xFF475569),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            if (chapter.revisionNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "📝 Chapter Revision Notes",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chapter.revisionNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    lineHeight = 18.sp
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
fun ExpandableItem(title: String, items: List<String>) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NavyLight
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Circle,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))
    }
}

@Composable
fun QuickRevisionTab(data: BookSummaryData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚡ Unified Exam Revision Sheet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = NavyPrimary
        )

        // Unified Glossary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔑 Master Technical Glossary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                val allDefs = data.chapters.flatMap { it.importantDefinitions }
                if (allDefs.isEmpty()) {
                    Text(
                        "No technical terms detected.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    allDefs.forEach { definition ->
                        val parts = definition.split(":", limit = 2)
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = parts.firstOrNull() ?: "",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (parts.size > 1) {
                                Text(
                                    text = parts[1].trim(),
                                    color = Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Unified Formula Sheet
        val allFormulas = data.chapters.flatMap { it.importantFormulas }
        if (allFormulas.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BoxBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧮 Unified Formula Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(color = CardBorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    allFormulas.forEach { formula ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formula,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        // Consolidated FAQ Checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BoxBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "❓ Consolidated Exam Q&A Checklist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                val allFaqs = data.chapters.flatMap { it.frequentlyAskedQuestions }
                if (allFaqs.isEmpty()) {
                    Text(
                        "No FAQs compiled for this book.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    allFaqs.forEach { faq ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Question: ${faq.question}",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Answer: ${faq.answer}",
                                color = Color(0xFF475569),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

private fun BoxBorder() = BorderStroke(1.dp, CardBorderColor)
