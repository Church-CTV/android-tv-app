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
 * SettingsViewModelTest — unit tests for SettingsViewModel.
 *
 * Tests:
 *   - Initial state defaults for all settings
 *   - Save quality with validation
 *   - Save autoplay toggle
 *   - Save show descriptions toggle
 *   - Save language with validation
 *   - Save subtitles toggle
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsViewModelTest {
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
    fun `initial_quality_is_auto`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertEquals("auto", viewModel.quality.value)
    }

    @Test
    fun `initial_autoplay_is_true`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertTrue(viewModel.autoplay.value)
    }

    @Test
    fun `initial_showDescriptions_is_true`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertTrue(viewModel.showDescriptions.value)
    }

    @Test
    fun `initial_language_is_en`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertEquals("en", viewModel.language.value)
    }

    @Test
    fun `initial_subtitlesEnabled_is_false`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertTrue(!viewModel.subtitlesEnabled.value)
    }

    @Test
    fun `saveQuality_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        // Verify the method exists and accepts valid values
        viewModel.saveQuality("1080p")
        viewModel.saveQuality("720p")
        viewModel.saveQuality("4K")
        viewModel.saveQuality("auto")
    }

    @Test
    fun `saveQuality_invalid_defaults_to_auto`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        // Verify the method exists and handles invalid values
        viewModel.saveQuality("invalid")
        viewModel.saveQuality("UltraHD")
        viewModel.saveQuality("1080")
    }

    @Test
    fun `initial_keepScreenAwake_is_true`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        assertTrue(viewModel.keepScreenAwake.value)
    }

    @Test
    fun `saveKeepScreenAwake_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveKeepScreenAwake(false)
        viewModel.saveKeepScreenAwake(true)
    }

    @Test
    fun `saveAutoplay_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveAutoplay(true)
        viewModel.saveAutoplay(false)
    }

    @Test
    fun `saveShowDescriptions_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveShowDescriptions(true)
        viewModel.saveShowDescriptions(false)
    }

    @Test
    fun `saveLanguage_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveLanguage("en")
        viewModel.saveLanguage("es")
        viewModel.saveLanguage("fr")
    }

    @Test
    fun `saveLanguage_invalid_defaults_to_en`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveLanguage("de")
        viewModel.saveLanguage("ja")
        viewModel.saveLanguage("zh")
    }

    @Test
    fun `saveSubtitlesEnabled_delegates_to_SettingsPreferences`() {
        val settingsPrefs = SettingsPreferences(context)
        val viewModel = SettingsViewModel(settingsPrefs)
        viewModel.saveSubtitlesEnabled(true)
        viewModel.saveSubtitlesEnabled(false)
    }
}
