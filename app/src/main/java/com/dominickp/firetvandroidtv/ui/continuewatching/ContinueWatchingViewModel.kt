package com.dominickp.firetvandroidtv.ui.continuewatching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dominickp.firetvandroidtv.dao.ContinueWatchingDao
import com.dominickp.firetvandroidtv.dao.ContinueWatchingEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ContinueWatchingViewModel — manages the continue watching screen state.
 *
 * Mirrors Roku's ContinueWatching screen data flow:
 *   - Loads continue-watching entries from Room DB
 *   - Filters to active (not fully watched) entries
 *   - Updates when entries change
 */
class ContinueWatchingViewModel(
    private val continueWatchingDao: ContinueWatchingDao,
) : ViewModel() {
    private val _entries = MutableStateFlow<List<ContinueWatchingEntry>>(emptyList())
    val entries: StateFlow<List<ContinueWatchingEntry>> = _entries.asStateFlow()

    init {
        // Observe Room DB changes and update ViewModel state
        viewModelScope.launch {
            continueWatchingDao.getActive().collect { activeEntries ->
                _entries.value = activeEntries
            }
        }
    }

    /**
     * Remove an entry from continue watching (user dismissed it).
     */
    fun dismiss(entry: ContinueWatchingEntry) {
        viewModelScope.launch {
            continueWatchingDao.delete(entry)
        }
    }

    /**
     * Mark a video as fully watched.
     */
    fun markAsWatched(videoId: String) {
        viewModelScope.launch {
            continueWatchingDao.markAsWatched(videoId)
        }
    }
}
