package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.AiProvider
import com.litsorbeklik.app.data.model.ProjectEntity
import com.litsorbeklik.app.data.repository.SecretsRepository
import com.litsorbeklik.app.data.session.SessionState

/**
 * Builds the concrete [AiEngine] / [BuildEngine] for a project from its saved `ai_engine` /
 * `build_engine` columns plus the caller's decrypted secrets. Centralized here so no screen has
 * to know how secrets get decrypted or which engine class backs which setting.
 */
class EngineFactory(
    private val secretsRepository: SecretsRepository = SecretsRepository(),
) {
    suspend fun buildAiEngine(project: ProjectEntity): Result<AiEngine> = runCatching {
        val ownerId = project.ownerId
        val password = SessionState.inMemoryPassword ?: error("No active session password — please log in again")

        when (project.aiEngine) {
            "LOCAL" -> {
                val tier = DeviceCapability.ModelTier.WEAK_1B // real detection wired at call sites with Context
                LocalAiEngine(tier)
            }
            else -> {
                val (providerName, apiKey) = secretsRepository.loadAiApiKeyWithProvider(ownerId, password)
                    ?: error("לא נמצא מפתח AI שמור — יש להתחבר מחדש ולהזין מפתח")
                val provider = runCatching { AiProvider.valueOf(providerName) }.getOrElse { AiProvider.GEMINI }
                CloudAiEngine(provider, apiKey)
            }
        }
    }

    suspend fun buildBuildEngine(project: ProjectEntity): Result<BuildEngine> = runCatching {
        val ownerId = project.ownerId
        val password = SessionState.inMemoryPassword ?: error("No active session password — please log in again")

        when (project.buildEngine) {
            "LOCAL" -> LocalBuildEngine()
            else -> {
                val token = secretsRepository.loadGithubToken(ownerId, password)
                    ?: error("לא נמצא טוקן GitHub שמור")
                val (owner, repo) = parseRepoUrl(project.repoUrl)
                    ?: error("לפרויקט אין repo_url מוגדר בהגדרות")
                GithubBuildEngine(owner, repo, token)
            }
        }
    }

    private fun parseRepoUrl(repoUrl: String?): Pair<String, String>? {
        if (repoUrl.isNullOrBlank()) return null
        val cleaned = repoUrl.removeSuffix(".git").removeSuffix("/")
        val parts = cleaned.substringAfter("github.com/").split("/")
        return if (parts.size >= 2) parts[0] to parts[1] else null
    }
}
