package com.whoistalking.core.domain.usecase

import com.whoistalking.core.domain.repository.SessionRepository
import javax.inject.Inject

class SendSessionMessageUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String, prompt: String) {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
        repository.sendMessage(sessionId = sessionId, prompt = prompt)
    }
}
