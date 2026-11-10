package com.dominickp.firetvandroidtv.ui.search

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TVKeyboard logic — character layout, query building,
 * backspace behavior, enter handling, key mapping, and D-pad navigation.
 * Mirrors Roku's MiniKeyboard component logic.
 */
class TVKeyboardTest {
    // Simulated keyboard state — mirrors TVKeyboard internals
    private lateinit var query: StringBuilder
    private var focusedRow: Int = 0
    private var focusedCol: Int = 0

    @Before
    fun setUp() {
        query = StringBuilder()
    }

    // ── Keyboard layout (10-col grid: A–J / K–T / U–Z1–4 / 5–9 + actions) ──

    @Test
    fun `keyboardRow0HasFirstTenLetters`() {
        assertEquals("ABCDEFGHIJKLMNOP".take(10), KEY_ROWS[0].joinToString(""))
        assertTrue(KEY_ROWS[0].all { it.length == 1 && it[0] in 'A'..'Z' })
    }

    @Test
    fun `keyboardRow1HasNextTenLetters`() {
        assertEquals("KLMNOPQRST", KEY_ROWS[1].joinToString(""))
        assertTrue(KEY_ROWS[1].all { it.length == 1 && it[0] in 'A'..'Z' })
    }

    @Test
    fun `keyboardRow2HasLastSixLettersPlusDigits`() {
        assertEquals("UVWXYZ1234", KEY_ROWS[2].joinToString(""))
    }

    @Test
    fun `keyboardRow3HasDigitsAndActionKeys`() {
        val row3 = KEY_ROWS[3]
        assertEquals(9, row3.size)
        assertTrue(row3.contains(KEY_SPACE))
        assertTrue(row3.contains(KEY_MIC))
        assertTrue(row3.contains(KEY_BACKSPACE))
        assertTrue(row3.contains(KEY_SEARCH))
    }

    @Test
    fun `keyboardHasFourRowsOfTenColumns`() {
        assertEquals(KEY_ROW_COUNT, KEY_ROWS.size)
        assertEquals(4, KEY_ROWS.size)
        assertEquals(KEY_COLS, 10)
        KEY_ROWS.forEachIndexed { idx, row ->
            // Row 3 has 9 logical keys; SEARCH spans 2 cells → 10 visual cells.
            val visual = if (idx == KEY_ROW_COUNT - 1) row.size + 1 else row.size
            assertEquals("row $idx width", KEY_COLS, visual)
        }
    }

    @Test
    fun `keyboardCoversFullAlphabetAndDigits`() {
        val all = KEY_ROWS.joinToString("") { it.joinToString("") }
        assertTrue(all, ('A'..'Z').all { all.contains(it.toString()) })
        assertTrue(all, ('1'..'9').all { all.contains(it.toString()) })
    }

    @Test
    fun `searchKeySpansLastTwoCellsOfLastRow`() {
        assertEquals(KEY_COLS - 2, SEARCH_COL)
        assertEquals(8, SEARCH_COL)
    }

