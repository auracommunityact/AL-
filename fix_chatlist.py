import re

with open("app/src/main/java/com/example/ui/chat/ChatListScreen.kt", "r") as f:
    content = f.read()

# I will replace the entire box that contains the conversations.
# We know where it starts: `// 2. Main Conversations Container`
# We know where it ends: `// 3. Conversation Context Options Bottom Sheet`

start_marker = "// 2. Main Conversations Container"
end_marker = "// 3. Conversation Context Options Bottom Sheet"

new_container = """// 2. Main Conversations Container
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
                                        text = "Tap the '+' button below to start a secure private chat.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val otherConversations = conversations
                        items(otherConversations) { conversation ->
                            val isPeerOnline = false // Mocked
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
        }

        // 3. Conversation Context Options Bottom Sheet"""

# Use regex to extract everything between those markers and replace
content = re.sub(
    re.escape(start_marker) + r".*?" + re.escape(end_marker),
    new_container,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/ui/chat/ChatListScreen.kt", "w") as f:
    f.write(content)
