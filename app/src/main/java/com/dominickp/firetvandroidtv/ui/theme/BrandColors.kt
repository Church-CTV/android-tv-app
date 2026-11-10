package com.dominickp.firetvandroidtv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand colors — 1:1 mapping from Roku `default.json` branding.colors.
 * All color values are defined here as the single source of truth.
 * BrandTheme converts config hex strings to these Compose Color values at runtime.
 *
 * From default.json:
 *   background: 0x1A1A2E  → Color(0xFF1A1A2E)
 *   surface:     0x16213E  → Color(0xFF16213E)
 *   accent:      0xE8B931  → Color(0xFFE8B931)
 *   textPrimary: 0xFFFFFF  → Color(0xFFFFFFFF)
 *   textSecondary: 0xA0A0B0 → Color(0xFFA0A0B0)
 *   focus:       0xE8B931  → Color(0xFFE8B931)
 *   error:       0xFF4444  → Color(0xFFFF4444)
 */
object BrandColors {
    private const val HEX_BG = "FF1A1A2E"
    private const val HEX_SURFACE = "FF16213E"
    private const val HEX_ACCENT = "FFE8B931"
    private const val HEX_TEXT_PRIMARY = "FFFFFFFF"
    private const val HEX_TEXT_SECONDARY = "FFA0A0B0"
    private const val HEX_FOCUS = "FFE8B931"
    private const val HEX_ERROR = "FFFF4444"
    private const val HEX_LIVE = "FFFF0000"
    private const val HEX_SUCCESS = "FF4CAF50"
    private const val HEX_NAV_FOCUS_BG = "FFE8B931"

    val Background = Color(HEX_BG.toLong(16))
    val Surface = Color(HEX_SURFACE.toLong(16))
    val Accent = Color(HEX_ACCENT.toLong(16))
    val TextPrimary = Color(HEX_TEXT_PRIMARY.toLong(16))
    val TextSecondary = Color(HEX_TEXT_SECONDARY.toLong(16))
    val Focus = Color(HEX_FOCUS.toLong(16))
    val Error = Color(HEX_ERROR.toLong(16))
    val Live = Color(HEX_LIVE.toLong(16))
    val Success = Color(HEX_SUCCESS.toLong(16))
    val NavFocusBg = Color(HEX_NAV_FOCUS_BG.toLong(16))
}
