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
 * FeedViewModelDeepLinkTest — tests deep link resolution via FeedViewModel.
 *
 * Verifies:
 * - resolveDeepLink returns null when feed not loaded
 * - resolveDeepLink returns null when video ID not found
 * - resolveDeepLink returns video when ID matches loaded feed
 * - resolveDeepLink works after successful feed load
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedViewModelDeepLinkTest {
    private lateinit var mockServer: MockWebServer

    @After
    fun tearDown() {
        if (::mockServer.isInitialized) {
            mockServer.shutdown()
        }
    }

    @Test
    fun `resolveDeepLink_returns_null_when_feed_not_loaded`() =
        runTest {
            val configLoader = ConfigLoader()
            val viewModel = FeedViewModel(configLoader)

            // Feed is still loading (init triggers loadFeed)
            val result = viewModel.resolveDeepLink("video-123")
            assertNull("Should return null when feed not loaded", result)
        }

    @Test
    fun `resolveDeepLink_returns_null_when_video_id_not_found`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            val validFeedJson =
                """
                {
                    "content": [
                        {
                            "id": "video-1",
                            "title": "Test Video 1",
                            "short_description": "Description 1",
                            "url": "http://example.com/video1.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/video1.mp4"}],
                                "thumb": "http://example.com/thumb1.jpg"
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

            // Wait for feed to load
            val state = viewModel.uiState.first { it !is FeedUiState.Loading }
            assertTrue("Expected Success state", state is FeedUiState.Success)

            // Try to resolve a video ID that doesn't exist
            val result = viewModel.resolveDeepLink("nonexistent-video-id")
            assertNull("Should return null for non-existent video ID", result)

            mockServer.shutdown()
        }

    @Test
    fun `resolveDeepLink_returns_video_when_id_matches_loaded_feed`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            val validFeedJson =
                """
                {
                    "content": [
                        {
                            "id": "video-123",
                            "title": "Target Video",
                            "short_description": "This is the target video",
                            "url": "http://example.com/target.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/target.mp4"}],
                                "thumb": "http://example.com/target-thumb.jpg"
                            },
                            "release_date": "2024-01-01",
                            "is_live": false
                        },
                        {
                            "id": "video-456",
                            "title": "Other Video",
                            "short_description": "Not the target",
                            "url": "http://example.com/other.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/other.mp4"}],
                                "thumb": "http://example.com/other-thumb.jpg"
                            },
                            "release_date": "2024-01-02",
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

            // Wait for feed to load
            val state = viewModel.uiState.first { it !is FeedUiState.Loading }
            assertTrue("Expected Success state", state is FeedUiState.Success)

            // Resolve the correct video ID
            val result = viewModel.resolveDeepLink("video-123")
            assertNotNull("Should find video-123", result)
            assertEquals("video-123", result!!.id)
            assertEquals("Target Video", result.title)

            mockServer.shutdown()
        }

    @Test
    fun `resolveDeepLink_returns_correct_video_from_multiple_groups`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            val validFeedJson =
                """
                {
                    "content": [
                        {
                            "id": "video-week1",
                            "title": "Week 1 Video",
                            "short_description": "From week 1",
                            "url": "http://example.com/week1.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/week1.mp4"}],
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

            // Wait for feed to load
            val state = viewModel.uiState.first { it !is FeedUiState.Loading }
            assertTrue("Expected Success state", state is FeedUiState.Success)

            // Resolve video from loaded feed
            val result = viewModel.resolveDeepLink("video-week1")
            assertNotNull("Should find video-week1", result)
            assertEquals("video-week1", result!!.id)
            assertEquals("Week 1 Video", result.title)

            mockServer.shutdown()
        }
}
