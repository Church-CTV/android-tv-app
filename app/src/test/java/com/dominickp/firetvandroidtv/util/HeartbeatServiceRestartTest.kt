package com.dominickp.firetvandroidtv.util

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * HeartbeatServiceRestartTest — verifies that stop() does not permanently
 * disable the service. FireTVApplication starts the heartbeat on app
 * foreground (onStart) and stops it on background (onStop); a background →
 * foreground cycle must resume the heartbeat.
 *
 * Regression: stop() previously cancelled the SupervisorJob that the scope is
 * tied to. A cancelled Job stays cancelled, so the next start() launched its
 * coroutine on an already-cancelled scope and it was cancelled immediately —
 * the heartbeat silently never resumed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class HeartbeatServiceRestartTest {
    private lateinit var server: MockWebServer
    private lateinit var service: HeartbeatService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        // Short interval so a resumed heartbeat fires quickly.
        service = HeartbeatService(server.url("/hb").toString(), 1)
    }

    @After
    fun tearDown() {
        service.stop()
        server.shutdown()
    }

    @Test
    fun `stop_then_start_resumes_heartbeats`() {
        // Prime the pool with enough responses for both start cycles.
        repeat(6) { server.enqueue(MockResponse().setResponseCode(200)) }

        service.start()
        Thread.sleep(1200) // first heartbeat fires
        val afterFirst = server.requestCount
        assertTrue("First start should send at least 1 heartbeat", afterFirst >= 1)

        service.stop()
        Thread.sleep(500) // settle — no heartbeats expected while stopped
        val afterStop = server.requestCount

        // Re-start (foreground again) — heartbeats must resume.
        service.start()
        Thread.sleep(1500) // allow at least one more heartbeat
        val afterRestart = server.requestCount

        assertTrue(
            "Heartbeat must resume after stop() then start() " +
                "(afterStop=$afterStop, afterRestart=$afterRestart) — " +
                "a permanently-cancelled scope would send nothing",
            afterRestart > afterStop,
        )
    }
}
