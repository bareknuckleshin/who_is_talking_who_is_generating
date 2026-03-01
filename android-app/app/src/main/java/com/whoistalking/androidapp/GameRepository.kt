package com.whoistalking.androidapp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GameRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build()

    suspend fun createSession(request: CreateSessionRequest): CreateSessionResponse {
        val body = json.encodeToString(CreateSessionRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url("${Config.API_BASE_URL}/sessions")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(httpReq).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(response.body?.string() ?: "HTTP ${response.code}")
            }
            val payload = response.body?.string() ?: throw IllegalStateException("Empty response")
            return json.decodeFromString(CreateSessionResponse.serializer(), payload)
        }
    }

    fun openSessionSocket(
        wsPath: String,
        onConnection: (connected: Boolean, reconnecting: Boolean, error: String?) -> Unit,
        onEvent: (WsInboundEvent) -> Unit,
    ): SessionSocket {
        return SessionSocket(
            client = client,
            json = json,
            wsUrl = toWsBaseUrl(Config.API_BASE_URL) + wsPath,
            onConnection = onConnection,
            onEvent = onEvent,
        )
    }

    private fun toWsBaseUrl(apiBaseUrl: String): String =
        if (apiBaseUrl.startsWith("https://")) apiBaseUrl.replace("https://", "wss://")
        else apiBaseUrl.replace("http://", "ws://")
}

class SessionSocket(
    private val client: OkHttpClient,
    private val json: Json,
    private val wsUrl: String,
    private val onConnection: (connected: Boolean, reconnecting: Boolean, error: String?) -> Unit,
    private val onEvent: (WsInboundEvent) -> Unit,
) {
    private val backoffMs = listOf(1000L, 2000L, 5000L, 10000L, 30000L)
    private val reconnectScheduler = Executors.newSingleThreadScheduledExecutor()
    private var retryIndex = 0
    private var userClosed = false
    private var webSocket: WebSocket? = null

    fun connect(joinPayload: JsonObject) {
        userClosed = false
        open(joinPayload)
    }

    fun send(jsonPayload: JsonObject) {
        webSocket?.send(json.encodeToString(JsonObject.serializer(), jsonPayload))
    }

    fun close() {
        userClosed = true
        reconnectScheduler.shutdownNow()
        webSocket?.close(1000, "Closed by user")
        webSocket = null
    }

    private fun open(joinPayload: JsonObject) {
        onConnection(false, retryIndex > 0, null)
        val req = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retryIndex = 0
                onConnection(true, false, null)
                send(joinPayload)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
                onEvent(parseInbound(obj))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (userClosed) {
                    onConnection(false, false, null)
                } else {
                    onConnection(false, true, t.message ?: "WebSocket error")
                    reconnect(joinPayload)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (userClosed) {
                    onConnection(false, false, null)
                } else {
                    reconnect(joinPayload)
                }
            }
        })
    }

    private fun reconnect(joinPayload: JsonObject) {
        val idx = retryIndex.coerceAtMost(backoffMs.lastIndex)
        val delay = backoffMs[idx]
        onConnection(false, true, "재연결 대기: ${delay / 1000}초")
        retryIndex += 1

        reconnectScheduler.schedule({
            if (!userClosed) {
                open(joinPayload)
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun parseInbound(obj: JsonObject): WsInboundEvent {
        return when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "session.state" -> WsInboundEvent.SessionState(
                topic = obj["topic"]?.jsonPrimitive?.contentOrNull ?: "",
                status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "LOBBY",
                participants = obj["participants"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["seat"]?.jsonPrimitive?.contentOrNull }
                    ?: emptyList(),
                turnCounts = obj["turn_counts"]?.jsonObject
                    ?.mapValues { it.value.jsonPrimitive.intOrNull ?: 0 }
                    ?: emptyMap(),
                currentSpeakerSeat = obj["current_speaker_seat"]?.jsonPrimitive?.contentOrNull,
                maxChars = obj["max_chars"]?.jsonPrimitive?.intOrNull ?: 160,
            )

            "turn.request_human" -> WsInboundEvent.TurnRequestHuman(
                timeoutSecs = obj["timeout_secs"]?.jsonPrimitive?.intOrNull,
                currentSpeakerSeat = obj["current_speaker_seat"]?.jsonPrimitive?.contentOrNull,
            )

            "message.typing" -> WsInboundEvent.MessageTyping(
                seat = obj["seat"]?.jsonPrimitive?.contentOrNull ?: "",
            )

            "message.new" -> WsInboundEvent.MessageNew(
                messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: "",
                turnIndex = obj["turn_index"]?.jsonPrimitive?.intOrNull ?: 0,
                seat = obj["seat"]?.jsonPrimitive?.contentOrNull ?: "",
                text = obj["text"]?.jsonPrimitive?.contentOrNull ?: "",
                sequenceId = obj["sequence_id"]?.jsonPrimitive?.intOrNull,
            )

            "message.delta" -> WsInboundEvent.MessageDelta(
                messageId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: "",
                turnIndex = obj["turn_index"]?.jsonPrimitive?.intOrNull ?: 0,
                seat = obj["seat"]?.jsonPrimitive?.contentOrNull ?: "",
                delta = obj["delta"]?.jsonPrimitive?.contentOrNull ?: "",
                sequenceId = obj["sequence_id"]?.jsonPrimitive?.intOrNull,
            )

            "turn.next" -> WsInboundEvent.TurnNext(
                currentSpeakerSeat = obj["current_speaker_seat"]?.jsonPrimitive?.contentOrNull,
                turnCounts = obj["turn_counts"]?.jsonObject?.mapValues { it.value.jsonPrimitive.intOrNull ?: 0 },
                sequenceId = obj["sequence_id"]?.jsonPrimitive?.intOrNull,
            )

            "session.finished" -> WsInboundEvent.SessionFinished(
                pickSeat = obj["pick_seat"]?.jsonPrimitive?.contentOrNull ?: "",
                confidence = obj["confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                why = obj["why"]?.jsonPrimitive?.contentOrNull ?: "",
                sequenceId = obj["sequence_id"]?.jsonPrimitive?.intOrNull,
            )

            else -> WsInboundEvent.Unknown(obj)
        }
    }
}
