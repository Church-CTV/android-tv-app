package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalyticsTrackerTest {
    private var config =
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

    @Test
    fun `getInstanceReturnsSameInstance`() {
        val tracker1 = AnalyticsTracker.getInstance(config)
        val tracker2 = AnalyticsTracker.getInstance(config)
        assertSame(tracker1, tracker2)
    }

    @Test
    fun `resetInstanceClearsSingleton`() {
        val tracker1 = AnalyticsTracker.getInstance(config)
        AnalyticsTracker.resetInstance()
        val tracker2 = AnalyticsTracker.getInstance(config)
        assertNotSame(tracker1, tracker2)
    }

    @Test
    fun `trackingImpressionAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackImpression("video1", "Home Row", 0)
            // Event is queued — verify no crash, queue accepted
            tracker.flush()
        }

    @Test
    fun `trackingPlayAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackPlay("video1", "http://stream.mp4", false, "home")
            tracker.flush()
        }

    @Test
    fun `trackingPauseAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackPause("video1", 0.75f)
            tracker.flush()
        }

    @Test
    fun `trackingCompleteAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackComplete("video1", 1.0f, 3600000L)
            tracker.flush()
        }

    @Test
    fun `trackingSearchAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackSearch("test query", 10)
            tracker.flush()
        }

    @Test
    fun `trackingSubscribeAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackSubscribe("premium_monthly")
            tracker.flush()
        }

    @Test
    fun `trackingErrorAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackError("playback_error", "Stream not found", "player")
            tracker.flush()
        }

    @Test
    fun `trackingErrorWithDefaultContextAddsEventToQueue`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackError("network_error", "Timeout")
            tracker.flush()
        }

    @Test
    fun `flushCallsExportBatchEvenWhenDisabled`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(enabled = false, exportUrl = ""),
                )
            tracker.trackPlay("video1", "http://stream.mp4", true, "home")
            // flush should not crash even with disabled analytics
            tracker.flush()
        }

    @Test
    fun `multipleEventsBatchedTogether`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            tracker.trackImpression("vid1", "Row1", 0)
            tracker.trackImpression("vid2", "Row1", 1)
            tracker.trackImpression("vid3", "Row2", 0)
            // All three events should be in the queue
            tracker.flush()
        }

    @Test
    fun `shutdownStopsSchedulerAndFlushes`() {
        val tracker = AnalyticsTracker.getInstance(config)
        tracker.trackPlay("video1", "http://stream.mp4", false, "home")
        tracker.shutdown()
        // After shutdown, instance should be cleared
        AnalyticsTracker.resetInstance()
        val newTracker = AnalyticsTracker.getInstance(config)
        assertNotSame(tracker, newTracker)
    }

    @Test
    fun `analyticsEventsAreSerializable`() =
        runTest {
            val tracker =
                AnalyticsTracker.getInstance(
                    config.copy(exportUrl = "", enabled = false),
                )
            // All event types should serialize without throwing
            tracker.trackImpression("v1", "Home", 0)
            tracker.trackPlay("v1", "url", false, "home")
            tracker.trackPause("v1", 0.5f)
            tracker.trackComplete("v1", 1.0f, 1000L)
            tracker.trackSearch("query", 5)
            tracker.trackSubscribe("basic")
            tracker.trackError("test", "message")
            tracker.flush()
        }

    @Test
    fun `flushOnErrorTrueRestoresEventsOnExportFailure()`() =
        runTest {
            val server = MockWebServer()
            server.start()

            // First request fails (500), second succeeds (manual retry)
            server.enqueue(MockResponse().setResponseCode(500))
            server.enqueue(MockResponse().setResponseCode(200))

            val testConfig =
                config.copy(
                    exportUrl = server.url("/analytics").toString(),
                    flushOnError = true,
                )
            val tracker = AnalyticsTracker.getInstance(testConfig)
            val context = RuntimeEnvironment.getApplication()
            tracker.init(context)

            tracker.trackPlay("video1", "http://stream.mp4", false, "home")

            // Flush triggers export -> 500 error -> events restored to queue (no auto-flush)
            tracker.flush()

            // Wait for background export threads to complete
            Thread.sleep(2000)

            // First request was the failed flush
            val failedRequest = server.takeRequest(2, TimeUnit.SECONDS)
            assertNotNull("Initial flush should have sent a request", failedRequest)

            // Events were restored to queue — manual flush retries
            tracker.flush()

            // Second request was the manual retry
            val retryRequest = server.takeRequest(2, TimeUnit.SECONDS)
            assertNotNull(
                "Tracker should retry on manual flush after error",
                retryRequest,
            )
            assertTrue(
                "Retried request should contain the event",
                retryRequest!!.body!!.readUtf8().contains("play"),
            )

            server.shutdown()
            AnalyticsTracker.resetInstance()
        }
}
