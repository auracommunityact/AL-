package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object StorageUtils {
    fun compressImage(context: Context, imageUri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024, quality: Int = 80): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val ratio = width.toFloat() / height.toFloat()
            var newWidth = width
            var newHeight = height
            
            if (width > maxWidth || height > maxHeight) {
                if (ratio > 1) {
                    newWidth = maxWidth
                    newHeight = (maxWidth / ratio).toInt()
                } else {
                    newHeight = maxHeight
                    newWidth = (maxHeight * ratio).toInt()
                }
            }
            
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
