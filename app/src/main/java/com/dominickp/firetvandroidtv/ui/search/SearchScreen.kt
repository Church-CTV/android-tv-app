package com.dominickp.firetvandroidtv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dominickp.firetvandroidtv.config.CardStyleConfig
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.components.ContentCard
import com.dominickp.firetvandroidtv.ui.focus.FocusManager
import com.dominickp.firetvandroidtv.ui.focus.FocusZone
import com.dominickp.firetvandroidtv.ui.focus.GridFocusController
import com.dominickp.firetvandroidtv.ui.theme.BrandColors
import com.dominickp.firetvandroidtv.util.Logger

/** Result cards per row in the results grid. */
private const val RESULTS_COLS = 3

/** Log tag for the search screen. */
private const val TAG = "SearchScreen"

/**
 * SearchScreen — full-screen search UI with debounced API calls.
 *
 * Layout (mirrors Roku's SearchScreen.xml: MiniKeyboard left, RowList right):
 *   - Top strip: full-width search field (live query + blinking cursor)
 *   - Left zone: compact 10×4 [TVKeyboard] + a small focus anchor
 *   - Right zone: results grid (rows of 3-up [ContentCard], same cards as home)
 *
 * Old layout stacked a full-width 3-row keyboard vertically and consumed
 * ~60% of screen height, leaving no room for results while typing.
 *
 * Focus model — single synchronous root handler (same pattern as LeftNav's
 * handleLeftNavDpad; GridFocusController's docblock explains why Compose
 * auto-traversal is not used for the grid):
 *   - Keyboard zone (focusedRow in 0..KEY_ROW_COUNT-1): ALL D-pad keys
 *     consumed by the root handler; keys append to the query.
 *   - Results zone (focusedRow < 0): D-pad routed through a per-screen
 *     [GridFocusController]; LEFT at the grid's left edge, UP at the top row,
 *     or DOWN at the last row returns focus to the keyboard.
 *
 * Remote Back exits via AppNavigation's onBack (not the D-pad).
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onCardSelected: (VideoItem) -> Unit = {},
    configLoader: ConfigLoader = remember { ConfigLoader() },
    viewModel: SearchViewModel =
        viewModel(
            factory = SearchViewModelFactory(configLoader),
        ),
    /**
     * Slot mode: called when the user presses LEFT at the keyboard's leftmost
     * column or the results grid's left edge. The shell (LeftNavScreen) uses it
     * to re-focus its sidebar on the "search" item. No-op when standalone.
     */
    onFocusSidebar: () -> Unit = {},
    /**
     * Slot mode: the shell's focus anchor, attached to the keyboard anchor so
     * pressing RIGHT from the sidebar re-enters search at the keyboard. When
     * null (standalone) a private anchor is created.
     */
    contentAnchor: FocusRequester? = null,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = FocusManager.getInstance()

    // Per-screen grid controller — a fresh instance so search cards never
    // collide with (or leak into) the home grid's singleton registrations.
    val gridController = remember { GridFocusController() }

    // Zone: focusedRow >= 0 → keyboard (row index into KEY_ROWS);
    // focusedRow < 0 → results grid.
    var focusedRow by remember { mutableIntStateOf(0) }
    var focusedCol by remember { mutableIntStateOf(0) }

    // Keyboard anchor doubles as the shell's re-entry anchor in slot mode.
    val keyboardAnchor = contentAnchor ?: remember { FocusRequester() }
    val cardStyle = remember(configLoader) { configLoader.getCardStyle() }

    // Initial focus: keyboard.
    LaunchedEffect(Unit) {
        gridController.resetPosition()
        focusManager.currentZone = FocusZone.SIDEBAR
        keyboardAnchor.requestFocus()
    }

    /**
     * Switch focus from the keyboard to the first result card.
     * Called when the user presses RIGHT at the keyboard's right edge and
     * results are displayed. No-op when there are no results.
     */
    fun gotoResults() {
        val items = (searchState as? SearchState.Results)?.items
        if (items.isNullOrEmpty()) return
        focusedRow = -1
        focusManager.currentZone = FocusZone.CONTENT
        gridController.focusCard(0, 0)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { keyEvent ->
                    handleSearchKey(
                        keyEvent = keyEvent,
                        focusedRow = focusedRow,
                        focusedCol = focusedCol,
                        onFocusedChange = { row, col ->
                            focusedRow = row
                            focusedCol = col
                        },
                        keyboardAnchor = keyboardAnchor,
                        gridController = gridController,
                        query = query,
                        viewModel = viewModel,
                        focusManager = focusManager,
                        onFocusSidebar = onFocusSidebar,
                        gotoResults = ::gotoResults,
                    )
                },
    ) {
        // ── Top strip: search field (Roku: SearchField at [100, 270]) ──
        SearchField(
            query = query,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 40.dp, top = 36.dp, bottom = 24.dp),
        )

        // ── Body: keyboard zone (left) + results zone (right) ──
        // Results "box" is deliberately larger than a hairline margin: the right
        // and bottom padding are reduced so the search-results region is ~50px
        // (25dp) wider and taller on 320dpi. The keyboard width is unchanged.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 40.dp, end = 15.dp, bottom = 12.dp),
        ) {
            // Left zone: focus anchor + compact keyboard.
            Column(modifier = Modifier.width(430.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .focusRequester(keyboardAnchor)
                            .focusable()
                            .onFocusChanged { state ->
                                // Focus on the anchor ⇒ keyboard zone owns D-pad.
                                if (state.isFocused && focusedRow < 0) {
                                    focusedRow = 0
                                    focusedCol = 0
                                }
                            }
                            .background(
                                if (focusedRow >= 0) {
                                    BrandColors.Focus.copy(alpha = 0.25f)
                                } else {
                                    Color.Transparent
                                },
                            ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TVKeyboard(
                    focusedRow = if (focusedRow >= 0) focusedRow else -1,
                    focusedCol = focusedCol,
                )
            }

            Spacer(modifier = Modifier.width(40.dp))

            // Right zone: results / loading / idle / error.
            SearchResultsZone(
                searchState = searchState,
                query = query,
                cardStyle = cardStyle,
                viewModel = viewModel,
                onCardSelected = onCardSelected,
                onCardFocused = { if (it != null) focusedRow = -1 },
                gridController = gridController,
            )
        }
    }
}

