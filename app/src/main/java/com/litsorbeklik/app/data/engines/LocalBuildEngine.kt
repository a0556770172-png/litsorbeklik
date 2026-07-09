package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.BuildRun
import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * Compiles and signs the APK entirely on-device using a lightweight embedded toolchain
 * (aapt2 + d8 + apksigner + zipalign extracted from Android SDK Build-Tools, plus a lightweight
 * Java/Kotlin compiler) rather than a full Gradle install — see architecture doc section 4.
 *
 * A device-capability check MUST gate this engine: if storage/RAM don't meet the minimum, the UI
 * should steer the user to [GithubBuildEngine] instead of letting a weak device grind for ages.
 */
class LocalBuildEngine(
    private val minFreeStorageMb: Long = 1_500,
) : BuildEngine {

    override val id: String = "local"

    override suspend fun isAvailable(): Boolean {
        // TODO: check free storage, RAM, and that the toolchain binaries are extracted/executable.
        return true
    }

    override suspend fun startBuild(project: GeneratedProject): Result<BuildRun> {
        // TODO: compile -> merge resources -> dex (d8) -> package (aapt2 link) -> sign (apksigner).
        throw NotImplementedError("Wire up embedded toolchain build pipeline")
    }

    override suspend fun refreshStatus(run: BuildRun): Result<BuildRun> {
        // Local builds are synchronous/blocking from the caller's perspective (no polling needed);
        // this just returns the last known in-memory state.
        return Result.success(run)
    }
}
