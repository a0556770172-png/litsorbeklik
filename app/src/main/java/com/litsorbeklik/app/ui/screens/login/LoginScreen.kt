package com.litsorbeklik.app.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.litsorbeklik.app.R
import com.litsorbeklik.app.data.model.AiProvider
import com.litsorbeklik.app.data.repository.AuthRepository
import com.litsorbeklik.app.data.repository.SecretsRepository
import com.litsorbeklik.app.data.session.SessionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository = AuthRepository(),
    secretsRepository: SecretsRepository = SecretsRepository(),
    onLoggedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf(AiProvider.GEMINI) }
    var apiKey by remember { mutableStateOf("") }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.login_title)) }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(stringResource(R.string.register_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text(stringResource(R.string.register_password)) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.login_ai_provider_label), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = providerMenuExpanded,
                onExpandedChange = { providerMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = provider.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.login_ai_provider_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = providerMenuExpanded,
                    onDismissRequest = { providerMenuExpanded = false },
                ) {
                    AiProvider.entries.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = {
                            provider = p
                            providerMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.login_ai_key_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("המפתח מוצפן ונשמר רק בשבילך — ראה מסמך אבטחה") },
            )

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    errorMessage = null
                    loading = true
                    scope.launch {
                        val loginResult = authRepository.login(email, password)
                        loginResult.onSuccess { profile ->
                            SessionState.onAuthenticated(profile.id, password)
                            runCatching {
                                secretsRepository.saveAiApiKey(
                                    ownerId = profile.id,
                                    provider = provider.name,
                                    plainApiKey = apiKey,
                                    password = password,
                                )
                            }.onFailure {
                                errorMessage = "התחברת, אבל שמירת מפתח ה-AI נכשלה: ${it.message}"
                            }
                            loading = false
                            onLoggedIn()
                        }.onFailure {
                            loading = false
                            errorMessage = it.message ?: "ההתחברות נכשלה"
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && apiKey.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login_cta))
                }
            }
        }
    }
}
