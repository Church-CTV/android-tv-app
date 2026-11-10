package com.dominickp.firetvandroidtv.ui.search

import com.dominickp.firetvandroidtv.config.ConfigLoader
import org.junit.Assert.*
import org.junit.Test

/**
 * SearchViewModelFactoryTest — verifies the factory creates SearchViewModel
 * instances with the correct ConfigLoader injection.
 */
class SearchViewModelFactoryTest {
    @Test
    fun `factoryCreatesViewModelReturnsNonNull`() {
        val configLoader = ConfigLoader()
        val factory = SearchViewModelFactory(configLoader)
        val viewModel = factory.create(SearchViewModel::class.java)
        assertNotNull(viewModel)
        assertTrue(viewModel is SearchViewModel)
    }

    @Test
    fun `factoryCreatesViewModelWithEmptyInitialQuery`() {
        val configLoader = ConfigLoader()
        val factory = SearchViewModelFactory(configLoader)
        val viewModel = factory.create(SearchViewModel::class.java)
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun `factoryCreatesViewModelWithIdleInitialState`() {
        val configLoader = ConfigLoader()
        val factory = SearchViewModelFactory(configLoader)
        val viewModel = factory.create(SearchViewModel::class.java)
        assertTrue(viewModel.state.value is SearchState.Idle)
    }

    @Test
    fun `factoryCanCreateMultipleInstances`() {
        val configLoader = ConfigLoader()
        val factory = SearchViewModelFactory(configLoader)
        val vm1 = factory.create(SearchViewModel::class.java)
        val vm2 = factory.create(SearchViewModel::class.java)
        assertNotSame(vm1, vm2)
    }
}
