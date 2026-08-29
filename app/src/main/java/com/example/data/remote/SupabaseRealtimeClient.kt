package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight Supabase Realtime Phoenix WebSocket Client.
 * Connects to Supabase Realtime Gateway and subscribes to postgres_changes
 * on public.messages for a specific group_id using the authenticated user's JWT.
 */
class SupabaseRealtimeClient(
    private val context: Context,
    private val accessToken: String,
    private val onMessageReceived: (ChatMessage) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) {
    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val HEARTBEAT_INTERVAL_MS = 25000L
        private const val MAX_RECONNECT_DELAY_MS = 10000L
        private const val INITIAL_RECONNECT_DELAY_MS = 2000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refCounter = AtomicInteger(1)
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    
    @Volatile
    private var isClosed = false
    
    @Volatile
    private var isConnected = false
    
    private var currentGroupId: String? = null
    private var reconnectAttempts = 0

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read for WebSocket
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS) // OkHttp-level ping
        .build()

    private fun nextRef(): String = refCounter.getAndIncrement().toString()

    /**
     * Start subscription for a specific group's messages.
     */
    fun subscribeToGroupMessages(groupId: String) {
        if (groupId.isBlank()) return
        currentGroupId = groupId
        isClosed = false
        connect()
    }

    private fun connect() {
        if (isClosed) return

        val rawUrl = SupabaseConfig.getSupabaseUrl(context)
        val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

        if (rawUrl.isBlank() || anonKey.isBlank() || accessToken.isBlank()) {
            Log.w(TAG, "Cannot connect to Realtime: missing URL, anonKey, or accessToken")
            return
        }

        val wsUrl = rawUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            webSocket?.cancel()
            webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating WebSocket connection", e)
            onError(e)
            scheduleReconnect()
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to Supabase Realtime")
                isConnected = true
                reconnectAttempts = 0
                
                // Start heartbeat loop
                startHeartbeat()

                // Join Postgres Changes channel for current group
                val groupId = currentGroupId
                if (!groupId.isNullOrBlank()) {
                    sendPhoenixJoin(groupId)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                isConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                isConnected = false
                if (!isClosed) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                isConnected = false
                onError(t)
                if (!isClosed) {
                    scheduleReconnect()
                }
            }
        }
    }

    private fun sendPhoenixJoin(groupId: String) {
        val topic = "realtime:public:messages"
        val ref = nextRef()

        try {
            val postgresChangesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("event", "INSERT")
                    put("schema", "public")
                    put("table", "messages")
                    put("filter", "group_id=eq.$groupId")
                })
            }

            val configObj = JSONObject().apply {
                put("postgres_changes", postgresChangesArray)
            }

            val payloadObj = JSONObject().apply {
                put("config", configObj)
                put("access_token", accessToken)
            }

            val joinMessage = JSONObject().apply {
                put("topic", topic)
                put("event", "phx_join")
                put("payload", payloadObj)
                put("ref", ref)
                put("join_ref", ref)
            }

            webSocket?.send(joinMessage.toString())
            Log.d(TAG, "Sent phx_join for topic $topic, group_id=$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending phx_join", e)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && !isClosed) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (isConnected && !isClosed) {
                    try {
                        val heartbeat = JSONObject().apply {
                            put("topic", "phoenix")
                            put("event", "heartbeat")
                            put("payload", JSONObject())
                            put("ref", nextRef())
                        }
                        webSocket?.send(heartbeat.toString())
                    } catch (e: Exception) {
                        Log.w(TAG, "Error sending Phoenix heartbeat: ${e.message}")
                    }
                }
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            val payload = json.optJSONObject("payload") ?: JSONObject()

            when (event) {
                "postgres_changes" -> {
                    val record = extractRecord(payload)
                    if (record != null) {
                        val id = record.optString("id", "")
                        val groupId = record.optString("group_id", "")
                        val senderId = record.optString("sender_id", "")
                        val content = record.optString("content", "")
                        val createdAt = record.optString("created_at", "").takeIf { it.isNotBlank() }
                        val updatedAt = record.optString("updated_at", "").takeIf { it.isNotBlank() }
                        val isDeleted = record.optBoolean("is_deleted", false)

                        val targetGroupId = currentGroupId
                        if (id.isNotBlank() && content.isNotBlank() && !isDeleted &&
                            (targetGroupId == null || groupId == targetGroupId)
                        ) {
                            val chatMessage = ChatMessage(
                                id = id,
                                groupId = groupId,
                                senderId = senderId,
                                content = content,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                isDeleted = isDeleted,
                                senderProfile = null
                            )
                            onMessageReceived(chatMessage)
                        }
                    }
                }
                "phx_reply" -> {
                    val status = payload.optString("status")
                    Log.d(TAG, "Phoenix reply status: $status")
                }
                "phx_error" -> {
                    Log.w(TAG, "Phoenix channel error received: $payload")
                }
                "phx_close" -> {
                    Log.d(TAG, "Phoenix channel closed")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing incoming Realtime message: ${e.message}")
        }
    }

    private fun extractRecord(payload: JSONObject): JSONObject? {
        val dataObj = payload.optJSONObject("data")
        if (dataObj != null) {
            val rec = dataObj.optJSONObject("record") ?: dataObj.optJSONObject("new")
            if (rec != null) return rec
        }
        val record = payload.optJSONObject("record")
        if (record != null) return record
        val newRecord = payload.optJSONObject("new")
        if (newRecord != null) return newRecord
        if (payload.has("id") && payload.has("content")) return payload
        return null
    }

    private fun scheduleReconnect() {
        if (isClosed) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = (INITIAL_RECONNECT_DELAY_MS * (1 shl reconnectAttempts.coerceAtMost(3)))
                .coerceAtMost(MAX_RECONNECT_DELAY_MS)
            reconnectAttempts++
            Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${delayMs}ms")
            delay(delayMs)
            if (!isClosed && isActive) {
                connect()
            }
        }
    }

    /**
     * Cleanly leave channel, stop heartbeats, and close WebSocket.
     */
    fun disconnect() {
        isClosed = true
        isConnected = false
        reconnectJob?.cancel()
        heartbeatJob?.cancel()

        scope.launch {
            try {
                val leaveMsg = JSONObject().apply {
                    put("topic", "realtime:public:messages")
                    put("event", "phx_leave")
                    put("payload", JSONObject())
                    put("ref", nextRef())
                }
                webSocket?.send(leaveMsg.toString())
            } catch (_: Exception) {
            }

            try {
                webSocket?.close(1000, "Client closed")
            } catch (_: Exception) {
            }

            try {
                scope.cancel()
            } catch (_: Exception) {
            }
        }
    }
}
