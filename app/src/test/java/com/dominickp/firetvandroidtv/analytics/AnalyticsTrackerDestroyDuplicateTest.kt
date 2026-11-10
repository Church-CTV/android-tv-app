package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnalyticsTrackerDestroyDuplicateTest — verifies that resetInstance() is the
 * canonical cleanup method. After removing the duplicate destroy() method,
 * resetInstance() must still fully clean up the singleton and shared resources.
 *
 * This test captures the behavior we want to preserve after deleting the
 * dead-code destroy() method (which was identical to resetInstance()).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerDestroyDuplicateTest {
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
     * Verifies that resetInstance() fully cleans up the singleton so that
     * a subsequent getInstance() returns a fresh instance.
     */
    @Test
    fun `resetInstanceProducesFreshInstance()`() {
        val tracker1 = AnalyticsTracker.getInstance(config)
        tracker1.trackPlay("video1", "http://stream.mp4", false, "home")

        AnalyticsTracker.resetInstance()

        val tracker2 = AnalyticsTracker.getInstance(config)
        assertNotSame(
            "resetInstance() should produce a fresh singleton instance",
            tracker1,
            tracker2,
        )
    }

    /**
     * Verifies that resetInstance() shuts down the shared executor so that
     * no orphaned threads remain.
     */
    @Test
    fun `resetInstanceShutsDownSharedExecutor()`() {
        val tracker = AnalyticsTracker.getInstance(config)
        tracker.trackPlay("video1", "http://stream.mp4", false, "home")

        val exportThreadsBefore = countAnalyticsExportThreads()

        AnalyticsTracker.resetInstance()

        Thread.sleep(500)

        val exportThreadsAfter = countAnalyticsExportThreads()

        assertEquals(
            "resetInstance() should shut down the shared export executor — " +
                "found $exportThreadsAfter AnalyticsExport threads after reset",
            0,
            exportThreadsAfter,
        )
    }

    private fun countAnalyticsExportThreads(): Int {
        return Thread.getAllStackTraces().keys
            .filter { it.name.startsWith("AnalyticsExport") }
            .count()
    }
}
