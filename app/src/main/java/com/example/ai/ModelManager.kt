package com.example.ai

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.MessageDigest

class ModelManager(private val context: Context) {
    
    companion object {
        const val DOWNLOAD_WORK_NAME = "model_download_work"
    }

    private val workManager = WorkManager.getInstance(context)

    fun startModelDownload(url: String, fileName: String, expectedChecksum: String? = null) {
        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_URL, url)
            .putString(ModelDownloadWorker.KEY_FILE_NAME, fileName)
            .putString(ModelDownloadWorker.KEY_EXPECTED_CHECKSUM, expectedChecksum)
            .build()

        val downloadWorkRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(DOWNLOAD_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            DOWNLOAD_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            downloadWorkRequest
        )
    }

    fun stopModelDownload() {
        workManager.cancelUniqueWork(DOWNLOAD_WORK_NAME)
    }

    fun getDownloadWorkInfo(): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkLiveData(DOWNLOAD_WORK_NAME).asFlow().map { it.firstOrNull() }
    }

    fun isModelDownloaded(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists() && file.length() > 0
    }

    fun deleteModel(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun getModelFile(fileName: String): File? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file else null
    }
}
