package com.dominickp.firetvandroidtv.ui.search

import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.config.ContentConfig
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.data.network.FeedRepository
import com.dominickp.firetvandroidtv.data.network.SearchApi
import com.dominickp.firetvandroidtv.data.network.createFeedApi
import com.dominickp.firetvandroidtv.data.network.createSearchApi
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SearchViewModelServerSearchTest — proves SearchViewModel wires up SearchApi
 * for server-side search instead of only using in-memory feed data.
 *
 * Without SearchApi wiring, a user on a fresh install (no feed cache) gets
 * zero results. This test verifies the server fallback actually returns data.
 *
 * Execution model: the ViewModel's search coroutine runs on a REAL background
 * dispatcher (Dispatchers.IO) rather than a StandardTestDispatcher. The search
 * path performs a blocking OkHttp call to MockWebServer; running it on the
 * Robolectric main thread (as StandardTestDispatcher + advanceUntilIdle would)
 * deadlocks the main looper (Sandbox.runOnMainThread). A bounded polling wait
 * on the terminal SearchState keeps the test deterministic without a virtual
 * clock, since the debounce is a real 300ms on the background thread.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SearchViewModelServerSearchTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var searchApi: SearchApi
    private lateinit var emptyRepo: FeedRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        searchApi = createSearchApi("http://localhost:${mockWebServer.port}/")

        // Provide an empty FeedRepository (no cached feed data) so searchLocal returns empty
        val contentConfig = ContentConfig(sourceUrl = "http://example.com/feed")
        val feedApi = createFeedApi("http://example.com", contentConfig)
        emptyRepo = FeedRepository(feedApi, contentConfig)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    /**
     * Poll the ViewModel's state until it reaches a terminal state
     * (Results or Error), or fail after [timeoutMs].
     */
    private fun awaitSettled(
        viewModel: SearchViewModel,
        timeoutMs: Long = 5_000,
    ): SearchState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s is SearchState.Results || s is SearchState.Error) return s
            Thread.sleep(20)
        }
        fail("Timed out waiting for search to settle; state=${viewModel.state.value}")
        throw AssertionError("unreachable")
    }

    @Test
    fun `search_returns_server_results_when_no_feed_cached`() {
        val json =
            """[{"id":"server-vid","title":"Server Result","url":"http://example.com/v.mp4","content":{"thumb":""}}]"""
        mockWebServer.enqueue(
            MockResponse()
                .setBody(json)
                .addHeader("Content-Type: application/json")
                .setResponseCode(200),
        )

        val viewModel =
            SearchViewModel(
                configLoader = ConfigLoader(),
                dispatcher = Dispatchers.IO,
                feedRepositoryOverride = emptyRepo,
                searchApiOverride = searchApi,
            )

        viewModel.onQueryChanged("test")

        val state = awaitSettled(viewModel)
        assertTrue("Expected Results state, got: $state", state is SearchState.Results)
        val results = (state as SearchState.Results).items
        assertEquals("Server should return 1 result", 1, results.size)
        assertEquals("server-vid", results[0].id)
        assertEquals("Server Result", results[0].title)
    }

    @Test
    fun `search_handles_server_500_error_gracefully`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"),
        )

        val viewModel =
            SearchViewModel(
                configLoader = ConfigLoader(),
                dispatcher = Dispatchers.IO,
                feedRepositoryOverride = emptyRepo,
                searchApiOverride = searchApi,
            )

        viewModel.onQueryChanged("error query")
        val state = awaitSettled(viewModel)

        // Server error returns empty results (not Error state) — user sees "no results"
        assertTrue("Expected Results state (even empty) on server error, got: $state", state is SearchState.Results)
        val results = (state as SearchState.Results).items
        assertTrue("Server error returns no results", results.isEmpty())
    }

    @Test
    fun `search_passes_query_parameter_to_server`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("[]")
                .addHeader("Content-Type: application/json")
                .setResponseCode(200),
        )

        val viewModel =
            SearchViewModel(
                configLoader = ConfigLoader(),
                dispatcher = Dispatchers.IO,
                feedRepositoryOverride = emptyRepo,
                searchApiOverride = searchApi,
            )

        viewModel.onQueryChanged("my query")
        awaitSettled(viewModel)

        val request = mockWebServer.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)
        assertNotNull("Server should receive a request", request)
        assertEquals("GET", request!!.method)
        assertTrue(
            "Request path should contain query",
            request.path!!.contains("my+query") || request.path!!.contains("my%20query"),
        )
    }

    @Test
    fun `search_uses_local_data_first_when_available`() {
        // Seed feed data with a match
        val videos =
            mapOf(
                "2024-01" to
                    listOf(
                        VideoItem(
                            id = "local-vid",
                            title = "Local Result",
                            shortDescription = "",
                            url = "http://example.com/v.mp4",
                            releaseDate = "2024-01-01",
                        ),
                    ),
            )
        emptyRepo.setFeedData(videos)

        mockWebServer.enqueue(
            MockResponse()
                .setBody("[]")
                .addHeader("Content-Type: application/json")
                .setResponseCode(200),
        )

        val viewModel =
            SearchViewModel(
                configLoader = ConfigLoader(),
                dispatcher = Dispatchers.IO,
                feedRepositoryOverride = emptyRepo,
                searchApiOverride = searchApi,
            )

        viewModel.onQueryChanged("Local")
        val state = awaitSettled(viewModel)

        // Local data should be returned, server should NOT be called
        assertTrue("Expected Results state, got: $state", state is SearchState.Results)
        val results = (state as SearchState.Results).items
        assertEquals("Should use local data, not call server", 1, results.size)
        assertEquals("local-vid", results[0].id)
        // No server request should have been made
        assertTrue(
            "Server should not be called when local data matches",
            mockWebServer.requestCount == 0,
        )
    }
}
