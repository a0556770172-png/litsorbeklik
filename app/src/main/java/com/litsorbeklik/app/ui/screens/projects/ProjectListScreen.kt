package com.litsorbeklik.app.ui.screens.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.litsorbeklik.app.R
import com.litsorbeklik.app.data.model.ProjectEntity
import com.litsorbeklik.app.data.repository.ProjectsRepository
import com.litsorbeklik.app.data.session.SessionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    projectsRepository: ProjectsRepository = ProjectsRepository(),
    onOpenProject: (ProjectEntity) -> Unit,
    onNewProject: (ProjectEntity) -> Unit,
) {
    var projects by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ownerId = SessionState.userId

    suspend fun refresh() {
        loading = true
        ownerId?.let { uid ->
            projectsRepository.listMyProjects(uid).onSuccess { projects = it }
        }
        loading = false
    }

    LaunchedEffect(ownerId) { refresh() }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, packageName ->
                showNewProjectDialog = false
                scope.launch {
                    val uid = ownerId ?: return@launch
                    projectsRepository.createProject(uid, name, packageName)
                        .onSuccess { created ->
                            refresh()
                            onNewProject(created)
                        }
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.projects_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showNewProjectDialog = true }, icon = {
                Icon(Icons.Filled.Add, contentDescription = null)
            }, text = { Text(stringResource(R.string.projects_new)) })
        },
    ) { padding ->
        when {
            loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            projects.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.projects_empty), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(projects) { project ->
                    ElevatedCard(onClick = { onOpenProject(project) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Android, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.titleLarge)
                                Text(project.packageName, style = MaterialTheme.typography.bodyMedium)
                            }
                            AssistChip(onClick = {}, label = { Text(project.status) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectDialog(onDismiss: () -> Unit, onCreate: (name: String, packageName: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("פרויקט חדש") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("שם האפליקציה") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = packageName, onValueChange = { packageName = it },
                    label = { Text("com.example.myapp") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, packageName) },
                enabled = name.isNotBlank() && packageName.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")),
            ) { Text("יצירה") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}
