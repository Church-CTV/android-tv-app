package com.dominickp.firetvandroidtv.util

import android.os.Build

/**
 * DeviceUtils — runtime device detection and platform adaptations.
 *
 * Provides:
 *   - Fire TV detection (Build.MANUFACTURER == "Amazon")
 *   - Android TV vs Fire TV distinction
 *   - Low-RAM device detection for animation reduction
 *
 * Used by AnalyticsTracker, CrashReportingService, and UI components
 * to adapt behavior per platform.
 */
object DeviceUtils {
    private val _isFireTV by lazy { Build.MANUFACTURER == "Amazon" }
    private val _isAndroidTV by lazy {
        Build.MANUFACTURER != "Amazon" &&
            (
                Build.MODEL.contains("Android TV", ignoreCase = true) ||
                    Build.HARDWARE == "qcom" ||
                    hasLeanback()
            )
    }

    val isFireTV: Boolean
        get() = _isFireTV

    val isAndroidTV: Boolean
        get() = _isAndroidTV

    val isTv: Boolean
        get() = isFireTV || isAndroidTV

    /**
     * Detect low-RAM devices (< 2GB) for potential animation reduction.
     */
    val isLowRam by lazy {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        maxMemory < 2L * 1024 * 1024 * 1024
    }

    /**
     * Device identifier for analytics and crash reporting.
     */
    val deviceModel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"

    private fun hasLeanback(): Boolean =
        try {
            Class.forName("androidx.leanback.app.LeanbackActivity")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
}
