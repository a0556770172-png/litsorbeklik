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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineSettingsScreen(
    initialAiEngine: AiEngineType = AiEngineType.CLOUD,
    initialBuildEngine: BuildEngineType = BuildEngineType.GITHUB,
    onSave: (AiEngineType, BuildEngineType) -> Unit,
) {
    var aiEngine by remember { mutableStateOf(initialAiEngine) }
    var buildEngine by remember { mutableStateOf(initialBuildEngine) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_engine_title)) }) }) { padding ->
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

            Spacer(Modifier.height(28.dp))
            Button(onClick = { onSave(aiEngine, buildEngine) }, modifier = Modifier.fillMaxWidth()) {
                Text("שמירה")
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
