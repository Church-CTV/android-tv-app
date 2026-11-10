package com.dominickp.firetvandroidtv.config

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * PageType — sealed interface for navigation page types.
 * Provides exhaustiveness checking for icon mapping (#3.9).
 * Mirrors Roku's pageRegistry concept.
 */
sealed interface PageType {
    data object Home : PageType

    data object Search : PageType

    data object Settings : PageType

    companion object {
        /**
         * Resolve a string page identifier to its corresponding PageType.
         * Returns null for unknown pages (handled gracefully with fallback icon).
         */
        fun fromString(page: String): PageType? {
            return when (page) {
                "home" -> Home
                "search" -> Search
                "settings" -> Settings
                else -> null
            }
        }

        /**
         * Get the Material icon for this page type.
         * Exhaustive when — no fallthrough possible.
         */
        fun toIcon(pageType: PageType): ImageVector {
            return when (pageType) {
                Home -> Icons.Default.Home
                Search -> Icons.Default.Search
                Settings -> Icons.Default.Settings
            }
        }

        /**
         * Get the Material icon for a page string.
         * Returns default home icon for unknown pages with a warning.
         */
        fun fromStringToIcon(page: String): ImageVector {
            return PageType.fromString(page)?.let { toIcon(it) } ?: Icons.Default.Home
        }
    }
}
