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
 * AnalyticsTrackerDeserializationTest — verifies that every AnalyticsEvent subtype
 * deserializes correctly from persisted JSON with all fields populated.
 *
 * These tests exercise the private mapFromJsonElement() path indirectly via
 * disk persistence round-trips (write JSON → init() loads → flush sends).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnalyticsTrackerDeserializationTest {
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

    @Test
    fun `impressionEventDeserializesWithAllFields()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"impression","videoId":"vid-imp","rowLabel":"Featured","position":3,"cardType":"contentCard"}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain impression type", body.contains("\"type\":\"impression\""))
        assertTrue("Should contain videoId", body.contains("\"videoId\":\"vid-imp\""))
        assertTrue("Should contain rowLabel", body.contains("\"rowLabel\":\"Featured\""))
        assertTrue("Should contain position", body.contains("\"position\":3"))
        assertTrue("Should contain cardType", body.contains("\"cardType\":\"contentCard\""))
    }

    @Test
    fun `playEventDeserializesWithAllFields()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"play","videoId":"vid-play","streamUrl":"http://stream.mp4","isLive":true,"referrer":"home","timestamp":999999}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain play type", body.contains("\"type\":\"play\""))
        assertTrue("Should contain streamUrl", body.contains("\"streamUrl\":\"http://stream.mp4\""))
        assertTrue("Should contain isLive true", body.contains("\"isLive\":true"))
        assertTrue("Should contain referrer", body.contains("\"referrer\":\"home\""))
    }

    @Test
    fun `pauseEventDeserializesWithFloatProgress()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"pause","videoId":"vid-pause","progress":0.75,"timestamp":1234}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain pause type", body.contains("\"type\":\"pause\""))
        assertTrue("Should contain progress 0.75", body.contains("0.75"))
    }

    @Test
    fun `completeEventDeserializesWithDuration()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"complete","videoId":"vid-comp","progress":1.0,"duration":3600000,"timestamp":5678}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain complete type", body.contains("\"type\":\"complete\""))
        assertTrue("Should contain duration", body.contains("\"duration\":3600000"))
    }

    @Test
    fun `searchEventDeserializesWithResultCount()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"search","query":"test query","resultCount":42,"timestamp":1111}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain search type", body.contains("\"type\":\"search\""))
        assertTrue("Should contain query", body.contains("\"query\":\"test query\""))
        assertTrue("Should contain resultCount", body.contains("\"resultCount\":42"))
    }

    @Test
    fun `subscribeEventDeserializesWithPlan()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"subscribe","plan":"premium_monthly","timestamp":2222}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain subscribe type", body.contains("\"type\":\"subscribe\""))
        assertTrue("Should contain plan", body.contains("\"plan\":\"premium_monthly\""))
    }

    @Test
    fun `errorEventDeserializesWithErrorTypeAndMessage()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"error","errorType":"playback_error","message":"Stream not found","context":"player","timestamp":3333}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Should contain error type", body.contains("\"type\":\"error\""))
        assertTrue("Should contain errorType", body.contains("\"errorType\":\"playback_error\""))
        assertTrue("Should contain message", body.contains("\"message\":\"Stream not found\""))
        assertTrue("Should contain context", body.contains("\"context\":\"player\""))
    }

    @Test
    fun `unknownEventTypeDeserializesAsErrorEvent()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"type":"unknown_type","videoId":"vid-x"}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Unknown type should become an error event", body.contains("\"type\":\"error\""))
        assertTrue("Should contain unknown type info", body.contains("Unknown event type"))
    }

    @Test
    fun `missingTypeFieldDeserializesAsErrorEvent()`() {
        val context = RuntimeEnvironment.getApplication()
        val queueFile = cacheFile(context)
        queueFile.delete()

        queueFile.writeText(
            """[{"videoId":"vid-no-type"}]""",
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
        assertNotNull(request)
        val body = request!!.body!!.readUtf8()
        assertTrue("Missing type should become an error event", body.contains("\"type\":\"error\""))
        assertTrue("Should contain missing type info", body.contains("Missing type"))
    }
}
