package com.dominickp.firetvandroidtv.ui.focus

import androidx.compose.ui.focus.FocusRequester

/**
 * GridFocusController — explicit D-pad navigation for the video grid on TV devices.
 *
 * WHY THIS EXISTS:
 * Compose's built-in focus traversal (FocusManager.nextFocus) is unreliable with the
 * nested LazyColumn > LazyRow > cards layout used on this TV app. On real Fire TV
 * devices the focus ring moves on the first DOWN press, then FREEZES on subsequent
 * presses because the system's geometry-based traversal can't cross the lazy row
 * boundary a second time. Confirmed on a device via the uiautomator
 * accessibility tree: DOWN-1 moved the focused node to the next row, then DOWN-2..6
 * all reported the SAME node.
 *
 * This controller takes over all four D-pad directions for the content grid. The
 * root onPreviewKeyEvent in LeftNavScreen calls moveUp/moveDown/moveLeft/moveRight
 * for every D-pad press while focus is in the content zone.
 *
 * GRID MODEL (ragged — each month row has a different card count):
 *   - rows  = month groups (row 0 = LIVE NOW when present)
 *   - cols  = card positions within a row (0-based)
 *   - Each card registers its FocusRequester under its (row, col) on composition.
 *   - Navigation never relies on Compose auto-traversal.
 *
 * NAVIGATION RULES (mirror Roku RowList behavior):
 *   - DOWN: go to next row that has cards, landing on the same col if it exists
 *     there, else the nearest col that does (clamped to that row's cards).
 *   - UP:   mirror of DOWN.
 *   - RIGHT: next card in the same row; if none (at row end), go to next row's
 *     card at the same col, else nearest.
 *   - LEFT:  previous card in the same row; if none (at row start), the root
 *     handler routes focus to the sidebar instead (handled in LeftNavScreen).
 *
 * Thread-safe via synchronized blocks on a single mutex.
 */
@androidx.compose.runtime.Stable
@Suppress("ReturnCount", "MagicNumber")
class GridFocusController {

    companion object {
        @Volatile
        private var instance: GridFocusController? = null

        /** Set to true in unit tests to skip FocusRequester.requestFocus() calls. */
        @Volatile
        var testMode: Boolean = false

        fun getInstance(): GridFocusController {
            return instance ?: synchronized(this) {
                instance ?: GridFocusController().also { instance = it }
            }
        }
    }

    private val _mutex = Any()

    // (row, col) -> FocusRequester, updated as cards compose and recycle.
    private var _requesters: MutableMap<Long, FocusRequester> = mutableMapOf()
        @Synchronized get

    // Current focus position. -1,-1 means "no card focused yet".
    private var _focusedRow: Int = -1
        @Synchronized get
    private var _focusedCol: Int = -1
        @Synchronized get

    /** Register a card's FocusRequester at its grid position. Call on composition. */
    fun register(row: Int, col: Int, requester: FocusRequester) {
        synchronized(_mutex) { _requesters[key(row, col)] = requester }
    }

    /** Remove a card's FocusRequester (on dispose). */
    fun unregister(row: Int, col: Int) {
        synchronized(_mutex) { _requesters.remove(key(row, col)) }
    }

    /**
     * Reset only the position state (keep registered cards). Called by
     * LeftNavScreen when it needs to force focus to a specific card.
     *
     * NOTE: do NOT clear _requesters here — LeftNavScreen previously used
     * reset() in a LaunchedEffect keyed on groupedVideos, which is rebuilt on
     * every recomposition, wiping all registrations between key events.
     */
    fun resetPosition() {
        synchronized(_mutex) {
            _focusedRow = -1
            _focusedCol = -1
        }
    }

    /** Full reset (position + requesters). Only for tests and app-level teardown. */
    fun reset() {
        synchronized(_mutex) {
            _requesters.clear()
            _focusedRow = -1
            _focusedCol = -1
        }
    }

    /** True if any card is currently focused. */
    fun hasFocus(): Boolean {
        synchronized(_mutex) { return _focusedRow >= 0 }
    }

    /**
     * Focus a card at (row, col) directly. Returns true if the card exists and focus
     * was set. Used by the sidebar-to-grid transfer and initial-focus logic.
     */
    fun focusCard(row: Int, col: Int): Boolean {
        val requester: FocusRequester
        synchronized(_mutex) {
            requester = _requesters[key(row, col)] ?: return false
            _focusedRow = row
            _focusedCol = col
        }
        if (!testMode) {
            requester.requestFocus()
        }
        return true
    }

    /** Move DOWN to the next row. Returns true if focus moved. */
    fun moveDown(): Boolean {
        val target = stepVertical(+1) ?: return false
        return focusCard(target.first, target.second)
    }

    /** Move UP to the previous row. Returns true if focus moved. */
    fun moveUp(): Boolean {
        val target = stepVertical(-1) ?: return false
        return focusCard(target.first, target.second)
    }

