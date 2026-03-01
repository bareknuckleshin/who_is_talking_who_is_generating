package com.whoistalking.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val sessionId: String,
    val nextCursor: Int?,
)
