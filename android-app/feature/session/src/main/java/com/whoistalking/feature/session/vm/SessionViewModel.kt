package com.whoistalking.feature.session.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.whoistalking.core.domain.usecase.ObserveSessionMessagesUseCase
import com.whoistalking.core.domain.usecase.SendSessionMessageUseCase
import com.whoistalking.feature.session.model.DEFAULT_SESSION_ID
import com.whoistalking.feature.session.model.SessionIntent
import com.whoistalking.feature.session.model.SessionSideEffect
import com.whoistalking.feature.session.model.SessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val observeSessionMessages: ObserveSessionMessagesUseCase,
    private val sendSessionMessage: SendSessionMessageUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle[KEY_SESSION_ID] ?: DEFAULT_SESSION_ID

    private val _uiState = MutableStateFlow(
        SessionUiState(
            isLoading = false,
            sessionId = sessionId,
            messages = observeSessionMessages(sessionId).cachedIn(viewModelScope),
            draft = savedStateHandle[KEY_DRAFT] ?: "",
        ),
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val sideEffectChannel = Channel<SessionSideEffect>(Channel.BUFFERED)
    val sideEffects = sideEffectChannel.receiveAsFlow()

    fun onIntent(intent: SessionIntent) {
        when (intent) {
            is SessionIntent.ChangeDraft -> {
                savedStateHandle[KEY_DRAFT] = intent.value
                _uiState.update { it.copy(draft = intent.value) }
            }
            SessionIntent.SubmitMessage -> submitMessage()
            SessionIntent.Retry -> submitMessage()
        }
    }

    private fun submitMessage() {
        val draft = uiState.value.draft.trim()
        if (draft.isBlank()) {
            viewModelScope.launch { sideEffectChannel.send(SessionSideEffect.ShowToast("메시지를 입력하세요.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                sendSessionMessage(sessionId = sessionId, prompt = draft)
            }.onSuccess {
                savedStateHandle[KEY_DRAFT] = ""
                _uiState.update { it.copy(isLoading = false, draft = "") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "오류") }
                sideEffectChannel.send(SessionSideEffect.ShowToast("전송 실패: ${throwable.message}"))
            }
        }
    }

    companion object {
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_DRAFT = "draft"
    }
}
