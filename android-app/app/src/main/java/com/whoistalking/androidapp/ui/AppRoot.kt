package com.whoistalking.androidapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whoistalking.androidapp.Config
import com.whoistalking.androidapp.HomeUiState
import com.whoistalking.androidapp.MainViewModel
import com.whoistalking.androidapp.ScreenState
import com.whoistalking.androidapp.SessionUiState

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val screen by viewModel.screenState.collectAsState()
    when (screen) {
        ScreenState.Home -> HomeScreen(
            state = viewModel.homeState.collectAsState().value,
            onTopicChange = viewModel::updateTopic,
            onNumSpeakersChange = viewModel::updateNumSpeakers,
            onTurnsChange = viewModel::updateTurns,
            onCreate = viewModel::createSession,
        )

        ScreenState.Session -> SessionScreen(
            state = viewModel.sessionState.collectAsState().value,
            onInputChange = viewModel::updateInput,
            onSend = viewModel::sendHumanMessage,
            onLeave = viewModel::leaveSession,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    onTopicChange: (String) -> Unit,
    onNumSpeakersChange: (Int) -> Unit,
    onTurnsChange: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Who-is-Human (Native Android)") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.topic,
                onValueChange = onTopicChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Topic") },
                enabled = !state.loading,
            )
            OutlinedTextField(
                value = state.numLlmSpeakers.toString(),
                onValueChange = { onNumSpeakersChange(it.toIntOrNull() ?: 1) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("num_llm_speakers (1~5)") },
                enabled = !state.loading,
            )
            OutlinedTextField(
                value = state.turnsPerSpeaker.toString(),
                onValueChange = { onTurnsChange(it.toIntOrNull() ?: 5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("turns_per_speaker (1~10)") },
                enabled = !state.loading,
            )
            Text("max_chars: 160 (fixed)")
            Button(onClick = onCreate, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.loading) "Creating..." else "Create")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreen(
    state: SessionUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onLeave: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.judgeResult) {
        state.judgeResult?.let {
            snackbar.showSnackbar("판정 결과: ${it.pickSeat} (${(it.confidence * 100).toInt()}%)")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.topic.ifBlank { "Session ${state.sessionId}" }) },
                actions = { Button(onClick = onLeave) { Text("나가기") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(state.reconnecting) {
                Banner("재연결 중...", Color(0xFFFFF3CD))
            }
            state.connectionError?.let {
                Banner(it, Color(0xFFF8D7DA))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { message ->
                    val mine = message.seat == Config.HUMAN_SEAT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${message.seat} · Turn ${message.turnIndex}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(message.text)
                            }
                        }
                    }
                }
                item {
                    if (state.typingSeats.isNotEmpty()) {
                        Text("${state.typingSeats.joinToString()} 입력 중...", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            val isMyTurn = state.currentSpeakerSeat == Config.HUMAN_SEAT
            val turnText = if (isMyTurn) "메시지 입력" else "상대 발화 중..."

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("상태: ${state.status} · 현재 턴: ${state.currentSpeakerSeat ?: "-"}")
                state.countdownSecs?.let { Text("남은 시간: ${it}s") }
                OutlinedTextField(
                    value = state.input,
                    onValueChange = { if (it.length <= state.maxChars) onInputChange(it) },
                    enabled = isMyTurn,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(turnText) },
                )
                Text("${state.input.length}/${state.maxChars}")
                Button(onClick = onSend, enabled = isMyTurn && state.input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("전송")
                }
            }
        }
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color.Black,
    )
}
