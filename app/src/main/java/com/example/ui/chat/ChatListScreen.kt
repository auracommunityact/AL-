package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Conversation
import com.example.ui.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatViewModel = viewModel(factory = ViewModelFactory)
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val presences by viewModel.userPresences.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Bottom Sheet State for contextual conversation actions
    var selectedConversationForActions by remember { mutableStateOf<Conversation?>(null) }
    var showActionsBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Discussions", 
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineLarge
                    ) 
                },
                actions = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Users")
                    }
                    IconButton(onClick = { viewModel.loadConversations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Conversations")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSearchDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        // 1. Search Users Dialog
        if (showSearchDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showSearchDialog = false 
                    searchQuery = ""
                    viewModel.searchUsers("")
                },
                title = { 
                    Text(
                        "Search Aura Users", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.searchUsers(it)
                            },
                            placeholder = { Text("Search by name, email, or ID...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        viewModel.searchUsers("")
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isLoading && searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                            Text(
                                "No active students or teachers found.", 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                                items(searchResults) { user ->
                                    val isOnline = presences[user.id]?.isOnline == true
                                    ListItem(
                                        headlineContent = { Text(user.name, fontWeight = FontWeight.SemiBold) },
                                        supportingContent = { Text(user.email, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingContent = {
                                            Box {
                                                AsyncImage(
                                                    model = user.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.name}&background=random" },
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                                if (isOnline) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Green)
                                                            .align(Alignment.BottomEnd)
                                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.startConversation(user.id, user.name) { convoId ->
                                                    showSearchDialog = false
                                                    navController.navigate("chat_room/$convoId")
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showSearchDialog = false 
                        searchQuery = ""
                        viewModel.searchUsers("")
                    }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 2. Main Conversations Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading && conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "No chats",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Your inbox is empty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the '+' button below to start a secure private chat with any learner or instructor on Aura Learning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Group: Pinned chats first, then normal chats
                    val pinnedConversations = conversations.filter { false } // Add field or logic if required
                    val otherConversations = conversations
                    
                    items(otherConversations) { conversation ->
                        val peerId = conversation.name ?: "Unknown"
                        val isPeerOnline = false // We can check if peer presence matches
                        
                        ConversationItem(
                            conversation = conversation,
                            isOnline = isPeerOnline,
                            onClick = {
                                navController.navigate("chat_room/${conversation.id}")
                            },
                            onLongClick = {
                                selectedConversationForActions = conversation
                                showActionsBottomSheet = true
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // 3. Conversation Context Options Bottom Sheet
        if (showActionsBottomSheet && selectedConversationForActions != null) {
            val currentConvo = selectedConversationForActions!!
            ModalBottomSheet(
                onDismissRequest = { 
                    showActionsBottomSheet = false
                    selectedConversationForActions = null
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = currentConvo.name ?: currentConvo.groupName ?: "Chat Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                    )
                    
                    ListItem(
                        headlineContent = { Text("Open Chat") },
                        leadingContent = { Icon(Icons.Default.Chat, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showActionsBottomSheet = false
                            navController.navigate("chat_room/${currentConvo.id}")
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Pin Conversation") },
                        leadingContent = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showActionsBottomSheet = false
                            // Pinned logic
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Mute Notifications") },
                        leadingContent = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showActionsBottomSheet = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Block Participant", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showActionsBottomSheet = false
                            // In private chats, peer name is used or store actual otherUserId
                            // Safely block peer if possible
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    isOnline: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val title = conversation.name ?: conversation.groupName ?: "Study Chat"
    val avatarUrl = conversation.groupPhotoUrl ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${title}"
    val timeFormatted = try {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(conversation.lastMessageTime))
    } catch (e: Exception) {
        "Just now"
    }
    
    val isUnread = false // Mocked but visual unread logic can be mapped in DB

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessageText ?: "No messages in this chat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
