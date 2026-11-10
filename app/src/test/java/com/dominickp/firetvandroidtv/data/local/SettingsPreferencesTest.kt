package com.dominickp.firetvandroidtv.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SettingsPreferencesTest — unit tests for SettingsPreferences validation logic.
 *
 * Tests the validation constants that drive quality/language validation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsPreferencesTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `validQualities_contains_auto`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validQualities.contains("auto"))
    }

    @Test
    fun `validQualities_contains_720p`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validQualities.contains("720p"))
    }

    @Test
    fun `validQualities_contains_1080p`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validQualities.contains("1080p"))
    }

    @Test
    fun `validQualities_contains_4K`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validQualities.contains("4K"))
    }

    @Test
    fun `validQualities_does_not_contain_invalid`() {
        val prefs = SettingsPreferences(context)
        assertTrue(!prefs.validQualities.contains("4Kp"))
        assertTrue(!prefs.validQualities.contains("UltraHD"))
    }

    @Test
    fun `validLanguages_contains_en`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validLanguages.contains("en"))
    }

    @Test
    fun `validLanguages_contains_es`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validLanguages.contains("es"))
    }

    @Test
    fun `validLanguages_contains_fr`() {
        val prefs = SettingsPreferences(context)
        assertTrue(prefs.validLanguages.contains("fr"))
    }

    @Test
    fun `validLanguages_does_not_contain_invalid`() {
        val prefs = SettingsPreferences(context)
        assertTrue(!prefs.validLanguages.contains("de"))
        assertTrue(!prefs.validLanguages.contains("ja"))
    }

    @Test
    fun `validQualities_count_is_four`() {
        val prefs = SettingsPreferences(context)
        assertEquals(4, prefs.validQualities.size)
    }

    @Test
    fun `validLanguages_count_is_three`() {
        val prefs = SettingsPreferences(context)
        assertEquals(3, prefs.validLanguages.size)
    }
}
