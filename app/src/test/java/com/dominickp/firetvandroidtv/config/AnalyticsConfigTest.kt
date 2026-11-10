package com.dominickp.firetvandroidtv.config

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * AnalyticsConfigTest — verifies AnalyticsConfig deserialization
 * without the dead Ga4Config/ga4 field.
 *
 * Ga4Config was dead code: defined in Config.kt but never used
 * anywhere in main or test code. This test ensures AnalyticsConfig
 * works correctly without it.
 */
class AnalyticsConfigTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun `AnalyticsConfigDeserializesWithoutGa4Field()`() {
        // JSON without ga4 field — should deserialize fine
        val jsonStr = """{
            "enabled": true,
            "exportUrl": "http://example.com/analytics",
            "exportIntervalSeconds": 300,
            "maxEventsPerBatch": 50,
            "flushOnError": true
        }"""

        val config = json.decodeFromString<AnalyticsConfig>(jsonStr)

        assertTrue(config.enabled)
        assertEquals("http://example.com/analytics", config.exportUrl)
        assertEquals(300, config.exportIntervalSeconds)
        assertEquals(50, config.maxEventsPerBatch)
        assertTrue(config.flushOnError)
    }

    @Test
    fun `AnalyticsConfigDefaultsToDisabled()`() {
        val config = AnalyticsConfig()
        assertFalse(config.enabled)
        assertEquals("", config.exportUrl)
        assertEquals(300, config.exportIntervalSeconds)
        assertEquals(50, config.maxEventsPerBatch)
        assertTrue(config.flushOnError)
    }

    @Test
    fun `AnalyticsConfigDeserializesWithMinimalFields()`() {
        // Just enabled — all other fields should use defaults
        val jsonStr = """{"enabled": false}"""

        val config = json.decodeFromString<AnalyticsConfig>(jsonStr)

        assertFalse(config.enabled)
        assertEquals("", config.exportUrl)
        assertEquals(300, config.exportIntervalSeconds)
        assertEquals(50, config.maxEventsPerBatch)
        assertTrue(config.flushOnError)
    }
}
