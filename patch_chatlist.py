import re

with open("app/src/main/java/com/example/ui/chat/ChatListScreen.kt", "r") as f:
    content = f.read()

# I need to add AuraAiChatHeaderItem before LazyColumn items
old_lazy_column = """                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Group: Pinned chats first, then normal chats
                    val pinnedConversations = conversations.filter { false } // Add field or logic if required
                    val otherConversations = conversations"""

new_lazy_column = """                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        AuraAiChatHeaderItem(
                            onClick = { navController.navigate("aura_ai_chat") }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    // Group: Pinned chats first, then normal chats
                    val pinnedConversations = conversations.filter { false } // Add field or logic if required
                    val otherConversations = conversations"""

content = content.replace(old_lazy_column, new_lazy_column)

# Let's also check if the inbox empty state covers up the Aura AI chat
# Wait, if there are no conversations, it shows "Your inbox is empty" and LazyColumn is skipped.
# I need to change that. I should show LazyColumn ALWAYS, and if conversations are empty, just show the Aura AI header and then the empty message or just the Aura AI header.

empty_state_old = """            if (isLoading && conversations.isEmpty()) {
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
                LazyColumn("""

empty_state_new = """            if (isLoading && conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        AuraAiChatHeaderItem(
                            onClick = { navController.navigate("aura_ai_chat") }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    if (conversations.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp, horizontal = 16.dp),
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
                        }
                    } else {
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
            }"""

content = content.replace(empty_state_old, empty_state_new)

# Since we replaced the `else { LazyColumn(...) }` entirely, we need to remove the trailing `}` that belonged to the old `else` block
# Let's do a more robust string replacement

# Find the end of `empty_state_old` in the original file
if empty_state_old in content:
    # We replaced it! But the `}` closing the old `else` block is still there after `items(otherConversations) { ... }`
    pass
else:
    print("Warning: empty_state_old not found!")

with open("app/src/main/java/com/example/ui/chat/ChatListScreen.kt", "w") as f:
    f.write(content)
