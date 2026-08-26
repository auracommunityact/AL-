package com.example.ui.study.websitereader

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.local.WebsiteChatEntity
import com.example.data.local.WebsiteReaderEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteReaderScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: WebsiteReaderViewModel = viewModel()

    val cachedWebsites by viewModel.cachedWebsites.collectAsState()
    val currentWebsite by viewModel.currentWebsite.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingStage by viewModel.loadingStage.collectAsState()
    val error by viewModel.error.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: AI Summary, 1: Reading Mode, 2: Ask Anything

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Website AI Reader",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        currentWebsite?.let {
                            Text(
                                text = it.domain,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentWebsite != null) {
                            viewModel.deselectWebsite()
                            activeTab = 0
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (currentWebsite != null) {
                        IconButton(onClick = {
                            viewModel.loadWebsite(currentWebsite!!.url, forceRefresh = true)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            shareText(context, "${currentWebsite!!.title}\n\nSummary:\n${currentWebsite!!.aiSummary ?: ""}")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share summary",
                                tint = Color.White
                            )
                        }
                    } else if (cachedWebsites.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.clearAllWebsites()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Cache",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentWebsite == null) {
                // HOME VIEW (URL Input + History List)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Title Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Reader Logo",
                                        tint = Color(0xFF818CF8)
                                    )
                                }
                                Text(
                                    text = "Summarize Any Webpage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Paste a public URL to extract headings, tables, clean up clutter, translate, and study with interactive AI Q&A.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("https://example.com/topic") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link icon") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF6366F1)
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    viewModel.loadWebsite(urlInput)
                                } else {
                                    Toast.makeText(context, "Please paste a website URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = Color.White)
                        }
                    }

                    error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Cache list / History
                    Text(
                        text = "Previously Opened Summaries",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (cachedWebsites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "No history",
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(60.dp)
                                )
                                Text(
                                    text = "No cached pages available.",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(cachedWebsites) { site ->
                                HistoryCard(
                                    site = site,
                                    onClick = { viewModel.selectWebsite(site) },
                                    onDelete = { viewModel.deleteCachedWebsite(site.url) }
                                )
                            }
                        }
                    }
                }
            } else {
                // ACTIVE WEBSITE TABBED VIEW
                val site = currentWebsite!!
                Column(modifier = Modifier.fillMaxSize()) {
                    // Navigation Tabs
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFF818CF8),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = Color(0xFF818CF8)
                            )
                        }
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("AI Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Summary") },
                            selectedContentColor = Color(0xFF818CF8),
                            unselectedContentColor = Color.White.copy(alpha = 0.6f)
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Reading Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Reader") },
                            selectedContentColor = Color(0xFF818CF8),
                            unselectedContentColor = Color.White.copy(alpha = 0.6f)
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Ask Anything", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = "Ask") },
                            selectedContentColor = Color(0xFF818CF8),
                            unselectedContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activeTab) {
                            0 -> SummaryView(site, viewModel)
                            1 -> ReadingView(site, viewModel)
                            2 -> ChatView(site, viewModel)
                        }
                    }
                }
            }

            // LOADING SCREEN OVERLAY
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF818CF8),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "Analyzing Webpage...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = loadingStage,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(site: WebsiteReaderEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Favicon with fallback
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF334155), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = site.faviconUrl,
                        contentDescription = "Favicon",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        onError = { /* fallback */ }
                    )
                }

                Column {
                    Text(
                        text = site.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = site.domain,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete from cache",
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

fun parseMarkdownSections(rawMarkdown: String): Map<String, String> {
    val sections = mutableMapOf<String, String>()
    if (rawMarkdown.isBlank()) return sections
    val lines = rawMarkdown.split("\n")
    var currentSection = ""
    val currentContent = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("### ")) {
            if (currentSection.isNotEmpty()) {
                sections[currentSection] = currentContent.toString().trim()
                currentContent.clear()
            }
            currentSection = line.replace("###", "").trim()
        } else {
            if (currentSection.isNotEmpty()) {
                currentContent.append(line).append("\n")
            }
        }
    }
    if (currentSection.isNotEmpty()) {
        sections[currentSection] = currentContent.toString().trim()
    }
    return sections
}

@Composable
fun SummaryView(site: WebsiteReaderEntity, viewModel: WebsiteReaderViewModel) {
    val context = LocalContext.current
    val isHindiMode by viewModel.isHindiMode.collectAsState()
    val translatedSummary by viewModel.translatedSummary.collectAsState()
    val translationLoading by viewModel.translationLoading.collectAsState()

    val currentText = if (isHindiMode) translatedSummary ?: "" else site.aiSummary ?: ""
    val sections = remember(currentText) { parseMarkdownSections(currentText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Toolbar inside view for language switching
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = site.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
                Text(
                    text = "AI Study Guide",
                    fontSize = 12.sp,
                    color = Color(0xFF818CF8)
                )
            }

            // Language Toggle Switch
            Button(
                onClick = { viewModel.toggleLanguageMode() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHindiMode) Color(0xFF10B981) else Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Translate",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindiMode) "Hindi Devanagari" else "English Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (translationLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Color(0xFF818CF8))
                    Text("Translating study kit...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        } else if (sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Analyzing webpage structure...", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Render each accordion segment
                items(sections.toList()) { pair ->
                    AccordionCard(title = pair.first, content = pair.second)
                }
            }
        }
    }
}

