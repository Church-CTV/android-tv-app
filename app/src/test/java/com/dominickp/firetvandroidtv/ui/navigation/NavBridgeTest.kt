package com.dominickp.firetvandroidtv.ui.navigation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NavBridgeTest — tests the NavBridge hardware-Back-to-Home bridge object.
 *
 * NavBridge is the mutable bridge that connects MainActivity's OnBackPressedCallback
 * (outside Compose) with AppNavigation's route state (inside Compose).
 *
 * Verifies:
 * - onBackToHome is null by default (no spurious back navigation)
 * - AppNavigation can set a callback and MainActivity's call invokes it
 * - AppNavigation can clear the callback (set null when on Home)
 * - The callback runs exactly once per invocation
 */
class NavBridgeTest {

    @After
    fun tearDown() {
        // Reset global state so tests don't leak into each other.
        NavBridge.onBackToHome = null
    }

    @Test
    fun `onBackToHome is null by default`() {
        assertNull("NavBridge starts with no callback", NavBridge.onBackToHome)
    }

    @Test
    fun `callback set by AppNavigation is invoked when triggered`() {
        var invoked = false
        NavBridge.onBackToHome = { invoked = true }
        NavBridge.onBackToHome?.invoke()
        assertTrue("the set callback must be invoked", invoked)
    }

    @Test
    fun `callback is not invoked when null`() {
        assertNull(NavBridge.onBackToHome)
        // Safe-call on null must be a no-op, no crash.
        NavBridge.onBackToHome?.invoke()
    }

    @Test
    fun `callback runs exactly once per invocation`() {
        var count = 0
        NavBridge.onBackToHome = { count++ }
        NavBridge.onBackToHome?.invoke()
        NavBridge.onBackToHome?.invoke()
        assertEquals("each invoke should run once", 2, count)
    }

    @Test
    fun `callback can be replaced`() {
        var target = "first"
        NavBridge.onBackToHome = { target = "first" }
        NavBridge.onBackToHome = { target = "second" }
        NavBridge.onBackToHome?.invoke()
        assertEquals("the latest callback wins", "second", target)
    }

    @Test
    fun `clearing callback by setting null stops invocation`() {
        var invoked = false
        NavBridge.onBackToHome = { invoked = true }
        NavBridge.onBackToHome = null
        NavBridge.onBackToHome?.invoke()
        assertTrue("after null, callback must not run", !invoked)
    }
}
