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
import com.litsorbeklik.app.data.engines.AiEngine
import com.litsorbeklik.app.data.engines.ChatTurn
import com.litsorbeklik.app.data.engines.EngineFactory
import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.repository.AppSpecRepository
import com.litsorbeklik.app.data.repository.ProjectsRepository
import kotlinx.coroutines.launch

/**
 * Two paths into an [AppSpec]: a guided chat with the project's configured [AiEngine], or pasting
 * an already-written spec directly. Either way, "אישור האפיון" persists a version into `app_specs`.
 *
 * NOTE: goal/screens/features extraction from the free-form chat transcript is intentionally
 * simple right now (see [buildDraftSpecFromHistory]) — a follow-up worth doing is a dedicated
 * "extract structured spec" prompt instead of just joining the transcript.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecChatScreen(
    projectId: String,
    projectsRepository: ProjectsRepository = ProjectsRepository(),
    appSpecRepository: AppSpecRepository = AppSpecRepository(),
    engineFactory: EngineFactory = EngineFactory(),
    onConfirmSpec: (AppSpec) -> Unit,
) {
    var history by remember { mutableStateOf<List<ChatTurn>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var aiEngine by remember { mutableStateOf<AiEngine?>(null) }
    var pastedSpecText by remember { mutableStateOf<String?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(projectId) {
        projectsRepository.getProject(projectId)
            .mapCatching { project -> engineFactory.buildAiEngine(project).getOrThrow() }
            .onSuccess { aiEngine = it }
            .onFailure { errorMessage = it.message ?: "טעינת מנוע ה-AI נכשלה" }
    }

    if (showUploadDialog) {
        UploadSpecDialog(
            onDismiss = { showUploadDialog = false },
            onSubmit = { text ->
                pastedSpecText = text
                showUploadDialog = false
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.spec_title)) }) },
        bottomBar = {
            Column(Modifier.padding(12.dp)) {
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                pastedSpecText?.let {
                    AssistChip(onClick = { pastedSpecText = null }, label = { Text("אפיון שהועלה מוכן ✓ (לחיצה להסרה)") })
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        enabled = !sending,
                        placeholder = { Text(stringResource(R.string.spec_start_chat)) },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        enabled = !sending && aiEngine != null,
                        onClick = {
                            val message = draft
                            if (message.isBlank()) return@FilledIconButton
                            draft = ""
                            history = history + ChatTurn("user", message)
                            sending = true
                            scope.launch {
                                val engine = aiEngine ?: return@launch
                                engine.chatSpecStep(history.dropLast(1), message)
                                    .let { reply -> history = history + reply }
                                sending = false
                            }
                        },
                    ) {
                        if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Send, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = { showUploadDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.spec_upload_existing))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !saving && (history.isNotEmpty() || pastedSpecText != null),
                        onClick = {
                            saving = true
                            scope.launch {
                                val draftSpec = buildDraftSpecFromHistory(history, pastedSpecText)
                                appSpecRepository.saveSpec(
                                    projectId = projectId,
                                    goal = draftSpec.goal,
                                    screens = draftSpec.screens,
                                    features = draftSpec.features,
                                    rawText = draftSpec.rawText,
                                ).onSuccess {
                                    saving = false
                                    onConfirmSpec(it)
                                }.onFailure {
                                    saving = false
                                    errorMessage = it.message ?: "שמירת האפיון נכשלה"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.spec_confirm))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadSpecDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הדבקת אפיון קיים") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                placeholder = { Text("הדביקו כאן את טקסט האפיון המלא...") },
            )
        },
        confirmButton = { TextButton(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) { Text("שמירה") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}

private data class DraftSpecFields(val goal: String, val screens: List<String>, val features: List<String>, val rawText: String?)

private fun buildDraftSpecFromHistory(history: List<ChatTurn>, pastedSpecText: String?): DraftSpecFields {
    if (pastedSpecText != null) {
        return DraftSpecFields(
            goal = pastedSpecText.lineSequence().firstOrNull { it.isNotBlank() }?.take(200) ?: "אפיון שהועלה",
            screens = emptyList(),
            features = emptyList(),
            rawText = pastedSpecText,
        )
    }
    val transcript = history.joinToString("\n") { "${it.role}: ${it.text}" }
    val lastAssistantSummary = history.lastOrNull { it.role == "assistant" }?.text ?: "אפיון מצ'אט"
    return DraftSpecFields(goal = lastAssistantSummary.take(300), screens = emptyList(), features = emptyList(), rawText = transcript)
}
