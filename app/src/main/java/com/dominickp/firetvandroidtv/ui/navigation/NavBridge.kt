package com.dominickp.firetvandroidtv.ui.navigation

/**
 * NavBridge — bridge object connecting MainActivity's OnBackPressedCallback
 * with AppNavigation's internal route state.
 *
 * Problem: OnBackPressedCallback lives in MainActivity (outside Compose), but
 * route state (currentRoute) lives in AppNavigation (inside Compose). We can't
 * share mutable state directly, so NavBridge exposes the callback as a mutable
 * property set by AppNavigation via LaunchedEffect.
 *
 * Usage:
 *   - AppNavigation sets NavBridge.onBackToHome when currentRoute is not Home
 *   - MainActivity calls NavBridge.onBackToHome?.invoke() on hardware Back
 */
object NavBridge {
    /**
     * Called when hardware Back is pressed from a sub-screen.
     * AppNavigation sets this to navigate currentRoute back to Home.
     * Set to null when already on Home so the callback doesn't fire erroneously.
     */
    var onBackToHome: (() -> Unit)? = null
}
