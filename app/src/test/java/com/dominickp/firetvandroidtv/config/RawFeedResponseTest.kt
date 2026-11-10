package com.dominickp.firetvandroidtv.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RawFeedResponseTest {
    @Test
    fun `parse minimal RawFeedResponse`() {
        val jsonStr = """{"providerName": "Test", "lastUpdated": "2026-01-01", "language": "en"}"""
        val result = RawFeedResponse.parse(jsonStr)
        assertTrue(result.isSuccess)
        val (metadata, groups) = result.getOrNull() ?: return
        assertEquals("Test", metadata.providerName)
        assertEquals("2026-01-01", metadata.lastUpdated)
        assertEquals("en", metadata.language)
    }

    @Test
    fun `parse RawFeedResponse with dynamic groups`() {
        val jsonStr =
            """
            {
                "providerName": "Test",
                "lastUpdated": "2026-01-01",
                "language": "en",
                "2026 07": [
                    {"id": "1", "title": "Video 1"},
                    {"id": "2", "title": "Video 2"}
                ]
            }
            """.trimIndent()

        val result = RawFeedResponse.parse(jsonStr)
        assertTrue(result.isSuccess)
        val (metadata, groups) = result.getOrNull() ?: return
        assertNotNull(groups["2026 07"])
        assertEquals(2, groups["2026 07"]!!.size)
    }

    @Test
    fun `parse RawFeedResponse with empty groups`() {
        val jsonStr =
            """
            {
                "providerName": "Test",
                "lastUpdated": "2026-01-01",
                "language": "en"
            }
            """.trimIndent()

        val result = RawFeedResponse.parse(jsonStr)
        assertTrue(result.isSuccess)
        val (_, groups) = result.getOrNull() ?: return
        assertNotNull(groups)
    }
}
