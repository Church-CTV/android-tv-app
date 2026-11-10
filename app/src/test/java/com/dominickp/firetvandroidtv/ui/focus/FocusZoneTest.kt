package com.dominickp.firetvandroidtv.ui.focus

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for FocusZone enum and FocusManager zone tracking.
 */
class FocusZoneTest {
    private lateinit var focusManager: FocusManager

    @BeforeEach
    fun setUp() {
        focusManager = FocusManager()
    }

    @Test
    fun `initial zone is CONTENT`() {
        assertEquals(FocusZone.CONTENT, focusManager.currentZone)
    }

    @Test
    fun `zone can be set to SIDEBAR`() {
        focusManager.currentZone = FocusZone.SIDEBAR
        assertEquals(FocusZone.SIDEBAR, focusManager.currentZone)
    }

    @Test
    fun `zone can be set to CONTENT`() {
        focusManager.currentZone = FocusZone.SIDEBAR
        focusManager.currentZone = FocusZone.CONTENT
        assertEquals(FocusZone.CONTENT, focusManager.currentZone)
    }

    @Test
    fun `zone can be set to NONE`() {
        focusManager.currentZone = FocusZone.NONE
        assertEquals(FocusZone.NONE, focusManager.currentZone)
    }

    @Test
    fun `zone transitions track focus movement`() {
        // Boot: content has focus
        assertEquals(FocusZone.CONTENT, focusManager.currentZone)

        // User presses left: focus moves to sidebar
        focusManager.currentZone = FocusZone.SIDEBAR
        assertEquals(FocusZone.SIDEBAR, focusManager.currentZone)

        // User presses right: focus moves back to content
        focusManager.currentZone = FocusZone.CONTENT
        assertEquals(FocusZone.CONTENT, focusManager.currentZone)

        // User navigates to video: zone becomes NONE (transient)
        focusManager.currentZone = FocusZone.NONE
        assertEquals(FocusZone.NONE, focusManager.currentZone)

        // Video closes: focus restored to sidebar
        focusManager.currentZone = FocusZone.SIDEBAR
        assertEquals(FocusZone.SIDEBAR, focusManager.currentZone)
    }

    @Test
    fun `zone_and_screen_state_work_together`() {
        focusManager.currentScreen = "home"
        focusManager.currentZone = FocusZone.CONTENT
        focusManager.lastFocusedItemId = "card_1"

        assertEquals("home", focusManager.currentScreen)
        assertEquals(FocusZone.CONTENT, focusManager.currentZone)
        assertEquals("card_1", focusManager.lastFocusedItemId)
    }

    @Test
    fun `zone_changes_during_navigation_cycle`() {
        // Home screen: content focus
        focusManager.currentScreen = "home"
        focusManager.currentZone = FocusZone.CONTENT
        focusManager.lastFocusedItemId = "card_3"

        // Navigate to search: sidebar focus
        focusManager.currentScreen = "search"
        focusManager.currentZone = FocusZone.SIDEBAR
        focusManager.lastFocusedItemId = "nav_search"

        // Navigate to video: no zone (transient)
        focusManager.saveFocusState("search", "nav_search")
        focusManager.currentZone = FocusZone.NONE

        // Return to home: restore to content
        focusManager.restoreFocus()
        focusManager.currentZone = FocusZone.CONTENT
        assertEquals(FocusZone.CONTENT, focusManager.currentZone)
    }
}
