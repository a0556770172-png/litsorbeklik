package com.litsorbeklik.app.data.repository

import com.litsorbeklik.app.data.model.ProjectEntity
import com.litsorbeklik.app.data.supabase.SupabaseModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** CRUD over the `projects` table — RLS (`projects_owner`) already scopes every call to the caller. */
class ProjectsRepository(
    private val client: SupabaseClient = SupabaseModule.client,
) {
    suspend fun listMyProjects(ownerId: String): Result<List<ProjectEntity>> = runCatching {
        client.from("projects")
            .select()
            .decodeList<ProjectRowDto>()
            .filter { it.ownerId == ownerId }
            .map {
                ProjectEntity(
                    id = it.id,
                    ownerId = it.ownerId,
                    name = it.name,
                    packageName = it.packageName,
                    status = it.status,
                    repoUrl = it.repoUrl,
                    aiEngine = it.aiEngine,
                    buildEngine = it.buildEngine,
                )
            }
    }

    suspend fun createProject(
        ownerId: String,
        name: String,
        packageName: String,
        aiEngine: String = "CLOUD",
        buildEngine: String = "GITHUB",
    ): Result<ProjectEntity> = runCatching {
        val row = ProjectInsertDto(
            ownerId = ownerId,
            name = name,
            packageName = packageName,
            aiEngine = aiEngine,
            buildEngine = buildEngine,
        )
        val inserted = client.from("projects")
            .insert(row) { select() }
            .decodeSingle<ProjectRowDto>()
        ProjectEntity(
            id = inserted.id,
            ownerId = inserted.ownerId,
            name = inserted.name,
            packageName = inserted.packageName,
            status = inserted.status,
            repoUrl = inserted.repoUrl,
            aiEngine = inserted.aiEngine,
            buildEngine = inserted.buildEngine,
        )
    }

    suspend fun getProject(projectId: String): Result<ProjectEntity> = runCatching {
        val row = client.from("projects")
            .select()
            .decodeList<ProjectRowDto>()
            .first { it.id == projectId }
        ProjectEntity(
            id = row.id,
            ownerId = row.ownerId,
            name = row.name,
            packageName = row.packageName,
            status = row.status,
            repoUrl = row.repoUrl,
            aiEngine = row.aiEngine,
            buildEngine = row.buildEngine,
        )
    }

    suspend fun updateEngines(projectId: String, aiEngine: String, buildEngine: String): Result<Unit> = runCatching {
        client.from("projects").update(mapOf("ai_engine" to aiEngine, "build_engine" to buildEngine)) {
            filter { eq("id", projectId) }
        }
        Unit
    }

    suspend fun updateStatus(projectId: String, status: String): Result<Unit> = runCatching {
        client.from("projects").update(mapOf("status" to status)) {
            filter { eq("id", projectId) }
        }
        Unit
    }
}

@Serializable
private data class ProjectInsertDto(
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("ai_engine") val aiEngine: String,
    @SerialName("build_engine") val buildEngine: String,
)

@Serializable
private data class ProjectRowDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("package_name") val packageName: String,
    val status: String,
    @SerialName("repo_url") val repoUrl: String? = null,
    @SerialName("ai_engine") val aiEngine: String,
    @SerialName("build_engine") val buildEngine: String,
)
