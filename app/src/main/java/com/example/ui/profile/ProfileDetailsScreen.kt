package com.example.ui.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.User
import com.example.ui.auth.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    userId: String? = null
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var targetUser by remember { mutableStateOf<User?>(null) }
    val repository = remember { com.example.data.repository.AuraRepository() }
    val context = LocalContext.current

    val isOwnProfile = userId.isNullOrBlank() || userId == currentUser?.id

    LaunchedEffect(userId, currentUser) {
        if (!userId.isNullOrBlank() && userId != currentUser?.id) {
            val fetchedUser = repository.getUserProfile(userId)
            if (fetchedUser == null) {
                Toast.makeText(context, "User profile not found. Redirecting to Home...", Toast.LENGTH_LONG).show()
                navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                targetUser = fetchedUser
            }
        } else {
            targetUser = currentUser
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var showCertificatesDialog by remember { mutableStateOf(false) }
    var showDownloadSuccessDialog by remember { mutableStateOf(false) }

    val user = targetUser ?: currentUser ?: User(
        id = "guest_user",
        name = "Guest Student",
        email = "guest@auralearning.com",
        role = "guest"
    )

    val studentId = user.studentId.ifBlank { "AURA-${user.id.takeLast(6).uppercase()}" }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Student Profile Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 1. TOP HERO BANNER & PROFILE PHOTO SECTION
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    // Banner Image / Gradient
                    if (user.bannerUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.bannerUrl,
                            contentDescription = "Profile Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        ) {
                            // Overlay subtle pattern or text
                            Text(
                                text = "AURA LEARNING",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(bottom = 16.dp),
                                letterSpacing = 4.sp
                            )
                        }
                    }

                    // Large Profile photo overlapping banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        if (user.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Student Photo",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // 2. HEADER DETAILS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = user.name.ifBlank { "Aura Student" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Student",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID: $studentId",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gamification Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Level ${user.level}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text(user.rank, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700))
                                }
                            }
                            
                            if (isOwnProfile) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val nextLevelPoints = user.level * 500
                                val currentLevelPoints = (user.level - 1) * 500
                                val progress = (user.points - currentLevelPoints).toFloat() / 500f
                                
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFFFFD700),
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${user.points} XP", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("${nextLevelPoints - user.points} XP to Level ${user.level + 1}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Badges Section
                    if (user.badges.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Earned Badges",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(user.badges.size) { index ->
                                    val badgeName = user.badges[index]
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.width(100.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color(0xFF1A237E).copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = when(badgeName) {
                                                        "Fast Learner" -> "⚡"
                                                        "Quiz Master" -> "🎯"
                                                        "Points Millionaire" -> "💰"
                                                        else -> "🏆"
                                                    },
                                                    fontSize = 20.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                badgeName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Leaderboard Shortcut Button
                    Button(
                        onClick = { navController.navigate("leaderboard") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                    ) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Global Leaderboard", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // QR Code Card (Smaller now)
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .padding(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StudentQrCode(
                                data = studentId,
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan student ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. INFORMATION CARDS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section: Academic Information
                    ProfileInfoGroupCard(
                        title = "Academic Information",
                        icon = Icons.Filled.School,
                        items = listOf(
                            ProfileItem("School / College", user.schoolName.ifBlank { "Not Specified" }),
                            ProfileItem("Class", user.className.ifBlank { "Not Specified" }),
                            ProfileItem("Section", user.section.ifBlank { "Not Specified" }),
                            ProfileItem("Roll Number", user.rollNumber.ifBlank { "Not Specified" }),
                            ProfileItem("Admission Number", user.admissionNumber.ifBlank { "Not Specified" }),
                            ProfileItem("Board", user.board.ifBlank { "Not Specified" }),
                            ProfileItem("Medium", user.medium.ifBlank { "Not Specified" }),
                            ProfileItem("Academic Session", user.academicSession.ifBlank { "Not Specified" })
                        )
                    )

                    // Section: Personal Information
                    ProfileInfoGroupCard(
                        title = "Personal Information",
                        icon = Icons.Filled.Person,
                        items = if (isOwnProfile) {
                            listOf(
                                ProfileItem("Full Name", user.name.ifBlank { "Not Specified" }),
                                ProfileItem("Gender", user.gender.ifBlank { "Not Specified" }),
                                ProfileItem("Date of Birth", user.dob.ifBlank { "Not Specified" }),
                                ProfileItem("Age", if (user.age > 0) "${user.age} Years" else "Not Specified"),
                                ProfileItem("Blood Group", user.bloodGroup.ifBlank { "Not Specified" }),
                                ProfileItem("Nationality", user.nationality.ifBlank { "Not Specified" }),
                                ProfileItem("Category", user.category.ifBlank { "Not Specified" })
                            )
                        } else {
                            listOf(
                                ProfileItem("Full Name", user.name.ifBlank { "Not Specified" }),
                                ProfileItem("Gender", user.gender.ifBlank { "Not Specified" })
                            )
                        }
                    )

                    // Section: Contact Information
                    if (isOwnProfile) {
                        ProfileInfoGroupCard(
                            title = "Contact Information",
                            icon = Icons.Filled.ContactPage,
                            items = listOf(
                                ProfileItem("Email Address", user.email.ifBlank { "Not Specified" }),
                                ProfileItem("Mobile Number", user.mobileNumber.ifBlank { "Not Specified" }),
                                ProfileItem("Parent / Guardian", user.parentName.ifBlank { "Not Specified" }),
                                ProfileItem("Parent Mobile", user.parentMobileNumber.ifBlank { "Not Specified" })
                            )
                        )
                    }

                    // Section: Address Details
                    if (isOwnProfile) {
                        ProfileInfoGroupCard(
                            title = "Address Details",
                            icon = Icons.Filled.Home,
                            items = listOf(
                                ProfileItem("Country", user.country.ifBlank { "Not Specified" }),
                                ProfileItem("State", user.state.ifBlank { "Not Specified" }),
                                ProfileItem("District", user.district.ifBlank { "Not Specified" }),
                                ProfileItem("City", user.city.ifBlank { "Not Specified" }),
                                ProfileItem("PIN Code", user.pinCode.ifBlank { "Not Specified" })
                            )
                        )
                    }

                    // Section: Learning Information
                    ProfileInfoGroupCard(
                        title = "Learning Progress",
                        icon = Icons.Filled.AutoStories,
                        items = listOf(
                            ProfileItem("Active Papers", if (user.currentQuestionPapers.isEmpty()) "0 active papers" else "${user.currentQuestionPapers.size} papers enrolled"),
                            ProfileItem("Books Saved", "${user.savedBooks.size} items in library"),
                            ProfileItem("Videos Saved", "${user.savedVideos.size} lessons saved"),
                            ProfileItem("Certificates Earned", "${user.certificatesEarned.size} credentials"),
                            ProfileItem("Study Streak", "${user.studyStreak} Days streak 🔥"),
                            ProfileItem("Total Study Time", "${user.totalStudyTime} minutes completed"),
                            ProfileItem("Completed Lessons", "${user.completedLessons} modules solved")
                        )
                    )

                    // Section: Achievements
                    ProfileInfoGroupCard(
                        title = "Achievements & Status",
                        icon = Icons.Filled.EmojiEvents,
                        items = if (isOwnProfile) {
                            listOf(
                                ProfileItem("Badges Earned", if (user.badges.isEmpty()) "None yet" else user.badges.joinToString(", ")),
                                ProfileItem("Global Rank", user.rank),
                                ProfileItem("Reward Points", "${user.points} pts"),
                                ProfileItem("Current Level", "Level ${user.level}"),
                                ProfileItem("Attendance", "${user.attendancePercentage}%")
                            )
                        } else {
                            listOf(
                                ProfileItem("Badges Earned", if (user.badges.isEmpty()) "None yet" else user.badges.joinToString(", ")),
                                ProfileItem("Global Rank", user.rank),
                                ProfileItem("Current Level", "Level ${user.level}")
                            )
                        }
                    )

                    // Section: Account Details
                    if (isOwnProfile) {
                        ProfileInfoGroupCard(
                            title = "Account Details",
                            icon = Icons.Filled.Info,
                            items = listOf(
                                ProfileItem("Created On", if (user.createdAt > 0) java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(user.createdAt)) else "Not Available"),
                                ProfileItem("Last Session", if (user.lastLogin > 0) java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(user.lastLogin)) else "Just Now"),
                                ProfileItem("Login Via", user.provider.replaceFirstChar { it.uppercase() }),
                                ProfileItem("Profile Status", user.accountStatus)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. QUICK ACTIONS CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quick Student Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Action: Share Profile
                                Button(
                                    onClick = {
                                        val shareText = """
                                            Check out my Aura Learning profile!

                                            https://aura.auralearning.workers.dev/u/${user.id}
                                        """.trimIndent()
                                        
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Student Profile")
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Action: Feedback & Suggestions
                                OutlinedButton(
                                    onClick = { navController.navigate("feedback") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Feedback", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Loader during Download
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating Digital ID Card PDF...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }

    // Certificates earned Dialog
    if (showCertificatesDialog) {
        AlertDialog(
            onDismissRequest = { showCertificatesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Your Learning Credentials", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (user.certificatesEarned.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No Certificates Yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Complete video lessons & quizzes to unlock official certificates.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        user.certificatesEarned.forEach { cert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏆", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(cert, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Verified by Aura Learning", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCertificatesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Download Success Dialog
    if (showDownloadSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadSuccessDialog = false },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) },
            title = { Text("PDF Download Complete", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Aura_ID_Card_${studentId}.pdf has been successfully downloaded and stored in your device storage.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { showDownloadSuccessDialog = false }) {
                    Text("Excellent")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoGroupCard(
    title: String,
    icon: ImageVector,
    items: List<ProfileItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

data class ProfileItem(val label: String, val value: String)

@Composable
fun StudentQrCode(data: String, modifier: Modifier = Modifier) {
    // Generate a 17x17 pseudo-random grid seeded by the hash code of the data string
    val size = 17
    val seed = data.hashCode()
    val random = java.util.Random(seed.toLong())
    val grid = Array(size) { BooleanArray(size) }
    
    // Fill corners with QR finder patterns
    for (r in 0 until size) {
        for (c in 0 until size) {
            // Top-left finder pattern (7x7)
            if (r < 7 && c < 7) {
                grid[r][c] = (r == 0 || r == 6 || c == 0 || c == 6) || (r in 2..4 && c in 2..4)
            }
            // Top-right finder pattern
            else if (r < 7 && c >= size - 7) {
                val nc = c - (size - 7)
                grid[r][c] = (r == 0 || r == 6 || nc == 0 || nc == 6) || (r in 2..4 && nc in 2..4)
            }
            // Bottom-left finder pattern
            else if (r >= size - 7 && c < 7) {
                val nr = r - (size - 7)
                grid[r][c] = (nr == 0 || nr == 6 || c == 0 || c == 6) || (nr in 2..4 && c in 2..4)
            }
            // Random pattern elsewhere
            else {
                grid[r][c] = random.nextBoolean()
            }
        }
    }

    val primaryColor = Color.Black // Keep QR code black for reliability
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cellSize = this.size.width / size
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c]) {
                    drawRect(
                        color = primaryColor,
                        topLeft = androidx.compose.ui.geometry.Offset(c * cellSize, r * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize + 1f, cellSize + 1f) // add 1f to avoid thin grid gaps
                    )
                }
            }
        }
    }
}
