package com.whoistalking.core.data.mapper

import com.whoistalking.core.data.db.MessageEntity
import com.whoistalking.core.data.network.MessageDto
import com.whoistalking.core.domain.model.SessionMessage

fun MessageDto.toEntity(sessionId: String): MessageEntity = MessageEntity(
    id = id,
    sessionId = sessionId,
    speaker = speaker,
    content = content,
    createdAt = createdAt,
)

fun MessageEntity.toDomain(): SessionMessage = SessionMessage(
    id = id,
    speaker = speaker,
    content = content,
    createdAt = createdAt,
)
