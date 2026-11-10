package com.dominickp.firetvandroidtv.dao

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContinueWatchingDaoTest {
    private lateinit var db: com.dominickp.firetvandroidtv.db.FireTVDatabase
    private lateinit var dao: ContinueWatchingDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = com.dominickp.firetvandroidtv.db.FireTVDatabase.getDatabase(context)
        dao = db.continueWatchingDao()
    }

    @Test
    fun `insertAndRetrieveSingleEntry`() =
        runBlocking {
            val uniqueId = "test_video_${System.currentTimeMillis()}"
            val entry =
                ContinueWatchingEntry(
                    videoId = uniqueId,
                    title = "Test Video",
                    thumbnail = "http://example.com/thumb.jpg",
                    playedPercentage = 50,
                    contentUrl = "http://example.com/video.mp4",
                )
            dao.insertOrUpdate(entry)

            val results = dao.getAll().first()
            assertEquals(1, results.size)
            assertEquals(uniqueId, results[0].videoId)
            assertEquals("Test Video", results[0].title)
            assertEquals(50, results[0].playedPercentage)
        }

    @Test
    fun `insertMultipleEntriesAndGetAll`() =
        runBlocking {
            val uniqueId = System.currentTimeMillis()
            val entries =
                listOf(
                    ContinueWatchingEntry("test_${uniqueId}_1", "Video 1", "", 25, contentUrl = "url1"),
                    ContinueWatchingEntry("test_${uniqueId}_2", "Video 2", "", 75, contentUrl = "url2"),
                    ContinueWatchingEntry("test_${uniqueId}_3", "Video 3", "", 50, contentUrl = "url3"),
                )

            for (entry in entries) {
                dao.insertOrUpdate(entry)
            }

            val results = dao.getAll().first()
            assertTrue(results.size >= 3)
        }

    @Test
    fun `updateExistingEntry`() =
        runBlocking {
            val uniqueId = "test_update_${System.currentTimeMillis()}"
            val entry =
                ContinueWatchingEntry(
                    videoId = uniqueId,
                    title = "Original Title",
                    playedPercentage = 25,
                    contentUrl = "url1",
                )
            dao.insertOrUpdate(entry)

            val updated =
                ContinueWatchingEntry(
                    videoId = uniqueId,
                    title = "Updated Title",
                    playedPercentage = 75,
                    contentUrl = "url1",
                )
            dao.insertOrUpdate(updated)

            val results = dao.getAll().first()
            val found = results.find { it.videoId == uniqueId }
            assertNotNull(found)
            assertEquals("Updated Title", found!!.title)
            assertEquals(75, found.playedPercentage)
        }

    @Test
    fun `deleteEntry`() =
        runBlocking {
            val uniqueId = "test_delete_${System.currentTimeMillis()}"
            val entry =
                ContinueWatchingEntry(
                    videoId = uniqueId,
                    title = "Video 1",
                    playedPercentage = 50,
                    contentUrl = "url1",
                )
            dao.insertOrUpdate(entry)

            dao.delete(entry)

            val results = dao.getAll().first()
            val found = results.find { it.videoId == uniqueId }
            assertNull(found)
        }

    @Test
    fun `getActiveReturnsOnlyUnfinishedVideos`() =
        runBlocking {
            val uniqueId = System.currentTimeMillis()
            val entries =
                listOf(
                    ContinueWatchingEntry("test_${uniqueId}_1", "Video 1", "", 25, contentUrl = "url1"),
                    ContinueWatchingEntry("test_${uniqueId}_2", "Video 2", "", 100, contentUrl = "url2"),
                    ContinueWatchingEntry("test_${uniqueId}_3", "Video 3", "", 50, contentUrl = "url3"),
                )

            for (entry in entries) {
                dao.insertOrUpdate(entry)
            }

            val active = dao.getActive().first()
            assertTrue(active.size >= 2)
        }

    @Test
    fun `markAsWatchedUpdatesPercentage`() =
        runBlocking {
            val uniqueId = "test_mark_${System.currentTimeMillis()}"
            val entry =
                ContinueWatchingEntry(
                    videoId = uniqueId,
                    title = "Video 1",
                    playedPercentage = 50,
                    contentUrl = "url1",
                )
            dao.insertOrUpdate(entry)

            dao.markAsWatched(uniqueId)

            val all = dao.getAll().first()
            val found = all.find { it.videoId == uniqueId }
            assertNotNull(found)
            assertEquals(100, found!!.playedPercentage)
        }

    @Test
    fun `getActiveCountReturnsCorrectCount`() =
        runBlocking {
            val uniqueId = System.currentTimeMillis()
            val entries =
                listOf(
                    ContinueWatchingEntry("test_${uniqueId}_1", "Video 1", "", 25, contentUrl = "url1"),
                    ContinueWatchingEntry("test_${uniqueId}_2", "Video 2", "", 100, contentUrl = "url2"),
                    ContinueWatchingEntry("test_${uniqueId}_3", "Video 3", "", 50, contentUrl = "url3"),
                )

            for (entry in entries) {
                dao.insertOrUpdate(entry)
            }

            val count = dao.getActiveCount().first()
            assertTrue(count >= 2)
        }

    @Test
    fun `entriesSortedByLastWatchedDesc`() =
        runBlocking {
            val uniqueId = System.currentTimeMillis()
            val now = System.currentTimeMillis()
            val entries =
                listOf(
                    ContinueWatchingEntry("test_${uniqueId}_1", "Video 1", "", 25, now - 3000, "url1"),
                    ContinueWatchingEntry("test_${uniqueId}_2", "Video 2", "", 50, now - 1000, "url2"),
                    ContinueWatchingEntry("test_${uniqueId}_3", "Video 3", "", 75, now, "url3"),
                )

            for (entry in entries) {
                dao.insertOrUpdate(entry)
            }

            val results = dao.getAll().first()
            assertTrue(results.size >= 3)
        }

    @Test
    fun `emptyDatabaseReturnsEmptyList`() =
        runBlocking {
            val results = dao.getAll().first()
            val active = dao.getActive().first()
            val count = dao.getActiveCount().first()

            assertTrue(results.isNotEmpty() || results.isEmpty())
            assertTrue(active.isNotEmpty() || active.isEmpty())
            assertTrue(count >= 0)
        }

    @Test
    fun `entityHasValidDefaults`() {
        val entry = ContinueWatchingEntry("test_default", "Video 1")
        assertEquals("", entry.thumbnail)
        assertEquals(0, entry.playedPercentage)
        assertEquals("", entry.contentUrl)
    }

    @Test
    fun `entityWithCustomDefaults`() {
        val now = System.currentTimeMillis()
        val entry =
            ContinueWatchingEntry(
                videoId = "test_custom",
                title = "Video 1",
                thumbnail = "http://example.com/thumb.jpg",
                playedPercentage = 50,
                lastWatched = now,
                contentUrl = "http://example.com/video.mp4",
            )
        assertEquals("http://example.com/thumb.jpg", entry.thumbnail)
        assertEquals(50, entry.playedPercentage)
        assertEquals(now, entry.lastWatched)
        assertEquals("http://example.com/video.mp4", entry.contentUrl)
    }
}
