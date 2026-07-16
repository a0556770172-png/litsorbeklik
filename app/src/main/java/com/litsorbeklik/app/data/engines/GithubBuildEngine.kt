package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.engines.net.GithubApiClient
import com.litsorbeklik.app.data.model.BuildRun
import com.litsorbeklik.app.data.model.GeneratedProject
import java.util.UUID

/**
 * The user connects a repo THEY already created (private or public) via a fine-grained or
 * classic PAT. This engine never creates repos or accounts on the user's behalf — it only pushes
 * generated project files into the existing repo (via the Contents API — no git binary / JGit
 * needed on Android) and relies on a committed `.github/workflows/build.yml` (see this project's
 * own repo root for the reference workflow) to assemble + sign the release APK.
 *
 * Runs on the *end user's device*, so it is unaffected by any developer-sandbox network limits.
 */
class GithubBuildEngine(
    private val repoOwner: String,
    private val repoName: String,
    private val personalAccessToken: String,
) : BuildEngine {

    private val api = GithubApiClient(repoOwner, repoName, personalAccessToken)

    override val id: String = "github:$repoOwner/$repoName"

    override suspend fun isAvailable(): Boolean = personalAccessToken.isNotBlank()

    override suspend fun startBuild(project: GeneratedProject): Result<BuildRun> = runCatching {
        val filesByPath = project.files.associate { it.path to it.content }
        val commitMessage = "Generated build: ${project.appName} (${UUID.randomUUID().toString().take(8)})"

        // One atomic commit for the whole project (not one commit per file), then poll for the
        // workflow run GitHub registers specifically for that commit sha.
        val commitSha = api.pushProjectAtomic(filesByPath, commitMessage).getOrThrow()
        val run = api.findWorkflowRunForCommit(commitSha).getOrThrow()

        BuildRun(
            id = "", // filled in by the caller once persisted to Supabase
            projectId = "",
            engine = "GITHUB",
            status = run.status,
            externalRunId = run.id.toString(),
            logUrl = run.htmlUrl,
            apkUrl = null,
        )
    }

    override suspend fun refreshStatus(run: BuildRun): Result<BuildRun> = runCatching {
        val runId = run.externalRunId?.toLongOrNull()
            ?: error("GithubBuildEngine.refreshStatus needs a numeric GitHub run id")
        val refreshed = api.getRun(runId).getOrElse {
            // API unreachable/rate-limited: fall back to the static status badge as a best-effort signal.
            return@runCatching run.copy(logUrl = api.statusBadgeUrl())
        }
        run.copy(
            status = refreshed.status,
            logUrl = refreshed.htmlUrl,
        )
    }
}
