package com.litsorbeklik.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val fullName: String,
    val userCode: String,
    val createdAt: String? = null,
)

enum class AiProvider { GEMINI, OPENAI, CLAUDE, GROK }

enum class AiEngineType { CLOUD, LOCAL }
enum class BuildEngineType { GITHUB, LOCAL }

@Serializable
data class ProjectEntity(
    val id: String,
    val ownerId: String,
    val name: String,
    val packageName: String,
    val status: String,
    val repoUrl: String? = null,
    val aiEngine: String = "CLOUD",
    val buildEngine: String = "GITHUB",
)

@Serializable
data class AppSpec(
    val id: String,
    val projectId: String,
    val version: Int,
    val goal: String,
    val screens: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val rawText: String? = null,
)

@Serializable
data class GeneratedFile(
    val path: String,
    val content: String,
)

@Serializable
data class GeneratedProject(
    val applicationId: String,
    val appName: String,
    val files: List<GeneratedFile>,
)

@Serializable
data class BuildRun(
    /** Supabase `build_runs.id` (uuid) once persisted; empty string before the first insert. */
    val id: String,
    val projectId: String,
    val engine: String,
    val status: String,
    /** Engine-specific run handle — e.g. the GitHub Actions numeric run id. Not the same as [id]. */
    val externalRunId: String? = null,
    val logUrl: String? = null,
    val apkUrl: String? = null,
)
