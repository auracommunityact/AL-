package com.example.data.repository
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.encodeToJsonElement
import io.github.jan.supabase.postgrest.query.Columns


import com.example.data.models.*
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import io.github.jan.supabase.postgrest.query.Order

class ChatRepository {
    private val lenientJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private inline fun <reified T : Any> getJsonWithoutId(item: T): Map<String, kotlinx.serialization.json.JsonElement> {
        val map = lenientJson.encodeToJsonElement(item).jsonObject.toMutableMap()
        map.remove("id")
        return map
    }
    
    private inline fun <reified T : Any> getJsonListWithoutId(items: List<T>): List<Map<String, kotlinx.serialization.json.JsonElement>> {
        return items.map { getJsonWithoutId(it) }
    }

    private val client = SupabaseService.client
    private val postgrest = client.postgrest
    private val auth = client.auth
    private val realtime = client.realtime
    
    suspend fun getConversations(): List<Conversation> {
        val currentUserId = auth.currentUserOrNull()?.id ?: return emptyList()
        val members = postgrest["conversation_members"]
            .select { filter { eq("userId", currentUserId) } }
            .decodeList<ConversationMember>()
            
        if (members.isEmpty()) return emptyList()
        
        val conversationIds = members.map { it.conversationId }
        return postgrest["conversations"]
            .select { filter { isIn("id", conversationIds) } }
            .decodeList<Conversation>()
            .sortedByDescending { it.lastMessageTime }
    }
    
