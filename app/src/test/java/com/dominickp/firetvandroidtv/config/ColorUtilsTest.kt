package com.dominickp.firetvandroidtv.config

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ColorUtilsTest {
    @Test
    fun `decodeColor parses hex6 to opaque color`() {
        val color = ColorUtils.decodeColor("#1A1A2E")
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `decodeColor parses hex8 to color with alpha`() {
        val color = ColorUtils.decodeColor("#FF1A1A2E")
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `decodeColor parses 0xRRGGBB prefix`() {
        val color = ColorUtils.decodeColor("0x1A1A2E")
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `decodeColor parses 0xAARRGGBB prefix`() {
        val color = ColorUtils.decodeColor("0xFF1A1A2E")
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `decodeColor parses hex without prefix`() {
        val color = ColorUtils.decodeColor("1A1A2E")
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `decodeColor trims whitespace`() {
        val color = ColorUtils.decodeColor("  #1A1A2E  ")
        assertEquals(Color(0xFF1A1A2E), color)
    }

    @Test
    fun `decodeColor returns transparent for invalid format`() {
        val color = ColorUtils.decodeColor("INVALID")
        assertEquals(Color.Transparent, color)
    }

    @Test
    fun `decodeColor returns transparent for empty string`() {
        val color = ColorUtils.decodeColor("")
        assertEquals(Color.Transparent, color)
    }

    @Test
    fun `decodeColorsMap converts map of color strings`() {
        val colors =
            mapOf(
                "background" to "#1A1A2E",
                "accent" to "#E8B931",
            )
        val result = ColorUtils.decodeColorsMap(colors)
        assertEquals(Color(0xFF1A1A2E), result["background"])
        assertEquals(Color(0xFFE8B931), result["accent"])
    }

    @Test
    fun `decodeColor handles all valid Roku color formats`() {
        // Test common Roku hex formats
        val formats =
            mapOf(
                "0xFFFFFFFF" to Color(0xFFFFFFFF),
                "0xFF000000" to Color(0xFF000000),
                "#FFFFFF" to Color(0xFFFFFFFF),
                "#000000" to Color(0xFF000000),
                "FFFFFFFF" to Color(0xFFFFFFFF),
                "FF000000" to Color(0xFF000000),
            )
        for ((input, expected) in formats) {
            assertEquals(expected, ColorUtils.decodeColor(input), "Failed for input: $input")
        }
    }

    @Test
    fun `decodeColor with single char hex returns transparent`() {
        val color = ColorUtils.decodeColor("A")
        assertEquals(Color.Transparent, color)
    }

    @Test
    fun `decodeColor with partial hex returns transparent`() {
        // Short hex strings that don't match 6 or 8 char pattern
        val color = ColorUtils.decodeColor("12345")
        assertEquals(Color.Transparent, color)
    }
}
