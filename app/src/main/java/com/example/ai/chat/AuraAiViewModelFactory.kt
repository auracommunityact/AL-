package com.example.ai.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ai.tools.AuraToolExecutor
import com.example.ai.tools.AuraToolRegistry
import com.example.ai.tools.AuraToolValidator
import com.example.data.local.PlannerDatabase
import com.example.ai.vision.VisionProviderFactory
import com.example.ai.vision.VisionAiRepository


class AuraAiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuraAiViewModel::class.java)) {
            val database = PlannerDatabase.getDatabase(context)
            val memoryDao = database.aiMemoryDao()
            val toolRegistry = AuraToolRegistry()
            val toolValidator = AuraToolValidator(toolRegistry)
            val toolExecutor = AuraToolExecutor(context)
            val visionProvider = VisionProviderFactory.createRemoteProvider(context)
            val visionRepository = VisionAiRepository(visionProvider)
            
            @Suppress("UNCHECKED_CAST")
            return AuraAiViewModel(memoryDao, toolRegistry, toolValidator, toolExecutor, visionRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
