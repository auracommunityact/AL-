package com.example.ui.study

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.repository.TranslationService
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTranslateScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val translationService = remember { TranslationService() }

    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    val languageMap = remember {
        mapOf(
            "Afrikaans" to "af", "Albanian" to "sq", "Arabic" to "ar", "Belarusian" to "be", 
            "Bengali" to "bn", "Bulgarian" to "bg", "Catalan" to "ca", "Chinese" to "zh", 
            "Croatian" to "hr", "Czech" to "cs", "Danish" to "da", "Dutch" to "nl", 
            "English" to "en", "Esperanto" to "eo", "Estonian" to "et", "Finnish" to "fi", 
            "French" to "fr", "Galician" to "gl", "Georgian" to "ka", "German" to "de", 
            "Greek" to "el", "Gujarati" to "gu", "Haitian Creole" to "ht", "Hebrew" to "he", 
            "Hindi" to "hi", "Hungarian" to "hu", "Icelandic" to "is", "Indonesian" to "id", 
            "Irish" to "ga", "Italian" to "it", "Japanese" to "ja", "Kannada" to "kn", 
            "Korean" to "ko", "Latvian" to "lv", "Lithuanian" to "lt", "Macedonian" to "mk", 
            "Malay" to "ms", "Maltese" to "mt", "Marathi" to "mr", "Norwegian" to "no", 
            "Persian" to "fa", "Polish" to "pl", "Portuguese" to "pt", "Romanian" to "ro", 
            "Russian" to "ru", "Slovak" to "sk", "Slovenian" to "sl", "Spanish" to "es", 
            "Swahili" to "sw", "Swedish" to "sv", "Tagalog" to "tl", "Tamil" to "ta", 
            "Telugu" to "te", "Thai" to "th", "Turkish" to "tr", "Ukrainian" to "uk", 
            "Urdu" to "ur", "Vietnamese" to "vi", "Welsh" to "cy"
        )
    }
    val languages = remember { listOf("Auto Detect") + languageMap.keys.toList().sorted() }

    var fromLanguage by remember { mutableStateOf(languages[0]) }
    var toLanguage by remember { mutableStateOf("English") }
    var fromLangExpanded by remember { mutableStateOf(false) }
    var toLangExpanded by remember { mutableStateOf(false) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.get(0) ?: ""
            sourceText += if (sourceText.isEmpty()) recognizedText else " $recognizedText"
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input not supported", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes Translate", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Translate your study notes into any language.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Language Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { fromLangExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(fromLanguage, maxLines = 1)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = fromLangExpanded, onDismissRequest = { fromLangExpanded = false }) {
                        languages.forEach { lang ->
                            DropdownMenuItem(text = { Text(lang) }, onClick = { fromLanguage = lang; fromLangExpanded = false })
                        }
                    }
                }
                
                IconButton(onClick = {
                    if (fromLanguage != "Auto Detect") {
                        val temp = fromLanguage
                        fromLanguage = toLanguage
                        toLanguage = temp
                    }
                }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap")
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { toLangExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(toLanguage, maxLines = 1)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = toLangExpanded, onDismissRequest = { toLangExpanded = false }) {
                        languages.filter { it != "Auto Detect" }.forEach { lang ->
                            DropdownMenuItem(text = { Text(lang) }, onClick = { toLanguage = lang; toLangExpanded = false })
                        }
                    }
                }
            }

            // Input Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = sourceText,
                        onValueChange = { sourceText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 300.dp),
                        placeholder = { Text("Paste or write your notes here...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${sourceText.length} chars | ${sourceText.split(Regex("\\s+")).count { it.isNotEmpty() }} words", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row {
                            IconButton(onClick = { sourceText = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = clipboard.primaryClip?.getItemAt(0)?.text
                                if (!text.isNullOrEmpty()) {
                                    sourceText += text
                                }
                            }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                            }
                            IconButton(onClick = { startVoiceInput() }) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice Typing")
                            }
                        }
                    }
                }
            }

            // Translate Button
            Button(
                onClick = {
                    if (sourceText.isNotBlank()) {
                        isTranslating = true
                        translatedText = ""
                        coroutineScope.launch {
                            var actualFrom = fromLanguage
                            if (actualFrom == "Auto Detect") {
                                val detectedCode = translationService.identifyLanguage(sourceText)
                                if (detectedCode != null) {
                                    val detectedLang = languageMap.entries.find { it.value == detectedCode }?.key
                                    if (detectedLang != null) {
                                        actualFrom = detectedLang
                                        fromLanguage = detectedLang // Update UI
                                    } else {
                                        actualFrom = "English"
                                    }
                                } else {
                                    actualFrom = "English"
                                }
                            }
                            
                            val sourceCode = languageMap[actualFrom] ?: "en"
                            val targetCode = languageMap[toLanguage] ?: "es"
                            
                            translatedText = translationService.translateText(sourceText, sourceCode, targetCode)
                            isTranslating = false
                        }
                    } else {
                        Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isTranslating
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Downloading model / Translating...")
                } else {
                    Icon(Icons.Filled.Translate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate Notes", fontWeight = FontWeight.Bold)
                }
            }

            // Output Area
            AnimatedVisibility(visible = translatedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Translation Result", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Translation", translatedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, translatedText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                tts?.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, null)
                            }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Speak")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