@Composable
fun AccordionCard(title: String, content: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (expanded) Color(0xFF818CF8) else Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = when (title.lowercase()) {
                        "short summary" -> Icons.Default.Subject
                        "detailed summary" -> Icons.Default.Description
                        "important points" -> Icons.Default.List
                        "faqs" -> Icons.Default.HelpOutline
                        "key facts" -> Icons.Default.FactCheck
                        "definitions" -> Icons.Default.BookmarkBorder
                        "tables" -> Icons.Default.TableChart
                        "study notes" -> Icons.Default.Notes
                        "easy explanation" -> Icons.Default.Lightbulb
                        "beginner friendly explanation" -> Icons.Default.ChildCare
                        "important dates" -> Icons.Default.CalendarToday
                        "important numbers" -> Icons.Default.Pin
                        "important links" -> Icons.Default.InsertLink
                        else -> Icons.Default.Book
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (expanded) Color(0xFF818CF8) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = if (expanded) Color(0xFF818CF8) else Color.White,
                        fontSize = 15.sp
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Divider(
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    RenderMarkdownBody(text = content, fontSize = 14f, textColor = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownBody(text: String, fontSize: Float, textColor: Color) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("- ")) {
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("•", fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                    Text(
                        text = parseBoldText(trimmed.substring(2)),
                        fontSize = fontSize.sp,
                        color = textColor,
                        lineHeight = (fontSize * 1.4f).sp
                    )
                }
            } else if (trimmed.startsWith("1. ") || trimmed.startsWith("2. ") || trimmed.startsWith("3. ")) {
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(trimmed.substringBefore(" ") + " ", fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                    Text(
                        text = parseBoldText(trimmed.substringAfter(" ")),
                        fontSize = fontSize.sp,
                        color = textColor,
                        lineHeight = (fontSize * 1.4f).sp
                    )
                }
            } else if (trimmed.startsWith("|")) {
                // Table layout
                Text(
                    text = trimmed,
                    fontSize = (fontSize - 1f).sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF34D399),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                )
            } else {
                Text(
                    text = parseBoldText(trimmed),
                    fontSize = fontSize.sp,
                    color = textColor,
                    lineHeight = (fontSize * 1.4f).sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

fun parseBoldText(input: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val parts = input.split("**")
    for (i in parts.indices) {
        if (i % 2 == 1) {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color(0xFF818CF8)))
            builder.append(parts[i])
            builder.pop()
        } else {
            builder.append(parts[i])
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun ReadingView(site: WebsiteReaderEntity, viewModel: WebsiteReaderViewModel) {
    val context = LocalContext.current
    val elements by viewModel.currentElements.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentMatchIndex by viewModel.currentSearchResultIndex.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Determine custom reading mode background colors
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val highlightBg = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFFDE047)

    // Estimate Reading Progress (0f - 1f)
    val progress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) 0f
            else {
                val firstVisibleIndex = listState.firstVisibleItemIndex
                val firstVisibleScrollOffset = listState.firstVisibleItemScrollOffset
                val totalScrollRange = totalItems * 100f
                val currentScrollPosition = firstVisibleIndex * 100f + (firstVisibleScrollOffset / 10f)
                (currentScrollPosition / totalScrollRange).coerceIn(0f, 1f)
            }
        }
    }

    // Scroll to matching item when search index changes
    LaunchedEffect(currentMatchIndex) {
        if (currentMatchIndex >= 0 && currentMatchIndex < searchResults.size) {
            val matchingElementIndex = searchResults[currentMatchIndex]
            listState.animateScrollToItem(matchingElementIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Reading Progress Indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF818CF8),
            trackColor = Color(0xFF334155).copy(alpha = 0.3f)
        )

        // Reading view header containing Search and Style Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Style adjust button (Font size & Light/Dark Theme)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.setFontSize(fontSize - 2) },
                    modifier = Modifier.background(cardColor, CircleShape)
                ) {
                    Text("A-", color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(
                    onClick = { viewModel.setFontSize(fontSize + 2) },
                    modifier = Modifier.background(cardColor, CircleShape)
                ) {
                    Text("A+", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                IconButton(
                    onClick = { viewModel.toggleDarkMode() },
                    modifier = Modifier.background(cardColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Word Search field
            var searchInput by remember { mutableStateOf(searchQuery) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.performSearch(it)
                    },
                    placeholder = { Text("Find...", fontSize = 11.sp) },
                    modifier = Modifier
                        .width(110.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    singleLine = true
                )

                if (searchResults.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.previousSearchResult() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev", tint = textColor)
                    }
                    IconButton(
                        onClick = { viewModel.nextSearchResult() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", tint = textColor)
                    }
                }
            }
        }

        if (searchResults.isNotEmpty()) {
            Text(
                text = "Match ${currentMatchIndex + 1} of ${searchResults.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF818CF8),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Main Distraction Free Scrollable Text list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            item {
                // Article Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = site.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (fontSize + 4f).sp,
                            color = textColor,
                            lineHeight = (fontSize + 10f).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = site.domain,
                            fontSize = (fontSize - 4f).sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            itemsIndexed(elements) { index, element ->
                val isSelectedMatch = searchResults.getOrNull(currentMatchIndex) == index
                val itemBorderColor = if (isSelectedMatch) Color(0xFF818CF8) else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, itemBorderColor, RoundedCornerShape(8.dp))
                        .background(
                            if (isSelectedMatch) Color(0xFF818CF8).copy(alpha = 0.1f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    when (element.type) {
                        "h1" -> {
                            Text(
                                text = parseHighlightText(element.text, searchQuery, highlightBg, textColor, FontWeight.Bold),
                                fontSize = (fontSize + 3f).sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                lineHeight = (fontSize + 8f).sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        "h2" -> {
                            Text(
                                text = parseHighlightText(element.text, searchQuery, highlightBg, textColor, FontWeight.Bold),
                                fontSize = (fontSize + 1.5f).sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                lineHeight = (fontSize + 6f).sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        "h3" -> {
                            Text(
                                text = parseHighlightText(element.text, searchQuery, highlightBg, textColor, FontWeight.Bold),
                                fontSize = fontSize.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                lineHeight = (fontSize + 4f).sp
                            )
                        }
                        "li" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                                Text(
                                    text = parseHighlightText(element.text, searchQuery, highlightBg, textColor),
                                    fontSize = fontSize.sp,
                                    color = textColor,
                                    lineHeight = (fontSize * 1.4f).sp
                                )
                            }
                        }
                        "table" -> {
                            // Scrollable rendered table markdown
                            Text(
                                text = element.text,
                                fontSize = (fontSize - 2f).sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF065F46),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .background(cardColor, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            )
                        }
                        "link" -> {
                            Button(
                                onClick = {
                                    element.linkUrl?.let { url ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF818CF8).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF818CF8)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Launch, contentDescription = "Launch link", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = parseHighlightText(element.text, searchQuery, highlightBg, Color(0xFF818CF8), FontWeight.SemiBold).text,
                                    fontSize = (fontSize - 1f).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = parseHighlightText(element.text, searchQuery, highlightBg, textColor),
                                fontSize = fontSize.sp,
                                color = textColor,
                                lineHeight = (fontSize * 1.45f).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun parseHighlightText(
    input: String,
    query: String,
    highlightColor: Color,
    textColor: Color,
    fontWeight: FontWeight = FontWeight.Normal
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    if (query.isBlank()) {
        builder.pushStyle(SpanStyle(color = textColor, fontWeight = fontWeight))
        builder.append(input)
        builder.pop()
        return builder.toAnnotatedString()
    }

    var index = 0
    while (index < input.length) {
        val nextIndex = input.indexOf(query, index, ignoreCase = true)
        if (nextIndex == -1) {
            builder.pushStyle(SpanStyle(color = textColor, fontWeight = fontWeight))
            builder.append(input.substring(index))
            builder.pop()
            break
        }

        // Append text before match
        if (nextIndex > index) {
            builder.pushStyle(SpanStyle(color = textColor, fontWeight = fontWeight))
            builder.append(input.substring(index, nextIndex))
            builder.pop()
        }

        // Append matching text highlighted
        builder.pushStyle(
            SpanStyle(
                background = highlightColor,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )
        builder.append(input.substring(nextIndex, nextIndex + query.length))
        builder.pop()

        index = nextIndex + query.length
    }
    return builder.toAnnotatedString()
}

@Composable
fun ChatView(site: WebsiteReaderEntity, viewModel: WebsiteReaderViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    var questionInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to latest message on update
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Suggested Questions Chips
        val suggestedQuestions = listOf(
            "What is this page about?",
            "Summarize this website.",
            "Explain in simple language.",
            "What are the important points?"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedQuestions.forEach { query ->
                Button(
                    onClick = { viewModel.askQuestion(query) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(query, fontSize = 11.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Chat",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Ask anything about this webpage!",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Gemini is locked specifically to this page's text to prevent hallucinations and provide precise academic answers.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(chatHistory) { msg ->
                    ChatBubbleItem(msg)
                }
            }
        }

        // Chat input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = questionInput,
                onValueChange = { questionInput = it },
                placeholder = { Text("Ask about this website...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6366F1)
                ),
                maxLines = 4,
                singleLine = false
            )

            IconButton(
                onClick = {
                    if (questionInput.isNotBlank()) {
                        viewModel.askQuestion(questionInput)
                        questionInput = ""
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF6366F1), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(msg: WebsiteChatEntity) {
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    val containerColor = if (msg.isUser) Color(0xFF6366F1) else Color(0xFF1E293B)
    val contentColor = Color.White
    val shape = if (msg.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                RenderMarkdownBody(text = msg.text, fontSize = 13.5f, textColor = contentColor)
            }
        }
        Text(
            text = if (msg.isUser) "You" else "Gemini AI",
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Study Summary"))
}
