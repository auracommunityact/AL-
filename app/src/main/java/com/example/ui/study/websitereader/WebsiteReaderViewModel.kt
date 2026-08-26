package com.example.ui.study.websitereader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.PlannerDatabase
import com.example.data.local.WebsiteChatEntity
import com.example.data.local.WebsiteReaderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class WebsiteReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PlannerDatabase.getDatabase(application)
    private val dao = db.websiteReaderDao()

    // OkHttpClient with 30s timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // State definitions
    val cachedWebsites: StateFlow<List<WebsiteReaderEntity>> = dao.getAllCachedWebsites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentWebsite = MutableStateFlow<WebsiteReaderEntity?>(null)
    val currentWebsite: StateFlow<WebsiteReaderEntity?> = _currentWebsite.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingStage = MutableStateFlow("")
    val loadingStage: StateFlow<String> = _loadingStage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // FontSize state (in sp) for Reading Mode
    private val _fontSize = MutableStateFlow(16f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    // DarkMode state for reading mode, defaults to dark theme in study tools
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Hindi translation state
    private val _isHindiMode = MutableStateFlow(false)
    val isHindiMode: StateFlow<Boolean> = _isHindiMode.asStateFlow()

    private val _translatedSummary = MutableStateFlow<String?>(null)
    val translatedSummary: StateFlow<String?> = _translatedSummary.asStateFlow()

    private val _translationLoading = MutableStateFlow(false)
    val translationLoading: StateFlow<Boolean> = _translationLoading.asStateFlow()

    // Search properties
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Int>>(emptyList())
    val searchResults: StateFlow<List<Int>> = _searchResults.asStateFlow()

    private val _currentSearchResultIndex = MutableStateFlow(-1)
    val currentSearchResultIndex: StateFlow<Int> = _currentSearchResultIndex.asStateFlow()

    // Observe parsed elements reactively
    val currentElements: StateFlow<List<ExtractedElement>> = _currentWebsite
        .map { website ->
            if (website == null) emptyList()
            else {
                try {
                    Json.decodeFromString<List<ExtractedElement>>(website.headingsAndParagraphsJson)
                } catch (e: Exception) {
                    listOf(ExtractedElement("p", website.extractedText))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observe active website chat history
    val chatHistory: StateFlow<List<WebsiteChatEntity>> = _currentWebsite
        .flatMapLatest { website ->
            if (website == null) flowOf(emptyList())
            else dao.getChatHistoryByUrl(website.url)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectWebsite(website: WebsiteReaderEntity) {
        _currentWebsite.value = website
        _error.value = null
        _isHindiMode.value = false
        _translatedSummary.value = null
        clearSearch()
    }

    fun deselectWebsite() {
        _currentWebsite.value = null
        _error.value = null
        _isHindiMode.value = false
        _translatedSummary.value = null
        clearSearch()
    }

    fun setFontSize(size: Float) {
        _fontSize.value = size.coerceIn(12f, 30f)
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _currentSearchResultIndex.value = -1
    }

    fun performSearch(query: String) {
        _searchQuery.value = query
        val elements = currentElements.value
        if (query.isBlank() || elements.isEmpty()) {
            _searchResults.value = emptyList()
            _currentSearchResultIndex.value = -1
            return
        }

        val matches = mutableListOf<Int>()
        elements.forEachIndexed { index, element ->
            if (element.text.contains(query, ignoreCase = true)) {
                matches.add(index)
            }
        }
        _searchResults.value = matches
        _currentSearchResultIndex.value = if (matches.isNotEmpty()) 0 else -1
    }

    fun nextSearchResult() {
        val results = _searchResults.value
        if (results.isEmpty()) return
        val nextIndex = (_currentSearchResultIndex.value + 1) % results.size
        _currentSearchResultIndex.value = nextIndex
    }

    fun previousSearchResult() {
        val results = _searchResults.value
        if (results.isEmpty()) return
        val prevIndex = (_currentSearchResultIndex.value - 1 + results.size) % results.size
        _currentSearchResultIndex.value = prevIndex
    }

    /**
     * Parse the webpage, extract readable content, and save or update cache.
     */
    fun loadWebsite(inputUrl: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _translatedSummary.value = null
            _isHindiMode.value = false
            clearSearch()

            val sanitizedUrl = formatAndVerifyUrl(inputUrl)
            if (sanitizedUrl == null) {
                _error.value = "Invalid URL. Please enter a valid public HTTP or HTTPS website link."
                _isLoading.value = false
                return@launch
            }

            // 1. Check local cache first if not forcing refresh
            if (!forceRefresh) {
                _loadingStage.value = "Checking offline cache..."
                val cached = dao.getWebsiteByUrl(sanitizedUrl)
                if (cached != null) {
                    // Update timestamp
                    val updated = cached.copy(timestamp = System.currentTimeMillis())
                    dao.insertWebsite(updated)
                    _currentWebsite.value = updated
                    _isLoading.value = false
                    return@launch
                }
            }

            // 2. Fetch live content
            try {
                _loadingStage.value = "Connecting to website..."
                val htmlContent = fetchHtmlContent(sanitizedUrl)
                if (htmlContent.isBlank()) {
                    _error.value = "The website returned an empty response. Access might be restricted."
                    _isLoading.value = false
                    return@launch
                }

                _loadingStage.value = "Extracting useful article content..."
                val doc = Jsoup.parse(htmlContent, sanitizedUrl)
                val title = doc.title().ifBlank { "Untitled Webpage" }
                val domain = Uri.parse(sanitizedUrl).host ?: ""
                val faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=$domain"

                // Extract structured elements
                val parsedElements = extractContent(doc)
                if (parsedElements.isEmpty()) {
                    _error.value = "No readable content could be extracted from this webpage."
                    _isLoading.value = false
                    return@launch
                }

                val fullText = parsedElements.joinToString("\n\n") { it.text }
                val headingsAndParagraphsJson = Json.encodeToString(parsedElements)

                // 3. Generate AI Summary using Gemini
                _loadingStage.value = "Analyzing content with Gemini AI..."
                val aiSummary = generateAiSummary(title, sanitizedUrl, fullText)

                // 4. Detect language
                val detectedLang = if (fullText.any { it in '\u0900'..'\u097F' }) "hi" else "en"

                // 5. Build entity and cache it
                val entity = WebsiteReaderEntity(
                    url = sanitizedUrl,
                    title = title,
                    domain = domain,
                    faviconUrl = faviconUrl,
                    extractedText = fullText,
                    headingsAndParagraphsJson = headingsAndParagraphsJson,
                    aiSummary = aiSummary,
                    detectedLanguage = detectedLang,
                    timestamp = System.currentTimeMillis()
                )

                dao.insertWebsite(entity)
                _currentWebsite.value = entity

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timed out. The website is taking too long to load."
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "Network error: Unable to resolve host. Check your internet connection."
                    else -> "Failed to load website: ${e.localizedMessage ?: "Unknown error"}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatAndVerifyUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        // Support partial input like "google.com" -> "https://google.com"
        val formatted = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        return try {
            val uri = Uri.parse(formatted)
            val host = uri.host ?: return null
            val scheme = uri.scheme ?: return null

            // Block local addresses (localhost, 127.0.0.1, local private ranges)
            val isLocalHost = host.equals("localhost", ignoreCase = true) ||
                    host.startsWith("127.") ||
                    host.startsWith("192.168.") ||
                    host.startsWith("10.") ||
                    host.startsWith("172.16.") ||
                    host.startsWith("172.17.") ||
                    host.startsWith("172.18.") ||
                    host.startsWith("172.19.") ||
                    host.startsWith("172.20.") ||
                    host.startsWith("172.21.") ||
                    host.startsWith("172.22.") ||
                    host.startsWith("172.23.") ||
                    host.startsWith("172.24.") ||
                    host.startsWith("172.25.") ||
                    host.startsWith("172.26.") ||
                    host.startsWith("172.27.") ||
                    host.startsWith("172.28.") ||
                    host.startsWith("172.29.") ||
                    host.startsWith("172.30.") ||
                    host.startsWith("172.31.")

            val isPublicScheme = scheme.equals("http", ignoreCase = true) ||
                    scheme.equals("https", ignoreCase = true)

            if (!isLocalHost && isPublicScheme) formatted else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchHtmlContent(urlStr: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(urlStr)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            response.body?.string() ?: ""
        }
    }

    private fun extractContent(doc: org.jsoup.nodes.Document): List<ExtractedElement> {
        val list = mutableListOf<ExtractedElement>()

        // Remove junk elements completely
        doc.select("script, style, noscript, iframe, svg, head, footer, header, nav, aside, [role=banner], [role=navigation]").remove()
        // Remove common ads/popups selectors
        doc.select(".ads, .ad-box, .popup, #popup, .cookie-banner, .banner-ads, #footer, #header, #sidebar, .sidebar, .comments").remove()

        // Select all text-bearing and structural nodes
        val elements = doc.select("h1, h2, h3, h4, h5, h6, p, li, table, a")

        for (el in elements) {
            if (isInsideIgnoredContainer(el)) continue

            val text = el.text().trim()
            if (text.isEmpty() || text.length < 2) continue

            val type = when (el.tagName().lowercase()) {
                "h1" -> "h1"
                "h2" -> "h2"
                "h3", "h4", "h5", "h6" -> "h3"
                "li" -> "li"
                "table" -> "table"
                "a" -> "link"
                else -> "p"
            }

            // Extract table structural content as markdown table
            if (type == "table") {
                val tableMarkdown = convertTableToMarkdown(el)
                if (tableMarkdown.isNotBlank()) {
                    list.add(ExtractedElement("table", tableMarkdown))
                }
            } else if (type == "link") {
                val href = el.absUrl("href")
                if (href.startsWith("http")) {
                    list.add(ExtractedElement("link", text, href))
                }
            } else {
                list.add(ExtractedElement(type, text))
            }
        }

        return list
    }

    private fun isInsideIgnoredContainer(element: Element): Boolean {
        var parent = element.parent()
        while (parent != null) {
            val tagName = parent.tagName().lowercase()
            if (tagName == "header" || tagName == "footer" || tagName == "nav" || tagName == "aside") {
                return true
            }
            val className = parent.className().lowercase()
            val id = parent.id().lowercase()
            val classOrId = "$className $id"
            if (classOrId.contains("menu") || classOrId.contains("footer") || classOrId.contains("nav") ||
                classOrId.contains("sidebar") || classOrId.contains("header") || classOrId.contains("ads") ||
                classOrId.contains("popup") || classOrId.contains("banner") || classOrId.contains("cookie") ||
                classOrId.contains("widget") || classOrId.contains("comment") || classOrId.contains("share")
            ) {
                return true
            }
            parent = parent.parent()
        }
        return false
    }

    private fun convertTableToMarkdown(table: Element): String {
        val sb = java.lang.StringBuilder()
        val rows = table.select("tr")
        if (rows.isEmpty()) return ""

        for (i in rows.indices) {
            val cols = rows[i].select("th, td")
            if (cols.isEmpty()) continue
            sb.append("| ")
            for (col in cols) {
                sb.append(col.text().replace("|", "\\|")).append(" | ")
            }
            sb.append("\n")

            // Add divider row after the first header row
            if (i == 0) {
                sb.append("|")
                for (j in cols.indices) {
                    sb.append(" --- |")
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    /**
     * Call Gemini to generate a structured Multi-Section Markdown Summary.
     */
    private suspend fun generateAiSummary(title: String, url: String, content: String): String {
        val systemInstruction = """
            You are "Gemini Intelligence", the expert AI assistant inside Aura Learning.
            Your role is to build a highly structured, comprehensive, and clear academic Study Kit summarizing website text.
            You must format your response STRICTLY with Markdown level-3 headings ('### ') followed exactly by the designated section titles below.
            
            Format your entire output exactly with these headers:
            
            ### Short Summary
            [Provide a 2-3 sentence overall core wrap-up of the page]
            
            ### Detailed Summary
            [Provide an exhaustive, detailed breakdown of the entire website's content]
            
            ### Important Points
            [List bullet points of critical details]
            
            ### FAQs
            - **Q:** [A high-yield question]?
              **A:** [Direct clear answer]
            
            ### Key Facts
            [Provide a bullet list of core factual truths from the text]
            
            ### Definitions
            - **[Term]**: [Clear exact academic definition]
            
            ### Tables
            [If the website had tabular data, represent it as clean markdown tables. Otherwise, state 'No data tables available on this webpage.']
            
            ### Study Notes
            [Structured learning notes, categorized logically with subheadings or bullet lists]
            
            ### Easy Explanation
            [Provide an intuitive explanation using an interesting, relatable real-world analogy]
            
            ### Beginner Friendly Explanation
            [Explain the entire webpage in simple terms as if teaching a 10-year old child with zero technical jargon]
            
            ### Important Dates
            [Bullet list of dates, timeline occurrences, or deadlines mentioned. If none, state 'No dates or timeline events mentioned.']
            
            ### Important Numbers
            [Bullet list of any stats, figures, measurements, percentages, or values mentioned. If none, state 'No statistical figures mentioned.']
            
            ### Important Links
            [List any critical links mentioned in the webpage content]
        """.trimIndent()

        val prompt = """
            Analyze this webpage content and generate the comprehensive academic Study Kit.
            Webpage Title: $title
            Webpage URL: $url
            
            Webpage Content:
            $content
        """.trimIndent()

        return callGemini(prompt, systemInstruction)
    }

    private suspend fun callGemini(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please enter your API key securely into the Secrets panel in AI Studio."
        }

        val requestBodyJson = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            if (systemInstruction != null) {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", systemInstruction)
                        }
                    }
                }
            }
        }.toString()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBodyJson.toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errString = response.body?.string() ?: ""
                    return@withContext "Error: Gemini API returned code ${response.code}.\n$errString"
                }
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response from AI service."

                val json = Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(responseBody)
                val text = element.jsonObject["candidates"]?.jsonArray
                    ?.getOrNull(0)?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray
                    ?.getOrNull(0)?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content

                text ?: "Error: Failed to extract AI response text."
            }
        } catch (e: Exception) {
            "Error calling Gemini: ${e.localizedMessage ?: "Unknown network exception"}"
        }
    }

    /**
     * Chat Q&A regarding the loaded website.
     * Restricts AI answers strictly to extracted text content.
     */
    fun askQuestion(question: String) {
        val currentWeb = _currentWebsite.value ?: return
        val trimmedQ = question.trim()
        if (trimmedQ.isBlank()) return

        viewModelScope.launch {
            // Insert user query locally
            val userMsg = WebsiteChatEntity(
                url = currentWeb.url,
                isUser = true,
                text = trimmedQ,
                timestamp = System.currentTimeMillis()
            )
            dao.insertChatMessage(userMsg)

            // Show temporary loading or typing indicator by updating list if required,
            // or just trigger background call
            val systemInstruction = """
                You are "Gemini Intelligence", a seamless AI assistant embedded inside Aura Learning.
                You are helping the user study the content of a specific website.
                
                Website Title: ${currentWeb.title}
                Website URL: ${currentWeb.url}
                Website Extracted Content:
                ${currentWeb.extractedText}
                
                Guidelines:
                1. Answer the user's question ONLY using the extracted content of the website provided above.
                2. If the answer cannot be found or reasonably inferred from the extracted content, state politely: "I'm sorry, but this information is not available on the loaded webpage. I am strictly restricted to answering questions based on this page's content."
                3. Do not invent facts, and do not use outside knowledge.
                4. Maintain a supportive, helpful academic tone. Keep it concise.
            """.trimIndent()

            val aiAnswer = callGemini(trimmedQ, systemInstruction)

            val aiMsg = WebsiteChatEntity(
                url = currentWeb.url,
                isUser = false,
                text = aiAnswer,
                timestamp = System.currentTimeMillis()
            )
            dao.insertChatMessage(aiMsg)
        }
    }

    /**
     * Toggle summary language translation (English <-> Hindi) using Gemini.
     */
    fun toggleLanguageMode() {
        val currentWeb = _currentWebsite.value ?: return
        val currentSummary = currentWeb.aiSummary ?: return
        val isHindi = _isHindiMode.value

        if (isHindi) {
            // Switch back to English (default cached)
            _isHindiMode.value = false
            _translatedSummary.value = null
        } else {
            // Translate summary to Hindi using Gemini
            _isHindiMode.value = true
            val existingTranslation = _translatedSummary.value
            if (existingTranslation == null) {
                viewModelScope.launch {
                    _translationLoading.value = true
                    val prompt = """
                        Translate the following Study Kit summary into clear, readable academic Hindi (Devanagari script), matching the original markdown formatting exactly.
                        Maintain all '### ' headers unchanged in English so the layout remains compatible, but translate the body content of each section perfectly.
                        
                        Study Kit summary content:
                        $currentSummary
                    """.trimIndent()

                    val translated = callGemini(prompt, "You are an expert English-to-Hindi educational translator.")
                    _translatedSummary.value = translated
                    _translationLoading.value = false
                }
            }
        }
    }

    fun deleteCachedWebsite(url: String) {
        viewModelScope.launch {
            dao.deleteWebsite(url)
            dao.deleteChatHistoryByUrl(url)
            if (_currentWebsite.value?.url == url) {
                deselectWebsite()
            }
        }
    }

    fun clearAllWebsites() {
        viewModelScope.launch {
            dao.clearCache()
            deselectWebsite()
        }
    }
}
