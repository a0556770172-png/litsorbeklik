package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * Runs a quantized model fully on-device via LiteRT-LM (CPU) or MLC-LLM (NPU-accelerated on
 * supported chipsets). Model tier is chosen by [DeviceCapability] — see architecture doc section 5.
 *
 * Honest expectation baked into the contract: [generateProject] is only meant to be offered to
 * the user for simple/small apps. The UI layer should surface a warning when this engine is
 * selected for a spec above a complexity threshold, steering back to [CloudAiEngine].
 */
class LocalAiEngine(
    private val modelTier: DeviceCapability.ModelTier,
) : AiEngine {

    override val id: String = "local:${modelTier.name.lowercase()}"

    override suspend fun isAvailable(): Boolean {
        // TODO: verify the model file is downloaded and the runtime (LiteRT-LM/MLC) initialized.
        return modelTier != DeviceCapability.ModelTier.UNSUPPORTED
    }

    override suspend fun chatSpecStep(history: List<ChatTurn>, userMessage: String): ChatTurn {
        throw NotImplementedError("Wire up on-device inference call (LiteRT-LM / MLC-LLM)")
    }

    override suspend fun generateProject(spec: AppSpec): Result<GeneratedProject> {
        throw NotImplementedError("Wire up on-device project generation — small apps only")
    }

    override suspend fun fixBuildError(
        spec: AppSpec,
        project: GeneratedProject,
        errorLog: String,
    ): Result<GeneratedProject> {
        throw NotImplementedError("Wire up on-device error-fix loop")
    }
}

/** Detects device RAM / chipset / NPU once at install time and picks a model tier. */
object DeviceCapability {
    enum class ModelTier { WEAK_1B, STRONG_NPU_3B_7B, UNSUPPORTED }

    fun detect(totalRamMb: Long, hasSupportedNpu: Boolean): ModelTier = when {
        hasSupportedNpu && totalRamMb >= 6_000 -> ModelTier.STRONG_NPU_3B_7B
        totalRamMb >= 3_000 -> ModelTier.WEAK_1B
        else -> ModelTier.UNSUPPORTED
    }
}
