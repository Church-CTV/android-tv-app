package com.dominickp.firetvandroidtv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography — mirrors Roku's branding.fonts config.
 *
 * Roku font mapping:
 *   title     → font:BoldSystemFont      → Title style
 *   body      → font:MediumSystemFont    → Body style
 *   caption   → font:SmallSystemFont     → Caption1 style
 *   heading   → font:ExtraBoldSystemFont → Headline style
 *
 * Card styles use SmallSystemFont / SmallBoldSystemFont.
 */
val AppTypography =
    Typography(
        // Heading — ExtraBoldSystemFont equivalent (Roku: large headings, screen titles)
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        // Title — BoldSystemFont equivalent (Roku: row labels, section titles)
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        // Body — MediumSystemFont equivalent (Roku: body text, descriptions)
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        // Body small — SmallBoldSystemFont (Roku: card titles, settings labels)
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        // Caption — SmallSystemFont equivalent (Roku: metadata, badges)
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        // Card title / row label — SmallBoldSystemFont equivalent
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
    )
