package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnalyticsTrackerFlushOnErrorBehaviorTest — verifies that flushOnError=true
 * causes an immediate re-flush of all queued events after an export failure.
 *
 * Bug: flushOnError is defined in AnalyticsConfig but never checked in
 * AnalyticsTracker. When export fails and flushOnError=true, the tracker
 * should immediately re-flush all events (not just restore to queue and wait).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerFlushOnErrorBehaviorTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
        server.shutdown()
    }

    @Test
    fun `flushOnErrorTrueReFlushesAllEventsAfterFailure()`() {
        // First request fails (500), second succeeds (re-flush)
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200))

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0, // disable scheduled flush
                    flushOnError = true,
                ),
            )

        // Add a few events
        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.trackImpression("vid2", "Row", 0)
        tracker.flush()

        // Wait for background export to complete
        Thread.sleep(1000)

        // With flushOnError=true, after the 500 error the tracker
        // should have immediately re-flushed all events.
        // We expect exactly 2 requests: initial flush + re-flush.
        assertEquals(
            "flushOnError=true should trigger immediate re-flush after export failure",
            2,
            server.requestCount,
        )
    }

    @Test
    fun `flushOnErrorFalseDoesNotReFlushAfterFailure()`() {
        // Only one request will be made (initial flush), no re-flush
        server.enqueue(MockResponse().setResponseCode(500))

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0, // disable scheduled flush
                    flushOnError = false,
                ),
            )

        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.flush()

        Thread.sleep(1000)

        // With flushOnError=false, no re-flush should happen.
        // Only the initial failed flush.
        assertEquals(
            "flushOnError=false should NOT trigger re-flush after export failure",
            1,
            server.requestCount,
        )
    }
}
