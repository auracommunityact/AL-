package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Banner
import com.example.data.models.HomeSectionConfig
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeCustomizationScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var banners by remember { mutableStateOf<List<Banner>>(emptyList()) }
    var sections by remember { mutableStateOf<List<HomeSectionConfig>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            try {
                banners = repository.getBanners()
                sections = repository.getHomeSectionConfigs()
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading config: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("admin_add_banner") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Banner") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Banners") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sections") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> BannerList(banners, repository, onRefresh = { loadData() }, navController)
                    1 -> {
                        val dynamicSections = sections.filter {
                            it.type != "home" && it.type != "books" && it.type != "videos"
                        }
                        SectionList(dynamicSections, repository, onRefresh = { loadData() })
                    }
                }
            }
        }
    }
}

@Composable
fun BannerList(
    banners: List<Banner>,
    repository: AuraRepository,
    onRefresh: () -> Unit,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (banners.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No banners found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(banners) { banner ->
                BannerAdminCard(
                    banner = banner,
                    onDelete = {
                        coroutineScope.launch {
                            try {
                                repository.deleteBanner(banner.id)
                                Toast.makeText(context, "Banner deleted", Toast.LENGTH_SHORT).show()
                                onRefresh()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onToggle = { enabled ->
                        coroutineScope.launch {
                            try {
                                repository.updateBanner(banner.copy(isEnabled = enabled))
                                onRefresh()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEdit = {
                        // Navigate to edit banner screen
                        navController.navigate("admin_edit_banner/${banner.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun BannerAdminCard(
    banner: Banner,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = if (banner.isEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (banner.isEnabled) "Active" else "Disabled",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(banner.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = banner.isEnabled,
                        onCheckedChange = onToggle,
                        modifier = Modifier.scale(0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionList(
    sections: List<HomeSectionConfig>,
    repository: AuraRepository,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sections) { section ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (section.isVisible) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(section.title, fontWeight = FontWeight.Bold)
                        Text("Type: ${section.type}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Switch(
                        checked = section.isVisible,
                        onCheckedChange = { visible ->
                            coroutineScope.launch {
                                try {
                                    repository.updateHomeSectionConfig(section.copy(isVisible = visible))
                                    onRefresh()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
