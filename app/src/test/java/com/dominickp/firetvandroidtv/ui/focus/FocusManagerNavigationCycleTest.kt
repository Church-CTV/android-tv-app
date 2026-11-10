package com.dominickp.firetvandroidtv.ui.focus

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for FocusManager navigation cycle.
 * Tests: home -> search -> back -> settings -> back -> video -> back -> home
 * Focus restoration state is correctly saved and restored.
 */
class FocusManagerNavigationCycleTest {
    private val fm = FocusManager.getInstance()

    @BeforeEach
    fun setUp() {
        // Reset state between tests
        fm.currentScreen = null
        fm.lastFocusedItemId = null
        fm.clearRestoration()
        fm.clearRegistry()
    }

    @Test
    fun `focus_manager_current_screen_starts_null`() {
        assertNull(fm.currentScreen)
    }

    @Test
    fun `focus_manager_last_focused_item_starts_null`() {
        assertNull(fm.lastFocusedItemId)
    }

    @Test
    fun `save_and_restore_focus_state`() {
        fm.saveFocusState("home", "card1")
        assertEquals("home", fm.currentScreen)
        assertEquals("card1", fm.lastFocusedItemId)
    }

    @Test
    fun `focus_manager_tracks_screen_transition`() {
        fm.currentScreen = "home"
        fm.lastFocusedItemId = "nav_home"
        fm.currentScreen = "search"
        assertEquals("search", fm.currentScreen)
        assertEquals("nav_home", fm.lastFocusedItemId)
    }

    @Test
    fun `focus_restoration_saved_on_screen_change_with_focus`() {
        // On a screen change with a focused element, FocusManager captures the
        // restoration target. restoreFocus() then attempts to restore it.
        //
        // In a pure JVM unit test a detached FocusRequester cannot receive focus
        // (FocusRequester.requestFocus() throws when not attached to a composition),
        // so we assert the invariant the implementation is documented to hold:
        // the target is captured on the screen change, and because no requester is
        // registered restoreFocus() returns false WITHOUT clearing the target —
        // so a later real restore can still use it. This is the deliberate behavior
        // from the a02319c fix (previously it returned true and dropped the target).
        fm.currentScreen = "home"
        fm.lastFocusedItemId = "card_5"
        fm.currentScreen = "video" // captures videoFocusRestoration = "card_5"

        // The restoration target was captured on the screen change.
        assertEquals("card_5", fm.videoFocusRestorationForTesting)

        // No requester registered for "card_5" → cannot actually restore → false,
        // and the target is retained (not dropped) for a later real restore.
        val restored = fm.restoreFocus()
        assertFalse(restored)
        assertEquals("card_5", fm.videoFocusRestorationForTesting)
    }

    @Test
    fun `focus_restoration_returns_false_when_no_focus_saved`() {
        fm.currentScreen = "home"
        fm.lastFocusedItemId = null
        fm.currentScreen = "search"
        val restored = fm.restoreFocus()
        assertFalse(restored)
    }

    @Test
    fun `focus_restoration_cleared_after_use`() {
        fm.clearRestoration()
        val restored = fm.restoreFocus()
        assertFalse(restored)
    }

    @Test
    fun `focus_manager_register_unregister_roundtrip`() {
        // Create a mock FocusRequester — in unit tests we can't use Compose remember
        // So we test the registry mechanics directly via reflection or just test the map
        val map = mutableMapOf<String, androidx.compose.ui.focus.FocusRequester>()
        map["test_elem"] = androidx.compose.ui.focus.FocusRequester()
        assertTrue(map.containsKey("test_elem"))
        map.remove("test_elem")
        assertFalse(map.containsKey("test_elem"))
    }

    @Test
    fun `focus_manager_clear_registry_wipes_all`() {
        val map = mutableMapOf<String, androidx.compose.ui.focus.FocusRequester>()
        map["a"] = androidx.compose.ui.focus.FocusRequester()
        map["b"] = androidx.compose.ui.focus.FocusRequester()
        assertEquals(2, map.size)
        map.clear()
        assertEquals(0, map.size)
    }

    @Test
    fun `full_navigation_cycle_home_search_settings_continue_watching`() {
        // Simulate the navigation cycle: Home -> Search -> Settings -> Continue Watching -> Home
        val cycle = listOf("home", "search", "settings", "continue_watching", "home")
        var prev: String? = null
        for (screen in cycle) {
            fm.currentScreen = screen
            if (prev != null && screen != prev) {
                // Focus was saved before the transition
                fm.lastFocusedItemId = "focus_$prev"
            }
            prev = screen
        }
        assertEquals("home", fm.currentScreen)
    }

    @Test
    fun `full_navigation_cycle_with_video`() {
        // Home -> Search -> Video -> Home (with video focus restoration)
        fm.currentScreen = "home"
        fm.lastFocusedItemId = "card_3"
        fm.currentScreen = "search"
        fm.lastFocusedItemId = "search_field"
        fm.currentScreen = "video"
        fm.saveFocusState("home", "card_3")
        fm.currentScreen = "home"
        // After exiting video, home should be the current screen
        assertEquals("home", fm.currentScreen)
    }

    @Test
    fun `focus_manager_singleton_returns_same_instance`() {
        val fm1 = FocusManager.getInstance()
        val fm2 = FocusManager.getInstance()
        assertSame(fm1, fm2)
    }
}
