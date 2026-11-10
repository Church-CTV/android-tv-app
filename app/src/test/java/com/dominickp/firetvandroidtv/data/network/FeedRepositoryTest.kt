package com.dominickp.firetvandroidtv.data.network

import com.dominickp.firetvandroidtv.config.ContentConfig
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
 * FeedRepositoryTest — tests feed fetching, parsing, and findVideoById.
 *
 * Verifies:
 * - Repository creation with valid API
 * - Feed fetch returns grouped videos
 * - findVideoById returns correct video
 * - findVideoById returns null for missing IDs
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedRepositoryTest {
    private lateinit var mockServer: MockWebServer

    @After
    fun tearDown() {
        if (::mockServer.isInitialized) {
            mockServer.shutdown()
        }
    }

    @Test
    fun `create repository with valid api does not throw`() {
        val contentConfig = ContentConfig(sourceUrl = "http://example.com/feed")
        val feedApi = createFeedApi("http://example.com", contentConfig)
        val repo = FeedRepository(feedApi, contentConfig)
        assertNotNull(repo)
    }

    @Test
    fun `fetchFeed returns grouped videos with valid JSON`() =
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
                        },
                        {
                            "id": "video-2",
                            "title": "Test Video 2",
                            "short_description": "Description 2",
                            "url": "http://example.com/video2.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/video2.mp4"}],
                                "thumb": "http://example.com/thumb2.jpg"
                            },
                            "release_date": "2024-01-02",
                            "is_live": false
                        }
                    ]
                }
                """.trimIndent()

            mockServer.enqueue(MockResponse().setBody(validFeedJson).setResponseCode(200))

            val contentConfig =
                ContentConfig(
                    sourceUrl = mockServer.url("/feed").toString(),
                    retryAttempts = 1,
                    retryDelayMs = 0,
                )
            val feedApi = createFeedApi(mockServer.url("/").toString(), contentConfig)
            val repo = FeedRepository(feedApi, contentConfig)

            val result = repo.fetchFeed()
            assertTrue(result.isSuccess)
            val groups = result.getOrNull()!!
            assertTrue("Expected at least one group", groups.isNotEmpty())
            assertEquals(2, groups.values.sumOf { it.size })
        }

    @Test
    fun `findVideoById returns video when found_in_feed`() =
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
                            "short_description": "This is the target",
                            "url": "http://example.com/target.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/target.mp4"}],
                                "thumb": "http://example.com/thumb.jpg"
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
                                "thumb": "http://example.com/thumb.jpg"
                            },
                            "release_date": "2024-01-02",
                            "is_live": false
                        }
                    ]
                }
                """.trimIndent()

            mockServer.enqueue(MockResponse().setBody(validFeedJson).setResponseCode(200))

            val contentConfig =
                ContentConfig(
                    sourceUrl = mockServer.url("/feed").toString(),
                    retryAttempts = 1,
                    retryDelayMs = 0,
                )
            val feedApi = createFeedApi(mockServer.url("/").toString(), contentConfig)
            val repo = FeedRepository(feedApi, contentConfig)

            val result = repo.fetchFeed()
            assertTrue(result.isSuccess)
            val groups = result.getOrNull()!!

            val foundVideo = repo.findVideoById("video-123", groups)
            assertNotNull("Expected to find video-123", foundVideo)
            assertEquals("video-123", foundVideo!!.id)
            assertEquals("Target Video", foundVideo.title)
        }

    @Test
    fun `findVideoById returns null when_video_not_found`() =
        runTest {
            mockServer = MockWebServer()
            mockServer.start()

            val validFeedJson =
                """
                {
                    "content": [
                        {
                            "id": "video-1",
                            "title": "Video 1",
                            "short_description": "Desc",
                            "url": "http://example.com/video1.mp4",
                            "content": {
                                "videos": [{"url": "http://example.com/video1.mp4"}],
                                "thumb": "http://example.com/thumb.jpg"
                            },
                            "release_date": "2024-01-01",
                            "is_live": false
                        }
                    ]
                }
                """.trimIndent()

            mockServer.enqueue(MockResponse().setBody(validFeedJson).setResponseCode(200))

            val contentConfig =
                ContentConfig(
                    sourceUrl = mockServer.url("/feed").toString(),
                    retryAttempts = 1,
                    retryDelayMs = 0,
                )
            val feedApi = createFeedApi(mockServer.url("/").toString(), contentConfig)
            val repo = FeedRepository(feedApi, contentConfig)

            val result = repo.fetchFeed()
            assertTrue(result.isSuccess)
            val groups = result.getOrNull()!!

            val foundVideo = repo.findVideoById("nonexistent-id", groups)
            assertNull("Expected null for nonexistent ID", foundVideo)
        }

    @Test
    fun `findVideoById returns null for_empty_groups`() {
        val contentConfig = ContentConfig(sourceUrl = "http://example.com/feed")
        val feedApi = createFeedApi("http://example.com", contentConfig)
        val repo = FeedRepository(feedApi, contentConfig)

        val foundVideo = repo.findVideoById("any-id", emptyMap())
        assertNull("Expected null for empty groups", foundVideo)
    }
}
