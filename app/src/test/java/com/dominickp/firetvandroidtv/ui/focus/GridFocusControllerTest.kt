package com.dominickp.firetvandroidtv.ui.focus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * GridFocusController tests.
 *
 * These encode a bug found on a device: with the nested
 * LazyColumn/LazyRow layout, Compose auto-traversal froze after ONE DOWN press.
 * The controller must reliably step DOWN/UP/LEFT/RIGHT across the grid.
 */
class GridFocusControllerTest {

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        GridFocusController.testMode = true
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        GridFocusController.testMode = false
    }

    private fun newController(): GridFocusController {
        // Use the singleton getter (tests share state, so reset before each)
        val ctrl = GridFocusController.getInstance()
        ctrl.reset()
        return ctrl
    }

    /**
     * Build a ragged grid matching the app's real shape:
     *   row 0 (LIVE NOW): 1 card
     *   row 1 (August):   4 cards
     *   row 2 (July):     4 cards
     *   row 3 (June):     4 cards
     */
    private fun buildRaggedGrid(ctrl: GridFocusController) {
        ctrl.register(0, 0, androidx.compose.ui.focus.FocusRequester())
        for (c in 0..3) ctrl.register(1, c, androidx.compose.ui.focus.FocusRequester())
        for (c in 0..3) ctrl.register(2, c, androidx.compose.ui.focus.FocusRequester())
        for (c in 0..3) ctrl.register(3, c, androidx.compose.ui.focus.FocusRequester())
    }

    @Test
    fun `down moves across every row repeatedly`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        // Start by focusing the LIVE card at (0,0).
        assertTrue(ctrl.focusCard(0, 0))

        // DOWN should walk: row0 -> row1 -> row2 -> row3
        assertTrue(ctrl.moveDown())
        assertEquals(1, ctrl.focusedPosition().first)

        assertTrue(ctrl.moveDown())
        assertEquals(2, ctrl.focusedPosition().first)

        assertTrue(ctrl.moveDown())
        assertEquals(3, ctrl.focusedPosition().first)

        // Past the last row: no move.
        assertFalse(ctrl.moveDown())
        assertEquals(3, ctrl.focusedPosition().first)
    }

    @Test
    fun `up moves back across rows repeatedly`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        assertTrue(ctrl.focusCard(3, 0))

        assertTrue(ctrl.moveUp())
        assertEquals(2, ctrl.focusedPosition().first)

        assertTrue(ctrl.moveUp())
        assertEquals(1, ctrl.focusedPosition().first)

        assertTrue(ctrl.moveUp())
        assertEquals(0, ctrl.focusedPosition().first)

        // Above the first row: no move.
        assertFalse(ctrl.moveUp())
        assertEquals(0, ctrl.focusedPosition().first)
    }

    @Test
    fun `down preserves column when target row has it`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        // Focus col 2 of row 1.
        assertTrue(ctrl.focusCard(1, 2))

        // DOWN to row 2, should keep col 2.
        assertTrue(ctrl.moveDown())
        val pos = ctrl.focusedPosition()
        assertEquals(2, pos.first)
        assertEquals(2, pos.second)
    }

    @Test
    fun `up clamps column when target row is shorter`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        // Focus (1,3) — col 3 exists in row 1.
        assertTrue(ctrl.focusCard(1, 3))

        // UP to row 0 which only has col 0 → must clamp to col 0.
        assertTrue(ctrl.moveUp())
        val pos = ctrl.focusedPosition()
        assertEquals(0, pos.first)
        assertEquals(0, pos.second)
    }

    @Test
    fun `right moves across cards then wraps to next row`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        // Focus last card of row 1 (col 3).
        assertTrue(ctrl.focusCard(1, 3))

        // RIGHT at row end → next row at nearest col (col 3).
        assertTrue(ctrl.moveRight())
        val pos = ctrl.focusedPosition()
        assertEquals(2, pos.first)
        assertEquals(3, pos.second)
    }

    @Test
    fun `left moves across cards`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        // Focus col 2 of row 1.
        assertTrue(ctrl.focusCard(1, 2))

        // LEFT → col 1.
        assertTrue(ctrl.moveLeft())
        val pos = ctrl.focusedPosition()
        assertEquals(1, pos.first)
        assertEquals(1, pos.second)
    }

    @Test
    fun `left at col 0 returns false so root can route to sidebar`() {
        val ctrl = newController()
        buildRaggedGrid(ctrl)

        // Focus col 0 of row 1.
        assertTrue(ctrl.focusCard(1, 0))

        // LEFT at col 0 → returns false (LeftNavScreen sends to sidebar).
        assertFalse(ctrl.moveLeft())
    }
}
