package com.dominickp.firetvandroidtv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dominickp.firetvandroidtv.data.local.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SettingsViewModel — manages settings state using MVVM pattern.
 *
 * Provides:
 *   - Quality setting (auto, 720p, 1080p, 4K)
 *   - Autoplay toggle
 *   - Show descriptions toggle
 *   - Language setting (en, es, fr)
 *   - Subtitles toggle
 *   - Layout preference (leftnav, grid)
 *   - Feature toggles (search, deep linking, analytics, crash reporting)
 *   - Save methods for each setting
 *
 * Mirrors Roku's Settings scene behavior with DataStore persistence.
 * Created via viewModel() lambda factory in AppNavigation for proper lifecycle ownership.
 */
class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
) : ViewModel() {
    /** Current video quality setting. */
    val quality: StateFlow<String> =
        settingsPreferences.quality
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = "auto",
            )

    /** Current autoplay setting. */
    val autoplay: StateFlow<Boolean> =
        settingsPreferences.autoplay
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Current show descriptions setting. */
    val showDescriptions: StateFlow<Boolean> =
        settingsPreferences.showDescriptions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Current language setting. */
    val language: StateFlow<String> =
        settingsPreferences.language
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = "en",
            )

    /** Current subtitles enabled setting. */
    val subtitlesEnabled: StateFlow<Boolean> =
        settingsPreferences.subtitlesEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    /** Current layout preference setting (leftnav, grid). */
    val layoutPreference: StateFlow<String> =
        settingsPreferences.layoutPreference
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = "leftnav",
            )

    /** Feature flag: search enabled. */
    val featureSearch: StateFlow<Boolean> =
        settingsPreferences.featureSearch
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Feature flag: deep linking enabled. */
    val featureDeepLinking: StateFlow<Boolean> =
        settingsPreferences.featureDeepLinking
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Feature flag: analytics enabled. */
    val featureAnalytics: StateFlow<Boolean> =
        settingsPreferences.featureAnalytics
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Feature flag: crash reporting enabled. */
    val featureCrashReporting: StateFlow<Boolean> =
        settingsPreferences.featureCrashReporting
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Keep the TV screen awake while the app is open (default ON). */
    val keepScreenAwake: StateFlow<Boolean> =
        settingsPreferences.keepScreenAwake
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    /** Save quality setting. */
    fun saveQuality(newQuality: String) {
        viewModelScope.launch {
            settingsPreferences.saveQuality(newQuality)
        }
    }

    /** Save autoplay setting. */
    fun saveAutoplay(newAutoplay: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveAutoplay(newAutoplay)
        }
    }

    /** Save show descriptions setting. */
    fun saveShowDescriptions(newShowDescriptions: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveShowDescriptions(newShowDescriptions)
        }
    }

    /** Save language setting. */
    fun saveLanguage(newLanguage: String) {
        viewModelScope.launch {
            settingsPreferences.saveLanguage(newLanguage)
        }
    }

    /** Save subtitles enabled setting. */
    fun saveSubtitlesEnabled(newSubtitlesEnabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveSubtitlesEnabled(newSubtitlesEnabled)
        }
    }

    /** Save layout preference setting. Values: "leftnav", "grid". */
    fun saveLayoutPreference(newLayout: String) {
        viewModelScope.launch {
            settingsPreferences.saveLayoutPreference(newLayout)
        }
    }

    /** Save feature search setting. */
    fun saveFeatureSearch(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveFeatureSearch(enabled)
        }
    }

    /** Save feature deep linking setting. */
    fun saveFeatureDeepLinking(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveFeatureDeepLinking(enabled)
        }
    }

    /** Save feature analytics setting. */
    fun saveFeatureAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveFeatureAnalytics(enabled)
        }
    }

    /** Save feature crash reporting setting. */
    fun saveFeatureCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveFeatureCrashReporting(enabled)
        }
    }

    /** Save keep-screen-awake setting (default ON). */
    fun saveKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveKeepScreenAwake(enabled)
        }
    }
}