/**
 * Search results zone (right half of the SearchScreen body): idle / loading /
 * empty / error / results-grid. Extracted from [SearchScreen] to keep the
 * screen composable within the detekt LongMethod budget. Behavior is
 * unchanged; state flows back through [onCardFocused] (card gains focus ⇒
 * results zone owns the D-pad, i.e. focusedRow = -1).
 */
@Composable
private fun SearchResultsZone(
    searchState: SearchState,
    query: String,
    cardStyle: CardStyleConfig,
    viewModel: SearchViewModel,
    onCardSelected: (VideoItem) -> Unit,
    onCardFocused: (VideoItem?) -> Unit,
    gridController: GridFocusController,
) {
    when (searchState) {
        is SearchState.Idle -> {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Search your library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start typing — results appear as you go",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextSecondary,
                )
            }
        }

        is SearchState.Loading -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = BrandColors.Accent,
                    strokeWidth = 4.dp,
                )
            }
        }

        is SearchState.Results -> {
            val results = (searchState as SearchState.Results).items

            if (results.isEmpty()) {
                // No results — Roku: noResultsGroup centered.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Results Found",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BrandColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try searching with different keywords",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp),
                        )
                    }
                }
            } else {
                val rowRanges =
                    List((results.size + RESULTS_COLS - 1) / RESULTS_COLS) { i ->
                        i * RESULTS_COLS until
                            minOf(
                                results.size,
                                (i + 1) * RESULTS_COLS,
                            )
                    }
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(start = 11.5.dp), // grid shifted left 25px (12.5dp) from the box edge
                    // Top/bottom room so a focused card (scales to 1.05) is not
                    // clipped by the scroll viewport: its enlarged top/bottom
                    // overhang + 3dp focus border needs ~16dp of clearance.
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    items(rowRanges, key = { it.first }) { range ->
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            // Left/right clearance so the first/last card's focus scale (1.05)
                            // is not clipped by the LazyRow viewport: 10dp = 20px at device
                            // density 2.0. Without it the left edge of the first focused
                            // thumbnail gets cut off.
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(30.dp),
                        ) {
                            itemsIndexed(
                                results.subList(range.first, range.last + 1),
                                key = { _, video -> video.id },
                            ) { colIdx, video ->
                                ContentCard(
                                    videoItem = video,
                                    cardStyle = cardStyle,
                                    onCardSelected = { onCardSelected(video) },
                                    onCardFocused = onCardFocused,
                                    gridController = gridController,
                                    gridRow = range.first / RESULTS_COLS,
                                    gridCol = colIdx,
                                )
                            }
                        }
                    }
                }
            }
        }

        is SearchState.Error -> {
            val errorMsg = (searchState as SearchState.Error).message
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Search failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandColors.Error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { viewModel.onQueryChanged(query) },
                    ) {
                        Text("Retry", color = BrandColors.Focus)
                    }
                }
            }
        }
    }
}

