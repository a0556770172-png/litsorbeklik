package com.litsorbeklik.app.domain

import com.litsorbeklik.app.data.model.GeneratedProject

/**
 * Cheap, offline structural checks run on a freshly AI-generated project BEFORE it is pushed to
 * a build engine. Catches obviously-broken output early so the (slow, network-dependent) build
 * step isn't wasted on something we could have rejected in milliseconds — the caller should loop
 * this against [com.litsorbeklik.app.data.engines.AiEngine.fixBuildError] until it passes or a
 * retry budget is exhausted.
 */
object ProjectValidator {

    private val REQUIRED_SUFFIXES = listOf(
        "AndroidManifest.xml",
        "build.gradle.kts",
        "settings.gradle.kts",
    )

    data class ValidationResult(val isValid: Boolean, val issues: List<String>)

    fun validate(project: GeneratedProject): ValidationResult {
        val issues = mutableListOf<String>()
        val pathsByFile = project.files.associateBy { it.path }

        REQUIRED_SUFFIXES.forEach { required ->
            if (pathsByFile.keys.none { it.endsWith(required) }) {
                issues += "קובץ חובה חסר: $required"
            }
        }

        if (project.applicationId.isBlank() || !project.applicationId.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))) {
            issues += "applicationId לא תקין: '${project.applicationId}'"
        }

        val manifestFile = pathsByFile.entries.firstOrNull { it.key.endsWith("AndroidManifest.xml") }?.value
        if (manifestFile != null && !manifestFile.content.contains("<manifest")) {
            issues += "AndroidManifest.xml לא מכיל תג <manifest> תקין"
        }

        val duplicatePaths = project.files.groupBy { it.path }.filter { it.value.size > 1 }.keys
        if (duplicatePaths.isNotEmpty()) {
            issues += "נתיבי קבצים כפולים: ${duplicatePaths.joinToString(", ")}"
        }

        project.files.forEach { file ->
            if (file.path.contains("..") || file.path.startsWith("/")) {
                issues += "נתיב לא בטוח (path traversal חשוד): ${file.path}"
            }
        }

        val xmlFiles = project.files.filter { it.path.endsWith(".xml") }
        xmlFiles.forEach { file ->
            val opens = Regex("<([a-zA-Z0-9_.-]+)(?:\\s[^>]*)?>").findAll(file.content).count()
            val selfClosing = Regex("<[a-zA-Z0-9_.-]+(?:\\s[^>]*)?/>").findAll(file.content).count()
            val closes = Regex("</[a-zA-Z0-9_.-]+>").findAll(file.content).count()
            if (opens - selfClosing != closes) {
                issues += "XML חשוד כלא מאוזן: ${file.path}"
            }
        }

        return ValidationResult(isValid = issues.isEmpty(), issues = issues)
    }
}
