package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.BuildRun
import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * Abstraction over "who compiles and signs the APK". Two first-class implementations:
 * - [GithubBuildEngine]: pushes to a private/public repo the USER already owns, runs Actions.
 * - [LocalBuildEngine]: compiles and signs entirely on-device (no network at all).
 */
interface BuildEngine {
    val id: String

    suspend fun isAvailable(): Boolean

    /** Assembles the project on disk/remote and starts a build. Returns a trackable [BuildRun]. */
    suspend fun startBuild(project: GeneratedProject): Result<BuildRun>

    /** Polls (or otherwise fetches) the latest status for a previously started build. */
    suspend fun refreshStatus(run: BuildRun): Result<BuildRun>
}
