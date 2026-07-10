package com.litsorbeklik.app.data.engines.net

import android.util.Base64
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Talks to the GitHub REST API from the *shipped app*, running on the end-user's own device —
 * this is unrelated to (and unaffected by) any network restrictions in a development sandbox.
 *
 * Uses the Contents API to create/update files one by one (no git binary / JGit needed on
 * Android) and the Actions API to find and poll the run that a push triggers.
 */
class GithubApiClient(
    private val owner: String,
    private val repo: String,
    private val token: String,
) {
    private val client get() = HttpClientProvider.client
    private val json get() = HttpClientProvider.json
    private val apiBase = "https://api.github.com/repos/$owner/$repo"

    /** Creates or updates a single file via the Contents API. Looks up the current sha first if it exists. */
    suspend fun putFile(path: String, content: String, commitMessage: String): Result<Unit> = runCatching {
        val existingSha = getFileSha(path)
        val body = ContentsPutRequest(
            message = commitMessage,
            content = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            sha = existingSha,
        )
        val response: HttpResponse = client.put {
            url("$apiBase/contents/$path")
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
        if (!response.status.isSuccess()) {
            error("PUT contents failed for $path: ${response.status}")
        }
    }

    private suspend fun getFileSha(path: String): String? {
        val response: HttpResponse = client.get {
            url("$apiBase/contents/$path")
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) return null
        return response.body<ContentsGetResponse>().sha
    }

    /** Pushes every file in the generated project, one Contents API call each. */
    suspend fun pushProject(files: Map<String, String>, commitMessage: String): Result<Unit> = runCatching {
        files.forEach { (path, content) ->
            putFile(path, content, commitMessage).getOrThrow()
        }
    }

    /** Finds the most recent workflow run (used right after a push to start tracking it). */
    suspend fun latestWorkflowRun(workflowFile: String = "build.yml"): Result<WorkflowRun> = runCatching {
        val response: HttpResponse = client.get {
            url("$apiBase/actions/workflows/$workflowFile/runs?per_page=1")
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
        }
        response.body<WorkflowRunsResponse>().workflowRuns.firstOrNull()
            ?: error("No workflow runs found yet — the push may not have triggered one")
    }

    suspend fun getRun(runId: Long): Result<WorkflowRun> = runCatching {
        val response: HttpResponse = client.get {
            url("$apiBase/actions/runs/$runId")
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
        }
        response.body()
    }

    /** Returns the static SVG badge URL as a network-restriction-proof fallback status check. */
    fun statusBadgeUrl(workflowFile: String = "build.yml"): String =
        "https://github.com/$owner/$repo/actions/workflows/$workflowFile/badge.svg"
}

@Serializable
private data class ContentsPutRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String? = null,
)

@Serializable
private data class ContentsGetResponse(val sha: String)

@Serializable
data class WorkflowRunsResponse(@SerialName("workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList())

@Serializable
data class WorkflowRun(
    val id: Long,
    val status: String,
    val conclusion: String? = null,
    @SerialName("html_url") val htmlUrl: String,
)
