package com.example.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class ToolItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val iconColor: Color,
    val iconBgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(navController: NavController) {
    val toolsList = listOf(
        ToolItem(
            id = "planner",
            title = "Study Planner",
            description = "Organize your study schedule and goals.",
            icon = Icons.Filled.CalendarMonth,
            route = "study_planner",
            iconColor = Color(0xFF60A5FA), // light blue
            iconBgColor = Color(0xFF1E3A8A).copy(alpha = 0.4f)
        ),
        ToolItem(
            id = "countdown",
            title = "Exam Countdown",
            description = "Track the remaining days for upcoming exams.",
            icon = Icons.Filled.Timer,
            route = "exam_countdown",
            iconColor = Color(0xFFF87171), // light red
            iconBgColor = Color(0xFF7F1D1D).copy(alpha = 0.4f)
        ),
        ToolItem(
            id = "pdf_reader",
            title = "PDF Reader",
            description = "Open and read PDF files from your device.",
            icon = Icons.Filled.PictureAsPdf,
            route = "pdf_tool",
            iconColor = Color(0xFF34D399), // light green
            iconBgColor = Color(0xFF065F46).copy(alpha = 0.4f)
        ),
        ToolItem(
            id = "translate",
            title = "Translator",
            description = "Translate text into multiple languages.",
            icon = Icons.Filled.Language,
            route = "notes_translate",
            iconColor = Color(0xFFFBBF24), // light amber
            iconBgColor = Color(0xFF78350F).copy(alpha = 0.4f)
        ),
        ToolItem(
            id = "result_analysis",
            title = "Result Analysis",
            description = "Scan & analyze marksheet exam results with Gemini AI.",
            icon = Icons.Filled.Analytics,
            route = "result_analysis",
            iconColor = Color(0xFFF472B6), // light pink
            iconBgColor = Color(0xFF831843).copy(alpha = 0.4f)
        ),
        ToolItem(
            id = "website_reader",
            title = "Website AI Reader",
            description = "Summarize, translate, search, and chat with public websites.",
            icon = Icons.Filled.AutoAwesome,
            route = "website_reader",
            iconColor = Color(0xFF818CF8), // light indigo
            iconBgColor = Color(0xFF312E81).copy(alpha = 0.4f)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Tools",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Study Smarter, Not Harder",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "A curated collection of essential learning tools tailored to boost your productivity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(28.dp))

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val columnsCount = if (isLandscape) 3 else 2
                val gridHeight = if (isLandscape) 380.dp else 580.dp
                
                Box(modifier = Modifier.height(gridHeight).fillMaxWidth()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnsCount),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(toolsList) { tool ->
                            ToolCardItem(tool = tool) {
                                navController.navigate(tool.route)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FullWidthToolCardItem(tool: ToolItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tool.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tool.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = tool.iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ToolCardItem(tool: ToolItem, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isShortcutPinned by remember(tool.id) {
        mutableStateOf(com.example.utils.ShortcutHelper.isShortcutPinned(context, "tool_${tool.id}"))
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tool.iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = tool.iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = tool.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = tool.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Overlay Pin Button at Top-Right
        IconButton(
            onClick = {
                coroutineScope.launch {
                    if (isShortcutPinned) {
                        com.example.utils.ShortcutHelper.removeShortcut(context, "tool_${tool.id}", tool.title)
                        isShortcutPinned = false
                    } else {
                        com.example.utils.ShortcutHelper.pinShortcut(
                            context = context,
                            id = "tool_${tool.id}",
                            title = tool.title,
                            imageUrl = null,
                            type = "tool",
                            internalRoute = tool.route
                        )
                        isShortcutPinned = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Pin to Home Screen",
                tint = if (isShortcutPinned) tool.iconColor else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
