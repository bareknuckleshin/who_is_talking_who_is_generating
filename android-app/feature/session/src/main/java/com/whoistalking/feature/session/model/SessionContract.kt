package com.whoistalking.feature.session.model

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import com.whoistalking.core.domain.model.SessionMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Immutable
data class SessionUiState(
    val isLoading: Boolean = false,
    val sessionId: String = DEFAULT_SESSION_ID,
    val draft: String = "",
    val errorMessage: String? = null,
    val messages: Flow<PagingData<SessionMessage>> = emptyFlow(),
)

sealed interface SessionIntent {
    data class ChangeDraft(val value: String) : SessionIntent
    data object SubmitMessage : SessionIntent
    data object Retry : SessionIntent
}

sealed interface SessionSideEffect {
    data class ShowToast(val message: String) : SessionSideEffect
}

const val DEFAULT_SESSION_ID = "default-session"
