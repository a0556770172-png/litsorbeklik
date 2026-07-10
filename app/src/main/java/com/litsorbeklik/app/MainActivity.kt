package com.litsorbeklik.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.litsorbeklik.app.data.repository.LatestAppVersion
import com.litsorbeklik.app.data.update.ApkInstaller
import com.litsorbeklik.app.domain.UpdateChecker
import com.litsorbeklik.app.ui.navigation.AppNavHost
import com.litsorbeklik.app.ui.theme.LitsorBeKlikTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LitsorBeKlikTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                    SelfUpdateCheck()
                }
            }
        }
    }
}

/**
 * Runs once per app launch: compares BuildConfig.VERSION_CODE against `app_versions` in Supabase
 * (see AppVersionsRepository) and offers a sideload update if a newer build was published there.
 */
@Composable
private fun SelfUpdateCheck(updateChecker: UpdateChecker = UpdateChecker()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var available by remember { mutableStateOf<LatestAppVersion?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        available = updateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
    }

    val update = available ?: return

    AlertDialog(
        onDismissRequest = { available = null },
        title = { Text(stringResource(R.string.update_available_title)) },
        text = { Text(update.changelog ?: "גרסה ${update.versionName} זמינה") },
        confirmButton = {
            TextButton(onClick = {
                available = null
                if (!ApkInstaller.canRequestInstalls(context)) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                    return@TextButton
                }
                scope.launch { ApkInstaller.downloadAndInstall(context, update.downloadUrl) }
            }) { Text(stringResource(R.string.update_now)) }
        },
        dismissButton = { TextButton(onClick = { available = null }) { Text(stringResource(R.string.update_later)) } },
    )
}
