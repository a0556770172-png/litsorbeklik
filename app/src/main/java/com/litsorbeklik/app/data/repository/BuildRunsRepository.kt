package com.litsorbeklik.app.data.repository

import com.litsorbeklik.app.data.model.BuildRun
import com.litsorbeklik.app.data.supabase.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** CRUD over `build_runs` — scoped by the `build_runs_via_project` RLS policy (joins to `projects`). */
class BuildRunsRepository(
    private val client: SupabaseClient = SupabaseModule.client,
) {
    suspend fun createRun(projectId: String, engine: String): Result<BuildRun> = runCatching {
        val row = BuildRunInsertDto(projectId = projectId, engine = engine, status = "pending")
        client.from("build_runs")
            .insert(row) { select() }
            .decodeSingle<BuildRunRowDto>()
            .toModel()
    }

    suspend fun updateRun(run: BuildRun): Result<Unit> = runCatching {
        client.from("build_runs").update(
            mapOf(
                "status" to run.status,
                "external_run_id" to run.externalRunId,
                "log_url" to run.logUrl,
                "apk_url" to run.apkUrl,
            ),
        ) {
            filter { eq("id", run.id) }
        }
        Unit
    }
}

@Serializable
private data class BuildRunInsertDto(
    @SerialName("project_id") val projectId: String,
    val engine: String,
    val status: String,
)

@Serializable
private data class BuildRunRowDto(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val engine: String,
    val status: String,
    @SerialName("external_run_id") val externalRunId: String? = null,
    @SerialName("log_url") val logUrl: String? = null,
    @SerialName("apk_url") val apkUrl: String? = null,
) {
    fun toModel() = BuildRun(
        id = id, projectId = projectId, engine = engine, status = status,
        externalRunId = externalRunId, logUrl = logUrl, apkUrl = apkUrl,
    )
}
