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

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial_query_is_empty`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = SearchViewModel(ConfigLoader())
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun `initial_state_is_Idle`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = SearchViewModel(ConfigLoader())
        assertTrue(viewModel.state.value is SearchState.Idle)
    }

    @Test
    fun `onQueryChanged_sets_query`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = SearchViewModel(ConfigLoader())
        viewModel.onQueryChanged("test")
        assertEquals("test", viewModel.query.value)
    }

    @Test
    fun `clear_resets_state_to_idle`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = SearchViewModel(ConfigLoader())
        viewModel.onQueryChanged("test")
        viewModel.clear()
        assertEquals("", viewModel.query.value)
        assertTrue(viewModel.state.value is SearchState.Idle)
    }

    @Test
    fun `clearResults_keeps_query_but_clears_state`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = SearchViewModel(ConfigLoader())
        viewModel.onQueryChanged("test")
        assertEquals("test", viewModel.query.value)
        viewModel.clearResults()
        assertEquals("test", viewModel.query.value)
        assertTrue(viewModel.state.value is SearchState.Idle)
    }
}
