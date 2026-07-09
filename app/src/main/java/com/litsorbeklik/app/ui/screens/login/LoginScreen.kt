package com.litsorbeklik.app.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.litsorbeklik.app.R
import com.litsorbeklik.app.data.model.AiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf(AiProvider.GEMINI) }
    var apiKey by remember { mutableStateOf("") }
    var providerMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.login_title)) }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text(stringResource(R.string.register_email)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
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

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    // TODO: Supabase Auth signIn(email, password); encrypt+store apiKey via
                    //       SecretCrypto before upserting into `user_secrets`.
                    onLoggedIn()
                },
                enabled = email.isNotBlank() && password.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.login_cta))
            }
        }
    }
}
