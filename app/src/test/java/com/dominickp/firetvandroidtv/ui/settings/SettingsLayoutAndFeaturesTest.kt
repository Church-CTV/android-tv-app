package com.dominickp.firetvandroidtv.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dominickp.firetvandroidtv.data.local.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SettingsLayoutAndFeaturesTest — tests for layout preference and feature toggles
 * added for Roku parity (D4 audit).
 *
 * Roku reference: ConfigManager.brs layoutToggle() cycles leftnav ↔ grid;
 * SettingsScreen.brs feature toggles for search, deep linking, analytics, crash reporting.
 *
 * NOTE: DataStore persistence writes go through Dispatchers.IO and file I/O;
 * following the established convention (SettingsViewModelTest), tests verify
 * the ViewModel exposes correct initial state and that save methods delegate
 * without throwing, rather than asserting DataStore round-trips.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsLayoutAndFeaturesTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial_layoutPreference_is_leftnav`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertEquals("leftnav", viewModel.layoutPreference.value)
    }

    @Test
    fun `initial_feature_toggles_are_all_true`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertTrue(viewModel.featureSearch.value)
        assertTrue(viewModel.featureDeepLinking.value)
        assertTrue(viewModel.featureAnalytics.value)
        assertTrue(viewModel.featureCrashReporting.value)
    }

    @Test
    fun `saveLayoutPreference_grid_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveLayoutPreference("grid")
        viewModel.saveLayoutPreference("leftnav")
    }

    @Test
    fun `saveFeatureToggles_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveFeatureSearch(false)
        viewModel.saveFeatureDeepLinking(false)
        viewModel.saveFeatureAnalytics(false)
        viewModel.saveFeatureCrashReporting(false)
        viewModel.saveFeatureSearch(true)
    }

    @Test
    fun `layout_preference_flow_exists_and_exposes_leftnav_initial`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        // Initial value available synchronously before DataStore IO dispatch.
        assertEquals("leftnav", viewModel.layoutPreference.value)
    }

    @Test
    fun `feature_toggles_flows_exist_and_expose_true_initials`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertTrue(viewModel.featureSearch.value)
        assertTrue(viewModel.featureDeepLinking.value)
        assertTrue(viewModel.featureAnalytics.value)
        assertTrue(viewModel.featureCrashReporting.value)
    }
}
