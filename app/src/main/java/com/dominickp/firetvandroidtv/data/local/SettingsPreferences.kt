package com.dominickp.firetvandroidtv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * SettingsPreferences — persists user preferences using DataStore.
 *
 * Provides typed access to settings with default values.
 */
class SettingsPreferences(
    context: Context,
) {
    private val dataStore = context.settingsDataStore

    companion object {
        private const val KEY_QUALITY = "quality"
        private const val KEY_AUTOPLAY = "autoplay"
        private const val KEY_SHOW_DESCRIPTIONS = "show_descriptions"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SUBTITLES = "subtitles"
        private const val KEY_LAYOUT_PREFERENCE = "layout_preference"
        private const val KEY_FEATURE_SEARCH = "feature_search"
        private const val KEY_FEATURE_DEEP_LINKING = "feature_deep_linking"
        private const val KEY_FEATURE_ANALYTICS = "feature_analytics"
        private const val KEY_FEATURE_CRASH_REPORTING = "feature_crash_reporting"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
    }

    /**
     * Video quality setting.
     * Values: "auto", "720p", "1080p", "4K"
     */
    val quality: Flow<String> = dataStore.data.map { it[stringPreferencesKey(KEY_QUALITY)] ?: "auto" }

    /**
     * Autoplay setting.
     */
    val autoplay: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey(KEY_AUTOPLAY)] ?: true }

    /**
     * Show descriptions setting.
     */
    val showDescriptions: Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(KEY_SHOW_DESCRIPTIONS)] ?: true }

    /**
     * Language setting.
     * Values: "en", "es", "fr"
     */
    val language: Flow<String> = dataStore.data.map { it[stringPreferencesKey(KEY_LANGUAGE)] ?: "en" }

    /**
     * Subtitles enabled setting.
     */
    val subtitlesEnabled: Flow<Boolean> = dataStore.data.map { it[booleanPreferencesKey(KEY_SUBTITLES)] ?: false }

    /**
     * Layout preference setting.
     * Values: "leftnav", "grid"
     * Mirrors Roku's features.layoutPreference
     */
    val layoutPreference: Flow<String> =
        dataStore.data.map { it[stringPreferencesKey(KEY_LAYOUT_PREFERENCE)] ?: "leftnav" }

    /**
     * Feature flag: search enabled.
     * Mirrors Roku's features.search
     */
    val featureSearch: Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(KEY_FEATURE_SEARCH)] ?: true }

    /**
     * Feature flag: deep linking enabled.
     * Mirrors Roku's features.deepLinking
     */
    val featureDeepLinking: Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(KEY_FEATURE_DEEP_LINKING)] ?: true }

    /**
     * Feature flag: analytics enabled.
     * Mirrors Roku's features.analytics
     */
    val featureAnalytics: Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(KEY_FEATURE_ANALYTICS)] ?: true }

    /**
     * Feature flag: crash reporting enabled.
     * Mirrors Roku's features.crashReporting
     */
    val featureCrashReporting: Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(KEY_FEATURE_CRASH_REPORTING)] ?: true }

    /**
     * Keep the TV screen awake while the app is open (default ON).
     * MainActivity reads this and applies/removes FLAG_KEEP_SCREEN_ON on the
     * window. Off = let the stick's normal display behavior take over.
     */
    val keepScreenAwake: Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(KEY_KEEP_SCREEN_AWAKE)] ?: true }

    /**
     * Save quality setting with validation (#3.3).
     * Values: "auto", "720p", "1080p", "4K"
     * Invalid values default to "auto".
     */
    suspend fun saveQuality(quality: String) {
        val validQualities = setOf("auto", "720p", "1080p", "4K")
        if (quality !in validQualities) {
            Logger.w("SettingsPreferences", "Invalid quality: $quality, defaulting to auto")
            dataStore.edit { it[stringPreferencesKey(KEY_QUALITY)] = "auto" }
        } else {
            dataStore.edit { it[stringPreferencesKey(KEY_QUALITY)] = quality }
        }
    }

    /**
     * Save autoplay setting.
     */
    suspend fun saveAutoplay(autoplay: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_AUTOPLAY)] = autoplay }
    }

    /**
     * Save show descriptions setting.
     */
    suspend fun saveShowDescriptions(showDescriptions: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_SHOW_DESCRIPTIONS)] = showDescriptions }
    }

    /**
     * Save language setting with validation (#3.3).
     * Values: "en", "es", "fr"
     * Invalid values default to "en".
     */
    suspend fun saveLanguage(language: String) {
        val validLanguages = setOf("en", "es", "fr")
        if (language !in validLanguages) {
            Logger.w("SettingsPreferences", "Invalid language: $language, defaulting to en")
            dataStore.edit { it[stringPreferencesKey(KEY_LANGUAGE)] = "en" }
        } else {
            dataStore.edit { it[stringPreferencesKey(KEY_LANGUAGE)] = language }
        }
    }

    /**
     * Save subtitles enabled setting.
     */
    suspend fun saveSubtitlesEnabled(subtitlesEnabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_SUBTITLES)] = subtitlesEnabled }
    }

    /**
     * Save layout preference setting with validation.
     * Values: "leftnav", "grid"
     * Invalid values default to "leftnav".
     * Mirrors Roku's ConfigManager.SetLayoutPreference()
     */
    suspend fun saveLayoutPreference(layout: String) {
        val validLayouts = setOf("leftnav", "grid")
        if (layout !in validLayouts) {
            Logger.w("SettingsPreferences", "Invalid layout preference: $layout, defaulting to leftnav")
            dataStore.edit { it[stringPreferencesKey(KEY_LAYOUT_PREFERENCE)] = "leftnav" }
        } else {
            dataStore.edit { it[stringPreferencesKey(KEY_LAYOUT_PREFERENCE)] = layout }
        }
    }

    /**
     * Save feature search setting.
     * Mirrors Roku's ConfigManager.featureSearch
     */
    suspend fun saveFeatureSearch(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_FEATURE_SEARCH)] = enabled }
    }

    /**
     * Save feature deep linking setting.
     * Mirrors Roku's ConfigManager.featureDeepLinking
     */
    suspend fun saveFeatureDeepLinking(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_FEATURE_DEEP_LINKING)] = enabled }
    }

    /**
     * Save feature analytics setting.
     * Mirrors Roku's ConfigManager.featureAnalytics
     */
    suspend fun saveFeatureAnalytics(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_FEATURE_ANALYTICS)] = enabled }
    }

    /**
     * Save feature crash reporting setting.
     * Mirrors Roku's ConfigManager.featureCrashReporting
     */
    suspend fun saveFeatureCrashReporting(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_FEATURE_CRASH_REPORTING)] = enabled }
    }

    /** Save keep-screen-awake setting (default ON). */
    suspend fun saveKeepScreenAwake(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_KEEP_SCREEN_AWAKE)] = enabled }
    }

    /**
     * Get supported quality values for validation display.
     */
    val validQualities: Set<String> = setOf("auto", "720p", "1080p", "4K")

    /**
     * Get supported language codes for validation display.
     */
    val validLanguages: Set<String> = setOf("en", "es", "fr")

    /**
     * Get supported layout preference values for validation display.
     */
    val validLayouts: Set<String> = setOf("leftnav", "grid")
}
