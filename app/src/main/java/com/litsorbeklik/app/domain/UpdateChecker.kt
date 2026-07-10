package com.litsorbeklik.app.domain

import com.litsorbeklik.app.data.repository.AppVersionsRepository
import com.litsorbeklik.app.data.repository.LatestAppVersion

/** Pure comparison against the running app's own versionCode — no Android APIs, easy to test. */
class UpdateChecker(
    private val repository: AppVersionsRepository = AppVersionsRepository(),
) {
    suspend fun checkForUpdate(currentVersionCode: Int): LatestAppVersion? {
        val latest = repository.getLatest().getOrNull() ?: return null
        return if (latest.versionCode > currentVersionCode) latest else null
    }
}
