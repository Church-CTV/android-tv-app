package com.dominickp.firetvandroidtv.ui.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the Settings screen's D-pad focus layout model ([SettingsLayout]).
 * Pure logic — no Compose runtime. The settings surface was reduced (2026-09-11)
 * to Back / Autoplay / (optional Donate) / About, so these cover the minimal row
 * construction, the single-column focus cells, the non-focusable About row, and
 * up/down focus-row routing with boundary handling.
 */
class SettingsLayoutTest {

    private val withGive = SettingsLayout.rows(hasGive = true)
    private val noGive = SettingsLayout.rows(hasGive = false)

    @Test
    fun `row model is built in expected order`() {
        val ids = noGive.map { it.id }
        assertEquals(listOf("back", "autoplay", "keep_awake", "about"), ids)
    }

    @Test
    fun `give row is included only when donation url present`() {
        assertTrue(noGive.none { it.id == "give" })
        assertTrue(withGive.any { it.id == "give" })
        assertEquals(noGive.size + 1, withGive.size)
        // give lands just above about.
        val giveIdx = withGive.indexOfFirst { it.id == "give" }
        assertEquals("about", withGive[giveIdx + 1].id)
    }

    @Test
    fun `all interactive rows expose a single focus cell`() {
        val byId = noGive.associate { it.id to it }
        assertEquals(1, byId["back"]!!.cols)
        assertEquals(1, byId["autoplay"]!!.cols)
        assertEquals(1, byId["keep_awake"]!!.cols)
        val byIdGive = withGive.associate { it.id to it }
        assertEquals(1, byIdGive["give"]!!.cols)
    }

    @Test
    fun `about row is non focusable`() {
        val about = noGive.last { it.id == "about" }
        assertEquals(0, about.cols)
    }

    @Test
    fun `every row id is unique`() {
        assertEquals(noGive.size, noGive.map { it.id }.toSet().size)
        assertEquals(withGive.size, withGive.map { it.id }.toSet().size)
    }

    @Test
    fun `nextFocusRow moves down to the next focusable row`() {
        val backIdx = noGive.indexOfFirst { it.id == "back" }
        assertEquals("autoplay", noGive[SettingsLayout.nextFocusRow(noGive, backIdx, +1)].id)
    }

    @Test
    fun `nextFocusRow moves up to the previous focusable row`() {
        val autoplayIdx = noGive.indexOfFirst { it.id == "autoplay" }
        assertEquals("back", noGive[SettingsLayout.nextFocusRow(noGive, autoplayIdx, -1)].id)
    }

    @Test
    fun `nextFocusRow skips the non focusable about row when moving up`() {
        // About (last row) is cols=0; moving up from about lands on keep_awake.
        val aboutIdx = noGive.indexOfFirst { it.id == "about" }
        assertEquals("keep_awake", noGive[SettingsLayout.nextFocusRow(noGive, aboutIdx, -1)].id)
    }

    @Test
    fun `nextFocusRow moves from about up to give when a give row is present`() {
        val aboutIdx = withGive.indexOfFirst { it.id == "about" }
        assertEquals("give", withGive[SettingsLayout.nextFocusRow(withGive, aboutIdx, -1)].id)
    }

    @Test
    fun `nextFocusRow returns minus one at the top boundary`() {
        val backIdx = noGive.indexOfFirst { it.id == "back" }
        assertEquals(-1, SettingsLayout.nextFocusRow(noGive, backIdx, -1))
    }

    @Test
    fun `nextFocusRow returns minus one at the bottom boundary`() {
        val aboutIdx = noGive.indexOfFirst { it.id == "about" }
        assertEquals(-1, SettingsLayout.nextFocusRow(noGive, aboutIdx, +1))
    }

    @Test
    fun `clampCol keeps the column within the target row width`() {
        // All interactive rows are 1-col; any incoming col clamps to 0.
        val autoplayIdx = noGive.indexOfFirst { it.id == "autoplay" }
        assertEquals(0, SettingsLayout.clampCol(3, noGive, autoplayIdx))
        // And a valid col 0 is preserved.
        assertEquals(0, SettingsLayout.clampCol(0, noGive, autoplayIdx))
    }

    @Test
    fun `colCount returns zero for an out of range row`() {
        assertEquals(0, SettingsLayout.colCount(noGive, -1))
        assertEquals(0, SettingsLayout.colCount(noGive, noGive.size))
    }

    @Test
    fun `headers mark the start of a section`() {
        val byId = noGive.associate { it.id to it }
        assertEquals("Playback", byId["autoplay"]!!.header)
        assertEquals("Display", byId["keep_awake"]!!.header)
        assertEquals("About", byId["about"]!!.header)
        // Back and give are not section opens.
        assertNull(byId["back"]!!.header)
        val byIdGive = withGive.associate { it.id to it }
        assertNull(byIdGive["give"]!!.header)
    }
}
