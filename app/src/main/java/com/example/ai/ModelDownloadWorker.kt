package com.example.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EXPECTED_CHECKSUM = "expected_checksum"
        const val PROGRESS = "progress"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urlString = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return@withContext Result.failure()
        val expectedChecksum = inputData.getString(KEY_EXPECTED_CHECKSUM)

        val outputFile = File(applicationContext.filesDir, fileName)
        val tempFile = File(applicationContext.filesDir, "$fileName.tmp")

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(workDataOf("error" to "Server returned HTTP ${connection.responseCode}"))
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(tempFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            var lastProgress = 0

            while (input.read(data).also { count = it } != -1) {
                if (isStopped) {
                    output.close()
                    input.close()
                    tempFile.delete()
                    return@withContext Result.failure()
                }

                total += count.toLong()
                output.write(data, 0, count)

                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    if (progress != lastProgress) {
                        setProgress(workDataOf(PROGRESS to progress))
                        lastProgress = progress
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            // Verification
            if (expectedChecksum != null) {
                setProgress(workDataOf(PROGRESS to 100, "status" to "Verifying integrity..."))
                val actualChecksum = calculateMD5(tempFile)
                if (actualChecksum != expectedChecksum) {
                    tempFile.delete()
                    return@withContext Result.failure(workDataOf("error" to "Checksum mismatch. Expected $expectedChecksum, got $actualChecksum"))
                }
            }

            // Move temp file to actual file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            tempFile.renameTo(outputFile)

            return@withContext Result.success(workDataOf("file_path" to outputFile.absolutePath))

        } catch (e: Exception) {
            tempFile.delete()
            return@withContext Result.failure(workDataOf("error" to e.message))
        }
    }

    private fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