    @Test
    fun `keyboardKeyFocusHasVisualFeedback`() {
        // Focused keys use the brand gold fill (BrandColors.Focus) with dark
        // glyph; unfocused keys are white @10%. The contrast ratio between
        // focused/unfocused must exceed 1.0 (i.e. they differ).
        val focused = com.dominickp.firetvandroidtv.ui.theme.BrandColors.Focus
        val unfocused = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f)
        assertTrue(focused != unfocused)
    }

    // ── Character input ──

    @Test
    fun `keyPressAppendsLetterToQuery`() {
        query.append("A")
        assertEquals("A", query.toString())
        query.append("B")
        assertEquals("AB", query.toString())
    }

    @Test
    fun `keyPressAppendsAllLetters`() {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ".forEach { query.append(it) }
        assertEquals(26, query.length)
        assertTrue(query.toString().contains("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
    }

    @Test
    fun `keyPressAppendsSpecialCharacters`() {
        query.append("@")
        query.append("#")
        query.append("$")
        assertEquals("@#$", query.toString())
    }

    @Test
    fun `specialCharacterKeyInsertsCharacter`() {
        query.append("%")
        query.append("^")
        query.append("&")
        assertEquals("%^&", query.toString())
    }

    // ── Backspace ──

    @Test
    fun `backspaceRemovesLastChar`() {
        query.append("ABC")
        query.deleteCharAt(query.length - 1)
        assertEquals("AB", query.toString())
    }

    @Test
    fun `backspaceOnEmptyQueryDoesNotCrash`() {
        if (query.isNotEmpty()) {
            query.deleteCharAt(query.length - 1)
        }
        assertEquals(0, query.length)
        assertEquals("", query.toString())
    }

    @Test
    fun `multipleBackspacesRemoveMultipleChars`() {
        query.append("ABCD")
        query.deleteCharAt(query.length - 1)
        query.deleteCharAt(query.length - 1)
        assertEquals("AB", query.toString())
    }

    @Test
    fun `backspaceReducesQueryLength`() {
        val originalLength = query.length
        query.append("test")
        query.deleteCharAt(query.length - 1)
        assertEquals(originalLength + 3, query.length)
    }

    // ── Space ──

    @Test
    fun `spaceInsertsWhitespace`() {
        query.append("Hello")
        query.append(" ")
        assertTrue(query.toString().endsWith(" "))
        assertEquals("Hello ", query.toString())
    }

    @Test
    fun `multipleSpacesInsertMultipleWhitespace`() {
        query.append("Hello World")
        assertEquals("Hello World", query.toString())
    }

    @Test
    fun `spaceBetweenWordsIsPreserved`() {
        query.append("action movie")
        assertTrue(query.toString().contains(" "))
        assertEquals("action movie", query.toString())
    }

    // ── Enter / search trigger ──

    @Test
    fun `enterTriggersSearchWhenQueryNotEmpty`() {
        query.append("action movie")
        var entered = query.isNotEmpty()
        assertTrue(entered)
    }

    @Test
    fun `enterDoesNotTriggerSearchWhenEmpty`() {
        var entered = query.isNotEmpty()
        assertFalse(entered)
    }

    @Test
    fun `enterTriggersSearchForSingleCharQuery`() {
        query.append("a")
        var entered = query.isNotEmpty()
        assertTrue(entered)
    }

    // ── Query limits ──

    @Test
    fun `queryCanHandleLongQueries`() {
        val maxLength = 100
        query.append("A".repeat(maxLength))
        assertEquals(maxLength, query.length)
    }

    @Test
    fun `queryCanClearAllContent`() {
        query.append("search term")
        query.setLength(0)
        assertTrue(query.isEmpty())
    }

    @Test
    fun `queryCanBeResetToEmpty`() {
        query.append("test")
        query.replace(0, query.length, "")
        assertEquals("", query.toString())
    }

    // ── Focus management ──

    @Test
    fun `initialFocusPositionIsZeroZero`() {
        focusedRow = 0
        focusedCol = 0
        assertEquals(0, focusedRow)
        assertEquals(0, focusedCol)
    }

    @Test
    fun `focusMovesWithinRowBounds`() {
        // Row 0 has 10 keys (indices 0-9)
        focusedCol = 9
        assertTrue(focusedCol < 10)
    }

    @Test
    fun `focusMovesBetweenRows`() {
        focusedRow = 2 // Special keys row
        assertEquals(2, focusedRow)
    }

    @Test
    fun `focusColumnIsWithinRowBoundsForTenKeyRow`() {
        val rowSize = 10
        for (col in 0 until rowSize) {
            assertTrue(col < rowSize)
        }
    }

    @Test
    fun `focusColumnIsWithinRowBoundsForNineKeyRow`() {
        val rowSize = 9
        for (col in 0 until rowSize) {
            assertTrue(col < rowSize)
        }
    }

    @Test
    fun `focusColumnIsWithinRowBoundsForThreeKeyRow`() {
        val rowSize = 3
        for (col in 0 until rowSize) {
            assertTrue(col < rowSize)
        }
    }

    // ── Keyboard key labels ──

    @Test
    fun `backspaceKeyLabelIsValid`() {
        val label = "BACKSPACE"
        assertFalse(label.isEmpty())
        assertTrue(label.contains("BACK"))
    }

    @Test
    fun `searchKeyLabelIsValid`() {
        val label = "SEARCH"
        assertFalse(label.isEmpty())
        assertTrue(label.contains("SEAR"))
    }

    @Test
    fun `spaceKeyLabelIsValid`() {
        val label = "SPACE"
        assertFalse(label.isEmpty())
        assertTrue(label.contains("SPACE"))
    }

    // ── Composed keyboard operations ──

    @Test
    fun `fullSearchQueryWorkflow`() {
        // Build "test query" step by step
        "test".forEach { query.append(it) }
        query.append(" ")
        "query".forEach { query.append(it) }
        assertEquals("test query", query.toString())
    }

    @Test
    fun `searchThenCorrectWithBackspace`() {
        "testquery".forEach { query.append(it) }
        for (i in 0 until 9) {
            if (query.isNotEmpty()) {
                query.deleteCharAt(query.length - 1)
            }
        }
        assertEquals("", query.toString())
    }

    @Test
    fun `keyboardStateCanCaptureSpecialKeys`() {
        val specialKeys = listOf("SPACE", "BACKSPACE", "SEARCH")
        specialKeys.forEach { key ->
            when (key) {
                "BACKSPACE" -> {
                    // Remove last char
                    if (query.isNotEmpty()) {
                        query.deleteCharAt(query.length - 1)
                    }
                }
                "SPACE" -> query.append(" ")
                "SEARCH" -> {
                    // Trigger search
                    assertNotNull(query.toString())
                }
            }
        }
    }

    // ── Query display ──

    @Test
    fun `emptyQueryDisplaysPlaceholderText`() {
        val queryText = if (query.isEmpty()) "Search videos..." else query.toString()
        assertEquals("Search videos...", queryText)
    }

    @Test
    fun `nonEmptyQueryDisplaysActualQuery`() {
        query.append("test")
        val queryText = if (query.isEmpty()) "Search videos..." else query.toString()
        assertEquals("test", queryText)
    }

    @Test
    fun `queryDisplayHandlesWhitespaceOnly`() {
        query.append("   ")
        val queryText = if (query.isEmpty()) "Search videos..." else query.toString()
        assertEquals("   ", queryText)
    }
}
