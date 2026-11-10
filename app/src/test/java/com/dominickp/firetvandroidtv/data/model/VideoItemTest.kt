package com.dominickp.firetvandroidtv.data.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VideoItemTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parse video item with all fields`() {
        val jsonStr =
            """
            {
                "id": "123",
                "title": "Test Video",
                "shortDescription": "A test video",
                "thumbnail": "https://example.com/thumb.jpg",
                "url": "https://example.com/video.mp4",
                "releaseDate": "2026-01-01"
            }
            """.trimIndent()

        val item = json.decodeFromString(VideoItem.serializer(), jsonStr)
        assertEquals("123", item.id)
        assertEquals("Test Video", item.title)
        assertEquals("A test video", item.shortDescription)
        assertEquals("https://example.com/video.mp4", item.url)
    }

    @Test
    fun `parse video item with minimal fields`() {
        val jsonStr = """{"id": "123", "title": "Test"}"""
        val item = json.decodeFromString(VideoItem.serializer(), jsonStr)
        assertEquals("123", item.id)
        assertEquals("Test", item.title)
        assertEquals("", item.url)
    }

    @Test
    fun `serialize video item to JSON`() {
        val item =
            VideoItem(
                id = "123",
                title = "Test Video",
                url = "https://example.com/video.mp4",
            )

        val jsonStr = json.encodeToString(VideoItem.serializer(), item)
        assertTrue(jsonStr.contains("\"id\":\"123\""))
        assertTrue(jsonStr.contains("\"title\":\"Test Video\""))
    }

    @Test
    fun `parse video item with nested content`() {
        val jsonStr =
            """
            {
                "id": "123",
                "title": "Test",
                "content": {
                    "duration": 300
                }
            }
            """.trimIndent()

        val item = json.decodeFromString(VideoItem.serializer(), jsonStr)
        assertEquals("123", item.id)
        assertNotNull(item.content)
        assertEquals(300, item.content!!.duration)
    }
}
