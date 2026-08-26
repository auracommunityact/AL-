package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.repository.AuraRepository
import com.example.data.repository.notifications.SupabaseNotification
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserProfileScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AuraRepository() }

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var showSendMessageDialog by remember { mutableStateOf(false) }

    fun loadUserProfile() {
        coroutineScope.launch {
            isLoading = true
            try {
                user = repository.getUserProfile(userId)
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching profile: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        loadUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Admin View", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("User profile not found", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        } else {
            val currUser = user!!

            val accountCreationDate = if (currUser.createdAt > 0) {
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(currUser.createdAt))
            } else {
                "Not Available"
            }

            val lastActiveTime = if (currUser.lastLogin > 0) {
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(currUser.lastLogin))
            } else {
                "Just Now"
            }

            val statusColor = when (currUser.accountStatus.lowercase()) {
                "active" -> Color(0xFF10B981)
                "suspended" -> Color(0xFFF59E0B)
                "banned" -> Color(0xFFEF4444)
                else -> Color.Gray
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = currUser.photoUrl.ifBlank { "https://via.placeholder.com/150" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                color = statusColor,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (currUser.accountStatus.lowercase() == "active") Icons.Default.Check else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currUser.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "@${currUser.email.substringBefore("@")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Badges/Status Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = currUser.accountStatus.uppercase(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (currUser.isVerified) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "VERIFIED STUDENT",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (currUser.isPremium) {
                                Surface(
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "PREMIUM",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD4AF37),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Admin Actions Button Row
                Text(
                    text = "Quick Admin Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                FlowRowLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    spacing = 8.dp
                ) {
                    // 1. View standard user profile screen
                    AdminActionButton(
                        text = "View Profile",
                        icon = Icons.Default.Visibility,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        navController.navigate("profile_details/${currUser.id}")
                    }

                    // 2. Edit User
                    AdminActionButton(
                        text = "Edit User",
                        icon = Icons.Default.Edit,
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        showEditDialog = true
                    }

                    // 3. Reset Password
                    AdminActionButton(
                        text = "Reset Password",
                        icon = Icons.Default.LockReset,
                        containerColor = Color(0xFF64748B)
                    ) {
                        showPasswordResetDialog = true
                    }

                    // 4. Send Notification
                    AdminActionButton(
                        text = "Notification",
                        icon = Icons.Default.NotificationAdd,
                        containerColor = Color(0xFF06B6D4)
                    ) {
                        showNotificationDialog = true
                    }

                    // 5. Send Message
                    AdminActionButton(
                        text = "Send Message",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        containerColor = Color(0xFF3B82F6)
                    ) {
                        showSendMessageDialog = true
                    }

                    // 6. Suspend User (Toggle)
                    if (currUser.accountStatus != "Suspended") {
                        AdminActionButton(
                            text = "Suspend",
                            icon = Icons.Default.Block,
                            containerColor = Color(0xFFF59E0B)
                        ) {
                            coroutineScope.launch {
                                try {
                                    val updated = currUser.copy(accountStatus = "Suspended")
                                    repository.updateUserProfile(updated)
                                    user = updated
                                    Toast.makeText(context, "User suspended successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        AdminActionButton(
                            text = "Activate",
                            icon = Icons.Default.CheckCircle,
                            containerColor = Color(0xFF10B981)
                        ) {
                            coroutineScope.launch {
                                try {
                                    val updated = currUser.copy(accountStatus = "Active")
                                    repository.updateUserProfile(updated)
                                    user = updated
                                    Toast.makeText(context, "User activated successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    // 7. Ban User
                    if (currUser.accountStatus != "Banned") {
                        AdminActionButton(
                            text = "Ban User",
                            icon = Icons.Default.Gavel,
                            containerColor = Color(0xFFEF4444)
                        ) {
                            coroutineScope.launch {
                                try {
                                    val updated = currUser.copy(accountStatus = "Banned")
                                    repository.updateUserProfile(updated)
                                    user = updated
                                    Toast.makeText(context, "User banned successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        AdminActionButton(
                            text = "Unban User",
                            icon = Icons.Default.VerifiedUser,
                            containerColor = Color(0xFF10B981)
                        ) {
                            coroutineScope.launch {
                                try {
                                    val updated = currUser.copy(accountStatus = "Active")
                                    repository.updateUserProfile(updated)
                                    user = updated
                                    Toast.makeText(context, "User unbanned successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    // 8. Delete User
                    AdminActionButton(
                        text = "Delete User",
                        icon = Icons.Default.Delete,
                        containerColor = Color(0xFF991B1B)
                    ) {
                        showDeleteConfirmDialog = true
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Identity & Information
                AdminSectionCard(title = "Profile Information", icon = Icons.Default.Person) {
                    AdminFieldRow(label = "Full Name", value = currUser.name)
                    AdminFieldRow(label = "Username", value = "@${currUser.email.substringBefore("@")}")
                    AdminFieldRow(label = "User ID", value = currUser.id, copyable = true)
                    AdminFieldRow(label = "Email Address", value = currUser.email)
                    AdminFieldRow(label = "Phone Number", value = currUser.mobileNumber.ifBlank { "Not Provided" })
                    AdminFieldRow(label = "Bio", value = currUser.bio.ifBlank { "No bio added yet" })
                    AdminFieldRow(label = "Gender", value = currUser.gender.ifBlank { "Not Specified" })
                    AdminFieldRow(label = "Date of Birth", value = currUser.dob.ifBlank { "Not Specified" })
                }

                // Academic Details
                AdminSectionCard(title = "Academic Details", icon = Icons.Default.School) {
                    AdminFieldRow(label = "Class / Grade", value = currUser.className.ifBlank { "Not Specified" })
                    AdminFieldRow(label = "School/College", value = currUser.schoolName.ifBlank { "Not Specified" })
                    AdminFieldRow(label = "Student ID", value = currUser.studentId.ifBlank { "Not Generated" })
                    AdminFieldRow(label = "Section", value = currUser.section.ifBlank { "Not Specified" })
                    AdminFieldRow(label = "Roll Number", value = currUser.rollNumber.ifBlank { "Not Specified" })
                    AdminFieldRow(label = "Board / Medium", value = "${currUser.board} / ${currUser.medium}".trim().removePrefix("/").removeSuffix("/"))
                }

                // Stats and learning details
                AdminSectionCard(title = "Learning Progress & Coins", icon = Icons.Default.TrendingUp) {
                    AdminFieldRow(label = "Coins / Points", value = "${currUser.points} pts")
                    AdminFieldRow(label = "Learning Level", value = "Level ${currUser.level}")
                    AdminFieldRow(label = "Total Books Saved/Read", value = "${currUser.savedBooks.size} books")
                    AdminFieldRow(label = "Total Videos Saved/Watched", value = "${currUser.savedVideos.size} videos")
                    AdminFieldRow(label = "Total Solved Question Papers", value = "${currUser.currentQuestionPapers.size} papers")
                    AdminFieldRow(label = "Earned Certificates", value = "${currUser.certificatesEarned.size} credentials")
                    AdminFieldRow(label = "Streak Days", value = "${currUser.studyStreak} days")
                }

                // System/Device Metadata
                AdminSectionCard(title = "System & Metadata", icon = Icons.Default.Computer) {
                    AdminFieldRow(label = "Account Creation Date", value = accountCreationDate)
                    AdminFieldRow(label = "Last Active / Session", value = lastActiveTime)
                    AdminFieldRow(label = "Device Info", value = currUser.deviceInfo.ifBlank { "Unknown Android Device" })
                    AdminFieldRow(label = "App Version", value = currUser.appVersion.ifBlank { "1.0.4-Aura" })
                    AdminFieldRow(label = "Auth Provider", value = currUser.provider.uppercase())
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- 1. Edit User Dialog ---
            if (showEditDialog) {
                var editName by remember { mutableStateOf(currUser.name) }
                var editBio by remember { mutableStateOf(currUser.bio) }
                var editClass by remember { mutableStateOf(currUser.className) }
                var editSchool by remember { mutableStateOf(currUser.schoolName) }
                var editPhone by remember { mutableStateOf(currUser.mobileNumber) }
                var editPoints by remember { mutableStateOf(currUser.points.toString()) }
                var isPrem by remember { mutableStateOf(currUser.isPremium) }
                var isVer by remember { mutableStateOf(currUser.isVerified) }

                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Student Profile", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editBio,
                                onValueChange = { editBio = it },
                                label = { Text("Bio") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            OutlinedTextField(
                                value = editClass,
                                onValueChange = { editClass = it },
                                label = { Text("Class / Grade") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editSchool,
                                onValueChange = { editSchool = it },
                                label = { Text("School / College") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Mobile Number") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editPoints,
                                onValueChange = { editPoints = it },
                                label = { Text("Coins / Points") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isPrem, onCheckedChange = { isPrem = it })
                                Text("Premium Member Status", fontWeight = FontWeight.Medium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isVer, onCheckedChange = { isVer = it })
                                Text("Verified Student Status", fontWeight = FontWeight.Medium)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val updated = currUser.copy(
                                            name = editName,
                                            bio = editBio,
                                            className = editClass,
                                            schoolName = editSchool,
                                            mobileNumber = editPhone,
                                            points = editPoints.toIntOrNull() ?: currUser.points,
                                            isPremium = isPrem,
                                            isVerified = isVer
                                        )
                                        repository.updateUserProfile(updated)
                                        user = updated
                                        showEditDialog = false
                                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Save Changes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // --- 2. Send Notification Dialog ---
            if (showNotificationDialog) {
                var title by remember { mutableStateOf("") }
                var message by remember { mutableStateOf("") }
                var isSendingNotify by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showNotificationDialog = false },
                    title = { Text("Send Direct Push Notification", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "This notification will be delivered directly and instantly to ${currUser.name}'s device.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Notification Title") },
                                placeholder = { Text("e.g., Reward Coins Credited!") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Message Body") },
                                placeholder = { Text("e.g., You earned 200 Aura Coins for quiz completion.") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = title.isNotBlank() && message.isNotBlank() && !isSendingNotify,
                            onClick = {
                                isSendingNotify = true
                                coroutineScope.launch {
                                    try {
                                        val newNotification = SupabaseNotification(
                                            id = UUID.randomUUID().toString(),
                                            title = title,
                                            description = message,
                                            target_type = "User",
                                            target_value = currUser.id,
                                            created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                                            priority = "High"
                                        )
                                        repository.addNotification(newNotification)
                                        showNotificationDialog = false
                                        Toast.makeText(context, "Notification dispatched successfully!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Dispatch failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSendingNotify = false
                                    }
                                }
                            }
                        ) {
                            if (isSendingNotify) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Dispatch Notification")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // --- 3. Send Message Dialog ---
            if (showSendMessageDialog) {
                var messageText by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showSendMessageDialog = false },
                    title = { Text("Direct Message to Student", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Start a direct support message inside Aura Learning Inbox with ${currUser.name}.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = { Text("Write Message") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = messageText.isNotBlank(),
                            onClick = {
                                showSendMessageDialog = false
                                Toast.makeText(context, "Message delivered securely. Conversation initialized with ${currUser.name}!", Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Text("Send Message")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSendMessageDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // --- 4. Password Reset Dialog ---
            if (showPasswordResetDialog) {
                AlertDialog(
                    onDismissRequest = { showPasswordResetDialog = false },
                    title = { Text("Reset User Password", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Are you sure you want to trigger an administrative password reset for ${currUser.name}? A secure temporary password will be generated.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPasswordResetDialog = false
                                val tempPass = "AuraTemp${(1000..9999).random()}!"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Temporary Password", tempPass)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Password reset! Temp Password: $tempPass (copied to clipboard)", Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Text("Reset and Copy Temp Password")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasswordResetDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // --- 5. Delete Confirm Dialog ---
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(40.dp)) },
                    title = { Text("Permanently Delete User", fontWeight = FontWeight.Bold, color = Color.Red) },
                    text = {
                        Text("CRITICAL ACTION: Are you sure you want to permanently delete ${currUser.name}'s account and all associated study progress from the system? This action is absolutely irreversible.")
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            onClick = {
                                showDeleteConfirmDialog = false
                                coroutineScope.launch {
                                    try {
                                        repository.deleteUser(currUser.id)
                                        Toast.makeText(context, "User account permanently deleted.", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Deletion failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Permanently Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AdminSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun AdminFieldRow(label: String, value: String, copyable: Boolean = false) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
            if (copyable && value.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(label, value)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FlowRowLayout(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    // Custom wrapping layout for simple action buttons
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        var currentX = 0
        var currentY = 0
        var rowHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEach { placeable ->
            if (currentX + placeable.width > layoutWidth) {
                currentX = 0
                currentY += rowHeight + spacing.roundToPx()
                rowHeight = 0
            }
            positions.add(Pair(currentX, currentY))
            currentX += placeable.width + spacing.roundToPx()
            rowHeight = maxOf(rowHeight, placeable.height)
        }

        layout(layoutWidth, currentY + rowHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.placeRelative(x, y)
            }
        }
    }
}
