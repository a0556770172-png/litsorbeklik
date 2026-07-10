package com.litsorbeklik.app.data.engines.net

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** OpenAI-compatible chat completions — also reused by [GrokClient] since xAI mirrors this shape. */
object OpenAiCompatibleClient {
    suspend fun generateText(
        endpoint: String,
        model: String,
        prompt: String,
        apiKey: String,
    ): String {
        val body = ChatRequest(model = model, messages = listOf(ChatMessage(role = "user", content = prompt)))
        val response: HttpResponse = HttpClientProvider.client.post {
            url(endpoint)
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(HttpClientProvider.json.encodeToString(body))
        }
        val parsed = response.body<ChatResponse>()
        return parsed.choices.firstOrNull()?.message?.content
            ?: error("$endpoint returned no choices (check quota / model name)")
    }
}

object OpenAiClient {
    private const val MODEL = "gpt-4o-mini"
    suspend fun generateText(prompt: String, apiKey: String): String =
        OpenAiCompatibleClient.generateText("https://api.openai.com/v1/chat/completions", MODEL, prompt, apiKey)
}

object GrokClient {
    private const val MODEL = "grok-2-latest"
    suspend fun generateText(prompt: String, apiKey: String): String =
        OpenAiCompatibleClient.generateText("https://api.x.ai/v1/chat/completions", MODEL, prompt, apiKey)
}

@Serializable private data class ChatRequest(val model: String, val messages: List<ChatMessage>)
@Serializable private data class ChatMessage(val role: String, val content: String)
@Serializable private data class ChatResponse(val choices: List<ChatChoice> = emptyList())
@Serializable private data class ChatChoice(val message: ChatMessage)
