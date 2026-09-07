package com.example.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

import com.example.BuildConfig
import kotlinx.coroutines.delay
import java.io.IOException

import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime

object SupabaseService {
    val client by lazy {
        val url = try { BuildConfig.SUPABASE_URL } catch (e: Throwable) { "" }
            .ifBlank { "https://qxoqflrqpwlythgqmjtq.supabase.co" }
        val key = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Throwable) { "" }
            .ifBlank { "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF4b3FmbHJxcHdseXRoZ3FtanRxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIxODIxMTQsImV4cCI6MjA5Nzc1ODExNH0.cJ3hIsEyRtH1m_nmyzwjrdvzsbGIKIiChnmXAjgFRfo" }

        val keyPreview = when {
            key.isBlank() -> "MISSING"
            key.endsWith(".placeholder") -> "PLACEHOLDER_ERROR"
            key.startsWith("eyJ") && key.length > 50 -> "VALID_FORMAT (length ${key.length})"
            else -> "UNKNOWN_FORMAT"
        }
        
        android.util.Log.i("SupabaseDiagnostic", "=== Supabase Configuration Check ===")
        android.util.Log.i("SupabaseDiagnostic", "URL format valid: ${url.startsWith("https://")}")
        android.util.Log.i("SupabaseDiagnostic", "Project Reference: ${url.removePrefix("https://").substringBefore(".supabase.co")}")
        android.util.Log.i("SupabaseDiagnostic", "Key Type Detected: $keyPreview")
        android.util.Log.i("SupabaseDiagnostic", "=====================================")

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(Realtime) {
                // Prevent aggressive reconnection failures when offline/flaky
                reconnectDelay = kotlin.time.Duration.parse("3s")
            }
        }
    }

    suspend fun <T> retryWithExponentialBackoff(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) {
            try {
                return block()
            } catch (e: Exception) {
                if (e is IOException) {
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    throw e
                }
            }
        }
        return block()
    }
}