    suspend fun getConversation(conversationId: String): Conversation? {
        return try {
            postgrest["conversations"]
                .select { filter { eq("id", conversationId) } }
                .decodeSingle<Conversation>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMessages(conversationId: String): List<Message> {
        return postgrest["messages"]
            .select { 
                filter { eq("conversationId", conversationId) }
                order("createdAt", Order.ASCENDING)
            }
            .decodeList<Message>()
    }
    
    suspend fun sendMessage(message: Message): Message {
        return postgrest["messages"]
            .insert(if (message.id.isEmpty() || message.id.length > 20) getJsonWithoutId(message) else message) { select() }
            .decodeSingle<Message>()
    }
    
    suspend fun updateConversationLastMessage(conversationId: String, text: String) {
        postgrest["conversations"]
            .update({
                set("lastMessageText", text)
                set("lastMessageTime", System.currentTimeMillis())
            }) {
                filter { eq("id", conversationId) }
            }
    }

    suspend fun getOrCreateConversation(otherUserId: String, otherUserName: String): String {
        val currentUserId = auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
        
        // Find existing 1-on-1 conversation
        // This is a bit simplified; real prod apps should use a more robust check
        val currentMembers = postgrest["conversation_members"]
            .select { filter { eq("userId", currentUserId) } }
            .decodeList<ConversationMember>()
            
        val otherMembers = postgrest["conversation_members"]
            .select { filter { eq("userId", otherUserId) } }
            .decodeList<ConversationMember>()
            
        val commonConvoId = currentMembers.map { it.conversationId }
            .intersect(otherMembers.map { it.conversationId }.toSet())
            .firstOrNull()
            
        if (commonConvoId != null) return commonConvoId
        
        // Create new
        val convoId = java.util.UUID.randomUUID().toString()
        val newConvo = Conversation(
            id = convoId,
            name = otherUserName,
            lastMessageTime = System.currentTimeMillis()
        )
        postgrest["conversations"].insert(if (newConvo.id.isEmpty() || newConvo.id.length > 20) getJsonWithoutId(newConvo) else newConvo)
        
        postgrest["conversation_members"].insert(getJsonListWithoutId(listOf(
            ConversationMember(conversationId = convoId, userId = currentUserId),
            ConversationMember(conversationId = convoId, userId = otherUserId)
        )))
        
        return convoId
    }
    
    fun subscribeToMessages(conversationId: String): Flow<Message> = callbackFlow {
        val channel = realtime.channel("messages-$conversationId")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>("public") {
            table = "messages"
        }
        
        val job = CoroutineScope(Dispatchers.IO).launch {
            changes.collect { action ->
                try {
                    val message = Json.decodeFromJsonElement<Message>(action.record)
                    if (message.conversationId == conversationId) {
                        trySend(message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            channel.subscribe()
        }
        
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                channel.unsubscribe()
            }
        }
    }

    fun subscribeToMessageUpdates(conversationId: String): Flow<MessageUpdate> = callbackFlow {
        val channel = realtime.channel("msg-updates-$conversationId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "messages"
        }
        
        val job = CoroutineScope(Dispatchers.IO).launch {
            changes.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert -> {
                            val message = Json.decodeFromJsonElement<Message>(action.record)
                            if (message.conversationId == conversationId) {
                                trySend(MessageUpdate.Insert(message))
                            }
                        }
                        is PostgresAction.Update -> {
                            val message = Json.decodeFromJsonElement<Message>(action.record)
                            if (message.conversationId == conversationId) {
                                trySend(MessageUpdate.Update(message))
                            }
                        }
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"]?.toString()?.replace("\"", "")
                            if (id != null) {
                                trySend(MessageUpdate.Delete(id))
                            }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            channel.subscribe()
        }
        
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                channel.unsubscribe()
            }
        }
    }

    fun subscribeToReactions(conversationId: String): Flow<MessageReaction> = callbackFlow {
        val channel = realtime.channel("reactions-$conversationId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "message_reactions"
        }
        val job = CoroutineScope(Dispatchers.IO).launch {
            changes.collect { action ->
                try {
                    val record = when (action) {
                        is PostgresAction.Insert -> action.record
                        is PostgresAction.Update -> action.record
                        else -> null
                    }
                    if (record != null) {
                        val reaction = Json.decodeFromJsonElement<MessageReaction>(record)
                        trySend(reaction)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch { channel.unsubscribe() }
        }
    }

    fun subscribeToReads(conversationId: String): Flow<MessageRead> = callbackFlow {
        val channel = realtime.channel("reads-$conversationId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "message_reads"
        }
        val job = CoroutineScope(Dispatchers.IO).launch {
            changes.collect { action ->
                try {
                    val record = when (action) {
                        is PostgresAction.Insert -> action.record
                        else -> null
                    }
                    if (record != null) {
                        val read = Json.decodeFromJsonElement<MessageRead>(record)
                        trySend(read)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch { channel.unsubscribe() }
        }
    }

    fun subscribeToTyping(conversationId: String): Flow<TypingStatus> = callbackFlow {
        val channel = realtime.channel("typing-$conversationId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "typing_status"
        }
        val job = CoroutineScope(Dispatchers.IO).launch {
            changes.collect { action ->
                try {
                    val record = when (action) {
                        is PostgresAction.Insert -> action.record
                        is PostgresAction.Update -> action.record
                        else -> null
                    }
                    if (record != null) {
                        val status = Json.decodeFromJsonElement<TypingStatus>(record)
                        if (status.conversationId == conversationId) {
                            trySend(status)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch { channel.unsubscribe() }
        }
    }

    fun subscribeToPresence(): Flow<UserPresence> = callbackFlow {
        val channel = realtime.channel("presence-global")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "user_presence"
        }
        val job = CoroutineScope(Dispatchers.IO).launch {
            changes.collect { action ->
                try {
                    val record = when (action) {
                        is PostgresAction.Insert -> action.record
                        is PostgresAction.Update -> action.record
                        else -> null
                    }
                    if (record != null) {
                        val presence = Json.decodeFromJsonElement<UserPresence>(record)
                        trySend(presence)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch { channel.unsubscribe() }
        }
    }

    suspend fun addReaction(messageId: String, reaction: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val r = MessageReaction(messageId = messageId, userId = userId, reaction = reaction)
            postgrest["message_reactions"].insert(r)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeReaction(messageId: String, reaction: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            postgrest["message_reactions"].delete {
                filter {
                    eq("messageId", messageId)
                    eq("userId", userId)
                    eq("reaction", reaction)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getReactions(messageId: String): List<MessageReaction> {
        return try {
            postgrest["message_reactions"].select {
                filter { eq("messageId", messageId) }
            }.decodeList<MessageReaction>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setTypingStatus(conversationId: String, status: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val ts = TypingStatus(conversationId = conversationId, userId = userId, status = status)
            postgrest["typing_status"].upsert(ts)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val up = UserPresence(userId = userId, isOnline = isOnline, lastSeen = System.currentTimeMillis())
            postgrest["user_presence"].upsert(up)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markMessageAsRead(messageId: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val mr = MessageRead(messageId = messageId, userId = userId, readAt = System.currentTimeMillis())
            postgrest["message_reads"].insert(mr)
        } catch (e: Exception) {
            // Ignore duplicate reads
        }
    }

    suspend fun getReads(messageId: String): List<MessageRead> {
        return try {
            postgrest["message_reads"].select {
                filter { eq("messageId", messageId) }
            }.decodeList<MessageRead>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun blockUser(blockedUserId: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val bu = BlockedUser(userId = userId, blockedUserId = blockedUserId)
            postgrest["blocked_users"].insert(bu)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun unblockUser(blockedUserId: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            postgrest["blocked_users"].delete {
                filter {
                    eq("userId", userId)
                    eq("blockedUserId", blockedUserId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun isUserBlocked(targetUserId: String): Boolean {
        val userId = auth.currentUserOrNull()?.id ?: return false
        return try {
            val results = postgrest["blocked_users"].select {
                filter {
                    or {
                        and { eq("userId", userId); eq("blockedUserId", targetUserId) }
                        and { eq("userId", targetUserId); eq("blockedUserId", userId) }
                    }
                }
            }.decodeList<BlockedUser>()
            results.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBlockedUsers(): List<BlockedUser> {
        val userId = auth.currentUserOrNull()?.id ?: return emptyList()
        return try {
            postgrest["blocked_users"].select {
                filter { eq("userId", userId) }
            }.decodeList<BlockedUser>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun reportUser(reportedUserId: String, reason: String) {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val rep = Report(reporterId = userId, reportedUserId = reportedUserId, reason = reason)
            postgrest["reports"].insert(rep)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMessage(messageId: String, forEveryone: Boolean) {
        try {
            if (forEveryone) {
                postgrest["messages"].update({
                    set("isDeleted", true)
                    set("text", "This message was deleted")
                }) {
                    filter { eq("id", messageId) }
                }
            } else {
                postgrest["messages"].delete {
                    filter { eq("id", messageId) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun editMessage(messageId: String, newText: String) {
        try {
            postgrest["messages"].update({
                set("text", newText)
                set("updatedAt", System.currentTimeMillis())
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uploadAttachment(fileName: String, byteArray: ByteArray): String {
        return try {
            val path = "attachments/$fileName"
            client.storage["chat_attachments"].upload(path, byteArray) {
                upsert = true
            }
            client.storage["chat_attachments"].publicUrl(path)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

