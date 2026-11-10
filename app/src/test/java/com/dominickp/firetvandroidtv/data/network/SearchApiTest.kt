package com.dominickp.firetvandroidtv.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SearchApiTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var searchApi: SearchApi

    @Before
    fun setUp() =
        runBlocking {
            mockWebServer = MockWebServer()
            mockWebServer.start()
            searchApi = createSearchApi("http://localhost:${mockWebServer.port}/")
        }

    @After
    fun tearDown() =
        runBlocking {
            mockWebServer.shutdown()
        }

    @Test
    fun `search_returns_success_with_valid_response`() =
        runBlocking {
            val json = """[{"id":"1","title":"Result 1","url":"http://example.com/v.mp4"}]"""
            mockWebServer.enqueue(MockResponse().setBody(json).addHeader("Content-Type: application/json").setResponseCode(200))

            val response = searchApi.search("test query", 10)
            assertTrue(response.isSuccessful)
            assertNotNull(response.body())

            val request = mockWebServer.takeRequest()
            assertEquals("GET", request.method)
            assertTrue(request.path!!.startsWith("/search"))
        }

    @Test
    fun `search_passes_query_parameter`() =
        runBlocking {
            val json = "[]"
            mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

            searchApi.search("my query", 25)
            val request = mockWebServer.takeRequest()
            assertTrue(request.path!!.startsWith("/search"))
            assertTrue(request.path!!.contains("my+query") || request.path!!.contains("my%20query"))
        }

    @Test
    fun `search_returns_404_on_not_found`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

            val response = searchApi.search("test")
            assertEquals(404, response.code())
            assertFalse(response.isSuccessful)
        }

    @Test
    fun `search_returns_500_on_server_error`() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setResponseCode(500))

            val response = searchApi.search("test")
            assertEquals(500, response.code())
            assertFalse(response.isSuccessful)
        }

    @Test
    fun `search_handles_empty_results`() =
        runBlocking {
            val json = "[]"
            mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

            val response = searchApi.search("no results")
            assertTrue(response.isSuccessful)
        }

    @Test
    fun `search_with_default_limit`() =
        runBlocking {
            val json = "[]"
            mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

            searchApi.search("test")
            assertTrue(mockWebServer.requestCount == 1)
        }
}
