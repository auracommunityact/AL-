import re

with open("app/src/main/java/com/example/ai/ui/AuraAiChatScreen.kt", "r") as f:
    content = f.read()

old_block = """        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (engineError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Offline AI Model Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            engineError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            val modelManager = com.example.ai.ModelManager(context)
                            modelManager.startModelDownload(
                                url = "https://your-hosted-model-url.com/gemma-1.1-2b-it-cpu-int4.bin",
                                fileName = com.example.ai.model.AiConfig.MODEL_FILE_NAME
                            )
                        }) {
                            Text("Download Model (~1.5 GB)")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = true
                ) {
                    if (isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                    
                    items(messages.reversed()) { message ->
                        ChatMessageItem(message)
                    }
                }
            }
        }"""

new_block = """        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true
            ) {
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
                
                items(messages.reversed()) { message ->
                    ChatMessageItem(message)
                }
            }
        }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/example/ai/ui/AuraAiChatScreen.kt", "w") as f:
    f.write(content)
