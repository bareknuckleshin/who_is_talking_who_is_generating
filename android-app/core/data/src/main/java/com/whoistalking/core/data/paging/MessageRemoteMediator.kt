package com.whoistalking.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.whoistalking.core.data.db.AppDatabase
import com.whoistalking.core.data.db.MessageEntity
import com.whoistalking.core.data.db.RemoteKeyEntity
import com.whoistalking.core.data.mapper.toEntity
import com.whoistalking.core.data.network.SessionApi

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val sessionId: String,
    private val api: SessionApi,
    private val db: AppDatabase,
) : RemoteMediator<Int, MessageEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>,
    ): MediatorResult {
        return try {
            val cursor = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> db.remoteKeyDao().get(sessionId)?.nextCursor
            }

            val page = api.getMessages(sessionId = sessionId, cursor = cursor)
            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.messageDao().clearSession(sessionId)
                    db.remoteKeyDao().clear(sessionId)
                }
                db.messageDao().upsertAll(page.items.map { it.toEntity(sessionId) })
                db.remoteKeyDao().upsert(RemoteKeyEntity(sessionId = sessionId, nextCursor = page.nextCursor))
            }
            MediatorResult.Success(endOfPaginationReached = page.nextCursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
