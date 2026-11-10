package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnalyticsTrackerResetLeakTest — verifies that resetInstance()
 * does not leak the flushScheduler's ScheduledExecutorService thread.
 *
 * Bug: stopScheduledFlush() creates a new ScheduledExecutorService after
 * shutting down the old one. When resetInstance() is called,
 * this new scheduler is orphaned — never shut down, never nulled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerResetLeakTest {
    private val config =
        AnalyticsConfig(
            enabled = true,
            exportUrl = "http://localhost:9999/analytics",
            exportIntervalSeconds = 300,
            maxEventsPerBatch = 50,
            flushOnError = true,
        )

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
    }

    /**
     * Verifying RED: This test fails because resetInstance() leaves an
     * orphaned "AnalyticsFlush" thread running after the instance is cleared.
     */
    @Test
    fun `resetInstanceDoesNotLeakFlushSchedulerThread()`() {
        // Create an instance — this starts the flushScheduler
        val tracker1 = AnalyticsTracker.getInstance(config)
        tracker1.trackPlay("video1", "http://stream.mp4", false, "home")

        // Count AnalyticsFlush threads before reset
        val threadsBefore = countAnalyticsFlushThreads()

        // Reset the singleton
        AnalyticsTracker.resetInstance()

        // Wait for any pending work to settle
        Thread.sleep(500)

        // Count AnalyticsFlush threads after reset
        val threadsAfter = countAnalyticsFlushThreads()

        // BUG: resetInstance() calls stopScheduledFlush() which creates a
        // new ScheduledExecutorService. This new scheduler is orphaned.
        // The test expects 0 flush threads after reset.
        assertEquals(
            "resetInstance() should shut down all flush scheduler threads — " +
                "found $threadsAfter AnalyticsFlush threads after reset",
            0,
            threadsAfter,
        )
    }

    private fun countAnalyticsFlushThreads(): Int {
        return Thread.getAllStackTraces().keys
            .filter { it.name == "AnalyticsFlush" }
            .count()
    }
}
