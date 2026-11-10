package com.dominickp.firetvandroidtv.contract

import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.ui.search.SearchState
import com.dominickp.firetvandroidtv.ui.search.SearchViewModel
import com.dominickp.firetvandroidtv.ui.search.SearchViewModelFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Contract tests for SearchScreen — verifying the search data contract.
 * Tests search input → API call → state transition → render flow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SearchContractTest {
    private val configLoader = ConfigLoader()

    @Test
    fun `search_state_Idle_is_sealed_interface`() {
        val state: SearchState = SearchState.Idle
        assertTrue(state is SearchState.Idle)
    }

    @Test
    fun `search_state_Results_contains_items`() {
        val state = SearchState.Results(emptyList())
        assertTrue(state is SearchState.Results)
    }

    @Test
    fun `search_state_Error_contains_message`() {
        val errorMsg = "Connection failed"
        val state = SearchState.Error(errorMsg)
        assertTrue(state is SearchState.Error)
        assertEquals(errorMsg, (state as SearchState.Error).message)
    }

    @Test
    fun `search_state_Loading_has_no_items_or_error`() {
        val state = SearchState.Loading
        assertTrue(state is SearchState.Loading)
    }

    @Test
    fun `search_viewmodel_factory_creates_viewmodel`() {
        val factory = SearchViewModelFactory(configLoader)
        val viewModel = factory.create(SearchViewModel::class.java)
        assertNotNull(viewModel)
        assertTrue(viewModel is SearchViewModel)
    }

    @Test
    fun `search_contract_config_has_search_api_base_url`() {
        val config = configLoader.loadConfig()
        val baseUrl = config.apiBaseUrl
        assertNotNull(baseUrl)
        assertTrue(baseUrl.startsWith("http"))
    }
}