/**
 * D-pad routing for SearchScreen. Owns ALL key handling at the root:
 *  - focusedRow >= 0  → keyboard zone (UP/DOWN/LEFT/RIGHT within grid,
 *    ENTER = key action or SEARCH commit)
 *  - focusedRow < 0   → results grid (GridFocusController; LEFT at the left
 *    edge / UP at the top / DOWN at the bottom return to the keyboard)
 *
 * Returns true if the key was consumed, false to pass through (e.g. Enter on
 * a focused result card activates it via the system's default behavior).
 */
@Suppress("LongParameterList")
private fun handleSearchKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    focusedRow: Int,
    focusedCol: Int,
    onFocusedChange: (Int, Int) -> Unit,
    keyboardAnchor: FocusRequester,
    gridController: GridFocusController,
    query: String,
    viewModel: SearchViewModel,
    focusManager: FocusManager,
    onFocusSidebar: () -> Unit,
    gotoResults: () -> Unit,
): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) return false

    return if (focusedRow >= 0) {
        handleKeyboardKey(
            keyEvent,
            focusedRow,
            focusedCol,
            onFocusedChange,
            query,
            viewModel,
            onFocusSidebar,
            gotoResults,
        )
    } else {
        handleResultsKey(keyEvent, gridController, onFocusedChange, keyboardAnchor, focusManager, onFocusSidebar)
    }
}

@Suppress("LongParameterList")
private fun handleKeyboardKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    focusedRow: Int,
    focusedCol: Int,
    onFocusedChange: (Int, Int) -> Unit,
    query: String,
    viewModel: SearchViewModel,
    onFocusSidebar: () -> Unit,
    gotoResults: () -> Unit,
): Boolean {
    val onSearchButton = focusedRow == KEY_ROW_COUNT - 1 && focusedCol >= SEARCH_COL

    return when (keyEvent.key) {
        Key.DirectionUp -> {
            if (focusedRow > 0) {
                onFocusedChange(focusedRow - 1, clampCol(focusedCol, focusedRow - 1))
            }
            true
        }

        Key.DirectionDown -> {
            if (focusedRow < KEY_ROW_COUNT - 1) {
                onFocusedChange(focusedRow + 1, clampCol(focusedCol, focusedRow + 1))
            }
            true
        }

        Key.DirectionLeft -> {
            if (onSearchButton) {
                onFocusedChange(focusedRow, SEARCH_COL - 1)
            } else if (focusedCol > 0) {
                onFocusedChange(focusedRow, focusedCol - 1)
            } else {
                // Leftmost column — return focus to the sidebar's "search" item.
                onFocusSidebar()
            }
            true
        }

        Key.DirectionRight -> {
            moveRight(onSearchButton, focusedRow, focusedCol, onFocusedChange, gotoResults)
            true
        }

        // The Fire TV remote OK button is KEYCODE_DPAD_CENTER → Compose
        // Key.DirectionCenter; a hardware keyboard's Enter is Key.Enter. BOTH
        // must activate — treating only Key.Enter makes the remote OK a no-op.
        Key.Enter, Key.DirectionCenter -> {
            activateKeyboardKey(
                onSearchButton = onSearchButton,
                focusedRow = focusedRow,
                focusedCol = focusedCol,
                query = query,
                viewModel = viewModel,
            )
            true
        }

        else -> false
    }
}

