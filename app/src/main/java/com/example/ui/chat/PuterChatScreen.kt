package com.example.ui.chat

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PuterChatScreen(navController: NavController, prompt: String? = null) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showPromptAlert by remember { mutableStateOf(false) }

    // Copy prompt to clipboard if present
    LaunchedEffect(prompt) {
        if (!prompt.isNullOrBlank()) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Gemini Prompt", prompt)
                clipboard.setPrimaryClip(clip)
                showPromptAlert = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Back Handler to navigate back in WebView history if possible, else pop backstack
    androidx.activity.compose.BackHandler(enabled = true) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            navController.navigateUp()
        }
    }

    Scaffold(
        // Top Bar is removed for a native, full-screen experience
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress Bar
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            
                            // Enable hardware acceleration
                            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

                            // Security & Settings
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                // Set modern User Agent to allow Google OAuth / Login to work
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                            }

                            // Enable Cookies
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                private fun injectHideBannerScript(view: WebView) {
                                    val css = "header, .header, [role=\"banner\"], div[class*=\"gb_\"], div[id*=\"gb\"], .top-nav, .navigation-bar { display: none !important; }"
                                    val js = """
                                        (function() {
                                            var style = document.createElement('style');
                                            style.type = 'text/css';
                                            style.appendChild(document.createTextNode('$css'));
                                            document.head.appendChild(style);
                                            
                                            // Periodic loop to make sure it is hidden continuously
                                            if (!window.bannerIntervalSet) {
                                                window.bannerIntervalSet = true;
                                                setInterval(function() {
                                                    var elements = document.querySelectorAll('header, .header, [role="banner"], div[class*="gb_"], div[id*="gb\"], .top-nav, .navigation-bar');
                                                    for (var i = 0; i < elements.length; i++) {
                                                        if (elements[i].style.display !== 'none') {
                                                            elements[i].style.setProperty('display', 'none', 'important');
                                                        }
                                                    }
                                                }, 300);
                                            }
                                        })();
                                    """.trimIndent()
                                    view.evaluateJavascript(js, null)
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    injectHideBannerScript(view)
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    injectHideBannerScript(view)
                                }

                                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    injectHideBannerScript(view)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progress = newProgress
                                    if (newProgress >= 20) {
                                        // Inject early as dynamic content builds up
                                        val css = "header, .header, [role=\"banner\"], div[class*=\"gb_\"], div[id*=\"gb\"], .top-nav, .navigation-bar { display: none !important; }"
                                        val js = """
                                            (function() {
                                                var style = document.createElement('style');
                                                style.type = 'text/css';
                                                style.appendChild(document.createTextNode('$css'));
                                                document.head.appendChild(style);
                                            })();
                                        """.trimIndent()
                                        view.evaluateJavascript(js, null)
                                    }
                                }
                            }

                            loadUrl("https://gemini.google.com/")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Beautiful prompt alert banner at the bottom if we have an incoming prompt
            AnimatedVisibility(
                visible = showPromptAlert,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Prompt Copied",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Prompt copied to clipboard!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please paste the copied prompt directly into the Gemini chat box to begin studying.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showPromptAlert = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
