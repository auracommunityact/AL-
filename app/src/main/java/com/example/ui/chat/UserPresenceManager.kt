package com.example.ui.chat

import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UserPresenceManager {
    fun setOnline() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = SupabaseService.client.auth.currentUserOrNull()?.id ?: return@launch
                SupabaseService.client.postgrest["user_presence"].upsert(
                    mapOf(
                        "userId" to userId,
                        "isOnline" to true,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun setOffline() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = SupabaseService.client.auth.currentUserOrNull()?.id ?: return@launch
                SupabaseService.client.postgrest["user_presence"].upsert(
                    mapOf(
                        "userId" to userId,
                        "isOnline" to false,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
