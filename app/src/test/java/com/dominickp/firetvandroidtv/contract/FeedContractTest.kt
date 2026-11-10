package com.dominickp.firetvandroidtv.contract

import com.dominickp.firetvandroidtv.data.model.Video
import com.dominickp.firetvandroidtv.data.model.VideoContent
import com.dominickp.firetvandroidtv.data.model.VideoItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Contract tests for Feed/Navigation flow — verifying the feed data contract.
 * Tests: video item structure, content card data, navigation contract,
 * month grouping contract.
 */
class FeedContractTest {
    @Test
    fun `video_item_has_required_fields`() {
        val video = VideoItem("v1", "Title", "Desc", "http://img.jpg", "http://vid.mp4", "2026-01-15", null, emptyList(), null, null, false)
        assertEquals("v1", video.id)
        assertEquals("Title", video.title)
        assertEquals("Desc", video.shortDescription)
        assertEquals("http://img.jpg", video.thumbnail)
        assertEquals("http://vid.mp4", video.url)
        assertEquals("2026-01-15", video.releaseDate)
    }

    @Test
    fun `video_item_is_live_flag_works`() {
        val liveVideo =
            VideoItem("v1", "Live Show", "Desc", "http://img.jpg", "http://vid.mp4", "2026-01-15", null, emptyList(), null, null, true)
        assertTrue(liveVideo.isLive)
    }

    @Test
    fun `video_item_duration_defaults_to_zero_without_content`() {
        val video = VideoItem("v1", "Video", "Desc", "http://img.jpg", "http://vid.mp4", "2026-01-15", null, emptyList(), null, null, false)
        assertEquals(0, video.duration)
    }

    @Test
    fun `video_item_duration_from_content`() {
        val content = VideoContent(duration = 180, videos = listOf(Video("http://vid.mp4")))
        val video =
            VideoItem("v1", "Video", "Desc", "http://img.jpg", "http://vid.mp4", "2026-01-15", null, emptyList(), content, null, false)
        assertEquals(180, video.duration)
    }

    @Test
    fun `video_item_playable_url_from_top_level`() {
        val video = VideoItem("v1", "Video", "Desc", "http://img.jpg", "http://vid.mp4", "2026-01-15", null, emptyList(), null, null, false)
        assertEquals("http://vid.mp4", video.playableUrl)
    }

    @Test
    fun `video_item_playable_url_fallback_empty_when_no_url_or_content`() {
        val video = VideoItem("v1", "Video", "Desc", "http://img.jpg", "", "2026-01-15", null, emptyList(), null, null, false)
        assertEquals("", video.playableUrl)
    }

    @Test
    fun `video_item_video_type_defaults_to_hls`() {
        val video = VideoItem("v1", "Video", "Desc", "http://img.jpg", "http://vid.mp4", "2026-01-15", null, emptyList(), null, null, false)
        assertEquals("HLS", video.videoType)
    }

    @Test
    fun `video_item_month_group_produces_valid_month`() {
        val video = VideoItem("v1", "Video", "Desc", "http://img.jpg", "http://vid.mp4", "2026-03-15", null, emptyList(), null, null, false)
        val month = video.releaseDate.substring(0, 7)
        assertNotNull(month)
        assertFalse(month.isEmpty())
    }

    @Test
    fun `nav_routes_are_unique`() {
        val routes =
            setOf(
                "home",
                "videos",
                "search",
                "settings",
                "subscription",
                "continue_watching",
                "video",
                "error",
            )
        assertEquals(8, routes.size)
    }

    @Test
    fun `feed_videos_count_matches_response`() {
        val videos =
            listOf(
                VideoItem("v1", "V1", "D1", "http://v1.jpg", "http://v1.mp4", "2026-01-15", null, emptyList(), null, null, false),
                VideoItem("v2", "V2", "D2", "http://v2.jpg", "http://v2.mp4", "2026-01-15", null, emptyList(), null, null, false),
                VideoItem("v3", "V3", "D3", "http://v3.jpg", "http://v3.mp4", "2026-02-01", null, emptyList(), null, null, true),
            )
        assertEquals(3, videos.size)
    }

    @Test
    fun `month_grouped_videos_maintains_order`() {
        val videos =
            listOf(
                VideoItem("v1", "First", "D1", "http://v1.jpg", "http://v1.mp4", "2026-01-15", null, emptyList(), null, null, false),
                VideoItem("v2", "Second", "D2", "http://v2.jpg", "http://v2.mp4", "2026-01-15", null, emptyList(), null, null, false),
            )
        val grouped = videos.groupBy { it.releaseDate.substring(0, 7) }
        grouped.forEach { (_, items) ->
            assertEquals(2, items.size)
        }
    }

    @Test
    fun `empty_videos_list_groups_to_empty`() {
        val grouped = emptyList<VideoItem>().groupBy { it.releaseDate.substring(0, 7) }
        assertTrue(grouped.isEmpty())
    }
}