/**
 * RIGHT on the on-screen keyboard. The SEARCH CTA spans the last two cells, so
 * pressing RIGHT on it is a no-op. At the right edge of a normal row, focus
 * hands off to the results grid. Otherwise the column advances, skipping the
 * SEARCH CTA's second cell (you land on it only via col SEARCH_COL).
 */
private fun moveRight(
    onSearchButton: Boolean,
    focusedRow: Int,
    focusedCol: Int,
    onFocusedChange: (Int, Int) -> Unit,
    gotoResults: () -> Unit,
) {
    if (onSearchButton) return
    // Right edge of a normal row → hand focus to the results grid. The last
    // row's right edge is the SEARCH CTA itself, handled above, so only
    // rows 0..KEY_ROW_COUNT-2 use this path.
    val atRightEdge = focusedRow != KEY_ROW_COUNT - 1 && focusedCol >= KEY_COLS - 1
    if (atRightEdge) {
        gotoResults()
        return
    }
    val target = focusedCol + 1
    val landed =
        if (focusedRow == KEY_ROW_COUNT - 1 && target == SEARCH_COL + 1) {
            SEARCH_COL
        } else {
            target.coerceAtMost(KEY_COLS - 1)
        }
    onFocusedChange(focusedRow, landed)
}

/**
 * Activates a focused key on the on-screen keyboard (remote OK / Enter):
 * the SEARCH CTA submits the current query; a letter appends to it; SPACE
 * appends a space; BACKSPACE drops the last character; MIC is a no-op
 * placeholder kept for Roku layout parity.
 */
@Suppress("LongParameterList")
private fun activateKeyboardKey(
    onSearchButton: Boolean,
    focusedRow: Int,
    focusedCol: Int,
    query: String,
    viewModel: SearchViewModel,
) {
    if (onSearchButton) {
        if (query.isNotBlank()) {
            Logger.d(TAG, "OK on search button (query=\"$query\")")
            viewModel.onQueryChanged(query)
        }
        return
    }
    val label = KEY_ROWS[focusedRow].getOrNull(focusedCol) ?: ""
    when (label) {
        KEY_SPACE -> viewModel.onQueryChanged("$query ")
        KEY_MIC -> {
            // No-op placeholder: voice search is a remote-button feature on
            // Roku; keep the key for layout parity.
        }
        KEY_BACKSPACE -> if (query.isNotEmpty()) viewModel.onQueryChanged(query.dropLast(1))
        else -> {
            Logger.d(TAG, "OK on key '$label' (query=\"${query}$label\")")
            viewModel.onQueryChanged("$query$label")
        }
    }
}

private fun handleResultsKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    gridController: GridFocusController,
    onFocusedChange: (Int, Int) -> Unit,
    keyboardAnchor: FocusRequester,
    focusManager: FocusManager,
    @Suppress("UNUSED_PARAMETER") onFocusSidebar: () -> Unit,
): Boolean {
    fun backToKeyboard() {
        gridController.resetPosition()
        focusManager.currentZone = FocusZone.SIDEBAR
        onFocusedChange(0, 0)
        keyboardAnchor.requestFocus()
    }

    return when (keyEvent.key) {
        // LEFT at the results left edge returns to the keyboard (two-step left
        // cascade: results → keyboard → sidebar). From the keyboard's own
        // leftmost column, LEFT then focuses the sidebar (handleKeyboardKey).
        Key.DirectionLeft -> if (gridController.moveLeft()) true else backToKeyboard().let { true }

        Key.DirectionRight -> gridController.moveRight()

        Key.DirectionUp -> if (gridController.moveUp()) true else backToKeyboard().let { true }

        Key.DirectionDown -> if (gridController.moveDown()) true else backToKeyboard().let { true }

        // Enter passes through: the focused ContentCard is clickable, so the
        // system's default activation fires onCardSelected.
        else -> false
    }
}

/** Clamp a column index to the width of the target keyboard row. */
private fun clampCol(
    col: Int,
    row: Int,
): Int {
    val width = if (row == KEY_ROW_COUNT - 1) SEARCH_COL else KEY_COLS
    return col.coerceIn(0, width - 1)
}

/**
 * SearchViewModelFactory — Compose ViewModelFactory that injects ConfigLoader.
 */
class SearchViewModelFactory(
    private val configLoader: ConfigLoader,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(configLoader) as T
    }
}
