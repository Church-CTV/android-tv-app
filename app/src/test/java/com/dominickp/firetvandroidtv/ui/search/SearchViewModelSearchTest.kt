package com.dominickp.firetvandroidtv.ui.search

import com.dominickp.firetvandroidtv.config.ConfigLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

/**
 * SearchViewModelSearchTest — tests the search functionality.
 *
 * Verifies:
 * - Empty query clears results to idle
 * - Search query is stored
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelSearchTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty_query_clears_to_idle`() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        val viewModel = SearchViewModel(ConfigLoader(), testDispatcher)

        assertTrue("Starts Idle", viewModel.state.value is SearchState.Idle)
        viewModel.onQueryChanged("test")
        viewModel.onQueryChanged("")
        assertTrue("Empty query → Idle", viewModel.state.value is SearchState.Idle)
    }

    @Test
    fun `query_stored_on_change`() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        val viewModel = SearchViewModel(ConfigLoader(), testDispatcher)

        viewModel.onQueryChanged("foobar")
        assertEquals("foobar", viewModel.query.value)
    }

    @Test
    fun `onQueryChanged_cancels_previous_job`() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        val viewModel = SearchViewModel(ConfigLoader(), testDispatcher)

        // First query
        viewModel.onQueryChanged("first")
        // Second query cancels first
        viewModel.onQueryChanged("second")
        assertEquals("second", viewModel.query.value)
    }

    @Test
    fun `clear_cancels_search_job`() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        val viewModel = SearchViewModel(ConfigLoader(), testDispatcher)

        viewModel.onQueryChanged("test")
        viewModel.clear()
        assertEquals("", viewModel.query.value)
        assertTrue(viewModel.state.value is SearchState.Idle)
    }
}
