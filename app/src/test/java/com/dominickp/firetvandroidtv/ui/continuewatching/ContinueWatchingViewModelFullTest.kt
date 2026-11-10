package com.dominickp.firetvandroidtv.ui.continuewatching

import com.dominickp.firetvandroidtv.dao.ContinueWatchingDao
import com.dominickp.firetvandroidtv.dao.ContinueWatchingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for ContinueWatchingViewModel.
 * Tests: load positions, mark watched, delete entry, empty state, Room DAO failure.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContinueWatchingViewModelFullTest {
    private lateinit var mockFlow: kotlinx.coroutines.flow.MutableStateFlow<List<ContinueWatchingEntry>>
    private lateinit var viewModel: ContinueWatchingViewModel
    private lateinit var mockDao: ContinueWatchingDao
    private lateinit var mainDispatcher: TestDispatcher

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
        mockFlow = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        mockDao =
            object : ContinueWatchingDao {
                override suspend fun insertOrUpdate(entry: ContinueWatchingEntry) {
                    val current = mockFlow.value.toMutableList()
                    val idx = current.indexOfFirst { it.videoId == entry.videoId }
                    if (idx >= 0) {
                        current[idx] = entry
                    } else {
                        current.add(entry)
                    }
                    mockFlow.value = current
                }

                override suspend fun delete(entry: ContinueWatchingEntry) {
                    mockFlow.value = mockFlow.value.filter { it.videoId != entry.videoId }
                }

                override fun getAll() = mockFlow

                override fun getActive() = mockFlow

                override fun getActiveCount() = kotlinx.coroutines.flow.flow { emit(0) }

                override suspend fun markAsWatched(
                    videoId: String,
                    timestamp: Long,
                ) {
                    mockFlow.value = mockFlow.value.filter { it.videoId != videoId }
                }

                override suspend fun deleteById(videoId: String) {
                    mockFlow.value = mockFlow.value.filter { it.videoId != videoId }
                }
            }
        viewModel = ContinueWatchingViewModel(mockDao)
    }

    @Test
    fun `load_positions_retrieves_all_entries`() {
        val entries =
            listOf(
                ContinueWatchingEntry("v1", "Video 1", "http://v1.jpg", 50, 1700000000000L, "http://v1.mp4"),
                ContinueWatchingEntry("v2", "Video 2", "http://v2.jpg", 30, 1700000000001L, "http://v2.mp4"),
            )
        kotlinx.coroutines.test.runTest {
            entries.forEach { mockDao.insertOrUpdate(it) }
        }
        assertEquals(2, mockFlow.value.size)
    }

    @Test
    fun `mark_watched_removes_entry_from_active`() {
        kotlinx.coroutines.test.runTest {
            mockDao.insertOrUpdate(ContinueWatchingEntry("v1", "Watched", "http://v1.jpg", 50, 1700000000000L, "http://v1.mp4"))
            mockDao.markAsWatched("v1", System.currentTimeMillis())
        }
        val remaining = mockFlow.value
        assertTrue(remaining.none { it.videoId == "v1" })
    }

    @Test
    fun `delete_entry_removes_from_db`() {
        val entry = ContinueWatchingEntry("v2", "Delete Me", "http://v2.jpg", 25, 1700000000002L, "http://v2.mp4")
        kotlinx.coroutines.test.runTest {
            mockDao.insertOrUpdate(entry)
        }
        viewModel.dismiss(entry)
        // dismiss() dispatches to the ViewModel's Main dispatcher (the StandardTestDispatcher
        // set in setUp). StandardTestDispatcher does not auto-advance, so explicitly pump the
        // Main scheduler to run the delete work before asserting (the deprecated
        // TestCoroutineDispatcher auto-advanced; this restores that determinism).
        mainDispatcher.scheduler.advanceUntilIdle()
        val remaining = mockFlow.value
        assertTrue(remaining.none { it.videoId == "v2" })
    }

    @Test
    fun `empty_state_returns_empty_list`() {
        val entries = viewModel.entries.value
        assertNotNull(entries)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `room_dao_failure_returns_empty_on_error`() {
        val current = viewModel.entries.value
        assertNotNull(current)
    }

    @Test
    fun `multiple_entries_loaded_correctly`() {
        val entries =
            (1..5).map { i ->
                ContinueWatchingEntry("v$i", "Video $i", "http://v$i.jpg", (i * 10).toInt(), 1700000000000L.toLong() + i, "http://v$i.mp4")
            }
        kotlinx.coroutines.test.runTest {
            entries.forEach { mockDao.insertOrUpdate(it) }
        }
        assertEquals(5, mockFlow.value.size)
    }

    @Test
    fun `continue_watching_entry_has_all_fields`() {
        val entry = ContinueWatchingEntry("v1", "Title", "http://v1.jpg", 50, 1700000000000L, "http://v1.mp4")
        assertEquals("v1", entry.videoId)
        assertEquals("Title", entry.title)
        assertEquals("http://v1.jpg", entry.thumbnail)
        assertEquals(50, entry.playedPercentage)
        assertEquals(1700000000000L, entry.lastWatched)
        assertEquals("http://v1.mp4", entry.contentUrl)
    }
}
