package com.litsorbeklik.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.litsorbeklik.app.R
import com.litsorbeklik.app.data.model.AiEngineType
import com.litsorbeklik.app.data.model.BuildEngineType
import com.litsorbeklik.app.data.repository.ProjectsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineSettingsScreen(
    projectId: String,
    projectsRepository: ProjectsRepository = ProjectsRepository(),
    onSaved: () -> Unit,
) {
    var aiEngine by remember { mutableStateOf(AiEngineType.CLOUD) }
    var buildEngine by remember { mutableStateOf(BuildEngineType.GITHUB) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(projectId) {
        projectsRepository.getProject(projectId)
            .onSuccess { project ->
                aiEngine = if (project.aiEngine == "LOCAL") AiEngineType.LOCAL else AiEngineType.CLOUD
                buildEngine = if (project.buildEngine == "LOCAL") BuildEngineType.LOCAL else BuildEngineType.GITHUB
                loading = false
            }
            .onFailure {
                errorMessage = it.message ?: "טעינת הפרויקט נכשלה"
                loading = false
            }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_engine_title)) }) }) { padding ->
        if (loading) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.padding(24.dp)) }
            return@Scaffold
        }
        Column(Modifier.padding(padding).padding(20.dp)) {
            Text(stringResource(R.string.settings_ai_engine), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            SegmentedChoice(
                selected = aiEngine == AiEngineType.CLOUD,
                onSelectLeft = { aiEngine = AiEngineType.CLOUD },
                onSelectRight = { aiEngine = AiEngineType.LOCAL },
                leftLabel = stringResource(R.string.settings_ai_cloud),
                rightLabel = stringResource(R.string.settings_ai_local),
            )

            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.settings_build_engine), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            SegmentedChoice(
                selected = buildEngine == BuildEngineType.GITHUB,
                onSelectLeft = { buildEngine = BuildEngineType.GITHUB },
                onSelectRight = { buildEngine = BuildEngineType.LOCAL },
                leftLabel = stringResource(R.string.settings_build_github),
                rightLabel = stringResource(R.string.settings_build_local),
            )

            if (aiEngine == AiEngineType.LOCAL || buildEngine == BuildEngineType.LOCAL) {
                Spacer(Modifier.height(20.dp))
                Card {
                    Text(
                        "מצב אופליין מתאים לפרויקטים פשוטים ולתיקונים ממוקדים. לפרויקט מורכב מומלץ Cloud API + GitHub.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        projectsRepository.updateEngines(projectId, aiEngine.name, buildEngine.name)
                            .onSuccess {
                                saving = false
                                onSaved()
                            }
                            .onFailure {
                                saving = false
                                errorMessage = it.message ?: "השמירה נכשלה"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("שמירה")
            }
        }
    }
}

@Composable
private fun SegmentedChoice(
    selected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    leftLabel: String,
    rightLabel: String,
) {
    Row(Modifier.fillMaxWidth()) {
        SegmentedButtonEntry(leftLabel, selected, onSelectLeft, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        SegmentedButtonEntry(rightLabel, !selected, onSelectRight, Modifier.weight(1f))
    }
}

@Composable
private fun SegmentedButtonEntry(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}
