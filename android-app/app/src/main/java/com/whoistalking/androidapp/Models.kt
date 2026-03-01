package com.whoistalking.androidapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CreateSessionRequest(
    val topic: String,
    @SerialName("num_llm_speakers") val numLlmSpeakers: Int,
    @SerialName("turns_per_speaker") val turnsPerSpeaker: Int,
    @SerialName("max_chars") val maxChars: Int,
    val language: String,
    val difficulty: String,
)

@Serializable
data class CreateSessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("ws_url") val wsUrl: String,
)

data class ChatMessage(
    val messageId: String,
    val turnIndex: Int,
    val seat: String,
    val text: String,
)

enum class SessionStatus {
    LOBBY, IN_PROGRESS, JUDGING, FINISHED
}

data class JudgeResult(
    val pickSeat: String,
    val confidence: Double,
    val why: String,
)

data class SessionUiState(
    val sessionId: String = "",
    val topic: String = "",
    val status: SessionStatus = SessionStatus.LOBBY,
    val participants: List<String> = emptyList(),
    val turnCounts: Map<String, Int> = emptyMap(),
    val currentSpeakerSeat: String? = null,
    val maxChars: Int = 160,
    val messages: List<ChatMessage> = emptyList(),
    val typingSeats: Set<String> = emptySet(),
    val input: String = "",
    val countdownSecs: Int? = null,
    val reconnecting: Boolean = false,
    val connectionError: String? = null,
    val judgeResult: JudgeResult? = null,
)

sealed interface WsInboundEvent {
    data class SessionState(
        val topic: String,
        val status: String,
        val participants: List<String>,
        val turnCounts: Map<String, Int>,
        val currentSpeakerSeat: String?,
        val maxChars: Int,
    ) : WsInboundEvent

    data class TurnRequestHuman(val timeoutSecs: Int?, val currentSpeakerSeat: String?) : WsInboundEvent
    data class MessageTyping(val seat: String) : WsInboundEvent
    data class MessageNew(val messageId: String, val turnIndex: Int, val seat: String, val text: String, val sequenceId: Int?) : WsInboundEvent
    data class MessageDelta(val messageId: String, val turnIndex: Int, val seat: String, val delta: String, val sequenceId: Int?) : WsInboundEvent
    data class TurnNext(val currentSpeakerSeat: String?, val turnCounts: Map<String, Int>?, val sequenceId: Int?) : WsInboundEvent
    data class SessionFinished(val pickSeat: String, val confidence: Double, val why: String, val sequenceId: Int?) : WsInboundEvent
    data class Unknown(val payload: JsonObject) : WsInboundEvent
}
