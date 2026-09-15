package com.example.ui.pdfmanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PlannerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HorizontalPdfViewModel : ViewModel() {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    var pageCount by mutableStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun openPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                withContext(Dispatchers.IO) {
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (fd != null) {
                        fileDescriptor = fd
                        pdfRenderer = PdfRenderer(fd)
                        pageCount = pdfRenderer?.pageCount ?: 0
                    } else {
                        errorMessage = "Could not open file"
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to open PDF"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        var bitmap: Bitmap? = null
        
        // PdfRenderer is not thread-safe; must synchronize access
        synchronized(renderer) {
            try {
                val page = renderer.openPage(pageIndex)
                
                // Calculate dimensions fitting the screen
                val pdfWidth = page.width
                val pdfHeight = page.height
                
                val scale = minOf(width.toFloat() / pdfWidth, height.toFloat() / pdfHeight)
                val destWidth = (pdfWidth * scale).toInt()
                val destHeight = (pdfHeight * scale).toInt()

                // Create a bitmap and render
                if (destWidth > 0 && destHeight > 0) {
                    bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
                    bitmap?.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
                page.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bitmap
    }

    override fun onCleared() {
        super.onCleared()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

@Composable
fun HorizontalPdfReader(
    uri: Uri,
    viewModel: HorizontalPdfViewModel = viewModel(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showNoteDialog by remember { mutableStateOf(false) }
    
    // Note taking setup
    val noteDao = PlannerDatabase.getDatabase(context).noteDao()
    val noteViewModel: com.example.ui.notes.NoteTakingViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.ui.notes.NoteTakingViewModel(noteDao) as T
            }
        }
    )

    LaunchedEffect(uri) {
        viewModel.openPdf(context, uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            viewModel.errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onClose) {
                        Text("Go Back")
                    }
                }
            }
            viewModel.pageCount > 0 -> {
                val pagerState = rememberPagerState(pageCount = { viewModel.pageCount })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PdfPageOverlay(
                        pageIndex = pageIndex,
                        viewModel = viewModel
                    )
                }

                // Page Indicator Overlay
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Page ${pagerState.currentPage + 1} of ${viewModel.pageCount}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Floating Note Button
                com.example.ui.notes.FloatingNoteButton(
                    onNoteClick = { showNoteDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        
        if (showNoteDialog) {
            com.example.ui.notes.NoteDialog(
                onDismiss = { showNoteDialog = false },
                onSave = { content -> noteViewModel.saveNote(content, uri.toString()) }
            )
        }
    }
}

@Composable
private fun PdfPageOverlay(pageIndex: Int, viewModel: HorizontalPdfViewModel) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        LaunchedEffect(pageIndex, width, height) {
            if (width > 0 && height > 0) {
                bitmap = viewModel.renderPage(pageIndex, width, height)
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
