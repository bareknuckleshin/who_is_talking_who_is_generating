package com.whoistalking.core.domain.repository

import androidx.paging.PagingData
import com.whoistalking.core.domain.model.SessionMessage
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessionMessages(sessionId: String): Flow<PagingData<SessionMessage>>
    suspend fun refreshSession(sessionId: String)
    suspend fun sendMessage(sessionId: String, prompt: String)
}
