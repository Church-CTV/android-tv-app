package com.dominickp.firetvandroidtv.ui.settings

/**
 * Pure, Compose-free model of the Settings screen's D-pad focus layout.
 *
 * The screen is a single vertical list where each focusable control is a
 * (row, col) cell. A "row" may hold several cols (a segmented control) or a
 * single col (a toggle / button). A row whose `cols` is 0 is non-focusable
 * (the About panel) and the D-pad must skip it. Item index in the LazyColumn
 * equals the row index, so "scroll focused row into view" is a 1:1 mapping.
 *
 * Keeping this logic here (rather than inside the composable) makes the
 * navigation deterministic and unit-testable without the Compose runtime.
 */
internal data class SettingsRow(val id: String, val cols: Int, val header: String? = null)

internal object SettingsLayout {

    const val BACK = "back"
    const val AUTOPLAY = "autoplay"
    const val KEEP_AWAKE = "keep_awake"
    const val GIVE = "give"
    const val ABOUT = "about"

    /**
     * Build the row model. The settings surface is intentionally minimal
     * (2026-09-11): Back, Autoplay, optional Donate CTA, and the About panel.
     * Quality, Show Descriptions, Language, Subtitles, Layout, and the whole
     * Features section (Search/Analytics/Crash) were removed at the user's
     * request. [hasGive] toggles the conditional "Support" section.
     */
    fun rows(hasGive: Boolean): List<SettingsRow> = buildList {
        add(SettingsRow(BACK, 1))
        add(SettingsRow(AUTOPLAY, 1, header = "Playback"))
        add(SettingsRow(KEEP_AWAKE, 1, header = "Display"))
        if (hasGive) add(SettingsRow(GIVE, 1))
        add(SettingsRow(ABOUT, 0, header = "About"))
    }

    fun colCount(rows: List<SettingsRow>, row: Int): Int =
        rows.getOrNull(row)?.cols ?: 0

    /**
     * Nearest focusable row in [dir] (-1 up, +1 down) from [from], skipping
     * non-focusable rows. Returns -1 when there is none (D-pad at a boundary).
     */
    fun nextFocusRow(rows: List<SettingsRow>, from: Int, dir: Int): Int {
        var i = from
        while (true) {
            i += dir
            if (i !in rows.indices) return -1
            if (colCount(rows, i) > 0) return i
        }
    }

    /** Clamp [col] to the target row's width, so crossing into a narrower row is safe. */
    fun clampCol(col: Int, rows: List<SettingsRow>, row: Int): Int =
        if (row in rows.indices) col.coerceIn(0, colCount(rows, row) - 1) else 0
}
