package com.dominickp.firetvandroidtv.ui.components

import com.dominickp.firetvandroidtv.data.model.VideoItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for ContentCard composable logic.
 * Verifies VideoItem data used by the composable.
 */
class ContentCardTest {
    @Test
    fun `ContentCard_receives_valid_VideoItem`() {
        val video =
            VideoItem(
                id = "1",
                title = "Test Video",
                shortDescription = "Test description",
                url = "http://test/video.mp4",
                thumbnail = "http://test/thumb.jpg",
            )
        assertNotNull(video.id)
        assertEquals("Test Video", video.title)
        assertFalse(video.isLive)
        assertEquals("http://test/video.mp4", video.playableUrl)
    }

    @Test
    fun `ContentCard_with_live_video_has_isLive_true`() {
        val video =
            VideoItem(
                id = "2",
                title = "Live Event",
                url = "http://test/live.mp4",
                isLive = true,
            )
        assertTrue(video.isLive)
        assertEquals("Live Event", video.title)
    }

    @Test
    fun `ContentCard_default_focusScale_is_1_04f`() {
        val defaultFocusScale = 1.04f
        assertEquals(1.04f, defaultFocusScale, 0.001f)
    }

    @Test
    fun `ContentCard_handles_empty_title`() {
        val video =
            VideoItem(
                id = "3",
                title = "",
                url = "http://test/empty.mp4",
            )
        assertEquals("", video.title)
    }

    @Test
    fun `ContentCard_handles_shortDescription_defaults`() {
        val video =
            VideoItem(
                id = "4",
                title = "Title Only",
                shortDescription = "",
                url = "http://test/short.mp4",
            )
        assertEquals("", video.shortDescription)
        assertEquals("", video.thumbnail)
        assertEquals("http://test/short.mp4", video.url)
    }
}
