package com.dominickp.firetvandroidtv.ui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Accessibility utilities — content descriptions and focus management.
 *
 * Ensures the app is accessible via D-pad navigation and screen readers.
 */

/**
 * Add content description to a modifier for accessibility.
 */
fun Modifier.accessibilityDescription(description: String): Modifier {
    return this.semantics {
        contentDescription = description
    }
}

/**
 * Track focus for accessibility announcements.
 */
fun Modifier.trackFocus(focusId: String): Modifier {
    return this.semantics {
        contentDescription = focusId
    }
}
