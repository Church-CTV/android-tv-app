package com.dominickp.firetvandroidtv.util

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * CrashReportingService — crash reporting with HTTP upload and persistent retry queue.
 *
 * Provides:
 *   - Basic crash logging with stack traces
 *   - Custom key-value context storage for debugging
 *   - HTTP upload to configurable export endpoint (#3.2)
 *   - Persistent retry queue via cache file for network resilience
 *   - Automatic background upload on next app start if previous upload failed
 */
class CrashReportingService private constructor() {
    companion object {
        private const val TAG = "CrashReporting"

        @Suppress(
            "StaticFieldLeak",
            "VariableNaming",
        ) // singleton pattern — instance holds Application context, not Activity
        @Volatile
        private var _instance: CrashReportingService? = null
        private val instanceMutex = Any()

        fun getInstance(): CrashReportingService =
            _instance ?: synchronized(instanceMutex) {
                _instance ?: CrashReportingService().also { _instance = it }
            }
    }

    private val customKeys = mutableMapOf<String, String>()
    private val crashLog = StringBuilder()
    private var uploadUrl: String? = null
    private var context: Context? = null
    private val httpClient by lazy {
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Initialize with export URL and application context.
     */
    fun init(
        exportUrl: String,
        appContext: Context,
    ) {
        this.uploadUrl = exportUrl
        this.context = appContext.applicationContext
        // Set device context keys (#3.5)
        setCustomKey("device_model", DeviceUtils.deviceModel)
        setCustomKey("platform", if (DeviceUtils.isFireTV) "firetv" else "androidtv")
        setCustomKey("low_ram", DeviceUtils.isLowRam.toString())
        // Attempt to upload any pending crash logs from previous session
        uploadPendingCrashLog()
    }

    /**
     * Set a custom key-value pair for crash context.
     */
    fun setCustomKey(
        key: String,
        value: String,
    ) {
        customKeys[key] = value
        Logger.d(TAG, "Custom key set: $key = $value")
    }

    /**
     * Log a message for crash context.
     */
    fun log(message: String) {
        crashLog.appendLine("[$timestamp] $message")
        Logger.d(TAG, message)
    }

    /**
     * Report a crash with stack trace.
     *
     * NOTE: This method may be called from an uncaught exception handler,
     * so all logging is defensive — Logger calls are wrapped in try/catch
     * to prevent secondary crashes from masking the original one.
     */
    fun reportCrash(throwable: Throwable) {
        val sw = java.io.StringWriter()
        throwable.printStackTrace(java.io.PrintWriter(sw))
        val stackTrace = sw.toString()

        crashLog.appendLine("\n=== CRASH REPORT ===")
        crashLog.appendLine("Exception: ${throwable.javaClass.name}")
        crashLog.appendLine("Message: ${throwable.message}")
        crashLog.appendLine("Stack trace:")
        crashLog.appendLine(stackTrace)
        crashLog.appendLine("==================\n")

        // Defensive logging — never let a logging failure mask the original crash
        try {
            Logger.e(TAG, throwable, "Crash reported: ${throwable.message}")
        } catch (e: Exception) {
            // Logger itself failed — silently ignore to avoid masking the crash
        }

        // Save and attempt immediate upload (#3.2)
        saveCrashLog()
        uploadPendingCrashLog()
    }

    /**
     * Get the crash log (for debugging or manual upload).
     */
    fun getCrashLog(): String =
        buildString {
            appendLine("=== CRASH CONTEXT ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Custom keys:")
            customKeys.forEach { (key, value) ->
                appendLine("  $key = $value")
            }
            appendLine("\n=== CRASH LOG ===")
            appendLine(crashLog)
            appendLine("=================")
        }

    /**
     * Upload crash log to the configured endpoint (#3.2).
     * Persists queue file after successful upload to clear pending state.
     * Runs on a background thread to avoid blocking the caller.
     */
    private fun uploadPendingCrashLog() {
        val url = uploadUrl ?: return
        val ctx = context ?: return
        uploadWithUrlAsync(url, ctx)
    }

    private fun uploadWithUrlAsync(
        url: String,
        ctx: Context,
    ) {
        val crashLogDir = File(ctx.cacheDir, "crash")
        crashLogDir.mkdirs()
        val crashLogFile = File(crashLogDir, "pending_crash_log.txt")
        if (!crashLogFile.exists()) return

        // Read crash content on calling thread, upload on background thread
        val crashContent = crashLogFile.readText()
        val request =
            Request
                .Builder()
                .url(url)
                .post(crashContent.toRequestBody("text/plain".toMediaType()))
                .header(
                    "X-Crash-Context",
                    customKeys.entries.joinToString("&") { "${it.key}=${it.value}" },
                ).build()

        // Use OkHttp's built-in async call to avoid blocking the caller
        httpClient
            .newCall(request)
            .enqueue(
                object : okhttp3.Callback {
                    override fun onFailure(
                        call: okhttp3.Call,
                        e: java.io.IOException,
                    ) {
                        Logger.w(TAG, "Failed to upload crash log: ${e.message}")
                        // File persists for retry on next app start
                    }

                    override fun onResponse(
                        call: okhttp3.Call,
                        response: okhttp3.Response,
                    ) {
                        if (response.isSuccessful) {
                            crashLogFile.delete() // Clear after successful upload
                            Logger.i(TAG, "Crash log uploaded successfully")
                        } else {
                            Logger.w(TAG, "Crash upload failed: HTTP ${response.code}")
                            // File persists for retry on next app start
                        }
                        response.close()
                    }
                },
            )
    }

    /**
     * Save crash log to file for persistence and future upload.
     */
    private fun saveCrashLog() {
        try {
            val ctx = context ?: return
            val crashLogDir = File(ctx.cacheDir, "crash")
            crashLogDir.mkdirs()
            val crashLogFile = File(crashLogDir, "pending_crash_log.txt")
            crashLogFile.writeText(crashLog.toString())
            Logger.i(TAG, "Crash log saved to: ${crashLogFile.absolutePath}")
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to save crash log: ${e.message}")
        }
    }

    private val timestamp: String
        get() = TimestampFormatter.format(Date())

    // Thread-safe timestamp formatter — ThreadLocal avoids SimpleDateFormat concurrency bugs.
    // SimpleDateFormat is small and CrashReportingService is a singleton for app lifetime,
    // so ThreadLocal retention is acceptable.
    private object TimestampFormatter {
        private val threadLocal =
            ThreadLocal.withInitial {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            }

        fun format(date: Date): String {
            val formatter =
                requireNotNull(threadLocal.get()) {
                    "ThreadLocal SimpleDateFormat not initialized"
                }
            return formatter.format(date)
        }
    }
}
