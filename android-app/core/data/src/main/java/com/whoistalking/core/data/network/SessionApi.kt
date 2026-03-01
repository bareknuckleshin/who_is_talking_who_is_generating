package com.whoistalking.core.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class MessageDto(
    val id: String,
    val speaker: String,
    val content: String,
    val createdAt: Long,
)

data class MessagePageDto(
    val items: List<MessageDto>,
    val nextCursor: Int?,
)

data class SendMessageRequest(
    val prompt: String,
)

interface SessionApi {
    @GET("sessions/messages")
    suspend fun getMessages(
        @Query("sessionId") sessionId: String,
        @Query("cursor") cursor: Int?,
    ): MessagePageDto

    @POST("sessions/send")
    suspend fun send(
        @Query("sessionId") sessionId: String,
        @Body request: SendMessageRequest,
    )
}
