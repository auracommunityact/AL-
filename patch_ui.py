with open("app/src/main/java/com/example/ai/ui/AuraAiChatScreen.kt", "r") as f:
    content = f.read()

# Add imports for image picker and coil
imports = """
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Close
"""
content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp" + imports)

# Extract selectedImageUri in the composable
old_vars = """    val engineError by viewModel.engineError.collectAsState()

    var inputText by remember { mutableStateOf("") }"""

new_vars = """    val engineError by viewModel.engineError.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    var inputText by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.selectImage(it) }
        }
    )
"""
content = content.replace(old_vars, new_vars)

# Fix the bottomBar area to include the image preview and correct send logic
old_bottom = """                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Image Attachment Stub */ }) {
                        Icon(Icons.Default.Image, contentDescription = "Attach Image")
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Aura...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        maxLines = 4
                    )
                    if (inputText.isBlank()) {
                        IconButton(onClick = { /* Mic Stub */ }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }"""

new_bottom = """                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp).size(100.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.clearSelectedImage() },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove selected image", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            imagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Attach image")
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask Aura...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            maxLines = 4
                        )
                        if (inputText.isBlank() && selectedImageUri == null) {
                            IconButton(onClick = { /* Mic Stub */ }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }"""

content = content.replace(old_bottom, new_bottom)

with open("app/src/main/java/com/example/ai/ui/AuraAiChatScreen.kt", "w") as f:
    f.write(content)
