package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnalyticsTrackerExecutorReuseTest — verifies that sendBatchAsync reuses
 * a single shared executor instead of creating a new one per batch.
 *
 * Bug: sendBatchAsync created Executors.newSingleThreadExecutor for every
 * batch export. With auto-export at half-queue (50 events), this created
 * 50 thread pools per flush — contradicting the comment "avoid flooding
 * with threads".
 *
 * Fix: use a shared single-thread executor (like sharedHttpClient).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerExecutorReuseTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
        server.shutdown()
    }

    @Test
    fun `multipleBatchExportsReuseSingleExecutor()`() {
        // Enqueue enough 200 responses for all exports
        // 55 events → auto-export at 50 → 1 request for 50 events
        // remaining 5 events → flush() → 1 request for 5 events
        // Total: 2 requests
        for (i in 0 until 5) {
            server.enqueue(MockResponse().setResponseCode(200))
        }

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )

        // Track 55 events — triggers auto-export at 50, then flush for remaining 5
        for (i in 0 until 55) {
            tracker.trackImpression("video_$i", "Row", i)
        }

        // Flush remaining events
        tracker.flush()

        // Wait for all background work to settle
        Thread.sleep(3000)

        // Two exports: 50 events auto-exported + 5 events on flush
        assertEquals(
            "All events should be exported exactly once",
            2,
            server.requestCount,
        )

        // Verify no thread count explosion: if each export created a
        // new executor, Thread.getAllStackTraces() would show many
        // "AnalyticsExport" threads. With a shared executor, there
        // should be at most 1.
        val exportThreads =
            Thread.getAllStackTraces().keys
                .filter { it.name.startsWith("AnalyticsExport") }
                .distinct()
                .count()

        // The fix: at most 1 export thread alive at any time
        assert(exportThreads <= 1) {
            "Expected at most 1 AnalyticsExport thread, found $exportThreads — " +
                "sendBatchAsync is creating new executors instead of reusing a shared one"
        }
    }
}
