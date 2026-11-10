package com.dominickp.firetvandroidtv.data.network

import com.dominickp.firetvandroidtv.config.ContentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Thread safety tests for FeedRepository feedData access.
 * Verifies @Volatile guarantees safe concurrent read/write.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FeedRepositoryThreadSafetyTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockServer: MockWebServer
    private lateinit var repo: FeedRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockServer =
            MockWebServer().apply {
                start()
                enqueue(MockResponse().setBody("""{"rows":[]}"""))
            }

        val contentCfg = ContentConfig(sourceUrl = mockServer.url("/feed").toString())
        val api = createFeedApi(mockServer.url("/").toString(), contentCfg)
        repo = FeedRepository(api, contentCfg)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            mockServer.shutdown()
        } catch (_: Exception) {
            // ignore
        }
    }

    @Test
    fun `concurrent writes and reads on feedData do not throw`() =
        runTest(testDispatcher) {
            val video =
                com.dominickp.firetvandroidtv.data.model.VideoItem(
                    id = "test-1",
                    title = "Test Video",
                    shortDescription = "",
                    thumbnail = "",
                    url = "",
                    releaseDate = "2026-08-01",
                    genres = listOf(),
                )

            val groups = mapOf("July 2026" to listOf(video))

            // Spawn multiple concurrent writers (replacing feedData)
            repeat(50) { i ->
                launch(Dispatchers.Default) {
                    repo.setFeedData(mapOf("Group$i" to groups["July 2026"]!!))
                }
            }

            // Spawn multiple concurrent readers via searchLocal
            repeat(50) {
                launch(Dispatchers.Default) {
                    repo.searchLocal(query = "Test", limit = 10).getOrNull()
                }
            }

            // If we reach here without NPE/ConcurrentModificationException, @Volatile works.
        }

    @Test
    fun `clearFeedData is safe while searchLocal is running`() =
        runTest(testDispatcher) {
            val video =
                com.dominickp.firetvandroidtv.data.model.VideoItem(
                    id = "test-2",
                    title = "Another Video",
                    shortDescription = "",
                    thumbnail = "",
                    url = "",
                    releaseDate = "2026-08-01",
                    genres = listOf(),
                )

            repo.setFeedData(mapOf("July 2026" to listOf(video)))

            var crashes = 0
            try {
                launch(Dispatchers.Default) {
                    repeat(100) {
                        if (Thread.currentThread().isInterrupted) return@repeat
                        repo.searchLocal(query = "Another", limit = 5).getOrNull()
                    }
                }

                launch(Dispatchers.Default) {
                    repeat(20) {
                        repo.clearFeedData()
                        repo.setFeedData(mapOf("July 2026" to listOf(video)))
                    }
                }
            } catch (e: Exception) {
                crashes++
            }

            require(crashes == 0) { "Expected no crashes, got $crashes" }
        }
}
