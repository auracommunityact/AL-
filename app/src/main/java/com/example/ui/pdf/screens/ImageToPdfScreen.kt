package com.example.ui.pdf.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import android.widget.Toast
import android.os.Environment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.pdf.viewmodels.ImageToPdfViewModel
import com.example.utils.ImageToPdfConverter
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: ImageToPdfViewModel = viewModel()
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsState()
    val pdfName by viewModel.pdfName.collectAsState()
    val scope = rememberCoroutineScope()
    
    var hasStoragePermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newBitmaps = uris.map { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }
        viewModel.addImages(newBitmaps)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Images to PDF") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = pdfName,
                onValueChange = { viewModel.setPdfName(it) },
                label = { Text("PDF Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { imageLauncher.launch("image/*") }) {
                Text("Select Images")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(images) { index, bitmap ->
                    Card(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(100.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.reorderImages(index, maxOf(0, index - 1)) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up")
                            }
                            IconButton(onClick = { viewModel.reorderImages(index, minOf(images.size - 1, index + 1)) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down")
                            }
                            IconButton(onClick = { viewModel.removeImage(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!hasStoragePermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        Toast.makeText(context, "Permission required to save file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    scope.launch {
                        val fileName = if (pdfName.endsWith(".pdf", ignoreCase = true)) pdfName else "$pdfName.pdf"
                        
                        var success = false
                        var savedPath = ""
                        
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val contentValues = android.content.ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                                }
                                val resolver = context.contentResolver
                                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                
                                if (uri != null) {
                                    resolver.openOutputStream(uri)?.use { outputStream ->
                                        success = ImageToPdfConverter.convertImagesToPdfStream(images, outputStream, isA4 = true, isPortrait = true)
                                        savedPath = "Downloads/$fileName"
                                    }
                                    contentValues.clear()
                                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                    resolver.update(uri, contentValues, null, null)
                                }
                            } else {
                                // For API < 29, fallback to Environment.getExternalStoragePublicDirectory
                                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val file = File(downloadDir, fileName)
                                success = ImageToPdfConverter.convertImagesToPdf(images, file, isA4 = true, isPortrait = true)
                                savedPath = file.absolutePath
                            }
                            
                            if (success) {
                                Toast.makeText(context, "Saved to $savedPath", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = images.isNotEmpty()
            ) {
                Text("Create PDF")
            }
        }
    }
}
