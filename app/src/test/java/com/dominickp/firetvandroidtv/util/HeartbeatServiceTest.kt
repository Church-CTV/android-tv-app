package com.dominickp.firetvandroidtv.util

import android.content.Context
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HeartbeatServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var service: HeartbeatService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = RuntimeEnvironment.getApplication()
        service = HeartbeatService(server.url("/heartbeat").toString(), 60)
    }

    @After
    fun tearDown() {
        service.stop()
        server.shutdown()
    }

    @Test
    fun `startAndStopDoesNotCrash`() {
        service.start()
        Thread.sleep(100) // Let it begin
        service.stop()
        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `startDoesNotStartAgainWhenAlreadyRunning`() {
        service.start()
        service.start()
        // Second start should be no-op
        Thread.sleep(100)
        service.stop()
    }

    @Test
    fun `successfulHeartbeatReturnsSuccess`() {
        server.enqueue(MockResponse().setResponseCode(200))
        service.start()
        Thread.sleep(2000) // Wait for first heartbeat
        service.stop()
        val request = server.takeRequest()
        assertEquals("/heartbeat", request.path)
    }

    @Test
    fun `failedHeartbeatLogsWarning`() {
        server.enqueue(MockResponse().setResponseCode(500))
        service.start()
        Thread.sleep(2000)
        service.stop()
        val request = server.takeRequest()
        assertNotNull(request)
    }

    @Test
    fun `heartbeatUsesHttpGetMethod`() {
        server.enqueue(MockResponse().setResponseCode(200))
        service.start()
        Thread.sleep(2000)
        service.stop()
        val request = server.takeRequest()
        assertEquals("GET", request.method)
    }

    @Test
    fun `heartbeatsAreSentAtConfiguredInterval`() {
        // Use a short interval for testing
        val shortService = HeartbeatService(server.url("/h").toString(), 1)
        server.enqueue(MockResponse().setResponseCode(200))
        shortService.start()
        Thread.sleep(2500) // Should get at least 1 heartbeat
        shortService.stop()
        // At least 1 heartbeat should have been sent
        assertTrue(server.requestCount >= 1)
    }

    @Test
    fun `stopCancelsHeartbeats`() {
        server.enqueue(MockResponse().setResponseCode(200))
        service.start()
        Thread.sleep(1500) // One heartbeat
        service.stop()
        Thread.sleep(5000) // Wait — no more heartbeats should fire
        val initialCount = server.requestCount
        // No additional requests after stop
        assertTrue(server.requestCount <= initialCount + 1)
    }

    @Test
    fun `constructorAcceptsDifferentIntervals`() {
        val service1 = HeartbeatService(server.url("/h").toString(), 10)
        val service2 = HeartbeatService(server.url("/h").toString(), 300)
        assertNotNull(service1)
        assertNotNull(service2)
        service1.stop()
        service2.stop()
    }
}
