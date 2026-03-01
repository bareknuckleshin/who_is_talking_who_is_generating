package com.whoistalking.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val speaker: String,
    val content: String,
    val createdAt: Long,
)
