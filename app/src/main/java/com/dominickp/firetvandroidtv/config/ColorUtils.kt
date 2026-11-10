package com.dominickp.firetvandroidtv.config

import androidx.compose.ui.graphics.Color

/**
 * ColorUtils — converts color strings between Roku hex (0xRRGGBB) and Compose Color.
 *
 * The Roku server sends colors in 0xRRGGBB format (e.g., "0x1A1A2E").
 * Compose Color() expects an integer or 0xAARRGGBB format.
 * This utility handles the conversion transparently.
 */
object ColorUtils {
    private const val DEFAULT_OPAQUE_ALPHA = 0xFF000000L

    /**
     * Decode a color string to Compose Color.
     * Accepts:
     *   - "#RRGGBB" or "#AARRGGBB" (CSS hex)
     *   - "0xRRGGBB" (Roku hex) — converts to 0xFFRRGGBB (opaque)
     *   - "0xAARRGGBB" (Roku hex with alpha)
     *   - "RRGGBB" (hex without prefix)
     */
    fun decodeColor(colorStr: String): Color {
        val cleaned = colorStr.trim().removePrefix("#").removePrefix("0x")

        return when {
            cleaned.length == 6 -> {
                // #RRGGBB or RRGGBB → 0xFFRRGGBB (fully opaque)
                Color(DEFAULT_OPAQUE_ALPHA or cleaned.toInt(16).toLong())
            }
            cleaned.length == 8 -> {
                // #AARRGGBB or 0xAARRGGBB
                Color(cleaned.toLong(16).toInt())
            }
            else -> {
                // Invalid color — return transparent for unrecognized formats
                Color.Transparent
            }
        }
    }

    /**
     * Convert a list of color strings from server config to Compose Colors.
     * Used when applying server-provided branding to the UI.
     */
    fun decodeColorsMap(colors: Map<String, String>): Map<String, Color> {
        return colors.mapValues { (_, v) -> decodeColor(v) }
    }
}
