package com.example.ai.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ai.model.LiteRtLlmEngine
import com.example.ai.tools.AuraToolExecutor
import com.example.ai.tools.AuraToolRegistry
import com.example.ai.tools.AuraToolValidator
import com.example.data.local.PlannerDatabase

class AuraAiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuraAiViewModel::class.java)) {
            val database = PlannerDatabase.getDatabase(context)
            val memoryDao = database.aiMemoryDao()
            val engine = LiteRtLlmEngine()
            val toolRegistry = AuraToolRegistry()
            val toolValidator = AuraToolValidator(toolRegistry)
            val toolExecutor = AuraToolExecutor(context)
            
            @Suppress("UNCHECKED_CAST")
            return AuraAiViewModel(memoryDao, engine, toolRegistry, toolValidator, toolExecutor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
