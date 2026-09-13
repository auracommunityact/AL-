package com.example.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class RemoteVisionAiProvider(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : VisionAiProvider {

    override suspend fun analyzeImage(request: VisionRequest, conversationId: String?): VisionResponse = withContext(Dispatchers.IO) {
        try {
            val apiUrl = BuildConfig.AURA_VISION_API_URL
            if (apiUrl.isEmpty()) {
                return@withContext VisionResponse(false, null, "Vision API URL is not configured.")
            }
            
            if (request.imageUri == null) {
                return@withContext VisionResponse(false, null, "No image provided.")
            }

            val file = getResizedImageFile(request.imageUri)
                ?: return@withContext VisionResponse(false, null, "Failed to process image.")

            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            requestBodyBuilder.addFormDataPart(
                "image", 
                file.name,
                file.asRequestBody(mediaType)
            )

            if (!request.text.isNullOrBlank()) {
                requestBodyBuilder.addFormDataPart("message", request.text)
            } else {
                requestBodyBuilder.addFormDataPart("message", "Analyze this image and describe the important information visible in it.")
            }

            if (conversationId != null) {
                requestBodyBuilder.addFormDataPart("conversation_id", conversationId)
            }

            val requestHttp = okhttp3.Request.Builder()
                .url(apiUrl)
                .post(requestBodyBuilder.build())
                .build()

            val response = httpClient.newCall(requestHttp).execute()
            val responseBody = response.body?.string()

            file.delete() // Clean up temp file

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val answer = json.optString("answer")
                    VisionResponse(true, answer)
                } else {
                    val error = json.optString("error", "Unknown API error")
                    VisionResponse(false, null, error)
                }
            } else {
                VisionResponse(false, null, "HTTP Error: ${response.code}")
            }
        } catch (e: Exception) {
            VisionResponse(false, null, "Network or unexpected error: ${e.message}")
        }
    }

    private fun getResizedImageFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream?.close()

            // Resize to a reasonable max dimension (e.g., 1024px)
            val maxDim = 1024
            var width = originalBitmap.width
            var height = originalBitmap.height

            if (width > maxDim || height > maxDim) {
                val ratio: Float = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    width = maxDim
                    height = (maxDim / ratio).toInt()
                } else {
                    height = maxDim
                    width = (maxDim * ratio).toInt()
                }
            }

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
            val tempFile = File(context.cacheDir, "vision_upload_temp.jpg")
            val outputStream = FileOutputStream(tempFile)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()
            
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
