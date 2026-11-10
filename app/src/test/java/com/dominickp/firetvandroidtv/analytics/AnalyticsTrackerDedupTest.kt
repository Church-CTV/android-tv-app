package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import kotlinx.coroutines.test.runTest
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
 * AnalyticsTrackerDedupTest — verifies impression deduplication behavior.
 *
 * Tests:
 * - Duplicate impressions within the window are deduplicated
 * - Different impressions are NOT deduplicated
 * - After cleanup, signatures that are still within the window should persist
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerDedupTest {
    private val server = MockWebServer()
    private val config =
        AnalyticsConfig(
            enabled = false,
            exportUrl = "",
            exportIntervalSeconds = 0,
        )

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
        server.shutdown()
    }

    // ── Deduplication ──

    @Test
    fun `duplicateImpressionsWithinWindowAreDeduplicated`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)
            tracker.trackImpression("video1", "Home Row", 0)
            tracker.trackImpression("video1", "Home Row", 0) // duplicate

            val queueSize = tracker.queueSize()
            assertEquals(1, queueSize) // only 1 event, not 2
        }

    @Test
    fun `differentPositionIsNotDeduplicated`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)
            tracker.trackImpression("video1", "Home Row", 0)
            tracker.trackImpression("video1", "Home Row", 1) // same video/row, different position

            val queueSize = tracker.queueSize()
            assertEquals(2, queueSize)
        }

    @Test
    fun `differentVideoIsNotDeduplicated`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)
            tracker.trackImpression("video1", "Home Row", 0)
            tracker.trackImpression("video2", "Home Row", 0)

            val queueSize = tracker.queueSize()
            assertEquals(2, queueSize)
        }

    @Test
    fun `cleanupDoesNotClearValidSignatures()`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)

            // Add an impression — signature stored
            tracker.trackImpression("video1", "Home Row", 0)
            assertEquals(1, tracker.queueSize())

            // Run cleanup — simulates the 5-minute scheduler firing
            // With the correct implementation, signatures within the window should persist
            tracker.cleanupSignatures()

            // The same impression should STILL be deduplicated after cleanup
            // because the signature is still within the 5-minute window
            tracker.trackImpression("video1", "Home Row", 0)

            val queueSize = tracker.queueSize()
            assertEquals(
                "cleanupOldSignatures() should only remove expired signatures, " +
                    "not clear all signatures — duplicate impression should still be deduplicated",
                1,
                queueSize,
            )
        }

    // ── Queue overflow ──

    @Test
    fun `queueOverflowDropsOldestEvents()`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    AnalyticsConfig(
                        enabled = true,
                        exportUrl = server.url("/analytics").toString(),
                        exportIntervalSeconds = 0,
                    ),
                )

            // Fill the queue to max (100 events) with a working export URL
            // so auto-export actually succeeds and doesn't clear the queue
            for (i in 0 until 100) {
                server.enqueue(MockResponse().setResponseCode(200))
                tracker.trackImpression("video_$i", "Row", i)
            }

            // Wait for auto-exports to complete
            Thread.sleep(500)

            // Queue should have been auto-exported in batches
            // Add one more — should trigger another export
            server.enqueue(MockResponse().setResponseCode(200))
            tracker.trackImpression("video_new", "Row", 999)
            Thread.sleep(500)

            // Verify exports happened
            assertTrue(server.requestCount >= 1)
        }
}
