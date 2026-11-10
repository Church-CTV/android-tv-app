package com.dominickp.firetvandroidtv.util

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * HeartbeatServiceConnectionPoolLeakTest — verifies that HeartbeatService
 * fully shuts down its OkHttpClient (including the connection pool) on stop().
 *
 * The connection pool holds HTTP/1.1 keep-alive connections and associated
 * threads. Without client.shutdown(), these persist after stop(), leaking
 * file descriptors and threads over time.
 *
 * Behavioral test: after stop(), the OkHttpClient's connection pool must
 * have zero idle connections (indicating full cleanup).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class HeartbeatServiceConnectionPoolLeakTest {
    private lateinit var server: MockWebServer

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `stopShutsDownOkHttpClientConnectionPoolToPreventResourceLeak()`() {
        server = MockWebServer()
        server.start()

        // Enqueue a response so a real connection is made
        server.enqueue(MockResponse().setResponseCode(200))

        val service = HeartbeatService(server.url("/h").toString(), 60)
        service.start()
        Thread.sleep(2000) // Wait for heartbeat to complete
        service.stop()

        // After stop(), the OkHttpClient must be fully shut down.
        // Check both dispatcher AND connection pool are cleaned up.
        val clientField = HeartbeatService::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val okClient = clientField.get(service) as okhttp3.OkHttpClient

        // Dispatcher must be shut down
        assertTrue(
            "OkHttpClient dispatcher executor should be terminated after stop()",
            okClient.dispatcher.executorService.isTerminated,
        )

        // Connection pool must have zero idle connections —
        // without client.shutdown(), idle keep-alive connections persist.
        assertTrue(
            "OkHttpClient connection pool should have zero idle connections after stop() — " +
                "idle connections indicate a connection pool leak. " +
                "idleCount=${okClient.connectionPool.idleConnectionCount()}, " +
                "activeCount=${okClient.connectionPool.connectionCount()}",
            okClient.connectionPool.idleConnectionCount() == 0,
        )
    }
}
