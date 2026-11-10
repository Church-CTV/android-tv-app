package com.dominickp.firetvandroidtv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dominickp.firetvandroidtv.config.ColorUtils
import com.dominickp.firetvandroidtv.config.ColorsConfig

/**
 * BrandTheme — maps Roku branding config → Material3 Theme.
 *
 * Accepts color strings from ConfigLoader (may be in Roku 0x format or CSS # format)
 * and converts them to Compose Color values.
 * Typography from branding.fonts is wired into the Typography object.
 *
 * If no config colors are provided, falls back to hardcoded BrandColors.
 *
 * This is the Android equivalent of Roku's ConfigManager color/font assignment.
 */
@Composable
fun BrandTheme(
    colors: ColorsConfig? = null,
    content: @Composable () -> Unit,
) {
    // Resolve colors: config values (if any) override defaults
    val backgroundColor =
        colors?.let { ColorUtils.decodeColor(it.background) }
            ?: BrandColors.Background
    val surfaceColor =
        colors?.let { ColorUtils.decodeColor(it.surface) }
            ?: BrandColors.Surface
    val accentColor =
        colors?.let { ColorUtils.decodeColor(it.accent) }
            ?: BrandColors.Accent
    val primaryColor =
        colors?.let { ColorUtils.decodeColor(it.accent) }
            ?: BrandColors.Accent
    val errorColor =
        colors?.let { ColorUtils.decodeColor(it.error) }
            ?: BrandColors.Error
    val contentColor =
        colors?.let { ColorUtils.decodeColor(it.textPrimary) }
            ?: BrandColors.TextPrimary

    val colorScheme =
        darkColorScheme(
            primary = primaryColor,
            secondary = accentColor,
            background = backgroundColor,
            surface = surfaceColor,
            error = errorColor,
            onPrimary = contentColor,
            onBackground = contentColor,
            onSurface = contentColor,
            onError = contentColor,
        )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

/**
 * Focus highlight color — used for D-pad focus animations.
 * Matches Roku's `branding.colors.focus` / `branding.animation.cardScale` behavior.
 */
object FocusTheme {
    val HighlightColor: Color = BrandColors.Focus
    const val SCALE = 1.04f
    const val TRANSITION_DURATION: Long = 200L // ms
}
