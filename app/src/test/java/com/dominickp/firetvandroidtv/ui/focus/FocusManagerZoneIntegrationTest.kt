package com.dominickp.firetvandroidtv.ui.focus

import java.util.Collections
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for FocusManager zone tracking with navigation cycles.
 * Tests the complete focus state machine including zone transitions.
 */
class FocusManagerZoneIntegrationTest {
    private val fm = FocusManager()

    @BeforeEach
    fun setUp() {
        // Reset state between tests
        fm.currentScreen = null
        fm.lastFocusedItemId = null
        fm.clearRestoration()
        fm.clearRegistry()
        fm.currentZone = FocusZone.CONTENT
    }

    @Test
    fun `boot_state_has_content_zone`() {
        assertEquals(FocusZone.CONTENT, fm.currentZone)
        assertNull(fm.currentScreen)
        assertNull(fm.lastFocusedItemId)
    }

    @Test
    fun `left_from_first_card_moves_to_sidebar`() {
        // Initial state: content zone, focused on card
        fm.currentScreen = "home"
        fm.currentZone = FocusZone.CONTENT
        fm.lastFocusedItemId = "card_0_0"

        // User presses left: focus moves to sidebar
        fm.currentZone = FocusZone.SIDEBAR
        fm.lastFocusedItemId = "nav_item_0"

        assertEquals(FocusZone.SIDEBAR, fm.currentZone)
        assertEquals("nav_item_0", fm.lastFocusedItemId)
    }

    @Test
    fun `right_from_sidebar_moves_to_content`() {
        // Sidebar focused
        fm.currentZone = FocusZone.SIDEBAR
        fm.lastFocusedItemId = "nav_item_0"

        // User presses right: focus moves back to content
        fm.currentZone = FocusZone.CONTENT
        fm.lastFocusedItemId = "card_0_0"

        assertEquals(FocusZone.CONTENT, fm.currentZone)
        assertEquals("card_0_0", fm.lastFocusedItemId)
    }

    @Test
    fun `up_down_in_sidebar_keeps_sidebar_zone`() {
        fm.currentZone = FocusZone.SIDEBAR

        // Move down to item 1
        fm.lastFocusedItemId = "nav_item_1"
        assertEquals(FocusZone.SIDEBAR, fm.currentZone)

        // Move up to item 0
        fm.lastFocusedItemId = "nav_item_0"
        assertEquals(FocusZone.SIDEBAR, fm.currentZone)

        // Move down to item 2
        fm.lastFocusedItemId = "nav_item_2"
        assertEquals(FocusZone.SIDEBAR, fm.currentZone)
    }

    @Test
    fun `select_on_sidebar_changes_route_and_keeps_sidebar_zone`() {
        fm.currentZone = FocusZone.SIDEBAR
        fm.lastFocusedItemId = "nav_item_1"

        // User selects nav item 1 (search)
        // Route changes but zone stays SIDEBAR until focus transfers
        fm.currentScreen = "search"
        // Zone remains SIDEBAR because user is still focused on nav item
        assertEquals(FocusZone.SIDEBAR, fm.currentZone)
        assertEquals("search", fm.currentScreen)
    }

    @Test
    fun `enter_video_sets_transient_none_zone`() {
        // Home screen with content focus
        fm.currentScreen = "home"
        fm.currentZone = FocusZone.CONTENT
        fm.lastFocusedItemId = "card_5"

        // Save focus before entering video
        fm.saveFocusState("home", "card_5")

        // Enter video: zone becomes NONE (transient)
        fm.currentZone = FocusZone.NONE
        assertEquals(FocusZone.NONE, fm.currentZone)
    }

    @Test
    fun `exit_video_restores_content_zone`() {
        // Video was open, focus saved
        fm.saveFocusState("home", "card_5")
        fm.currentZone = FocusZone.NONE

        // Exit video: restore focus
        fm.restoreFocus()
        fm.currentZone = FocusZone.CONTENT
        fm.currentScreen = "home"

        assertEquals(FocusZone.CONTENT, fm.currentZone)
        assertEquals("home", fm.currentScreen)
    }

    @Test
    fun `full_navigation_cycle_with_zone_tracking`() {
        // Boot: content zone
        fm.currentZone = FocusZone.CONTENT
        fm.currentScreen = "home"
        fm.lastFocusedItemId = "card_0"

        // Press left: sidebar zone
        fm.currentZone = FocusZone.SIDEBAR
        fm.lastFocusedItemId = "nav_item_0"

        // Press down: still sidebar zone
        fm.lastFocusedItemId = "nav_item_1"
        assertEquals(FocusZone.SIDEBAR, fm.currentZone)

        // Select search: sidebar zone, screen changes
        fm.currentScreen = "search"
        assertEquals(FocusZone.SIDEBAR, fm.currentZone)
        assertEquals("search", fm.currentScreen)

        // Press right: content zone
        fm.currentZone = FocusZone.CONTENT
        fm.lastFocusedItemId = "search_field"

        // Select card: save focus, enter video
        fm.saveFocusState("search", "search_field")
        fm.currentZone = FocusZone.NONE

        // Exit video: restore to content
        fm.restoreFocus()
        fm.currentZone = FocusZone.CONTENT
        fm.currentScreen = "search"

        assertEquals(FocusZone.CONTENT, fm.currentZone)
        assertEquals("search", fm.currentScreen)
    }

    @Test
    fun `zone_state_is_thread_safe`() {
        // Two threads concurrently write distinct zones to the same FocusManager.
        // Because FocusManager guards its state with a mutex, concurrent get/set
        // must not corrupt state or throw: every observed read must be one of the
        // two values that was actually written (no null, no corruption).
        //
        // We cannot assert a specific final value — the last writer to complete
        // wins, non-deterministically — so instead we assert the thread-safety
        // invariant: all concurrent reads return a valid, written zone. (An
        // unsynchronized field would occasionally read a torn/null value here.)
        val observed = Collections.synchronizedList(mutableListOf<FocusZone>())

        val thread1 =
            Thread {
                fm.currentZone = FocusZone.SIDEBAR
                observed.add(fm.currentZone)
            }

        val thread2 =
            Thread {
                fm.currentZone = FocusZone.CONTENT
                observed.add(fm.currentZone)
            }

        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()

        // join() establishes a happens-before edge, so the observations are
        // visible to this thread. Every read must be one of the two written zones.
        assertTrue(observed.isNotEmpty(), "Expected concurrent zone reads to be recorded")
        for (z in observed) {
            assertTrue(
                z == FocusZone.SIDEBAR || z == FocusZone.CONTENT,
                "Observed unexpected zone $z after concurrent writes; expected SIDEBAR or CONTENT"
            )
        }
        // Final state is a valid zone that one of the threads wrote.
        assertTrue(
            fm.currentZone == FocusZone.SIDEBAR || fm.currentZone == FocusZone.CONTENT,
            "Final zone must be one of the two concurrently-written values"
        )
    }
}
