package com.whoistalking.feature.session.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.whoistalking.feature.session.model.SessionIntent
import com.whoistalking.feature.session.model.SessionSideEffect
import com.whoistalking.feature.session.model.SessionUiState
import com.whoistalking.feature.session.vm.SessionViewModel

@Composable
fun SessionRoute(viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { sideEffect ->
            when (sideEffect) {
                is SessionSideEffect.ShowToast -> Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SessionScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
fun SessionScreen(
    state: SessionUiState,
    onIntent: (SessionIntent) -> Unit,
) {
    val pagingItems = state.messages.collectAsLazyPagingItems()
    val canSubmit by remember(state.draft, state.isLoading) {
        derivedStateOf { state.draft.isNotBlank() && !state.isLoading }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Session: ${state.sessionId}", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(count = pagingItems.itemCount) { index ->
                pagingItems[index]?.let { message ->
                    Text("${message.speaker}: ${message.content}")
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.draft,
                onValueChange = { onIntent(SessionIntent.ChangeDraft(it)) },
                label = { Text("Prompt") },
            )
            Button(onClick = { onIntent(SessionIntent.SubmitMessage) }, enabled = canSubmit) {
                Text("Send")
            }
        }
    }
}
