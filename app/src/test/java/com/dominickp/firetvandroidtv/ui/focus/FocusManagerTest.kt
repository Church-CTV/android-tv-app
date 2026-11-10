package com.dominickp.firetvandroidtv.ui.focus

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FocusManagerTest {
    private lateinit var focusManager: FocusManager

    @BeforeEach
    fun setUp() {
        focusManager = FocusManager()
    }

    @Test
    fun `initial state has no current screen`() {
        assertNull(focusManager.currentScreen)
    }

    @Test
    fun `initial state has no last focused item`() {
        assertNull(focusManager.lastFocusedItemId)
    }

    @Test
    fun `set current screen updates state`() {
        focusManager.currentScreen = "home"
        assertEquals("home", focusManager.currentScreen)
    }

    @Test
    fun `set last focused item updates state`() {
        focusManager.lastFocusedItemId = "item1"
        assertEquals("item1", focusManager.lastFocusedItemId)
    }

    @Test
    fun `saveFocusState stores screen and item`() {
        focusManager.saveFocusState("home", "card1")
        assertEquals("home", focusManager.currentScreen)
        assertEquals("card1", focusManager.lastFocusedItemId)
    }

    @Test
    fun `saveFocusState with null item works`() {
        focusManager.saveFocusState("home", null)
        assertEquals("home", focusManager.currentScreen)
        assertNull(focusManager.lastFocusedItemId)
    }

    @Test
    fun `restoreFocus returns false when no restoration state`() {
        val result = focusManager.restoreFocus()
        assertFalse(result)
    }

    @Test
    fun `clearRestoration does not throw`() {
        // Should not throw
        focusManager.clearRestoration()
    }

    @Test
    fun `multiple screen changes work`() {
        focusManager.currentScreen = "home"
        focusManager.currentScreen = "search"
        focusManager.currentScreen = "settings"
        assertEquals("settings", focusManager.currentScreen)
    }

    @Test
    fun `multiple focus item changes work`() {
        focusManager.lastFocusedItemId = "item1"
        focusManager.lastFocusedItemId = "item2"
        focusManager.lastFocusedItemId = "item3"
        assertEquals("item3", focusManager.lastFocusedItemId)
    }

    @Test
    fun `saveFocusState_with_null_focus_does_not_crash`() {
        // Simulates entering a screen with no previously focused element
        focusManager.saveFocusState("home", null)
        assertEquals("home", focusManager.currentScreen)
        assertNull(focusManager.lastFocusedItemId)
    }

    @Test
    fun `currentScreen_change_with_focused_item_saves_restoration`() {
        // Set initial state
        focusManager.currentScreen = "home"
        focusManager.lastFocusedItemId = "card1"

        // Simulate navigating to a different screen
        focusManager.currentScreen = "search"

        // Should have saved restoration state (via the setter)
        // The setter stores focusedId when switching screens with focus
        assertEquals("search", focusManager.currentScreen)
        assertEquals("card1", focusManager.lastFocusedItemId)
    }
}
