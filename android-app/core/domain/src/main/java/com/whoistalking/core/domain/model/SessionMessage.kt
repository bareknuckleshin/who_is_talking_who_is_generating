package com.whoistalking.core.domain.model

import androidx.annotation.Keep

@Keep
data class SessionMessage(
    val id: String,
    val speaker: String,
    val content: String,
    val createdAt: Long,
)
