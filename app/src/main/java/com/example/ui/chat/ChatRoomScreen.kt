package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.models.Message
import com.example.ui.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    conversationId: String,
    viewModel: ChatViewModel = viewModel(factory = ViewModelFactory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val messages by viewModel.currentMessages.collectAsState()
    val currentConversation by viewModel.currentConversation.collectAsState()
    val currentUserId = viewModel.currentUserId
    val presences by viewModel.userPresences.collectAsState()
    val typingStatuses by viewModel.typingStatuses.collectAsState()
    val messageReactions by viewModel.reactions.collectAsState()
    val messageReads by viewModel.reads.collectAsState()
    val replyToMessage by viewModel.replyToMessage.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Peer info
    val peerName = currentConversation?.name ?: currentConversation?.groupName ?: "Aura Member"
    val isPeerOnline = false // Mock check or find the peer user status in presences

    // Edit and Attachment states
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var showMessageOptionsMenu by remember { mutableStateOf(false) }

    // Setup active typing simulator when user types
    LaunchedEffect(messageText) {
        if (messageText.isNotBlank()) {
            viewModel.setTypingStatus(conversationId, "typing")
            delay(2000)
            viewModel.setTypingStatus(conversationId, "none")
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            // Can show profile info
                        }
                    ) {
                        AsyncImage(
                            model = currentConversation?.groupPhotoUrl ?: "https://api.dicebear.com/7.x/avataaars/png?seed=$conversationId",
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = peerName, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPeerOnline) "Active now" else "Offline", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = if (isPeerOnline) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        Toast.makeText(context, "Initiating secure voice call...", Toast.LENGTH_SHORT).show()
                    }) { 
                        Icon(Icons.Default.Phone, contentDescription = "Call") 
                    }
                    IconButton(onClick = { 
                        Toast.makeText(context, "Initiating secure video call...", Toast.LENGTH_SHORT).show()
                    }) { 
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call") 
                    }
                    IconButton(onClick = { 
                        // Show block user alert
                        viewModel.blockUser(conversationId)
                        Toast.makeText(context, "User blocked successfully", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }) { 
                        Icon(Icons.Default.Block, contentDescription = "Block User", tint = MaterialTheme.colorScheme.error) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Typing indicator banner
                    val typingUsers = typingStatuses.filter { it.key != currentUserId && it.value.status != "none" }
                    if (typingUsers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Someone is typing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Reply message preview banner
                    if (replyToMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = replyToMessage!!.text ?: "Attachment",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.selectMessageToReply(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Reply", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Editing message preview banner
                    if (editingMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Editing message",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = editingMessage!!.text ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { 
                                editingMessage = null 
                                messageText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Edit", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Message input field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAttachmentDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Attach Options", 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Write a message...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            maxLines = 5
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (messageText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val editMsg = editingMessage
                                    if (editMsg != null) {
                                        viewModel.editMessage(editMsg.id, messageText)
                                        editingMessage = null
                                    } else {
                                        viewModel.sendMessage(conversationId, messageText)
                                    }
                                    messageText = ""
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send, 
                                    contentDescription = "Send", 
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    // Voice note mock
                                    viewModel.setTypingStatus(conversationId, "recording")
                                    coroutineScope.launch {
                                        Toast.makeText(context, "🎤 Recording audio note... tap again to stop", Toast.LENGTH_SHORT).show()
                                        delay(3000)
                                        val mockAudioBytes = "mockAudioData".toByteArray()
                                        viewModel.uploadAndSendAttachment(conversationId, "voice_clip_${System.currentTimeMillis()}.mp3", mockAudioBytes, "voice")
                                        Toast.makeText(context, "Voice note sent successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice note", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == currentUserId
                val reactions = messageReactions[message.id] ?: emptyList()
                val reads = messageReads[message.id] ?: emptyList()
                val isSeen = reads.any { it.userId != currentUserId }

                MessageBubbleItem(
                    message = message,
                    isMe = isMe,
                    reactions = reactions,
                    isSeen = isSeen,
                    onLongClick = {
                        selectedMessageForMenu = message
                        showMessageOptionsMenu = true
                    },
                    onReactionClick = { emoji ->
                        viewModel.addReaction(message.id, emoji)
                    }
                )
            }
        }

        // 4. Attachments Selector Modal
        if (showAttachmentDialog) {
            AlertDialog(
                onDismissRequest = { showAttachmentDialog = false },
                title = { Text("Share media & documents", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAttachmentDialog = false
                                    val mockImg = "mockImageContent".toByteArray()
                                    viewModel.uploadAndSendAttachment(conversationId, "learning_infographic_${System.currentTimeMillis()}.jpg", mockImg, "image")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Photo / Graphic Illustration", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAttachmentDialog = false
                                    val mockDoc = "mockPdfContent".toByteArray()
                                    viewModel.uploadAndSendAttachment(conversationId, "notes_unit_1_${System.currentTimeMillis()}.pdf", mockDoc, "pdf")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("PDF Study material", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAttachmentDialog = false
                                    val mockZip = "mockZipContent".toByteArray()
                                    viewModel.uploadAndSendAttachment(conversationId, "assignments_${System.currentTimeMillis()}.zip", mockZip, "document")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("ZIP archive", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachmentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 5. Message Options sheet (Edit, Delete, Copy, Reply, React)
        if (showMessageOptionsMenu && selectedMessageForMenu != null) {
            val message = selectedMessageForMenu!!
            AlertDialog(
                onDismissRequest = { 
                    showMessageOptionsMenu = false
                    selectedMessageForMenu = null
                },
                title = { Text("Message Actions", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick Reaction row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("❤️", "👍", "😂", "😮", "😢", "😡").forEach { emoji ->
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.addReaction(message.id, emoji)
                                            showMessageOptionsMenu = false
                                            selectedMessageForMenu = null
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        ListItem(
                            headlineContent = { Text("Reply") },
                            leadingContent = { Icon(Icons.Default.Reply, contentDescription = null) },
                            modifier = Modifier.clickable {
                                viewModel.selectMessageToReply(message)
                                showMessageOptionsMenu = false
                                selectedMessageForMenu = null
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Copy Text") },
                            leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Chat Message", message.text ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                showMessageOptionsMenu = false
                                selectedMessageForMenu = null
                            }
                        )
                        if (message.senderId == currentUserId) {
                            ListItem(
                                headlineContent = { Text("Edit Message") },
                                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    editingMessage = message
                                    messageText = message.text ?: ""
                                    showMessageOptionsMenu = false
                                    selectedMessageForMenu = null
                                }
                            )
                            ListItem(
                                headlineContent = { Text("Delete for Everyone", color = MaterialTheme.colorScheme.error) },
                                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.clickable {
                                    viewModel.deleteMessage(message.id, forEveryone = true)
                                    showMessageOptionsMenu = false
                                    selectedMessageForMenu = null
                                }
                            )
                        }
                        ListItem(
                            headlineContent = { Text("Delete for Me") },
                            leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            modifier = Modifier.clickable {
                                viewModel.deleteMessage(message.id, forEveryone = false)
                                showMessageOptionsMenu = false
                                selectedMessageForMenu = null
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showMessageOptionsMenu = false
                        selectedMessageForMenu = null
                    }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: Message,
    isMe: Boolean,
    reactions: List<com.example.data.models.MessageReaction>,
    isSeen: Boolean,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit
) {
    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.createdAt))
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleShape = if (isMe) {
        RoundedCornerShape(20.dp, 20.dp, 3.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 3.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // Reply context preview inside the bubble
            if (message.replyToId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Replied to a previous message",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Message content card
            Card(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { /* Tap actions */ },
                        onLongClick = onLongClick
                    ),
                shape = bubbleShape,
                colors = CardDefaults.cardColors(containerColor = bubbleColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Render attachment if present
                    if (!message.attachmentUrl.isNullOrBlank()) {
                        when (message.type) {
                            "image" -> {
                                AsyncImage(
                                    model = message.attachmentUrl,
                                    contentDescription = "Shared graphic",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            "pdf" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.Red, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Study Material PDF", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Tap to view", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            "voice" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Voice Clip (0:03)", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            else -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = "File", modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Shared Attachment", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Render main text
                    Text(
                        text = message.text ?: "",
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Reactions badges below bubble
            if (reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reactions.groupBy { it.reaction }.forEach { (emoji, list) ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "$emoji ${list.size}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Time and Delivery checks
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "✓✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSeen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
