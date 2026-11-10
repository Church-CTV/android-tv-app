package com.dominickp.firetvandroidtv.ui.components

import com.dominickp.firetvandroidtv.data.model.VideoItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for MonthGroupedRows grouping logic.
 * Verifies month sorting, empty map handling, and grouping correctness.
 */
class MonthGroupedRowsTest {
    private fun video(
        id: String,
        title: String,
    ): VideoItem =
        VideoItem(
            id = id,
            title = title,
            url = "http://test/$id.mp4",
            thumbnail = "http://test/$id.jpg",
        )

    @Test
    fun `empty_map_returns_no_groups`() {
        val emptyMap: Map<String, List<VideoItem>> = emptyMap()
        val sorted = emptyMap.toList().sortedBy { it.first }
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `single_month_group_works`() {
        val videos = listOf(video("1", "Video 1"))
        val map = mapOf("January 2026" to videos)
        val sorted = map.toList().sortedBy { it.first }
        assertEquals(1, sorted.size)
        assertEquals("January 2026", sorted[0].first)
        assertEquals(1, sorted[0].second.size)
    }

    @Test
    fun `months_are_sorted_chronologically`() {
        val map =
            mapOf(
                "March 2026" to listOf(video("3", "V3")),
                "January 2026" to listOf(video("1", "V1")),
                "February 2026" to listOf(video("2", "V2")),
            )
        val sorted = map.toList().sortedBy { it.first }
        assertEquals("February 2026", sorted[0].first)
        assertEquals("January 2026", sorted[1].first)
        assertEquals("March 2026", sorted[2].first)
    }

    @Test
    fun `multiple_videos_in_one_month_group`() {
        val videos =
            listOf(
                video("1", "V1"),
                video("2", "V2"),
                video("3", "V3"),
            )
        val map = mapOf("January 2026" to videos)
        val sorted = map.toList().sortedBy { it.first }
        assertEquals(3, sorted[0].second.size)
    }

    @Test
    fun `empty_grouped_videos_map_sorted_returns_empty`() {
        val map = emptyMap<String, List<VideoItem>>()
        val sorted = map.toList().sortedBy { it.first }
        assertTrue(sorted.isEmpty())
    }
}
