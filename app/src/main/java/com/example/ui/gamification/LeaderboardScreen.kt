package com.example.ui.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.LeaderboardEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val primaryBlue = Color(0xFF1A237E) // Navy Blue
    val lightBlue = Color(0xFF3F51B5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Leaderboard", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("user_search") }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search Users", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(primaryBlue, Color.White)))
        ) {
            if (isLoading && leaderboard.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                // Top 3 Podium
                TopThreePodium(leaderboard.take(3))

                // List of other users
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        itemsIndexed(leaderboard.drop(3)) { index, entry ->
                            LeaderboardItem(entry, index + 4)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopThreePodium(topThree: List<LeaderboardEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        topThree.getOrNull(1)?.let { entry ->
            PodiumItem(entry, 2, 100.dp, Color(0xFFC0C0C0))
        }

        // 1st Place
        topThree.getOrNull(0)?.let { entry ->
            PodiumItem(entry, 1, 130.dp, Color(0xFFFFD700))
        }

        // 3rd Place
        topThree.getOrNull(2)?.let { entry ->
            PodiumItem(entry, 3, 90.dp, Color(0xFFCD7F32))
        }
    }
}

@Composable
fun PodiumItem(entry: LeaderboardEntry, rank: Int, height: androidx.compose.ui.unit.Dp, medalColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = entry.photoUrl.ifBlank { "https://via.placeholder.com/150" },
                contentDescription = null,
                modifier = Modifier
                    .size(if (rank == 1) 80.dp else 65.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = medalColor,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(rank.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("${entry.points} pts", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        AsyncImage(
            model = entry.photoUrl.ifBlank { "https://via.placeholder.com/150" },
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("Level ${entry.level}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.points.toString(),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF1A237E)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
        }
    }
}
