package com.litsorbeklik.app.data.engines.net

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Google Gemini — generateContent REST endpoint. */
object GeminiClient {
    private const val MODEL = "gemini-2.0-flash"

    suspend fun generateText(prompt: String, apiKey: String): String {
        val body = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))
        val response: HttpResponse = HttpClientProvider.client.post {
            url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
            contentType(ContentType.Application.Json)
            setBody(HttpClientProvider.json.encodeToString(body))
        }
        val parsed = response.body<GeminiResponse>()
        return parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Gemini returned no candidates (check quota / safety filters)")
    }
}

@Serializable private data class GeminiRequest(val contents: List<GeminiContent>)
@Serializable private data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)
@Serializable private data class GeminiPart(val text: String)
@Serializable private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())
@Serializable private data class GeminiCandidate(val content: GeminiContent? = null)
