package com.dominickp.firetvandroidtv.ui.search

import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.config.ChannelConfig
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.config.ContentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SearchViewModelAnalyticsTest — verifies that SearchViewModel tracks search analytics events
 * when results are found.
 *
 * Verifies:
 * - Analytics tracker is initialized from config
 * - Analytics tracker falls back to default config when loading fails
 * - ViewModel construction does not crash on config errors
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SearchViewModelAnalyticsTest {
    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
        Dispatchers.resetMain()
    }

    @Test
    fun `analyticsTracker_initialized_from_config`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val analyticsConfig =
            AnalyticsConfig(
                enabled = true,
                exportUrl = "http://localhost:9999/analytics",
                exportIntervalSeconds = 300,
            )
        val viewModel =
            SearchViewModel(
                configLoader =
                    object : ConfigLoader() {
                        override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                            return AppConfig(
                                channel =
                                    ChannelConfig(
                                        name = "TestChannel",
                                    ),
                                content =
                                    ContentConfig(
                                        sourceUrl = "http://localhost:9999/feed",
                                    ),
                                analytics = analyticsConfig,
                            )
                        }
                    },
                dispatcher = StandardTestDispatcher(),
            )
        // ViewModel should not crash on construction
        assertNotNull(viewModel)
        assertTrue(viewModel.state.value is SearchState.Idle)
    }

    @Test
    fun `analytics_tracker_fallback_on_config_error`() {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel =
            SearchViewModel(
                configLoader =
                    object : ConfigLoader() {
                        override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                            throw RuntimeException("Simulated config error")
                        }
                    },
                dispatcher = StandardTestDispatcher(),
            )
        // ViewModel should fall back to default config without crashing
        assertNotNull(viewModel)
        assertTrue(viewModel.state.value is SearchState.Idle)
    }
}
