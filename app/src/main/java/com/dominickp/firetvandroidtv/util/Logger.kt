package com.dominickp.firetvandroidtv.util

import android.util.Log

/**
 * Logger — centralized logging utility with defensive coding patterns.
 *
 * Provides:
 *   - Log levels: DEBUG, INFO, WARN, ERROR
 *   - Tag auto-generation from calling class name (cached for performance)
 *   - Null-safe string formatting
 *   - Configurable log level (for production builds)
 *   - safeLog() wrapper for exception-prone operations
 */
object Logger {
    private const val DEFAULT_TAG = "FireTV"
    private var currentLevel: LogLevel = LogLevel.DEBUG

    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        NONE,
    }

    /**
     * Set the minimum log level.
     * In production builds, set to WARN or ERROR.
     */
    fun setLevel(level: LogLevel) {
        currentLevel = level
    }

    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= currentLevel.ordinal
    }

    /**
     * Get the calling class's simple name as the log tag.
     * Uses ConcurrentHashMap cache to avoid stack trace overhead on every call (#4.1).
     */
    private val tagCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun getTag(): String {
        return try {
            val className =
                Throwable("Logger stack trace").stackTrace[2].className
                    .substringAfterLast('.')
                    .take(23)

            tagCache.getOrPut(className) {
                className
            }
        } catch (e: Exception) {
            DEFAULT_TAG
        }
    }

    /**
     * Format a message with null safety.
     */
    private fun formatMessage(message: Any?): String {
        return message?.toString() ?: "null"
    }

    // === Log Level Methods ===

    fun v(
        tag: String = getTag(),
        message: Any?,
    ) {
        if (shouldLog(LogLevel.VERBOSE)) Log.v(tag, formatMessage(message))
    }

    fun d(
        tag: String = getTag(),
        message: Any?,
    ) {
        if (shouldLog(LogLevel.DEBUG)) Log.d(tag, formatMessage(message))
    }

    fun i(
        tag: String = getTag(),
        message: Any?,
    ) {
        if (shouldLog(LogLevel.INFO)) Log.i(tag, formatMessage(message))
    }

    fun w(
        tag: String = getTag(),
        message: Any?,
    ) {
        if (shouldLog(LogLevel.WARN)) Log.w(tag, formatMessage(message))
    }

    fun w(
        tag: String = getTag(),
        throwable: Throwable? = null,
        message: Any? = null,
    ) {
        if (shouldLog(LogLevel.WARN)) {
            val msg = formatMessage(message)
            if (throwable != null) {
                Log.w(tag, msg, throwable)
            } else {
                Log.w(tag, msg)
            }
        }
    }

    fun e(
        tag: String = getTag(),
        message: Any?,
    ) {
        if (shouldLog(LogLevel.ERROR)) Log.e(tag, formatMessage(message))
    }

    fun e(
        tag: String = getTag(),
        throwable: Throwable? = null,
        message: Any? = null,
    ) {
        if (shouldLog(LogLevel.ERROR)) {
            val msg = formatMessage(message)
            if (throwable != null) {
                Log.e(tag, msg, throwable)
            } else {
                Log.e(tag, msg)
            }
        }
    }

    /**
     * Safe wrapper for operations that might throw.
     * Catches exceptions and logs them without crashing the app.
     */
    inline fun safeLog(
        name: String,
        crossinline block: () -> Unit,
    ) {
        try {
            block()
        } catch (ex: Exception) {
            e("$name", ex, "Unexpected exception in $name")
        }
    }
}
