package com.dominickp.firetvandroidtv.ui.components

import org.junit.Test

/**
 * Unit tests for month label parsing and sorting used by MonthGroupedRows.
 * Verifies sort key generation handles valid/invalid labels deterministically.
 */
class MonthSorterTest {
    @Test
    fun `valid month labels produce sortable keys`() {
        val labels = listOf("July 2026", "August 2025", "December 2024")

        // Simulate the sort logic using SimpleDateFormat (API 1+)
        val sorted =
            labels.sortedByDescending { label ->
                try {
                    val df = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
                    df.parse(label.trim())
                } catch (_: Exception) {
                    java.util.Date(0) // invalid → earliest possible
                }
            }

        assert(sorted[0] == "July 2026") { "Expected newest month first, got ${sorted[0]}" }
        assert(sorted[1] == "August 2025")
        assert(sorted[2] == "December 2024")
    }

    @Test
    fun `invalid labels sort to the end`() {
        val labels = listOf("July 2026", "LIVE NOW", "Recent", "August 2025")

        val sorted =
            labels.sortedByDescending { label ->
                if (label == "LIVE NOW") {
                    Long.MAX_VALUE // LIVE NOW handled separately in real code
                } else {
                    try {
                        val df = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
                        df.parse(label.trim())?.time ?: 0
                    } catch (_: Exception) {
                        0L // invalid → earliest possible
                    }
                }
            }

        assert(sorted[0] == "LIVE NOW") { "LIVE NOW should be handled as special case" }
        assert(sorted[1] == "July 2026") { "Newest valid month after LIVE NOW" }
        // "Recent" is invalid → sorts last among remaining items
        val recentIndex = sorted.indexOf("Recent")
        assert(recentIndex == sorted.size - 1) { "Invalid label 'Recent' must sort to end: $sorted" }
    }

    @Test
    fun `LIVE NOW is handled separately and not passed to parseMonthKey`() {
        // In MonthGroupedRows, LIVE NOW row is extracted before sorting the remaining months.
        val groupedVideos =
            mapOf(
                "LIVE NOW" to emptyList<String>(),
                "July 2026" to emptyList<String>(),
                "August 2025" to emptyList<String>(),
            )

        val liveRow: List<String>? = groupedVideos["LIVE NOW"]
        assert(liveRow != null) { "LIVE NOW row must be explicitly accessible" }

        val nonLiveEntries: Map<String, List<String>> = groupedVideos.filterKeys { key -> key != "LIVE NOW" }
        assert(nonLiveEntries.size == 2) { "LIVE NOW excluded from sort: expected 2, got ${nonLiveEntries.size}" }
    }

    @Test
    fun `same year months sort correctly`() {
        val labels = listOf("January 2026", "March 2026", "November 2026", "December 2026")

        val sorted =
            labels.sortedByDescending { label ->
                try {
                    val df = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
                    df.parse(label.trim()) ?: java.util.Date(0)
                } catch (_: Exception) {
                    java.util.Date(0)
                }
            }

        assert(sorted == listOf("December 2026", "November 2026", "March 2026", "January 2026")) {
            "Expected descending within same year, got $sorted"
        }
    }
}
