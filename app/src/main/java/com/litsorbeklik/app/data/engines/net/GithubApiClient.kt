package com.litsorbeklik.app.data.engines.net

import android.util.Base64
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Talks to the GitHub REST API from the *shipped app*, running on the end-user's own device —
 * this is unrelated to (and unaffected by) any network restrictions in a development sandbox.
 *
 * Pushes the whole generated project as ONE atomic commit via the Git Data API (blobs -> tree ->
 * commit -> ref update — mirrors the reference implementation in the sibling Electron app's
 * src/github.js), instead of one Contents-API PUT per file. That earlier per-file approach created
 * a separate commit (and a separate workflow trigger) per file, and picking "the latest workflow
 * run" afterwards had no way to know it was actually looking at the run for the finished project.
 */
class GithubApiClient(
    private val owner: String,
    private val repo: String,
    private val token: String,
) {
    private val client get() = HttpClientProvider.client
    private val json get() = HttpClientProvider.json
    private val apiBase = "https://api.github.com/repos/$owner/$repo"

    private fun io.ktor.client.request.HttpRequestBuilder.githubHeaders() {
        header("Authorization", "Bearer $token")
        header("Accept", "application/vnd.github+json")
    }

    private suspend fun getDefaultBranch(): String {
        val response: HttpResponse = client.get { url(apiBase); githubHeaders() }
        if (!response.status.isSuccess()) error("GET repo failed: ${response.status}")
        return response.body<RepoResponse>().defaultBranch
    }

    /** Pushes every file in the generated project as a single commit. Returns the new commit sha. */
    suspend fun pushProjectAtomic(files: Map<String, String>, commitMessage: String): Result<String> = runCatching {
        val branch = getDefaultBranch()

        val refResponse: HttpResponse = client.get { url("$apiBase/git/ref/heads/$branch"); githubHeaders() }
        if (!refResponse.status.isSuccess()) error("GET ref failed: ${refResponse.status}")
        val baseCommitSha = refResponse.body<GitRefResponse>().objectRef.sha

        val baseCommitResponse: HttpResponse = client.get { url("$apiBase/git/commits/$baseCommitSha"); githubHeaders() }
        if (!baseCommitResponse.status.isSuccess()) error("GET base commit failed: ${baseCommitResponse.status}")
        val baseTreeSha = baseCommitResponse.body<GitCommitResponse>().tree.sha

        val treeEntries = files.map { (path, content) ->
            val blobResponse: HttpResponse = client.post {
                url("$apiBase/git/blobs"); githubHeaders(); contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        BlobRequest(
                            content = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
                            encoding = "base64",
                        ),
                    ),
                )
            }
            if (!blobResponse.status.isSuccess()) error("Blob create failed for $path: ${blobResponse.status}")
            TreeEntry(path = path, mode = "100644", type = "blob", sha = blobResponse.body<BlobResponse>().sha)
        }

        val treeResponse: HttpResponse = client.post {
            url("$apiBase/git/trees"); githubHeaders(); contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TreeRequest(baseTree = baseTreeSha, tree = treeEntries)))
        }
        if (!treeResponse.status.isSuccess()) error("Tree create failed: ${treeResponse.status}")
        val newTreeSha = treeResponse.body<TreeResponse>().sha

        val commitResponse: HttpResponse = client.post {
            url("$apiBase/git/commits"); githubHeaders(); contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CommitRequest(message = commitMessage, tree = newTreeSha, parents = listOf(baseCommitSha))))
        }
        if (!commitResponse.status.isSuccess()) error("Commit create failed: ${commitResponse.status}")
        val newCommitSha = commitResponse.body<CommitResponse>().sha

        val updateRefResponse: HttpResponse = client.patch {
            url("$apiBase/git/refs/heads/$branch"); githubHeaders(); contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateRefRequest(sha = newCommitSha, force = false)))
        }
        if (!updateRefResponse.status.isSuccess()) error("Ref update failed: ${updateRefResponse.status}")

        newCommitSha
    }

    /**
     * Polls (up to [maxAttempts], every [delayMs]) for the workflow run GitHub registers for
     * [commitSha] specifically — unlike grabbing "the latest run" this can't attach to an unrelated
     * run left over from earlier activity in the same repo.
     */
    suspend fun findWorkflowRunForCommit(
        commitSha: String,
        workflowFile: String = "build.yml",
        maxAttempts: Int = 10,
        delayMs: Long = 3_000,
    ): Result<WorkflowRun> = runCatching {
        var lastStatus: String? = null
        for (attempt in 1..maxAttempts) {
            val response: HttpResponse = client.get {
                url("$apiBase/actions/workflows/$workflowFile/runs?head_sha=$commitSha")
                githubHeaders()
            }
            if (response.status.isSuccess()) {
                response.body<WorkflowRunsResponse>().workflowRuns.firstOrNull()?.let { return@runCatching it }
            } else {
                lastStatus = response.status.toString()
            }
            if (attempt < maxAttempts) delay(delayMs)
        }
        error(
            "No workflow run registered for commit $commitSha after $maxAttempts attempts" +
                (lastStatus?.let { " (last HTTP status: $it)" } ?: "") +
                " — check that .github/workflows/$workflowFile exists on the default branch",
        )
    }

    suspend fun getRun(runId: Long): Result<WorkflowRun> = runCatching {
        val response: HttpResponse = client.get { url("$apiBase/actions/runs/$runId"); githubHeaders() }
        response.body()
    }

    /** Returns the static SVG badge URL as a network-restriction-proof fallback status check. */
    fun statusBadgeUrl(workflowFile: String = "build.yml"): String =
        "https://github.com/$owner/$repo/actions/workflows/$workflowFile/badge.svg"
}

@Serializable private data class RepoResponse(@SerialName("default_branch") val defaultBranch: String)
@Serializable private data class GitRefObject(val sha: String)
@Serializable private data class GitRefResponse(@SerialName("object") val objectRef: GitRefObject)
@Serializable private data class GitCommitTree(val sha: String)
@Serializable private data class GitCommitResponse(val tree: GitCommitTree)
@Serializable private data class BlobRequest(val content: String, val encoding: String)
@Serializable private data class BlobResponse(val sha: String)
@Serializable private data class TreeEntry(val path: String, val mode: String, val type: String, val sha: String)
@Serializable private data class TreeRequest(@SerialName("base_tree") val baseTree: String, val tree: List<TreeEntry>)
@Serializable private data class TreeResponse(val sha: String)
@Serializable private data class CommitRequest(val message: String, val tree: String, val parents: List<String>)
@Serializable private data class CommitResponse(val sha: String)
@Serializable private data class UpdateRefRequest(val sha: String, val force: Boolean)

@Serializable
data class WorkflowRunsResponse(@SerialName("workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList())

@Serializable
data class WorkflowRun(
    val id: Long,
    val status: String,
    val conclusion: String? = null,
    @SerialName("html_url") val htmlUrl: String,
)
