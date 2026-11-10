package com.dominickp.firetvandroidtv.data.network

import com.dominickp.firetvandroidtv.config.ContentConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * FeedRepositoryDeadCodeTest — proves FeedRepository does not depend on SearchApi.
 *
 * Verifies:
 * - searchLocal() operates on in-memory feedData, never calls SearchApi
 * - FeedRepository is fully functional without SearchApi wiring
 *
 * This test guards against re-introducing dead SearchApi wiring into FeedRepository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedRepositoryDeadCodeTest {
    @Test
    fun `searchLocal uses in-memory feedData not SearchApi`() =
        runTest {
            val contentConfig = ContentConfig(sourceUrl = "http://example.com/feed")
            val feedApi = createFeedApi("http://example.com", contentConfig)
            val repo = FeedRepository(feedApi, contentConfig)

            // Seed in-memory data
            val videos =
                mapOf(
                    "2024-01" to
                        listOf(
                            com.dominickp.firetvandroidtv.data.model.VideoItem(
                                id = "vid-1",
                                title = "Test Video",
                                shortDescription = "Test desc",
                                url = "http://example.com/v.mp4",
                                releaseDate = "2024-01-01",
                            ),
                        ),
                )
            repo.setFeedData(videos)

            // Search should find it via in-memory data, not SearchApi
            val result = repo.searchLocal("Test", limit = 50)
            assertTrue(result.isSuccess)
            val results = result.getOrNull()!!
            assertEquals(1, results.size)
            assertEquals("vid-1", results[0].id)
        }

    @Test
    fun `searchLocal returns empty when no feed data set`() =
        runTest {
            val contentConfig = ContentConfig(sourceUrl = "http://example.com/feed")
            val feedApi = createFeedApi("http://example.com", contentConfig)
            val repo = FeedRepository(feedApi, contentConfig)

            // Never set feed data — searchLocal should return empty
            val result = repo.searchLocal("anything", limit = 50)
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }
}
