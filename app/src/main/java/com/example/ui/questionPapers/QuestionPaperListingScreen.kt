package com.example.ui.questionPapers

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.QuestionPaper
import com.example.ui.ViewModelFactory
import com.example.ui.home.HomeViewModel

enum class SelectionLevel {
    SECTION, SUBJECT, PAPERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaperListingScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = ViewModelFactory)
) {
    val allPapers by viewModel.allQuestionPapers.collectAsState()
    val sectionsFromDb by viewModel.questionPaperSections.collectAsState()
    
    val context = LocalContext.current
    var selectionLevel by remember { mutableStateOf(SelectionLevel.SECTION) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    val activeSections = sectionsFromDb.filter { it.isActive }
    val displaySections = if (activeSections.isNotEmpty()) {
        activeSections.map { it.name }
    } else {
        allPapers.map { it.className }.distinct().sorted()
    }
    
    val subjectsForSection = allPapers.filter { it.className == selectedSection || it.section == selectedSection }.map { it.subject }.distinct().sorted()
    val papersForSubject = allPapers.filter { (it.className == selectedSection || it.section == selectedSection) && it.subject == selectedSubject }.sortedByDescending { it.year }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectionLevel) {
                            SelectionLevel.SECTION -> "Select Class"
                            SelectionLevel.SUBJECT -> "Select Subject ($selectedSection)"
                            SelectionLevel.PAPERS -> "$selectedSubject Papers"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    if (selectionLevel != SelectionLevel.SECTION) {
                        IconButton(onClick = {
                            selectionLevel = when (selectionLevel) {
                                SelectionLevel.PAPERS -> SelectionLevel.SUBJECT
                                SelectionLevel.SUBJECT -> SelectionLevel.SECTION
                                else -> SelectionLevel.SECTION
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = selectionLevel,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "LevelTransition"
            ) { level ->
                when (level) {
                    SelectionLevel.SECTION -> {
                        SectionSelection(displaySections, sectionsFromDb) { section ->
                            selectedSection = section
                            selectionLevel = SelectionLevel.SUBJECT
                        }
                    }
                    SelectionLevel.SUBJECT -> {
                        SubjectSelection(subjectsForSection) { subject ->
                            selectedSubject = subject
                            selectionLevel = SelectionLevel.PAPERS
                        }
                    }
                    SelectionLevel.PAPERS -> {
                        PapersList(papersForSubject) { paper ->
                            com.example.utils.AdsManager.showInterstitial(context) {
                                navController.navigate("pdf_viewer/${paper.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionSelection(
    sections: List<String>, 
    sectionsFromDb: List<com.example.data.models.QuestionPaperSection>,
    onSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(sections) { sectionName ->
            val sectionFromDb = sectionsFromDb.find { it.name == sectionName }
            
            Card(
                onClick = { onSelected(sectionName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (sectionFromDb?.thumbnail?.isNotEmpty() == true) {
                        AsyncImage(
                            model = sectionFromDb.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                        startY = 100f
                                    )
                                )
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (sectionFromDb?.thumbnail?.isEmpty() != false) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            text = sectionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sectionFromDb?.thumbnail?.isNotEmpty() == true) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        if (sectionFromDb?.description?.isNotEmpty() == true) {
                            Text(
                                text = sectionFromDb.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sectionFromDb.thumbnail.isNotEmpty()) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectSelection(subjects: List<String>, onSelected: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(subjects) { subject ->
            Surface(
                onClick = { onSelected(subject) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PapersList(papers: List<QuestionPaper>, onPaperClick: (QuestionPaper) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(papers) { paper ->
            QuestionPaperCard(paper, onPaperClick)
        }
    }
}

@Composable
fun QuestionPaperCard(paper: QuestionPaper, onClick: (QuestionPaper) -> Unit) {
    Card(
        onClick = { onClick(paper) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = paper.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f) ) {
                Text(
                    text = paper.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${paper.board} • ${paper.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${paper.totalPages} Pages • ${paper.fileSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var isShortcutPinned by remember(paper.id) { mutableStateOf(com.example.utils.ShortcutHelper.isShortcutPinned(context, "qpaper_${paper.id}")) }

            IconButton(onClick = {
                com.example.utils.ShareHelper.shareContent(
                    context = context,
                    title = paper.title,
                    contentType = "questionPaper",
                    id = paper.id
                )
            }) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share Question Paper",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                coroutineScope.launch {
                    if (isShortcutPinned) {
                        com.example.utils.ShortcutHelper.removeShortcut(context, "qpaper_${paper.id}", paper.title)
                        isShortcutPinned = false
                    } else {
                        com.example.utils.ShortcutHelper.pinShortcut(
                            context = context,
                            id = "qpaper_${paper.id}",
                            title = paper.title,
                            imageUrl = paper.thumbnail,
                            type = "questionpaper",
                            internalRoute = "deeplink_loader?type=questionPaper&slug=${paper.id}"
                        )
                        isShortcutPinned = true
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Add to Home Screen",
                    tint = if (isShortcutPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Icon(
                Icons.Default.Download,
                contentDescription = "View PDF",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
