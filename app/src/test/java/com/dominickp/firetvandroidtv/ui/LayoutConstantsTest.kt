package com.dominickp.firetvandroidtv.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * LayoutConstantsTest — TDD tests for density-corrected layout constants.
 *
 * Device: 1920x1080 Fire TV Stick at density 320 (xxhdpi).
 * Roku:   1920x1080 at density 160 (1dp = 1px).
 * Conversion: dp = rokuPx / (deviceDensity / 160) = rokuPx / 2
 *
 * These tests verify that all layout dimensions are correctly scaled
 * for the target device density. Without this correction, UI elements
 * render at 2x the intended physical size.
 */
@DisplayName("Layout Constants — Density Correction")
class LayoutConstantsTest {

    @Test
    @DisplayName("NAV_BAR_WIDTH: Roku 80px → 40dp at density 320")
    fun nav_bar_width_correct() {
        assertEquals(40f, LayoutConstants.NAV_BAR_WIDTH.value,
            "NAV_BAR_WIDTH should be 40dp (Roku 80px / 2 for density 320)")
    }

    @Test
    @DisplayName("NAV_ITEM_HEIGHT: Roku 60px → 30dp at density 320")
    fun nav_item_height_correct() {
        assertEquals(30f, LayoutConstants.NAV_ITEM_HEIGHT.value,
            "NAV_ITEM_HEIGHT should be 30dp (Roku 60px / 2 for density 320)")
    }

    @Test
    @DisplayName("NAV_ICON_SIZE: Roku 40px → 20dp at density 320")
    fun nav_icon_size_correct() {
        assertEquals(20f, LayoutConstants.NAV_ICON_SIZE.value,
            "NAV_ICON_SIZE should be 20dp (Roku 40px / 2 for density 320)")
    }

    @Test
    @DisplayName("CARD_WIDTH: Roku 480px → 240dp at density 320")
    fun card_width_correct() {
        assertEquals(240f, LayoutConstants.CARD_WIDTH.value,
            "CARD_WIDTH should be 240dp (Roku 480px / 2 for density 320)")
    }

    @Test
    @DisplayName("CARD_HEIGHT: Roku 270px → 135dp at density 320")
    fun card_height_correct() {
        assertEquals(135f, LayoutConstants.CARD_HEIGHT.value,
            "CARD_HEIGHT should be 135dp (Roku 270px / 2 for density 320)")
    }

    @Test
    @DisplayName("MONTH_CONTENT_START: Roku 150px → 75dp at density 320")
    fun month_content_start_correct() {
        assertEquals(75f, LayoutConstants.MONTH_CONTENT_START.value,
            "MONTH_CONTENT_START should be 75dp (Roku 150px / 2 for density 320)")
    }

    @Test
    @DisplayName("MONTH_CONTENT_TOP: Roku 300px → 150dp at density 320")
    fun month_content_top_correct() {
        assertEquals(150f, LayoutConstants.MONTH_CONTENT_TOP.value,
            "MONTH_CONTENT_TOP should be 150dp (Roku 300px / 2 for density 320)")
    }

    @Test
    @DisplayName("OVERHANG_HEIGHT: Roku 100px → 50dp at density 320")
    fun overhang_height_correct() {
        assertEquals(50f, LayoutConstants.OVERHANG_HEIGHT.value,
            "OVERHANG_HEIGHT should be 50dp (Roku 100px / 2 for density 320)")
    }

    @Test
    @DisplayName("CARD_CORNER_RADIUS: 8dp (density-independent, matches Roku MaskGroup)")
    fun card_corner_radius_correct() {
        assertEquals(8f, LayoutConstants.CARD_CORNER_RADIUS.value,
            "CARD_CORNER_RADIUS should be 8dp (matches Roku 8px MaskGroup radius)")
    }
}
