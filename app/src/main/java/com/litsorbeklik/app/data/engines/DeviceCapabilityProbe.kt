package com.litsorbeklik.app.data.engines

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Real on-device measurement feeding [DeviceCapability.detect]. Kept separate from the pure
 * function so the tiering logic itself stays trivially testable without a Context/Robolectric.
 *
 * NPU detection is necessarily heuristic — there is no public Android API that cleanly answers
 * "does this SoC expose an NPU my chosen runtime can use". We match known-good chipset families;
 * anything unrecognized falls back to CPU-only tiering rather than over-promising.
 */
object DeviceCapabilityProbe {

    private val NPU_HARDWARE_HINTS = listOf(
        "sm8650", "sm8550", "sm8450", // Snapdragon 8 Gen 3/2/1 (Hexagon NPU)
        "sm7675", "sm7550",           // Snapdragon 7+ Gen 3/7 Gen 3
        "exynos2400", "exynos2200",   // Samsung Exynos NPU
    )

    fun detect(context: Context): DeviceCapability.ModelTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)

        val hardware = (Build.HARDWARE + Build.BOARD + socModelSafe()).lowercase()
        val hasSupportedNpu = NPU_HARDWARE_HINTS.any { hardware.contains(it) }

        return DeviceCapability.detect(totalRamMb, hasSupportedNpu)
    }

    /** Build.SOC_MODEL requires API 31+; falls back to empty string on older devices. */
    private fun socModelSafe(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL ?: "" else ""
}
