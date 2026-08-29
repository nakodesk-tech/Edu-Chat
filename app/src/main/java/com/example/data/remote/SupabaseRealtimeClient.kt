package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Supabase Realtime Client using official io.github.jan-tennert.supabase:realtime-kt SDK.
 * Subscribes to postgres_changes on public.messages for a specific group_id
 * using the authenticated user's JWT.
 */
class SupabaseRealtimeClient(
    private val context: Context,
    private val accessToken: String,
    private val onMessageReceived: (ChatMessage) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) {
    companion object {
        private const val TAG = "SupabaseRealtime"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var subscriptionJob: Job? = null
    private var activeChannel: RealtimeChannel? = null

    /**
     * Start subscription for a specific group's messages using official Realtime SDK.
     */
    fun subscribeToGroupMessages(groupId: String) {
        if (groupId.isBlank()) return

        val rawUrl = SupabaseConfig.getSupabaseUrl(context)
        val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

        if (rawUrl.isBlank() || anonKey.isBlank() || accessToken.isBlank()) {
            Log.w(TAG, "Cannot connect to Realtime: missing URL, anonKey, or accessToken")
            return
        }

        // Validate that accessToken is a real JWT (3 non-empty dot-separated segments)
        if (!isValidJwt(accessToken)) {
            Log.w(TAG, "Cannot connect to Realtime: access token is not a valid JWT")
            return
        }

        val formattedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }.trimEnd('/')

        subscriptionJob?.cancel()
        subscriptionJob = scope.launch {
            try {
                val client = createSupabaseClient(
                    supabaseUrl = formattedUrl,
                    supabaseKey = anonKey
                ) {
                    install(Realtime)
                }

                // Connect to realtime with user JWT
                client.realtime.setAuth(accessToken)
                client.realtime.connect()

                val channelTopic = "messages-$groupId"
                val channel = client.realtime.channel(channelTopic)
                activeChannel = channel

                val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                }

                channel.subscribe()
                Log.d(TAG, "Subscribed to official Realtime channel: $channelTopic for group $groupId")

                insertFlow.collect { action ->
                    try {
                        val record: JsonObject = action.record
                        val id = record["id"]?.jsonPrimitive?.content ?: ""
                        val msgGroupId = record["group_id"]?.jsonPrimitive?.content ?: ""
                        val senderId = record["sender_id"]?.jsonPrimitive?.content ?: ""
                        val content = record["content"]?.jsonPrimitive?.content ?: ""
                        val createdAt = record["created_at"]?.jsonPrimitive?.content
                        val updatedAt = record["updated_at"]?.jsonPrimitive?.content
                        val isDeleted = record["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false

                        if (id.isNotBlank() && content.isNotBlank() && !isDeleted && msgGroupId == groupId) {
                            val chatMessage = ChatMessage(
                                id = id,
                                groupId = msgGroupId,
                                senderId = senderId,
                                content = content,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                isDeleted = isDeleted,
                                senderProfile = null
                            )
                            Log.d(TAG, "Realtime SDK received INSERT: id=$id, group=$msgGroupId")
                            onMessageReceived(chatMessage)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing Realtime PostgresAction.Insert: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error in official Realtime subscription", e)
                onError(e)
            }
        }
    }

    private fun isValidJwt(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isBlank() || trimmed.startsWith("mock_", ignoreCase = true)) {
            return false
        }
        val parts = trimmed.split(".")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }

    /**
     * Cleanly leave channel, unsubscribe, and close scope.
     */
    fun disconnect() {
        subscriptionJob?.cancel()
        scope.launch {
            try {
                activeChannel?.unsubscribe()
            } catch (_: Exception) {
            }
            try {
                scope.cancel()
            } catch (_: Exception) {
            }
        }
    }
}
