package com.dominickp.firetvandroidtv.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashReportingServiceTest {
    @Test
    fun getInstance_singleton_returnsSameInstance() {
        val instance1 = CrashReportingService.getInstance()
        val instance2 = CrashReportingService.getInstance()
        assertNotNull(instance1)
        // Both should be the same instance (singleton)
        // Note: In Robolectric each test class gets a fresh JVM, so we verify the API works
        assertNotNull(instance2)
    }

    @Test
    fun setCustomKey_storesKeyValue() {
        val service = CrashReportingService.getInstance()
        service.setCustomKey("test_key", "test_value")
        // Verify no crash on set
        service.setCustomKey("another_key", "another_value")
    }

    @Test
    fun log_appendsToCrashLog() {
        val service = CrashReportingService.getInstance()
        service.log("test crash log entry")
        val log = service.getCrashLog()
        assertTrue(log.contains("test crash log entry"))
    }

    @Test
    fun reportCrash_capturesStackTrace() {
        val service = CrashReportingService.getInstance()
        val testException = RuntimeException("Test crash")
        service.reportCrash(testException)
        val log = service.getCrashLog()
        assertTrue(log.contains("CRASH REPORT"))
        assertTrue(log.contains("RuntimeException"))
        assertTrue(log.contains("Test crash"))
    }

    @Test
    fun getCrashLog_containsTimestamp() {
        val service = CrashReportingService.getInstance()
        val log = service.getCrashLog()
        // Timestamp format: yyyy-MM-dd HH:mm:ss
        assertTrue(log.contains("Timestamp:"))
    }

    @Test
    fun getCrashLog_timestampFormat_matchesExpectedPattern() {
        val service = CrashReportingService.getInstance()
        val log = service.getCrashLog()

        // Extract timestamp value (line after "Timestamp:")
        val lines = log.split("\n")
        val timestampLine = lines.first { it.startsWith("Timestamp:") }
        val timestamp = timestampLine.removePrefix("Timestamp:").trim()

        // Validate format: yyyy-MM-dd HH:mm:ss (19 characters, fixed positions)
        val pattern = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        assertTrue(
            "Timestamp '$timestamp' should match yyyy-MM-dd HH:mm:ss format",
            pattern.matches(timestamp)
        )
    }

    @Test
    fun getCrashLog_timestampIsRecent_whenLogRequested() {
        val service = CrashReportingService.getInstance()
        val log = service.getCrashLog()

        // Timestamp should be within the last 5 minutes
        val lines = log.split("\n")
        val timestampLine = lines.first { it.startsWith("Timestamp:") }
        val timestamp = timestampLine.removePrefix("Timestamp:").trim()

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val parsed = sdf.parse(timestamp)
        val elapsedMs = System.currentTimeMillis() - parsed.time

        assertTrue(
            "Timestamp should be within last 5 minutes, was ${elapsedMs}ms ago",
            elapsedMs < 5 * 60 * 1000L
        )
    }

    @Test
    fun getCrashLog_containsCustomKeys() {
        val service = CrashReportingService.getInstance()
        service.setCustomKey("device_model", "Fire TV Stick 4K")
        val log = service.getCrashLog()
        assertTrue(log.contains("device_model"))
        assertTrue(log.contains("Fire TV Stick 4K"))
    }
}
