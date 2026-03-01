package com.whoistalking.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

abstract class NetworkBoundResource<Result, Request> {
    fun asFlow(): Flow<Result> = flow {
        val localData = query().first()
        emit(localData)
        try {
            val remote = fetch()
            saveFetchResult(remote)
            emit(query().first())
        } catch (_: Exception) {
            emit(query().first())
        }
    }

    protected abstract fun query(): Flow<Result>
    protected abstract suspend fun fetch(): Request
    protected abstract suspend fun saveFetchResult(item: Request)
}
