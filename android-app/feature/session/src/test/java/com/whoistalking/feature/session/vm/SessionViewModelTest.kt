package com.whoistalking.feature.session.vm

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.whoistalking.core.domain.repository.SessionRepository
import com.whoistalking.core.domain.usecase.ObserveSessionMessagesUseCase
import com.whoistalking.core.domain.usecase.SendSessionMessageUseCase
import com.whoistalking.feature.session.model.SessionIntent
import com.whoistalking.feature.session.model.SessionSideEffect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank draft emits toast side effect`() = runTest {
        val repository = mockk<SessionRepository>()
        coEvery { repository.getSessionMessages(any()) } returns emptyFlow()
        val vm = SessionViewModel(
            observeSessionMessages = ObserveSessionMessagesUseCase(repository),
            sendSessionMessage = SendSessionMessageUseCase(repository),
            savedStateHandle = SavedStateHandle(),
        )

        vm.sideEffects.test {
            vm.onIntent(SessionIntent.SubmitMessage)
            advanceUntilIdle()
            assert(awaitItem() is SessionSideEffect.ShowToast)
        }
    }

    @Test
    fun `submit sends message and clears draft`() = runTest {
        val repository = mockk<SessionRepository>()
        coEvery { repository.getSessionMessages(any()) } returns emptyFlow()
        coEvery { repository.sendMessage(any(), any()) } returns Unit

        val vm = SessionViewModel(
            observeSessionMessages = ObserveSessionMessagesUseCase(repository),
            sendSessionMessage = SendSessionMessageUseCase(repository),
            savedStateHandle = SavedStateHandle(mapOf("draft" to "hello")),
        )

        vm.onIntent(SessionIntent.SubmitMessage)
        advanceUntilIdle()

        coVerify { repository.sendMessage("default-session", "hello") }
        assert(vm.uiState.value.draft.isEmpty())
    }
}
