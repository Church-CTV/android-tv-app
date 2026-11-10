package com.dominickp.firetvandroidtv.config

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for loadFallbackConfig default values.
 * These serve as regression tests ensuring fallback config is always valid
 * even when app_config.json cannot be loaded (e.g., corrupt, missing).
 */
class ConfigLoaderFallbackTest {
    @Test
    fun `loadFallbackConfig returns valid config with all required fields`() {
        val config = loadFallbackConfig()
        assertNotNull(config)
        assertFalse(config.channel.name.isEmpty())
        assertFalse(config.content.sourceUrl.isEmpty())
        assertFalse(config.navItems.isEmpty())
        assertNotNull(config.apiBaseUrl)
        assertNotNull(config.analytics)
    }

    @Test
    fun `loadFallbackConfig has all navigation items`() {
        val config = loadFallbackConfig()
        val pages = config.navItems.map { it.page }
        assertTrue(pages.contains("home"))
        assertTrue(pages.contains("search"))
        assertTrue(pages.contains("continue"))
        assertTrue(pages.contains("settings"))
    }

    @Test
    fun `loadFallbackConfig has valid retry defaults`() {
        val config = loadFallbackConfig()
        assertEquals(3, config.content.retryAttempts)
        assertEquals(2000, config.content.retryDelayMs)
        assertEquals(30, config.content.fetchTimeout)
    }
}
