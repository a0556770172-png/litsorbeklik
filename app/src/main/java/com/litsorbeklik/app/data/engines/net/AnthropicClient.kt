package com.litsorbeklik.app.data.engines.net

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Anthropic Claude — Messages API. */
object AnthropicClient {
    private const val MODEL = "claude-sonnet-4-5"
    private const val API_VERSION = "2023-06-01"

    suspend fun generateText(prompt: String, apiKey: String): String {
        val body = MessagesRequest(
            model = MODEL,
            maxTokens = 8192,
            messages = listOf(AnthropicMessage(role = "user", content = prompt)),
        )
        val response: HttpResponse = HttpClientProvider.client.post {
            url("https://api.anthropic.com/v1/messages")
            header("x-api-key", apiKey)
            header("anthropic-version", API_VERSION)
            contentType(ContentType.Application.Json)
            setBody(HttpClientProvider.json.encodeToString(body))
        }
        val parsed = response.body<MessagesResponse>()
        return parsed.content.firstOrNull()?.text ?: error("Claude returned no content blocks")
    }
}

@Serializable private data class MessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
)
@Serializable private data class AnthropicMessage(val role: String, val content: String)
@Serializable private data class MessagesResponse(val content: List<AnthropicContentBlock> = emptyList())
@Serializable private data class AnthropicContentBlock(val text: String? = null)
