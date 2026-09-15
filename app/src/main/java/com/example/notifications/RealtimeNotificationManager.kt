package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.data.local.PlannerDatabase
import com.example.data.local.notifications.NotificationEntity
import com.example.data.repository.notifications.SupabaseNotification
import com.example.data.supabase.SupabaseService
import com.example.utils.NotificationHelper
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement

object RealtimeNotificationManager {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabase = SupabaseService.client

    private var isListening = false

    fun start(context: Context) {
        if (!isListening) {
            startListening(context.applicationContext)
            isListening = true
        }
    }

    private fun startListening(context: Context) {
        val channel = supabase.realtime.channel("notifications_realtime")
        
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "notifications"
        }.onEach { action ->
            val data = action.record
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val notification = json.decodeFromJsonElement<SupabaseNotification>(data)
                
                handleNewNotification(context, notification)
            } catch (e: Exception) {
                Log.e("RealtimeManager", "Error decoding notification", e)
            }
        }.catch { e ->
            Log.e("RealtimeManager", "Flow error", e)
        }.launchIn(serviceScope)

        serviceScope.launch {
            try {
                channel.subscribe()
                Log.d("RealtimeManager", "Subscribed to notifications")
            } catch (e: Exception) {
                Log.e("RealtimeManager", "Subscription failed", e)
            }
        }
    }

    private suspend fun handleNewNotification(context: Context, notif: SupabaseNotification) {
        val db = PlannerDatabase.getDatabase(context)
        
        // Save locally
        val entity = NotificationEntity(
            id = notif.id,
            title = notif.title,
            description = notif.description,
            imageUrl = notif.image_url,
            category = notif.category,
            deepLink = notif.deep_link,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            priority = notif.priority,
            actionButtonText = notif.action_button_text
        )
        db.notificationDao().insertNotification(entity)

        // Show system notification
        NotificationHelper.showNotification(
            context,
            notif.title,
            notif.description,
            notif.category,
            notif.deep_link
        )
        
        // Notify the app that data changed
        com.example.data.repository.AuraRepository.notifyNotificationsChanged()
    }
}
