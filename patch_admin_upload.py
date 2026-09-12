with open("app/src/main/java/com/example/ui/admin/AdminContentUploadScreen.kt", "r") as f:
    content = f.read()

import re

state_vars = """    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    
    var downloadEnabled by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoName by remember { mutableStateOf("") }"""

content = content.replace("    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }\n    var selectedPdfName by remember { mutableStateOf(\"\") }", state_vars)

pdf_launcher = """    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex != -1) {
                    selectedPdfName = c.getString(nameIndex)
                }
            }
        }
    }"""

video_launcher = """    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex != -1) {
                    selectedPdfName = c.getString(nameIndex)
                }
            }
        }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex != -1) {
                    selectedVideoName = c.getString(nameIndex)
                }
            }
        }
    }"""

content = content.replace(pdf_launcher, video_launcher)

ui_section = """            if (isVideo) {
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Teacher Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = downloadEnabled, onCheckedChange = { downloadEnabled = it })
                    Text("Enable Download (Authorized Video)")
                }
                
                if (downloadEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { videoPickerLauncher.launch("video/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = if (selectedVideoUri != null) selectedVideoName else "Tap to Select Authorized Video (MP4)")
                        }
                    }
                }
            }"""

content = re.sub(r'            if \(isVideo\) \{\s*OutlinedTextField\(\s*value = teacher,\s*onValueChange = \{ teacher = it \},\s*label = \{ Text\("Teacher Name"\) \},\s*modifier = Modifier\.fillMaxWidth\(\)\s*\)\s*\}', ui_section, content)

upload_logic = """                                if (isVideo) {
                                    val videoId = extractYoutubeVideoId(finalContentUrl)
                                    val finalVideoUrl = if (finalContentUrl.contains("youtube.com") || finalContentUrl.contains("youtu.be")) {
                                        finalContentUrl
                                    } else {
                                        "https://www.youtube.com/watch?v=$finalContentUrl"
                                    }
                                    
                                    var authorizedDownloadUrl = ""
                                    if (downloadEnabled && selectedVideoUri != null) {
                                        authorizedDownloadUrl = repository.uploadFileToStorage(
                                            uri = selectedVideoUri!!,
                                            path = "aura-learning-videos/${System.currentTimeMillis()}_${selectedVideoName}",
                                            mimeType = "video/mp4"
                                        )
                                    }

                                    val video = Video(
                                        title = title,
                                        description = description,
                                        className = className,
                                        subject = subject,
                                        thumbnail = finalImageUrl.ifEmpty { "https://images.unsplash.com/photo-1596496050827-8299e0220de1?auto=format&fit=crop&w=300&q=80" },
                                        videoUrl = finalVideoUrl,
                                        youtubeVideoId = videoId,
                                        chapter = title,
                                        partNumber = 1,
                                        teacher = teacher.ifEmpty { "Aura Teacher" },
                                        duration = "15:00",
                                        downloadEnabled = downloadEnabled,
                                        authorizedDownloadUrl = authorizedDownloadUrl,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.addVideo(video)
                                    Toast.makeText(context, "Video uploaded successfully", Toast.LENGTH_SHORT).show()
                                }"""

# Replacing the old video logic
old_upload_logic_pattern = r'                                if \(isVideo\) \{\s*val videoId = extractYoutubeVideoId\(finalContentUrl\)\s*val finalVideoUrl = if \(finalContentUrl\.contains\("youtube\.com"\) \|\| finalContentUrl\.contains\("youtu\.be"\)\) \{\s*finalContentUrl\s*\} else \{\s*"https://www\.youtube\.com/watch\?v=\$finalContentUrl"\s*\}\s*val video = Video\([\s\S]*?createdAt = System\.currentTimeMillis\(\)\s*\)\s*repository\.addVideo\(video\)\s*Toast\.makeText\(context, "Video uploaded successfully", Toast\.LENGTH_SHORT\)\.show\(\)\s*\}'

content = re.sub(old_upload_logic_pattern, upload_logic, content)

with open("app/src/main/java/com/example/ui/admin/AdminContentUploadScreen.kt", "w") as f:
    f.write(content)
