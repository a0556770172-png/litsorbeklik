package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.model.BuildRun
import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * The user connects a repo THEY already created (private or public) via a fine-grained PAT.
 * This engine never creates repos or accounts on the user's behalf — it only pushes generated
 * project files into the existing repo and relies on a committed `.github/workflows/build.yml`
 * (see repo root of THIS project for the reference workflow) to assemble + sign the release APK.
 *
 * Because the GitHub REST API may not be reachable from every execution environment, status is
 * tracked primarily through the git push result and, where available, the Actions run page —
 * polling falls back to asking the user to confirm pass/fail if the API can't be reached.
 */
class GithubBuildEngine(
    private val repoOwner: String,
    private val repoName: String,
    private val personalAccessToken: String,
) : BuildEngine {

    override val id: String = "github:$repoOwner/$repoName"

    override suspend fun isAvailable(): Boolean = personalAccessToken.isNotBlank()

    override suspend fun startBuild(project: GeneratedProject): Result<BuildRun> {
        // TODO: write `project.files` into a local git working copy, commit, and push over
        // https://<token>@github.com/<owner>/<repo>.git — a push to the default branch is what
        // triggers the workflow (see build.yml `on: push`).
        throw NotImplementedError("Wire up git push via JGit or a bundled git binary")
    }

    override suspend fun refreshStatus(run: BuildRun): Result<BuildRun> {
        // TODO: try the Actions REST API first; if unreachable, fall back to the workflow's
        // static status badge (https://github.com/<owner>/<repo>/actions/workflows/build.yml/badge.svg)
        // or ask the user to check manually.
        throw NotImplementedError("Wire up status polling")
    }
}
