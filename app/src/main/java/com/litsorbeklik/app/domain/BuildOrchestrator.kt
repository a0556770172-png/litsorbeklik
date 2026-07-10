package com.litsorbeklik.app.domain

import com.litsorbeklik.app.data.engines.AiEngine
import com.litsorbeklik.app.data.engines.BuildEngine
import com.litsorbeklik.app.data.model.AppSpec
import com.litsorbeklik.app.data.model.BuildRun
import com.litsorbeklik.app.data.model.GeneratedProject
import com.litsorbeklik.app.data.repository.BuildRunsRepository

/**
 * Drives the whole "spec → code → validate → (fix loop) → build" pipeline described in the
 * architecture doc (section 3, steps 4-7) end to end, independent of any UI framework so it can
 * be unit tested and reused by whichever screen kicks off a build.
 */
class BuildOrchestrator(
    private val aiEngine: AiEngine,
    private val buildEngine: BuildEngine,
    private val buildRunsRepository: BuildRunsRepository = BuildRunsRepository(),
    private val maxFixAttempts: Int = 3,
) {
    sealed interface Progress {
        data object GeneratingCode : Progress
        data class Validating(val attempt: Int) : Progress
        data class FixingErrors(val attempt: Int) : Progress
        data object StartingBuild : Progress
        data class Done(val run: BuildRun) : Progress
        data class Failed(val reason: String) : Progress
    }

    suspend fun run(projectId: String, spec: AppSpec, onProgress: suspend (Progress) -> Unit) {
        onProgress(Progress.GeneratingCode)
        var project = aiEngine.generateProject(spec).getOrElse {
            onProgress(Progress.Failed("יצירת הקוד נכשלה: ${it.message}"))
            return
        }

        var attempt = 0
        var validation = ProjectValidator.validate(project)
        while (!validation.isValid && attempt < maxFixAttempts) {
            attempt += 1
            onProgress(Progress.Validating(attempt))
            onProgress(Progress.FixingErrors(attempt))
            val errorSummary = "בדיקת תקינות מקומית נכשלה:\n" + validation.issues.joinToString("\n")
            project = aiEngine.fixBuildError(spec, project, errorSummary).getOrElse {
                onProgress(Progress.Failed("תיקון הקוד נכשל בניסיון $attempt: ${it.message}"))
                return
            }
            validation = ProjectValidator.validate(project)
        }

        if (!validation.isValid) {
            onProgress(Progress.Failed("הפרויקט לא עבר ולידציה אחרי $maxFixAttempts ניסיונות: ${validation.issues.joinToString("; ")}"))
            return
        }

        onProgress(Progress.StartingBuild)
        val engineName = if (buildEngine.id.startsWith("github")) "GITHUB" else "LOCAL"
        val persistedRun = buildRunsRepository.createRun(projectId, engineName).getOrElse {
            onProgress(Progress.Failed("לא ניתן היה ליצור רשומת build_runs: ${it.message}"))
            return
        }

        val startedRun = buildEngine.startBuild(project).getOrElse {
            val failedRun = persistedRun.copy(status = "failed")
            buildRunsRepository.updateRun(failedRun)
            onProgress(Progress.Failed("הפעלת הבנייה נכשלה: ${it.message}"))
            return
        }

        val mergedRun = startedRun.copy(id = persistedRun.id, projectId = projectId)
        buildRunsRepository.updateRun(mergedRun)
        onProgress(Progress.Done(mergedRun))
    }

    /** Called by a screen/worker later to poll an in-flight run and persist any status change. */
    suspend fun refresh(run: BuildRun): Result<BuildRun> {
        val refreshed = buildEngine.refreshStatus(run).getOrElse { return Result.failure(it) }
        buildRunsRepository.updateRun(refreshed)
        return Result.success(refreshed)
    }
}
