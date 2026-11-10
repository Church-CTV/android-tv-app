package com.dominickp.firetvandroidtv.ui.components

import org.junit.Test

/**
 * Unit tests for parseMonthKey — ensures month labels parse correctly
 * without depending on java.time (API 26+), keeping compatibility with
 * minSdk 25 (Android 7.1).
 */
class ParseMonthKeyTest {
    @Test
    fun `valid month label returns yyyy-MM key`() {
        val result = parseMonthKey("July 2026")
        assert(result == "2026-07") { "Expected 2026-07, got $result" }
    }

    @Test
    fun `valid month labels produce comparable keys for sorting`() {
        val a = parseMonthKey("July 2026")
        val b = parseMonthKey("August 2025")
        val c = parseMonthKey("December 2024")
        assert(a != null && b != null && c != null) { "All valid labels must parse" }
        // Descending: 2026-07 > 2025-08 > 2024-12
        assert(a!! > b!!) { "July 2026 should sort after August 2025" }
        assert(b!! > c!!) { "August 2025 should sort after December 2024" }
    }

    @Test
    fun `single digit months zero-padded`() {
        assert(parseMonthKey("January 2024") == "2024-01")
        assert(parseMonthKey("October 2023") == "2023-10")
    }

    @Test
    fun `null for invalid labels`() {
        assert(parseMonthKey("Recent") == null)
        assert(parseMonthKey("LIVE NOW") == null)
        assert(parseMonthKey("") == null)
        assert(parseMonthKey("not a date") == null)
    }

    @Test
    fun `whitespace trimmed`() {
        assert(parseMonthKey("  July 2026  ") == "2026-07")
    }
}
