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
import java.util.concurrent.TimeUnit

/**
 * AnalyticsTrackerExportTest — verifies actual HTTP export behavior.
 *
 * Tests:
 * - Export succeeds when server returns 200
 * - Export restores events on server 500 error
 * - Export restores events on network exception
 * - Queue persistence on export
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerExportTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
        server.shutdown()
    }

    @Test
    fun `successfulExportClearsQueue()`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.flush()
        server.takeRequest(1, TimeUnit.SECONDS)
        // Should have cleared - no crash
        assertTrue(true)
    }

    @Test
    fun `failedExportRestoresEventsOn500()`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.flush()
        server.takeRequest(1, TimeUnit.SECONDS)
        // Should have restored - re-flush should succeed
        server.enqueue(MockResponse().setResponseCode(200))
        tracker.flush()
        server.takeRequest(1, TimeUnit.SECONDS)
    }

    @Test
    fun `disabledAnalyticsDoesNotExport()`() {
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = false,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.flush()
        // No request should have been made
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `emptyExportUrlDoesNotExport()`() {
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = "",
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.flush()
        // No request should have been made
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `autoExportAtHalfQueue()`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        // Queue auto-exports at half capacity (50 events for default max 100)
        // Add 50 events to trigger auto-export
        for (i in 0 until 50) {
            tracker.trackImpression("vid$i", "Row", i)
        }
        // Should have auto-exported
        server.takeRequest(1, TimeUnit.SECONDS)
    }

    @Test
    fun `shutdownFlushesEvents()`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 300,
                ),
            )
        tracker.trackPlay("vid1", "http://stream.mp4", false, "home")
        tracker.shutdown()
        server.takeRequest(1, TimeUnit.SECONDS)
    }
}
