package com.dominickp.firetvandroidtv.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PageTypeTest — tests the PageType sealed interface and its companion helpers.
 *
 * Verifies:
 * - fromString maps known page identifiers to the correct PageType
 * - fromString returns null for unknown pages (graceful, not crash)
 * - toIcon is exhaustive over all PageType variants
 * - fromStringToIcon falls back to Home for unknown pages
 *
 * PageType is pure logic (no Android state beyond ImageVector constants)
 * and is the single source of truth for sidebar navigation icon mapping.
 */
class PageTypeTest {

    // === fromString: known mappings ===

    @Test
    fun `fromString maps home to Home`() {
        val result = PageType.fromString("home")
        assertTrue("home should resolve to Home", result is PageType.Home)
        assertEquals(PageType.Home, result)
    }

    @Test
    fun `fromString maps search to Search`() {
        val result = PageType.fromString("search")
        assertTrue("search should resolve to Search", result is PageType.Search)
        assertEquals(PageType.Search, result)
    }

    @Test
    fun `fromString maps settings to Settings`() {
        val result = PageType.fromString("settings")
        assertTrue("settings should resolve to Settings", result is PageType.Settings)
        assertEquals(PageType.Settings, result)
    }

    // === fromString: unknown / edge cases ===

    @Test
    fun `fromString returns null for unknown page`() {
        assertNull("unknown pages should return null", PageType.fromString("unknown_page"))
    }

    @Test
    fun `fromString returns null for empty string`() {
        assertNull("empty string should return null", PageType.fromString(""))
    }

    @Test
    fun `fromString is case sensitive`() {
        // Mirrors config values which are lowercase; "HOME" must not resolve.
        assertNull("uppercase should not resolve", PageType.fromString("HOME"))
    }

    // === toIcon: exhaustive mapping ===

    @Test
    fun `toIcon returns non-null for Home`() {
        assertNotNull("Home must have an icon", PageType.toIcon(PageType.Home))
    }

    @Test
    fun `toIcon returns non-null for Search`() {
        assertNotNull("Search must have an icon", PageType.toIcon(PageType.Search))
    }

    @Test
    fun `toIcon returns non-null for Settings`() {
        assertNotNull("Settings must have an icon", PageType.toIcon(PageType.Settings))
    }

    // === fromStringToIcon: null-safe icon lookup ===

    @Test
    fun `fromStringToIcon returns Home icon for unknown page`() {
        // Unknown pages fall back to Home icon, never throw.
        val fallback = PageType.fromStringToIcon("unknown_page")
        assertEquals(PageType.toIcon(PageType.Home), fallback)
    }

    @Test
    fun `fromStringToIcon returns Home icon for home page`() {
        assertEquals(PageType.toIcon(PageType.Home), PageType.fromStringToIcon("home"))
    }

    @Test
    fun `fromStringToIcon returns Search icon for search page`() {
        assertEquals(PageType.toIcon(PageType.Search), PageType.fromStringToIcon("search"))
    }
}
