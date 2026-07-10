package com.litsorbeklik.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.litsorbeklik.app.data.engines.net.HttpClientProvider
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File

/**
 * Downloads a self-update APK from Supabase Storage and hands it to the system package installer.
 * Requires `REQUEST_INSTALL_PACKAGES` (already in AndroidManifest.xml) — on Android 8+ the user
 * must also explicitly allow "install unknown apps" for this app the first time, via the system
 * prompt that `ACTION_INSTALL_PACKAGE`/`ACTION_VIEW` triggers automatically if not yet granted.
 */
object ApkInstaller {

    suspend fun downloadAndInstall(context: Context, downloadUrl: String, fileName: String = "litsorbeklik-update.apk") {
        val targetFile = File(context.cacheDir, fileName)
        HttpClientProvider.client.get(downloadUrl).bodyAsChannel().copyTo(targetFile.outputStream())

        val apkUri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", targetFile,
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }

    fun canRequestInstalls(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
}
