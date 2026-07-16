package com.litsorbeklik.app.data.engines.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Single shared Ktor client for all Cloud AI provider calls — plain device networking, no proxy. */
object HttpClientProvider {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                // Generous headroom for a full project-generation response (tens of thousands of
                // output tokens) on a non-streaming call — see AnthropicClient's MAX_TOKENS comment.
                requestTimeoutMillis = 300_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 300_000
            }
        }
    }
}

/** Strips ```json ... ``` fences some models wrap their JSON answer in, before parsing. */
fun extractJsonPayload(raw: String): String {
    val trimmed = raw.trim()
    val fenceStart = trimmed.indexOf("```")
    if (fenceStart == -1) return trimmed
    val afterFence = trimmed.substring(fenceStart + 3).removePrefix("json").trimStart('\n', '\r')
    val fenceEnd = afterFence.lastIndexOf("```")
    return if (fenceEnd == -1) afterFence else afterFence.substring(0, fenceEnd)
}
