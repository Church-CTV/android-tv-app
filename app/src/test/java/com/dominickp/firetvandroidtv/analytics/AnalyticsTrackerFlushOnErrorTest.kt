package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnalyticsTrackerFlushOnErrorTest — verifies that flushOnError does NOT
 * create unbounded executors on repeated failures.
 *
 * Bug: sendBatchAsync creates a new Executors.newSingleThreadExecutor
 * for every retry when flushOnError=true. If the server stays down,
 * this creates an unbounded chain of threads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerFlushOnErrorTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
        server.shutdown()
    }

    @Test
    fun `flushOnErrorRepeatedFailuresDoNotCreateUnboundedExecutors()`() {
        // All requests fail — server stays down
        for (i in 0 until 20) {
            server.enqueue(MockResponse().setResponseCode(500))
        }

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                    flushOnError = true,
                ),
            )

        // Track enough events to trigger auto-export at half capacity
        for (i in 0 until 55) {
            tracker.trackImpression("video_$i", "Row", i)
        }

        // Wait for all background work to settle
        Thread.sleep(5000)

        // Verify we didn't create an explosion of HTTP requests.
        // With the bug, each retry creates a new executor and sends
        // a request, so 20 enqueued responses could be drained rapidly.
        // With the fix, only the initial flush + ONE retry happens,
        // then events sit in the queue for the scheduled flush.
        // We expect at most 2 requests (initial + one retry), not 20+.
        val requestCount = server.requestCount
        // If the bug exists, requestCount could be 20+ (one per retry).
        // The fix should limit retries to at most 2 (initial + 1 retry).
        assert(requestCount <= 3) {
            "Expected at most 3 requests (initial + 1 retry), got $requestCount — " +
                "flushOnError is creating unbounded executors on repeated failures"
        }
    }
}
