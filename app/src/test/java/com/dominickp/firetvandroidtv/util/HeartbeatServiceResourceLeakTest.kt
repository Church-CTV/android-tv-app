package com.dominickp.firetvandroidtv.util

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * HeartbeatServiceResourceLeakTest — verifies that HeartbeatService
 * properly shuts down its OkHttpClient to prevent resource leaks.
 *
 * The HeartbeatService creates an OkHttpClient that holds a connection
 * pool and dispatcher threads. When stop() is called, the coroutine
 * is cancelled but the OkHttpClient is never shut down, leaking
 * connection pool resources and threads.
 *
 * Behavioral test: after stop(), the OkHttpClient should be shut down.
 * A shut-down OkHttp client's dispatcher executor must be terminated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class HeartbeatServiceResourceLeakTest {
    private lateinit var server: MockWebServer

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `stopCleansUpOkHttpClientToPreventResourceLeak()`() {
        server = MockWebServer()
        server.start()

        val service = HeartbeatService(server.url("/h").toString(), 60)
        service.start()
        Thread.sleep(100)
        service.stop()

        // After stop(), the OkHttpClient should be shut down.
        // OkHttpClient.shutdown() terminates its dispatcher executor,
        // which cleans up the connection pool and associated threads.
        // Without shutdown(), these resources leak.
        //
        // Behavioral verification: after stop(), the internal client's
        // dispatcher executor must be terminated.
        val clientField = HeartbeatService::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val okClient = clientField.get(service) as okhttp3.OkHttpClient

        // OkHttp 4.x: dispatcher is a property, executorService is a property
        val executor = okClient.dispatcher.executorService

        // After proper shutdown, the executor must be terminated.
        // If it's not terminated, the client was not shut down — resource leak.
        assertTrue(
            "OkHttpClient dispatcher executor should be terminated after stop() — " +
                "unterminated executor indicates a resource leak. " +
                "isTerminated=${executor.isTerminated}, isShutdown=${executor.isShutdown}",
            executor.isTerminated,
        )
    }

    @Test
    fun `stopIsIdempotentWhenNoClientCreated()`() {
        server = MockWebServer()
        server.start()

        val service = HeartbeatService(server.url("/h").toString(), 60)
        service.stop()
        service.stop()

        assertTrue(true) // No crash = pass
    }
}
