package com.example.ai.vision

import android.content.Context

object VisionProviderFactory {
    fun createRemoteProvider(context: Context): VisionAiProvider {
        return RemoteVisionAiProvider(context)
    }
}
