package com.litsorbeklik.app.data.engines

import com.litsorbeklik.app.data.engines.net.AnthropicClient
import com.litsorbeklik.app.data.engines.net.GeminiClient
import com.litsorbeklik.app.data.engines.net.GrokClient
import com.litsorbeklik.app.data.engines.net.HttpClientProvider
import com.litsorbeklik.app.data.engines.net.OpenAiClient
import com.litsorbeklik.app.data.engines.net.extractJsonPayload
import com.litsorbeklik.app.data.model.AiProvider
import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.model.GeneratedProject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Calls the end-user's own API key against Gemini / OpenAI / Claude / Grok directly from the
 * device (no proxy). Every provider is normalized behind the same [AiEngine] contract — the rest
 * of the app never branches on which one is active.
 */
class CloudAiEngine(
    private val provider: AiProvider,
    private val apiKey: String,
) : AiEngine {

    override val id: String = "cloud:${provider.name.lowercase()}"

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank()

    private suspend fun callProvider(prompt: String): String = when (provider) {
        AiProvider.GEMINI -> GeminiClient.generateText(prompt, apiKey)
        AiProvider.OPENAI -> OpenAiClient.generateText(prompt, apiKey)
        AiProvider.CLAUDE -> AnthropicClient.generateText(prompt, apiKey)
        AiProvider.GROK -> GrokClient.generateText(prompt, apiKey)
    }

    override suspend fun chatSpecStep(history: List<ChatTurn>, userMessage: String): ChatTurn {
        val transcript = history.joinToString("\n") { "${it.role}: ${it.text}" }
        val prompt = """
            את/ה מרכז/ת אפיון של אפליקציית אנדרואיד עם משתמש/ת. נהל/י ראיון קצר וממוקד כדי להבין:
            מטרת האפליקציה, מסכים עיקריים, פיצ'רים חשובים, ומודל הנתונים. שאל/י שאלה אחת בכל פעם.
            כשיש מידע מספיק, סכם/י ותציע/י לעבור לאישור האפיון.

            היסטוריית השיח עד כה:
            $transcript

            הודעת המשתמש/ת האחרונה: $userMessage

            השב/י בעברית, בקצרה, בשאלה או סיכום אחד בלבד.
        """.trimIndent()
        val reply = callProvider(prompt)
        return ChatTurn(role = "assistant", text = reply)
    }

    override suspend fun generateProject(spec: AppSpec): Result<GeneratedProject> = runCatching {
        val prompt = buildProjectGenerationPrompt(spec)
        val raw = callProvider(prompt)
        parseGeneratedProject(raw)
    }

    override suspend fun fixBuildError(
        spec: AppSpec,
        project: GeneratedProject,
        errorLog: String,
    ): Result<GeneratedProject> = runCatching {
        val filesJson = HttpClientProvider.json.encodeToString(project)
        val prompt = """
            הפרויקט הבא נכשל בקומפילציה ב-CI (GitHub Actions / Gradle). קבל/י את לוג השגיאה, אתר/י את
            הקבצים הבעייתיים, ותקן/י אותם. החזר/י מחדש **את כל הפרויקט המלא** (כל הקבצים, גם אלו שלא
            השתנו) באותו פורמט JSON קשיח כמו קודם — ללא טקסט נוסף מסביב, ללא markdown fences.

            הפרויקט הנוכחי (JSON):
            $filesJson

            לוג השגיאה מה-CI:
            ${errorLog.take(8000)}
        """.trimIndent()
        val raw = callProvider(prompt)
        parseGeneratedProject(raw)
    }

    private fun buildProjectGenerationPrompt(spec: AppSpec): String = """
        צור/י פרויקט אנדרואיד מלא (Kotlin + Jetpack Compose, Material 3, compileSdk 35, minSdk 26)
        לפי האפיון הבא:

        מטרה: ${spec.goal}
        מסכים: ${spec.screens.joinToString(", ")}
        פיצ'רים: ${spec.features.joinToString(", ")}
        ${spec.rawText?.let { "אפיון חופשי נוסף:\n$it" } ?: ""}

        חובה להחזיר **רק** JSON תקין (ללא markdown fences, ללא הסברים) במבנה המדויק הזה:
        {
          "applicationId": "com.example.something",
          "appName": "שם האפליקציה",
          "files": [
            { "path": "app/src/main/AndroidManifest.xml", "content": "..." },
            { "path": "app/build.gradle.kts", "content": "..." }
          ]
        }

        חובה לכלול לפחות: AndroidManifest.xml, app/build.gradle.kts, settings.gradle.kts, MainActivity.kt,
        וכל קובצי ה-res/Kotlin הדרושים כדי שהפרויקט יתקמפל בעצמו (ללא תלות בקבצים חיצוניים).
    """.trimIndent()

    private fun parseGeneratedProject(raw: String): GeneratedProject {
        val jsonPayload = extractJsonPayload(raw)
        return HttpClientProvider.json.decodeFromString<GeneratedProject>(jsonPayload)
    }
}
