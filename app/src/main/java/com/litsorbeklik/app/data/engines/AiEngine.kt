package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * Abstraction over "who writes the code". Two first-class implementations from day one:
 * - [CloudAiEngine]: calls the user's own Gemini/GPT/Claude/Grok API key.
 * - [LocalAiEngine]: runs a quantized model on-device (LiteRT-LM / MLC), no network needed.
 * Both are equal citizens behind this interface so the rest of the app never branches on which one is active.
 */
interface AiEngine {
    val id: String

    /** True if this engine can run right now (e.g. local engine checks device RAM/chipset tier). */
    suspend fun isAvailable(): Boolean

    /** Conducts (or continues) the spec-building interview. Returns the assistant's next message. */
    suspend fun chatSpecStep(history: List<ChatTurn>, userMessage: String): ChatTurn

    /** Given a finalized spec, returns a full, ready-to-assemble Android project. */
    suspend fun generateProject(spec: AppSpec): Result<GeneratedProject>

    /** Sends a compiler/build error back to the model and asks for a fix to specific files. */
    suspend fun fixBuildError(spec: AppSpec, project: GeneratedProject, errorLog: String): Result<GeneratedProject>
}

data class ChatTurn(val role: String, val text: String)
