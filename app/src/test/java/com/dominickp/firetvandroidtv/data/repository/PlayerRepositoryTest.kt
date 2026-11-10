package com.dominickp.firetvandroidtv.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dominickp.firetvandroidtv.db.FireTVDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerRepositoryTest {
    private lateinit var database: FireTVDatabase
    private lateinit var repository: PlayerRepository

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FireTVDatabase::class.java,
            ).build()
        val dao = database.continueWatchingDao()
        repository = PlayerRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savePlaybackPosition_createsEntry() =
        runTest {
            repository.savePlaybackPosition(
                videoId = "test-video-1",
                title = "Test Video",
                thumbnail = "https://example.com/thumb.jpg",
                playedPercentage = 45,
                contentUrl = "https://example.com/stream.m3u8",
            )

            val entries = database.continueWatchingDao().getActive().first()
            assertEquals(1, entries.size)
            assertEquals("test-video-1", entries[0].videoId)
            assertEquals("Test Video", entries[0].title)
            assertEquals(45, entries[0].playedPercentage)
        }

    @Test
    fun savePlaybackPosition_updatesExistingEntry() =
        runTest {
            // Save initial position
            repository.savePlaybackPosition(
                videoId = "test-video-2",
                title = "Test Video",
                thumbnail = "https://example.com/thumb.jpg",
                playedPercentage = 20,
                contentUrl = "https://example.com/stream.m3u8",
            )

            // Update position
            repository.savePlaybackPosition(
                videoId = "test-video-2",
                title = "Test Video",
                thumbnail = "https://example.com/thumb.jpg",
                playedPercentage = 75,
                contentUrl = "https://example.com/stream.m3u8",
            )

            val entries = database.continueWatchingDao().getActive().first()
            assertEquals(1, entries.size)
            assertEquals(75, entries[0].playedPercentage)
        }

    @Test
    fun markAsWatched_removesFromActiveList() =
        runTest {
            repository.savePlaybackPosition(
                videoId = "test-video-3",
                title = "Test Video",
                thumbnail = "https://example.com/thumb.jpg",
                playedPercentage = 80,
                contentUrl = "https://example.com/stream.m3u8",
            )

            // Verify it exists
            var entries = database.continueWatchingDao().getActive().first()
            assertEquals(1, entries.size)

            // Mark as watched
            repository.markAsWatched("test-video-3")

            // Verify it's removed
            entries = database.continueWatchingDao().getActive().first()
            assertEquals(0, entries.size)
        }

    @Test
    fun markAsWatched_nonExistent_doesNotThrow() =
        runTest {
            // Should not throw for non-existent video
            repository.markAsWatched("non-existent-video-id")
            val entries = database.continueWatchingDao().getActive().first()
            assertEquals(0, entries.size)
        }

    @Test
    fun saveMultipleVideos_multipleEntries() =
        runTest {
            for (i in 1..3) {
                repository.savePlaybackPosition(
                    videoId = "test-video-$i",
                    title = "Test Video $i",
                    thumbnail = "https://example.com/thumb$i.jpg",
                    playedPercentage = i * 20,
                    contentUrl = "https://example.com/stream$i.m3u8",
                )
            }

            val entries = database.continueWatchingDao().getActive().first()
            assertEquals(3, entries.size)
        }

    @Test
    fun savePlaybackPosition_sameVideoId_updatesNotCreates() =
        runTest {
            for (i in 1..5) {
                repository.savePlaybackPosition(
                    videoId = "same-video",
                    title = "Same Video",
                    thumbnail = "https://example.com/thumb.jpg",
                    playedPercentage = i * 10,
                    contentUrl = "https://example.com/stream.m3u8",
                )
            }

            val entries = database.continueWatchingDao().getActive().first()
            assertEquals(1, entries.size)
            assertEquals(50, entries[0].playedPercentage)
        }

    @Test
    fun getActive_excludesWatched() =
        runTest {
            repository.savePlaybackPosition(
                videoId = "active-video",
                title = "Active",
                thumbnail = "https://example.com/thumb.jpg",
                playedPercentage = 30,
                contentUrl = "https://example.com/stream.m3u8",
            )
            repository.savePlaybackPosition(
                videoId = "watched-video",
                title = "Watched",
                thumbnail = "https://example.com/thumb.jpg",
                playedPercentage = 100,
                contentUrl = "https://example.com/stream.m3u8",
            )
            repository.markAsWatched("watched-video")

            val entries = database.continueWatchingDao().getActive().first()
            assertEquals(1, entries.size)
            assertEquals("active-video", entries[0].videoId)
        }

    @Test
    fun repository_createdWithDatabase_notNull() {
        assertNotNull(repository)
    }
}
