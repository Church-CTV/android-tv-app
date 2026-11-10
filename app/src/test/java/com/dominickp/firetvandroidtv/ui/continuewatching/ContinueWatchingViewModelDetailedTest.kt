package com.dominickp.firetvandroidtv.ui.continuewatching

import com.dominickp.firetvandroidtv.dao.ContinueWatchingDao
import com.dominickp.firetvandroidtv.dao.ContinueWatchingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContinueWatchingViewModelDetailedTest {
    private lateinit var viewModel: ContinueWatchingViewModel

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        val mockDao =
            object : ContinueWatchingDao {
                private val _entries = kotlinx.coroutines.flow.MutableStateFlow<List<ContinueWatchingEntry>>(emptyList())

                override suspend fun insertOrUpdate(entry: ContinueWatchingEntry) {
                    val current = _entries.value.toMutableList()
                    val idx = current.indexOfFirst { it.videoId == entry.videoId }
                    if (idx >= 0) {
                        current[idx] = entry
                    } else {
                        current.add(entry)
                    }
                    _entries.value = current
                }

                override suspend fun delete(entry: ContinueWatchingEntry) {
                    _entries.value = _entries.value.filter { it.videoId != entry.videoId }
                }

                override fun getAll() = _entries

                override fun getActive() = _entries

                override fun getActiveCount() = kotlinx.coroutines.flow.flow { emit(0) }

                override suspend fun markAsWatched(
                    videoId: String,
                    timestamp: Long,
                ) {
                    _entries.value = _entries.value.filter { it.videoId != videoId }
                }

                override suspend fun deleteById(videoId: String) {
                    _entries.value = _entries.value.filter { it.videoId != videoId }
                }
            }
        viewModel = ContinueWatchingViewModel(mockDao)
    }

    @Test
    fun `entries_is_a_StateFlow`() {
        assertTrue(viewModel.entries is kotlinx.coroutines.flow.StateFlow)
    }

    @Test
    fun `initial_entries_are_empty`() {
        val entries = viewModel.entries.value
        assertNotNull(entries)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `continue_watching_entry_has_valid_fields`() {
        val entry = ContinueWatchingEntry("vid3", "Test Title", "http://test.jpg", 50, 1700000000002L, "http://test.mp4")
        assertEquals("vid3", entry.videoId)
        assertEquals("Test Title", entry.title)
        assertEquals(50, entry.playedPercentage)
    }

    @Test
    fun `continue_watching_entry_defaults`() {
        val entry = ContinueWatchingEntry("vid4", "Default")
        assertEquals("vid4", entry.videoId)
        assertEquals("Default", entry.title)
        assertEquals(0, entry.playedPercentage)
    }
}
