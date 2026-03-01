package com.whoistalking.core.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.whoistalking.core.data.db.AppDatabase
import com.whoistalking.core.data.mapper.toDomain
import com.whoistalking.core.data.network.SendMessageRequest
import com.whoistalking.core.data.network.SessionApi
import com.whoistalking.core.data.paging.MessageRemoteMediator
import com.whoistalking.core.domain.model.SessionMessage
import com.whoistalking.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val api: SessionApi,
    private val db: AppDatabase,
) : SessionRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getSessionMessages(sessionId: String): Flow<PagingData<SessionMessage>> = Pager(
        config = PagingConfig(pageSize = 20),
        remoteMediator = MessageRemoteMediator(sessionId = sessionId, api = api, db = db),
        pagingSourceFactory = { db.messageDao().pagingSource(sessionId) },
    ).flow.map { paging -> paging.map { it.toDomain() } }

    override suspend fun refreshSession(sessionId: String) {
        api.getMessages(sessionId = sessionId, cursor = null)
    }

    override suspend fun sendMessage(sessionId: String, prompt: String) {
        api.send(sessionId = sessionId, request = SendMessageRequest(prompt = prompt))
    }
}
