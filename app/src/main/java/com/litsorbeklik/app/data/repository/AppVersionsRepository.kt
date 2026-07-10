package com.litsorbeklik.app.data.repository

import com.litsorbeklik.app.data.supabase.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class LatestAppVersion(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String?,
)

/** Reads `app_versions` (public read-only, see `app_versions_read_all` policy) for the self-update check. */
class AppVersionsRepository(
    private val client: SupabaseClient = SupabaseModule.client,
    private val storageBucket: String = "app-releases",
) {
    suspend fun getLatest(): Result<LatestAppVersion?> = runCatching {
        val row = client.from("app_versions")
            .select()
            .decodeList<AppVersionRowDto>()
            .maxByOrNull { it.versionCode }
            ?: return@runCatching null

        val publicUrl = client.storage.from(storageBucket).publicUrl(row.apkStoragePath)
        LatestAppVersion(
            versionCode = row.versionCode,
            versionName = row.versionName,
            downloadUrl = publicUrl,
            changelog = row.changelog,
        )
    }
}

@Serializable
private data class AppVersionRowDto(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("apk_storage_path") val apkStoragePath: String,
    val changelog: String? = null,
)
