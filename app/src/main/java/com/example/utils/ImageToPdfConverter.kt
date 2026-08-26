package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageToPdfConverter {
    fun convertImagesToPdf(
        images: List<Bitmap>,
        outputFile: File,
        isA4: Boolean,
        isPortrait: Boolean
    ): Boolean {
        return try {
            FileOutputStream(outputFile).use { fos ->
                convertImagesToPdfStream(images, fos, isA4, isPortrait)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun convertImagesToPdfStream(
        images: List<Bitmap>,
        outputStream: OutputStream,
        isA4: Boolean,
        isPortrait: Boolean
    ): Boolean {
        val document = PdfDocument()
        
        val pageWidth = if (isA4) 595 else 612 // A4/Letter in points
        val pageHeight = if (isA4) 842 else 792
        
        val finalWidth = if (isPortrait) pageWidth else pageHeight
        val finalHeight = if (isPortrait) pageHeight else pageWidth

        try {
            for (bitmap in images) {
                val pageInfo = PdfDocument.PageInfo.Builder(finalWidth, finalHeight, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                
                val paint = Paint()
                val scale = minOf(finalWidth.toFloat() / bitmap.width, finalHeight.toFloat() / bitmap.height)
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
                
                canvas.drawBitmap(scaledBitmap, (finalWidth - scaledBitmap.width) / 2f, (finalHeight - scaledBitmap.height) / 2f, paint)
                document.finishPage(page)
            }
            
            document.writeTo(outputStream)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
