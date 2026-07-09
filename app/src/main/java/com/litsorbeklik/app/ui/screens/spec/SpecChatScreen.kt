package com.litsorbeklik.app.ui.screens.spec

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.litsorbeklik.app.R
import com.litsorbeklik.app.data.engines.ChatTurn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecChatScreen(
    history: List<ChatTurn> = emptyList(),
    onSendMessage: (String) -> Unit,
    onUploadExistingSpec: () -> Unit,
    onConfirmSpec: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.spec_title)) }) },
        bottomBar = {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.spec_start_chat)) },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = {
                        if (draft.isNotBlank()) {
                            onSendMessage(draft)
                            draft = ""
                        }
                    }) { Icon(Icons.Filled.Send, contentDescription = null) }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = onUploadExistingSpec, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.spec_upload_existing))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirmSpec, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.spec_confirm))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(history) { turn ->
                val isUser = turn.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    ElevatedCard {
                        Text(
                            turn.text,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
