package com.whoistalking.core.domain.usecase

import androidx.paging.PagingData
import com.whoistalking.core.domain.model.SessionMessage
import com.whoistalking.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionMessagesUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    operator fun invoke(sessionId: String): Flow<PagingData<SessionMessage>> = repository.getSessionMessages(sessionId)
}
