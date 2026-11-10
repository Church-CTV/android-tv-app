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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * AnalyticsTrackerPersistenceRoundTripTest - verifies that events with Float fields
 * survive a full persistence round-trip (serialize -> persist -> load -> deserialize -> flush).
 *
 * Regression test for: Float progress values were serialized as quoted strings
 * by the custom mapToJson() (Float falls through to the else branch -> "0.75"),
 * then kotlinx Json deserialized them as Strings,
 * causing ClassCastException when mapFromEvent() cast them back to Float.
 *
 * Affected events: Pause (progress: Float), Complete (progress: Float, duration: Long).
 *
 * Tests write JSON directly to disk to avoid background-thread timing issues.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerPersistenceRoundTripTest {
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

    private fun cacheFile(context: android.content.Context): File {
        return File(context.cacheDir, "analytics_queue.json")
    }

    /**
     * Pause events with Float progress must survive disk persistence.
     *
     * Bug: Float -> serialized as quoted string "0.75" -> loaded as String -> ClassCastException -> emptyList().
     * Fix: Float -> serialized as unquoted 0.75 -> loaded as Double -> asFloat() converts to Float.
     */
    @Test
    fun `persistedPauseEventDeserializesAndCanBeFlushed()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        // Write serialized Pause event directly to disk (matches mapToJson output with fix)
        queueFile.writeText(
            """[{"type":"pause","videoId":"vid1","progress":0.75,"timestamp":1234567890}]""",
        )

        // Create fresh tracker - should load events from disk
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.init(context)

        // Flush should send the paused event
        server.enqueue(MockResponse().setResponseCode(200))
        tracker.flush()
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Events should have been flushed from disk", request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Request body should contain a Pause event", body.contains("pause"))
        assertTrue("Request body should contain the video id", body.contains("vid1"))
    }

    @Test
    fun `persistedCompleteEventDeserializesAndCanBeFlushed()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        // Write serialized Complete event directly to disk
        queueFile.writeText(
            """[{"type":"complete","videoId":"vid1","progress":1.0,"duration":3600000,"timestamp":1234567890}]""",
        )

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.init(context)

        server.enqueue(MockResponse().setResponseCode(200))
        tracker.flush()
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Events should have been flushed from disk", request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Request body should contain a Complete event", body.contains("complete"))
    }

    @Test
    fun `persistedMixedEventsDeserializesAndCanBeFlushed()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        // Write mixed events including Float fields
        queueFile.writeText(
            """[{"type":"play","videoId":"vid1","streamUrl":"http://stream.mp4","isLive":false,"referrer":"home","timestamp":1},{"type":"pause","videoId":"vid2","progress":0.5,"timestamp":2},{"type":"complete","videoId":"vid3","progress":0.9,"duration":120000,"timestamp":3},{"type":"impression","videoId":"vid4","rowLabel":"Home Row","position":0,"cardType":"contentCard"}]""",
        )

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.init(context)

        server.enqueue(MockResponse().setResponseCode(200))
        tracker.flush()
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Events should have been flushed from disk", request)
        val body = request!!.body!!.readUtf8()
        // Verify all 4 event types are present
        assertTrue("Should contain play event", body.contains("play"))
        assertTrue("Should contain pause event", body.contains("pause"))
        assertTrue("Should contain complete event", body.contains("complete"))
        assertTrue("Should contain impression event", body.contains("impression"))
    }

    @Test
    fun `emptyPersistedQueueDoesNotCrash()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()
        // Write empty array
        queueFile.writeText("[]")

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.init(context)

        // Flush with empty queue should not crash or send request
        tracker.flush()
        assertNull("No request should be made for empty queue", server.takeRequest(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `malformedPersistedQueueDoesNotCrash()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()
        // Write invalid JSON
        queueFile.writeText("not valid json {{{")

        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(
                    enabled = true,
                    exportUrl = server.url("/analytics").toString(),
                    exportIntervalSeconds = 0,
                ),
            )
        tracker.init(context)

        // Flush with invalid JSON should not crash (deserialization returns emptyList)
        tracker.flush()
        assertNull("No request should be made for invalid JSON", server.takeRequest(100, TimeUnit.MILLISECONDS))
    }
}
