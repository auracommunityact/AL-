package com.example.ui.pdf.screens

import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfBuilderScreen(navController: NavController) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("PDF Content") }
        )
        Button(
            onClick = {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint()
                canvas.drawText(text, 10f, 20f, paint)
                pdfDocument.finishPage(page)
                
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GeneratedPDF_${System.currentTimeMillis()}.pdf")
                try {
                    FileOutputStream(file).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    Toast.makeText(context, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    pdfDocument.close()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate PDF")
        }
    }
}
