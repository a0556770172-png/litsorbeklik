package com.litsorbeklik.app.ui.screens.build

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.litsorbeklik.app.data.engines.EngineFactory
import com.litsorbeklik.app.data.repository.AppSpecRepository
import com.litsorbeklik.app.data.repository.ProjectsRepository
import com.litsorbeklik.app.domain.BuildOrchestrator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(
    projectId: String,
    projectsRepository: ProjectsRepository = ProjectsRepository(),
    appSpecRepository: AppSpecRepository = AppSpecRepository(),
    engineFactory: EngineFactory = EngineFactory(),
    onFinished: () -> Unit,
) {
    var statusText by remember { mutableStateOf("מכין בנייה...") }
    var isRunning by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var logUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(projectId) {
        scope.launch {
            val project = projectsRepository.getProject(projectId).getOrElse {
                statusText = "לא ניתן לטעון את הפרויקט: ${it.message}"
                isError = true; isRunning = false
                return@launch
            }
            val spec = appSpecRepository.latestSpec(projectId).getOrNull()
            if (spec == null) {
                statusText = "לא נמצא אפיון שמור לפרויקט הזה — יש לחזור למסך האיפיון"
                isError = true; isRunning = false
                return@launch
            }

            val aiEngine = engineFactory.buildAiEngine(project).getOrElse {
                statusText = "לא ניתן לאתחל את מנוע ה-AI: ${it.message}"
                isError = true; isRunning = false
                return@launch
            }
            val buildEngine = engineFactory.buildBuildEngine(project).getOrElse {
                statusText = "לא ניתן לאתחל את מנוע הבנייה: ${it.message}"
                isError = true; isRunning = false
                return@launch
            }

            val orchestrator = BuildOrchestrator(aiEngine, buildEngine)
            orchestrator.run(projectId, spec) { progress ->
                when (progress) {
                    is BuildOrchestrator.Progress.GeneratingCode -> statusText = "ה-AI כותב את הפרויקט..."
                    is BuildOrchestrator.Progress.Validating -> statusText = "בודק תקינות (ניסיון ${progress.attempt})..."
                    is BuildOrchestrator.Progress.FixingErrors -> statusText = "מתקן שגיאות (ניסיון ${progress.attempt})..."
                    is BuildOrchestrator.Progress.StartingBuild -> statusText = "מפעיל בנייה..."
                    is BuildOrchestrator.Progress.Done -> {
                        statusText = "הבנייה הופעלה בהצלחה! סטטוס: ${progress.run.status}"
                        logUrl = progress.run.logUrl
                        isRunning = false
                    }
                    is BuildOrchestrator.Progress.Failed -> {
                        statusText = progress.reason
                        isError = true
                        isRunning = false
                    }
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("בנייה") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            if (isRunning) CircularProgressIndicator() else Spacer(Modifier.height(0.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            logUrl?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(32.dp))
            if (!isRunning) {
                Button(onClick = onFinished) { Text("חזרה לפרויקטים") }
            }
        }
    }
}
