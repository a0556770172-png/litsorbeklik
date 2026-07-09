package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.AiProvider
import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * Calls the end-user's own API key against Gemini / OpenAI / Claude / Grok directly from the
 * device (no proxy) — the request/response schema is normalized to a single internal contract
 * regardless of provider, so swapping providers doesn't touch the rest of the app.
 */
class CloudAiEngine(
    private val provider: AiProvider,
    private val apiKey: String,
) : AiEngine {

    override val id: String = "cloud:${provider.name.lowercase()}"

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun chatSpecStep(history: List<ChatTurn>, userMessage: String): ChatTurn {
        // TODO: provider-specific HTTP call (Ktor) using apiKey; normalize response into ChatTurn.
        throw NotImplementedError("Wire up ${provider.name} chat completion call")
    }

    override suspend fun generateProject(spec: AppSpec): Result<GeneratedProject> {
        // TODO: send strict JSON-schema prompt describing the required file list/contract,
        // parse + validate the response into GeneratedProject before returning.
        throw NotImplementedError("Wire up ${provider.name} project generation call")
    }

    override suspend fun fixBuildError(
        spec: AppSpec,
        project: GeneratedProject,
        errorLog: String,
    ): Result<GeneratedProject> {
        throw NotImplementedError("Wire up ${provider.name} error-fix loop")
    }
}
