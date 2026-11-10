package com.dominickp.firetvandroidtv.util

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CrashReportingServiceAsyncUploadTest - verifies that crash upload
 * does NOT block the calling thread (main thread safety).
 *
 * CrashReportingService.uploadWithUrl() must execute HTTP uploads
 * on a background thread so that reportCrash() and init() never
 * block the caller - especially critical when called from the main
 * thread during FireTVApplication.onCreate() or from an uncaught
 * exception handler.
 */
@RunWith(RobolectricTestRunner::class)
class CrashReportingServiceAsyncUploadTest {
    private val mockServer = MockWebServer()

    @After
    fun tearDown() {
        try {
            mockServer.shutdown()
        } catch (_: Exception) {
        }
    }

    @Test
    fun `reportCrash_doesNotBlockCallingThread_whenServerIsSlow`() {
        // Use a custom dispatcher that blocks for 5 seconds before responding.
        // This reliably blocks the calling thread if the HTTP call is synchronous.
        mockServer.dispatcher =
            object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    try {
                        Thread.sleep(5000)
                    } catch (_: InterruptedException) {
                    }
                    return MockResponse().setResponseCode(200).setBody("OK")
                }
            }
        mockServer.start()

        val service = CrashReportingService.getInstance()
        service.init(mockServer.url("/crash").toString(), RuntimeEnvironment.getApplication())

        val start = System.currentTimeMillis()
        service.reportCrash(RuntimeException("test crash"))
        val elapsed = System.currentTimeMillis() - start

        // If upload is synchronous, this blocks for 5+ seconds (test timeout).
        // With async upload, it should return almost immediately (< 2s).
        assertTrue(
            "reportCrash should not block for upload (took " + elapsed + "ms, expected < 2000ms)",
            elapsed < 2000,
        )
    }

    @Test
    fun `init_doesNotBlockCallingThread_whenServerIsSlow`() {
        // Same test but for init() - which is called from Application.onCreate()
        // on the main thread. Blocking here causes noticeable app startup delay.
        mockServer.dispatcher =
            object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    try {
                        Thread.sleep(5000)
                    } catch (_: InterruptedException) {
                    }
                    return MockResponse().setResponseCode(200).setBody("OK")
                }
            }
        mockServer.start()

        val start = System.currentTimeMillis()
        CrashReportingService.getInstance().init(
            mockServer.url("/crash").toString(),
            RuntimeEnvironment.getApplication(),
        )
        val elapsed2 = System.currentTimeMillis() - start

        assertTrue(
            "init should not block for upload (took " + elapsed2 + "ms, expected < 2000ms)",
            elapsed2 < 2000,
        )
    }

    @Test
    fun `reportCrash_uploadCompletesSuccessfully_whenServerResponds`() {
        // Verify that the async upload actually completes and succeeds.
        val latch = CountDownLatch(1)
        mockServer.dispatcher =
            object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    latch.countDown()
                    return MockResponse().setResponseCode(200).setBody("OK")
                }
            }
        mockServer.start()

        val service = CrashReportingService.getInstance()
        service.init(mockServer.url("/crash").toString(), RuntimeEnvironment.getApplication())
        service.reportCrash(RuntimeException("test crash"))

        // Wait for the upload to complete (max 5 seconds)
        val completed = latch.await(5, TimeUnit.SECONDS)
        assertTrue("Upload should have completed within 5 seconds", completed)
    }
}