    /** Move RIGHT within a row (falls to next row at row end). Returns true if moved. */
    fun moveRight(): Boolean {
        val target = stepHorizontal(+1) ?: return false
        return focusCard(target.first, target.second)
    }

    /**
     * Move LEFT within the same row only. Returns false if focus is on the first
     * card of its row (col 0) — LeftNavScreen routes to sidebar in that case.
     *
     * Intentionally does NOT wrap to another row: LEFT at the left edge of the grid
     * is a "go to sidebar" signal, not "continue left into another row".
     */
    fun moveLeft(): Boolean {
        val curRow: Int
        val curCol: Int
        synchronized(_mutex) {
            curRow = _focusedRow
            curCol = _focusedCol
        }
        if (curRow < 0) return false

        val targetCol: Int?
        synchronized(_mutex) {
            targetCol = colsInRow(curRow).filter { it < curCol }.maxOrNull()
        }
        if (targetCol == null) return false

        return focusCard(curRow, targetCol)
    }

    /**
     * Called by ContentCard when it gains focus. Updates the controller's position
     * so subsequent moveDown/Up/Left/Right calls know where to start from.
     * Does NOT call requestFocus (the card is already focused).
     */
    fun reportFocus(row: Int, col: Int) {
        synchronized(_mutex) {
            _focusedRow = row
            _focusedCol = col
        }
    }

    // ---------- internals (all synchronized) ----------

    /**
     * Vertical step: find the next/previous row (in direction dir) that has any cards,
     * then land on the same col if present, else the nearest col that exists in that row.
     */
    private fun stepVertical(dir: Int): Pair<Int, Int>? {
        synchronized(_mutex) {
            if (_focusedRow < 0) return null
            val curRow = _focusedRow
            val curCol = _focusedCol

            var row = curRow + dir
            val rows = allRows()
            while (true) {
                if (row < 0 || row !in rows) return null
                val cols = colsInRow(row)
                if (cols.isNotEmpty()) {
                    val targetCol = nearestCol(cols, curCol)
                    return row to targetCol
                }
                row += dir
            }
        }
    }

    /**
     * Horizontal step within the current row. If a card exists in that direction, use it.
     * If at the row's edge, try the next/previous row at the same (or nearest) col.
     */
    private fun stepHorizontal(dir: Int): Pair<Int, Int>? {
        synchronized(_mutex) {
            if (_focusedRow < 0) return null
            val curRow = _focusedRow
            val curCol = _focusedCol

            // Same row first.
            val cols = colsInRow(curRow)
            val sameRowTarget = neighborCol(cols, curCol, dir)
            if (sameRowTarget != null) return curRow to sameRowTarget

            // At row edge: fall to the adjacent row. Use DOWN for RIGHT, UP for LEFT.
            val rowDir = if (dir > 0) 1 else -1
            val rows = allRows()
            var row = curRow + rowDir
            while (true) {
                if (row < 0 || row !in rows) return null
                val rowCols = colsInRow(row)
                if (rowCols.isEmpty()) {
                    row += rowDir
                    continue
                }
                val targetCol = neighborCol(rowCols, curCol, dir) ?: nearestCol(rowCols, curCol)
                if (targetCol != null) return row to targetCol
                row += rowDir
            }
        }
    }

    /** All row indices that have at least one registered card, sorted ascending. */
    private fun allRows(): List<Int> {
        val set = mutableSetOf<Int>()
        for ((row, _) in requestersKeySet()) set.add(row)
        return set.sorted()
    }

    /** Sorted list of columns that have a registered card in the given row. */
    private fun colsInRow(row: Int): List<Int> {
        val list = mutableListOf<Int>()
        for ((r, c) in requestersKeySet()) {
            if (r == row) list.add(c)
        }
        return list.sorted()
    }

    /** Nearest registered col in [cols] to [target], exact match wins. */
    private fun nearestCol(cols: List<Int>, target: Int): Int {
        return cols.minByOrNull { kotlin.math.abs(it - target) } ?: target
    }

    /**
     * The registered col in [cols] immediately in [dir] from [cur], or null if there is
     * none (i.e., we're at the row's edge in that direction).
     */
    private fun neighborCol(cols: List<Int>, cur: Int, dir: Int): Int? {
        if (dir > 0) {
            return cols.filter { it > cur }.minOrNull()
        }
        return cols.filter { it < cur }.maxOrNull()
    }

    private fun requestersKeySet(): List<Pair<Int, Int>> {
        return _requesters.keys.map { decode(it) }
    }

    private fun key(row: Int, col: Int): Long = (row.toLong() shl 32) or (col.toLong() and 0xFFFFFFFFL)

    private fun decode(k: Long): Pair<Int, Int> = ((k shr 32).toInt()) to (k and 0xFFFFFFFFL).toInt()

    // ---------- test accessors ----------

    internal fun focusedPosition(): Pair<Int, Int> = synchronized(_mutex) {
        _focusedRow to _focusedCol
    }

    internal fun registeredCount(): Int = synchronized(_mutex) { _requesters.size }
}
