package com.dominickp.firetvandroidtv.ui

import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.config.ChannelConfig
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.config.ContentConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * FeedViewModelStateTransitionTest — tests the feed loading state machine.
 *
 * This test verifies the core state machine behavior that drives the splash screen dismissal:
 * - Initial state is Loading
 * - Successful feed load transitions to Success
 * - Failed feed load transitions to Error
 * - Retry from Error goes back to Loading
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedViewModelStateTransitionTest {
    private lateinit var mockServer: MockWebServer

    @After
    fun tearDown() {
        if (::mockServer.isInitialized) {
            mockServer.shutdown()
        }
    }

    @Test
    fun `initial_state_is_Loading`() =
        runTest {
            val configLoader = ConfigLoader()
            val viewModel = FeedViewModel(configLoader, StandardTestDispatcher(testScheduler))
            val state = viewModel.uiState.first()
            assertTrue(state is FeedUiState.Loading)
        }

    @Test
    fun `FeedUiState_Loading_is_singleton`() {
        val loading = FeedUiState.Loading
        assertSame(loading, FeedUiState.Loading)
    }

    @Test
    fun `FeedUiState_Error_contains_message`() {
        val error = FeedUiState.Error("Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun `FeedUiState_Success_contains_groups`() {
        val groups = mapOf<String, List<com.dominickp.firetvandroidtv.data.model.VideoItem>>()
        val success = FeedUiState.Success(groups)
        assertEquals(0, success.groups.size)
    }

    @Test
    fun `FeedViewModel_has_retry_method`() {
        val configLoader = ConfigLoader()
        val viewModel = FeedViewModel(configLoader)
        val method = viewModel::class.java.methods.find { it.name == "retry" }
        assertNotNull(method)
    }

    @Test
    fun `FeedViewModel_has_isLoading_property`() =
        runTest {
            val configLoader = ConfigLoader()
            val viewModel = FeedViewModel(configLoader, StandardTestDispatcher(testScheduler))
            val isLoading = viewModel.isLoading
            assertTrue(isLoading)
        }

    @Test
    fun `successful_feed_load_transitions_to_Success`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            // Enqueue a valid feed response
            val validFeedJson =
                """
                {
                    "content": [
                        {
                            "id": "1",
                            "title": "Test Video",
                            "short_description": "A test video",
                            "url": "http://example.com/video.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/video.mp4"}],
                                "thumb": "http://example.com/thumb.jpg"
                            },
                            "release_date": "2024-01-01",
                            "is_live": false
                        }
                    ]
                }
                """.trimIndent()

            mockServer.enqueue(MockResponse().setBody(validFeedJson).setResponseCode(200))

            val configLoader =
                object : ConfigLoader() {
                    override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                        return AppConfig(
                            channel = ChannelConfig(name = "Test"),
                            content =
                                ContentConfig(
                                    sourceUrl = mockServer.url("/feed").toString(),
                                    retryAttempts = 1,
                                    retryDelayMs = 0,
                                ),
                        )
                    }
                }

            val viewModel = FeedViewModel(configLoader, StandardTestDispatcher(testScheduler))

            // Wait for state to transition
            val state = viewModel.uiState.first { it !is FeedUiState.Loading }

            assertTrue("Expected Success state, got: $state", state is FeedUiState.Success)
            val successState = state as FeedUiState.Success
            assertTrue("Expected at least one group", successState.groups.isNotEmpty())

            mockServer.shutdown()
        }

    @Test
    fun `failed_feed_load_transitions_to_Error`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            // Enqueue a 500 error response
            mockServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

            val configLoader =
                object : ConfigLoader() {
                    override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                        return AppConfig(
                            channel = ChannelConfig(name = "Test"),
                            content =
                                ContentConfig(
                                    sourceUrl = mockServer.url("/feed").toString(),
                                    retryAttempts = 1,
                                    retryDelayMs = 0,
                                ),
                        )
                    }
                }

            val viewModel = FeedViewModel(configLoader, StandardTestDispatcher(testScheduler))

            // Wait for state to transition
            val state = viewModel.uiState.first { it !is FeedUiState.Loading }

            assertTrue("Expected Error state, got: $state", state is FeedUiState.Error)
            val errorState = state as FeedUiState.Error
            assertNotNull(errorState.message)
            assertTrue(errorState.message!!.isNotEmpty())

            mockServer.shutdown()
        }

    @Test
    fun `retry_from_error_goes_back_to_Loading`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            // First request: 500 error
            mockServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

            val configLoader =
                object : ConfigLoader() {
                    override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                        return AppConfig(
                            channel = ChannelConfig(name = "Test"),
                            content =
                                ContentConfig(
                                    sourceUrl = mockServer.url("/feed").toString(),
                                    retryAttempts = 1,
                                    retryDelayMs = 0,
                                ),
                        )
                    }
                }

            val viewModel = FeedViewModel(configLoader, StandardTestDispatcher(testScheduler))

            // Wait for first error
            val firstState = viewModel.uiState.first { it !is FeedUiState.Loading }
            assertTrue("Expected Error state after first load", firstState is FeedUiState.Error)

            // Second request: success
            val validFeedJson =
                """
                {
                    "content": [
                        {
                            "id": "1",
                            "title": "Test Video",
                            "short_description": "A test video",
                            "url": "http://example.com/video.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/video.mp4"}],
                                "thumb": "http://example.com/thumb.jpg"
                            },
                            "release_date": "2024-01-01",
                            "is_live": false
                        }
                    ]
                }
                """.trimIndent()
            mockServer.enqueue(MockResponse().setBody(validFeedJson).setResponseCode(200))

            // Retry
            viewModel.retry()

            // Wait for state to transition again
            val secondState = viewModel.uiState.first { it !is FeedUiState.Loading }

            assertTrue("Expected Success state after retry", secondState is FeedUiState.Success)

            mockServer.shutdown()
        }
}
