package com.whoistalking.androidapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface ScreenState {
    data object Home : ScreenState
    data object Session : ScreenState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = GameRepository()
    private val store = LocalStore(app)
    private var sessionSocket: SessionSocket? = null
    private var countdownJob: Job? = null

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Home)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionUiState())
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    fun updateTopic(topic: String) = _homeState.update { it.copy(topic = topic) }
    fun updateNumSpeakers(value: Int) = _homeState.update { it.copy(numLlmSpeakers = value.coerceIn(1, 5)) }
    fun updateTurns(value: Int) = _homeState.update { it.copy(turnsPerSpeaker = value.coerceIn(1, 10)) }
    fun updateInput(value: String) = _sessionState.update { it.copy(input = value) }

    fun createSession() {
        val home = _homeState.value
        if (home.topic.trim().isEmpty()) {
            _homeState.update { it.copy(error = "Topic은 필수입니다.") }
            return
        }

        viewModelScope.launch {
            _homeState.update { it.copy(loading = true, error = null) }
            runCatching {
                repository.createSession(
                    CreateSessionRequest(
                        topic = home.topic.trim(),
                        numLlmSpeakers = home.numLlmSpeakers,
                        turnsPerSpeaker = home.turnsPerSpeaker,
                        maxChars = 160,
                        language = "ko",
                        difficulty = "normal",
                    )
                )
            }.onSuccess { response ->
                _sessionState.value = SessionUiState(sessionId = response.sessionId)
                openSession(response.sessionId, response.wsUrl)
                _screenState.value = ScreenState.Session
                _homeState.update { it.copy(loading = false) }
            }.onFailure { err ->
                _homeState.update { it.copy(loading = false, error = err.message ?: "세션 생성 실패") }
            }
        }
    }

    fun sendHumanMessage() {
        val ui = _sessionState.value
        val text = ui.input.trim()
        if (text.isEmpty() || ui.currentSpeakerSeat != Config.HUMAN_SEAT) return
        sessionSocket?.send(
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("human.message"),
                    "client_id" to JsonPrimitive(store.getOrCreateClientId()),
                    "text" to JsonPrimitive(text),
                )
            )
        )
        _sessionState.update { it.copy(input = "") }
    }

    fun leaveSession() {
        countdownJob?.cancel()
        sessionSocket?.close()
        _screenState.value = ScreenState.Home
    }

    private fun openSession(sessionId: String, wsPath: String) {
        sessionSocket?.close()

        val joinPayload = store.getLastSeenMessageId(sessionId)?.let { lastSeen ->
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("session.resume"),
                    "client_id" to JsonPrimitive(store.getOrCreateClientId()),
                    "last_seen_message_id" to JsonPrimitive(lastSeen),
                )
            )
        } ?: JsonObject(
            mapOf(
                "type" to JsonPrimitive("session.join"),
                "client_id" to JsonPrimitive(store.getOrCreateClientId()),
            )
        )

        sessionSocket = repository.openSessionSocket(
            wsPath = wsPath,
            onConnection = { _, reconnecting, error ->
                _sessionState.update { it.copy(reconnecting = reconnecting, connectionError = error) }
            },
            onEvent = { event -> handleEvent(sessionId, event) }
        )

        sessionSocket?.connect(joinPayload)
    }

    private fun startCountdown(secs: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var left = secs
            while (left >= 0) {
                _sessionState.update { it.copy(countdownSecs = left) }
                delay(1000)
                left -= 1
            }
        }
    }

    private fun handleEvent(sessionId: String, event: WsInboundEvent) {
        when (event) {
            is WsInboundEvent.SessionState -> _sessionState.update {
                it.copy(
                    topic = event.topic,
                    status = runCatching { SessionStatus.valueOf(event.status) }.getOrDefault(SessionStatus.LOBBY),
                    participants = event.participants,
                    turnCounts = event.turnCounts,
                    currentSpeakerSeat = event.currentSpeakerSeat,
                    maxChars = event.maxChars,
                )
            }

            is WsInboundEvent.TurnRequestHuman -> event.timeoutSecs?.let(::startCountdown)

            is WsInboundEvent.MessageTyping -> {
                if (event.seat == Config.HUMAN_SEAT) return
                _sessionState.update { it.copy(typingSeats = it.typingSeats + event.seat) }
            }

            is WsInboundEvent.MessageNew -> _sessionState.update { ui ->
                if (ui.messages.any { it.messageId == event.messageId }) return@update ui
                store.setLastSeenMessageId(sessionId, event.messageId)
                ui.copy(
                    messages = ui.messages + ChatMessage(event.messageId, event.turnIndex, event.seat, event.text),
                    typingSeats = ui.typingSeats - event.seat,
                )
            }

            is WsInboundEvent.TurnNext -> _sessionState.update {
                it.copy(
                    currentSpeakerSeat = event.currentSpeakerSeat,
                    turnCounts = event.turnCounts ?: it.turnCounts,
                )
            }

            is WsInboundEvent.SessionFinished -> _sessionState.update {
                it.copy(
                    status = SessionStatus.FINISHED,
                    judgeResult = JudgeResult(event.pickSeat, event.confidence, event.why),
                )
            }

            is WsInboundEvent.Unknown -> Unit
        }
    }
}

data class HomeUiState(
    val topic: String = "",
    val numLlmSpeakers: Int = 1,
    val turnsPerSpeaker: Int = 5,
    val loading: Boolean = false,
    val error: String? = null,
)
