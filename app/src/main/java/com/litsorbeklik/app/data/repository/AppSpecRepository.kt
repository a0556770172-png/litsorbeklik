package com.litsorbeklik.app.data.repository

import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.supabase.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** CRUD over `app_specs` — versioned per project, scoped by the `app_specs_via_project` RLS policy. */
class AppSpecRepository(
    private val client: SupabaseClient = SupabaseModule.client,
) {
    suspend fun latestSpec(projectId: String): Result<AppSpec?> = runCatching {
        client.from("app_specs")
            .select()
            .decodeList<AppSpecRowDto>()
            .filter { it.projectId == projectId }
            .maxByOrNull { it.version }
            ?.toModel()
    }

    suspend fun saveSpec(
        projectId: String,
        goal: String,
        screens: List<String>,
        features: List<String>,
        rawText: String?,
    ): Result<AppSpec> = runCatching {
        val previousVersion = latestSpec(projectId).getOrNull()?.version ?: 0
        val row = AppSpecInsertDto(
            projectId = projectId,
            version = previousVersion + 1,
            goal = goal,
            screens = screens,
            features = features,
            rawText = rawText,
        )
        client.from("app_specs")
            .insert(row) { select() }
            .decodeSingle<AppSpecRowDto>()
            .toModel()
    }
}

@Serializable
private data class AppSpecInsertDto(
    @SerialName("project_id") val projectId: String,
    val version: Int,
    val goal: String,
    val screens: List<String>,
    val features: List<String>,
    @SerialName("raw_text") val rawText: String?,
)

@Serializable
private data class AppSpecRowDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val version: Int,
    val goal: String? = null,
    val screens: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    @SerialName("raw_text") val rawText: String? = null,
) {
    fun toModel() = AppSpec(
        id = id, projectId = projectId, version = version,
        goal = goal.orEmpty(), screens = screens, features = features, rawText = rawText,
    )
}
