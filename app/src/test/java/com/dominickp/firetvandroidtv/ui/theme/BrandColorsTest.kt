package com.dominickp.firetvandroidtv.ui.theme

import com.dominickp.firetvandroidtv.config.ColorUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for BrandColors config-to-Compose mapping.
 * Verifies every hex color maps correctly to Compose Color values.
 */
class BrandColorsTest {
    @Test
    fun `BrandColors object can be accessed`() {
        val bg = BrandColors.Background
        assertNotNull(bg)
    }

    @Test
    fun `BrandColors Background equals #1A1A2E`() {
        val bg = BrandColors.Background
        assertEquals(0.10f, bg.red, 0.01f)
        assertEquals(0.10f, bg.green, 0.01f)
        assertEquals(0.18f, bg.blue, 0.01f)
        assertEquals(1.0f, bg.alpha)
    }

    @Test
    fun `BrandColors Surface equals #16213E`() {
        val s = BrandColors.Surface
        assertEquals(0.09f, s.red, 0.01f)
        assertEquals(0.13f, s.green, 0.01f)
        assertEquals(0.24f, s.blue, 0.01f)
    }

    @Test
    fun `BrandColors Accent equals #E8B931`() {
        val a = BrandColors.Accent
        assertEquals(0.91f, a.red, 0.01f)
        assertEquals(0.72f, a.green, 0.01f)
        assertEquals(0.19f, a.blue, 0.01f)
    }

    @Test
    fun `BrandColors TextPrimary equals #FFFFFF`() {
        val tp = BrandColors.TextPrimary
        assertEquals(1.0f, tp.red)
        assertEquals(1.0f, tp.green)
        assertEquals(1.0f, tp.blue)
    }

    @Test
    fun `BrandColors TextSecondary equals #A0A0B0`() {
        val ts = BrandColors.TextSecondary
        assertEquals(0xA0, (ts.red * 255).toInt())
        assertEquals(0xA0, (ts.green * 255).toInt())
        assertEquals(0xB0, (ts.blue * 255).toInt())
    }

    @Test
    fun `BrandColors Focus equals #E8B931 (same as Accent)`() {
        val focus = BrandColors.Focus
        val accent = BrandColors.Accent
        assertEquals(accent.red, focus.red)
        assertEquals(accent.green, focus.green)
        assertEquals(accent.blue, focus.blue)
    }

    @Test
    fun `BrandColors Error equals #FF4444`() {
        val e = BrandColors.Error
        assertEquals(1.0f, e.red)
        assertEquals(0.27f, e.green, 0.01f)
        assertEquals(0.27f, e.blue, 0.01f)
    }

    @Test
    fun `BrandColors Live equals #FF0000`() {
        val l = BrandColors.Live
        assertEquals(1.0f, l.red)
        assertEquals(0.0f, l.green)
        assertEquals(0.0f, l.blue)
    }

    @Test
    fun `BrandColors Success equals #4CAF50`() {
        val s = BrandColors.Success
        assertEquals(0.30f, s.red, 0.01f)
        assertEquals(0.69f, s.green, 0.01f)
        assertEquals(0.31f, s.blue, 0.01f)
    }

    @Test
    fun `BrandColors NavFocusBg equals #E8B931`() {
        val nf = BrandColors.NavFocusBg
        val accent = BrandColors.Accent
        assertEquals(accent.red, nf.red)
        assertEquals(accent.green, nf.green)
        assertEquals(accent.blue, nf.blue)
    }

    @Test
    fun `BrandColors has all defined colors`() {
        assertNotNull(BrandColors.Background)
        assertNotNull(BrandColors.Surface)
        assertNotNull(BrandColors.Accent)
        assertNotNull(BrandColors.TextPrimary)
        assertNotNull(BrandColors.TextSecondary)
        assertNotNull(BrandColors.Focus)
        assertNotNull(BrandColors.Error)
        assertNotNull(BrandColors.Live)
        assertNotNull(BrandColors.Success)
        assertNotNull(BrandColors.NavFocusBg)
    }

    @Test
    fun `ColorUtils_decodeConfigBackground_matches_BrandColors`() {
        val decoded = ColorUtils.decodeColor("#1A1A2E")
        assertEquals(BrandColors.Background.red, decoded.red)
        assertEquals(BrandColors.Background.green, decoded.green)
        assertEquals(BrandColors.Background.blue, decoded.blue)
    }

    @Test
    fun `ColorUtils_decodeConfigAccent_matches_BrandColors`() {
        val decoded = ColorUtils.decodeColor("#E8B931")
        assertEquals(BrandColors.Accent.red, decoded.red)
        assertEquals(BrandColors.Accent.green, decoded.green)
        assertEquals(BrandColors.Accent.blue, decoded.blue)
    }

    @Test
    fun `ColorUtils_decodeConfigError_matches_BrandColors`() {
        val decoded = ColorUtils.decodeColor("#FF4444")
        assertEquals(BrandColors.Error.red, decoded.red)
        assertEquals(BrandColors.Error.green, decoded.green)
        assertEquals(BrandColors.Error.blue, decoded.blue)
    }

    @Test
    fun `ColorUtils_decodeConfigSurface_matches_BrandColors`() {
        val decoded = ColorUtils.decodeColor("#16213E")
        assertEquals(BrandColors.Surface.red, decoded.red)
        assertEquals(BrandColors.Surface.green, decoded.green)
        assertEquals(BrandColors.Surface.blue, decoded.blue)
    }
}
