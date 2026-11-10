package com.dominickp.firetvandroidtv.ui

import androidx.compose.ui.unit.dp

/**
 * LayoutConstants — single source of truth for all layout dimensions.
 *
 * Density correction: Roku runs at 1920x1080 / density 160 (1dp = 1px).
 * Fire TV Stick at 1920x1080 runs at density 320 (1dp = 2px).
 *
 * Every Roku pixel value MUST be divided by 2 to get the correct dp
 * for our target devices. These constants hold the density-corrected
 * values and are shared by all screens to avoid inconsistent sizing.
 *
 * Formula: dp = rokuPx / (deviceDensity / 160) = rokuPx / 2
 */
object LayoutConstants {

    /** Roku LeftNavScreen width="80" px → 40dp at density 320 */
    val NAV_BAR_WIDTH = 40.dp

    /** Roku nav item container 60x60 px → 30dp */
    val NAV_ITEM_HEIGHT = 30.dp

    /** Roku nav icon 40x40 px → 20dp */
    val NAV_ICON_SIZE = 20.dp

    /** Roku ContentCard 480px wide → 240dp */
    val CARD_WIDTH = 240.dp

    /** Roku ContentCard 270px tall → 135dp */
    val CARD_HEIGHT = 135.dp

    /** Roku RowList translation x=150 px → 75dp (content left inset) */
    val MONTH_CONTENT_START = 75.dp

    /** Roku RowList translation y=300 px → 150dp (content top from scene) */
    val MONTH_CONTENT_TOP = 150.dp

    /** Roku Overhang 1920x100 px → 50dp bar height */
    val OVERHANG_HEIGHT = 50.dp

    /** Roku ContentCard MaskGroup corner radius 8 px → 8dp */
    val CARD_CORNER_RADIUS = 8.dp
}
