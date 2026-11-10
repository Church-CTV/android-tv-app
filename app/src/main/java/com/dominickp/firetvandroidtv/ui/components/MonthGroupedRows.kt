package com.dominickp.firetvandroidtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.dominickp.firetvandroidtv.config.CardStyleConfig
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.focus.GridFocusController
import com.dominickp.firetvandroidtv.ui.theme.BrandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Throttle interval (ms) for the scroll-position polling loop in
 * [MonthGroupedRows]. Named constant to satisfy Detekt's MagicNumber rule.
 */
private const val SCROLL_POLL_INTERVAL_MS = 150L

/**
 * Parse a month label like "July 2026" into a sortable key (yyyy-MM).
 * Uses java.text.SimpleDateFormat (API 1+) instead of java.time (API 26+)
 * to maintain minSdk 25 compatibility.
 * Returns null on failure.
 *
 * Thread-safe: creates a fresh SimpleDateFormat per call (no shared mutable state).
 */
internal fun parseMonthKey(label: String): String? {
    val trimmed = label.trim()
    return try {
        val parser = SimpleDateFormat("MMMM yyyy", Locale.US)
        val date: Date = parser.parse(trimmed) ?: return null
        val cal = Calendar.getInstance(Locale.US).apply { time = date }
        String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    } catch (_: Exception) {
        null
    }
}

/**
 * MonthGroupedRows — groups videos by month and renders each group as a horizontal row.
 *
 * Roku RowList (GridScreen.xml) specs:
 *   - translation="[150,300]" — content area starts at x=150, y=300
 *   - rowItemSpacing="[[30,0]]" — 30px between cards
 *   - rowLabelOffset="[[75,30]]" — label offset from row
 *   - showRowLabel="[true]" — month labels visible
 *   - rowFocusAnimationStyle="floatingfocus"
 *
 * Features:
 *   - Lazy pagination: renders first N months, loads more on scroll
 *   - Prevents UI freeze with large feeds (57+ groups)
 *   - Smooth scroll with D-pad support via LazyRow
 *
 * @param groupedVideos Map of month label -> video list
 * @param onCardSelected Callback when a card is selected
 * @param modifier Modifier for the composable
 * @param cardStyle Card style config from app_config.json
 * @param initialVisibleMonths Number of months to render initially (default: 1)
 * @param loadMoreChunk Number of additional months to load per scroll (default: 3)
 * @param onLeftFromFirstCard Callback when left is pressed on the first card (LIVE NOW row)
 */
@Composable
fun MonthGroupedRows(
    groupedVideos: Map<String, List<VideoItem>>,
    modifier: Modifier = Modifier,
    onCardSelected: (VideoItem) -> Unit = {},
    cardStyle: CardStyleConfig = CardStyleConfig(),
    initialVisibleMonths: Int = 3,
    loadMoreChunk: Int = 3,
    firstCardFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onLeftFromFirstCard: (() -> Unit)? = null,
    onCardFocused: ((VideoItem?) -> Unit)? = null,
    gridController: GridFocusController? = null,
) {
    // "LIVE NOW" always first (live stream), then remaining groups sorted by newest month first
    val liveVideos = groupedVideos["LIVE NOW"]

    // Pre-compute sorted months with simple string keys for fast comparison
    val sortedMonths =
        remember(groupedVideos) {
            val entries =
                groupedVideos
                    .filterKeys { it != "LIVE NOW" }
                    .toList()

            // Parse sortable key once per entry, sort by string comparison (fast, no allocations)
            val sorted =
                entries.sortedWith { (labelA, _), (labelB, _) ->
                    val keyA = parseMonthKey(labelA) ?: ""
                    val keyB = parseMonthKey(labelB) ?: ""
                    keyB.compareTo(keyA) // newest first (descending)
                }

            sorted
        }

    val listState = rememberLazyListState()
    var visibleCount by remember { mutableIntStateOf(initialVisibleMonths) }

    // Load more months when user scrolls near the end.
    // Uses a single LaunchedEffect that monitors scroll position and loads more
    // when the user approaches the end of visible content. Stops when all months
    // are loaded to avoid unnecessary recompositions.
    LaunchedEffect(listState) {
        while (isActive) {
            val info = listState.layoutInfo
            val totalItems = info.totalItemsCount
            val visibleItems = info.visibleItemsInfo
            val lastVisibleIndex = if (visibleItems.isNotEmpty()) visibleItems.last().index else -1

            if (totalItems > visibleCount && lastVisibleIndex >= visibleCount - 2) {
                visibleCount = minOf(visibleCount + loadMoreChunk, sortedMonths.size)
            }

            delay(SCROLL_POLL_INTERVAL_MS) // throttle polling to reduce recomposition overhead
        }
    }

    val visibleMonths = sortedMonths.take(visibleCount)

    // Grid shift (user-tuned): UP 100px / LEFT 25px = 50dp / 12.5dp at 320dpi.
    // Precompute the px here because Modifier.layout is NOT a composable context.
    val shiftLeftPx = with(LocalDensity.current) { (-12.5).dp.toPx() }
    val shiftUpPx = with(LocalDensity.current) { (-50).dp.toPx() }
    val growRightPx = with(LocalDensity.current) { 12.5.dp.toPx() }
    val growBottomPx = with(LocalDensity.current) { 50.dp.toPx() }

    LazyColumn(
        state = listState,
        // User-tuned: the whole grid is shifted UP 100px and LEFT 25px (50dp / 12.5dp at
        // 320dpi). A plain .offset would move the LazyColumn's CLIP window up-left too,
        // so the window's right edge lands 25px short and its bottom 100px short — cropping
        // the row's right side and bottom row (and a focused card's 1.05x scale). Instead
        // we GROW the clip window by the shift amount and place it up-left, so the visible
        // window still spans the full parent (nothing cropped on right/bottom) while the
        // grid content sits 100px up / 25px left.
        modifier =
            modifier.then(
                Modifier
                    .layout { m, constraints ->
                        // Node reports its ORIGINAL size (respects weight/fillMaxSize,
                        // and a plain layout node does not clip its children).
                        val cw = constraints.maxWidth.coerceAtLeast(0).toInt()
                        val ch = constraints.maxHeight.coerceAtLeast(0).toInt()
                        // Measure the LazyColumn child LARGER than the node so its own
                        // clip region extends past the node's top-left. Placed at
                        // (-25px, -100px), the child's clip spans exactly the full parent
                        // area (bottom-right on screen) — so the up-left grid shift is not
                        // cropped on the right or bottom.
                        val childW = cw + growRightPx.toInt()
                        val childH = ch + growBottomPx.toInt()
                        val p =
                            m.measure(
                                Constraints(
                                    minWidth = 0,
                                    maxWidth = childW,
                                    minHeight = 0,
                                    maxHeight = childH,
                                ),
                            )
                        layout(cw, ch) {
                            p.place(shiftLeftPx.toInt(), shiftUpPx.toInt())
                        }
                    },
            ),
        contentPadding =
            PaddingValues(
                top = 40.dp, // room for FocusedInfoBar above first row
                bottom = 16.dp,
                start = 75.dp, // Roku RowList x=150 → halved for density 320
                end = 15.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(25.dp), // Roku: 50px between rows → halved
        horizontalAlignment = Alignment.Start,
    ) {
        // Live stream row first
        liveVideos?.let { videos ->
            item {
                MonthRow(
                    month = "LIVE NOW",
                    videos = videos,
                    onCardSelected = onCardSelected,
                    onCardFocused = onCardFocused,
                    cardStyle = cardStyle,
                    isFirstCardInRow = true,
                    onLeftFromFirstCard = onLeftFromFirstCard,
                    firstCardFocusRequester = firstCardFocusRequester,
                    gridController = gridController,
                    rowIndex = 0,
                )
            }
        }

        // Monthly rows (lazy) — first card of first month row gets initial focus
        itemsIndexed(visibleMonths, key = { _, entry -> entry.first }) { monthIndex, (month, videos) ->
            val isTopMonth = monthIndex == 0
            val rowIndex = if (liveVideos != null) 1 + monthIndex else monthIndex
            MonthRow(
                month = month,
                videos = videos,
                onCardSelected = onCardSelected,
                onCardFocused = onCardFocused,
                cardStyle = cardStyle,
                isFirstCardInRow = isTopMonth && liveVideos.isNullOrEmpty(),
                onLeftFromFirstCard =
                    if (isTopMonth && liveVideos.isNullOrEmpty()) {
                        onLeftFromFirstCard
                    } else {
                        null
                    },
                firstCardFocusRequester =
                    if (isTopMonth && liveVideos.isNullOrEmpty()) {
                        firstCardFocusRequester
                    } else {
                        null
                    },
                gridController = gridController,
                rowIndex = rowIndex,
            )
        }

        // Loading indicator when more months are available
        if (visibleCount < sortedMonths.size) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * MonthRow — a single month's header + horizontally scrollable row of cards.
 * Mirrors Roku's RowList row behavior.
 * Uses LazyRow for proper D-pad horizontal scrolling on Fire TV.
 *
 * Roku rowItemSpacing: 30px between cards.
 */
@Composable
fun MonthRow(
    month: String,
    videos: List<VideoItem>,
    modifier: Modifier = Modifier,
    onCardSelected: (VideoItem) -> Unit = {},
    onCardFocused: ((VideoItem?) -> Unit)? = null,
    cardStyle: CardStyleConfig = CardStyleConfig(),
    isFirstCardInRow: Boolean = false,
    onLeftFromFirstCard: (() -> Unit)? = null,
    firstCardFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    gridController: GridFocusController? = null,
    rowIndex: Int = -1,
) {
    Column(modifier = modifier) {
        // Month header — matches Roku rowLabelOffset "[75,30]" and SmallBoldSystemFont
        Text(
            text = month,
            style = MaterialTheme.typography.titleMedium,
            color = BrandColors.TextSecondary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 30.dp),
        )

        // Horizontally scrollable row of cards using LazyRow
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                ContentCard(
                    videoItem = video,
                    cardStyle = cardStyle,
                    onCardSelected = onCardSelected,
                    onCardFocused = onCardFocused,
                    isFirstCard = isFirstCardInRow && index == 0,
                    onLeftFromFirstCard = if (isFirstCardInRow && index == 0) onLeftFromFirstCard else null,
                    firstCardFocusRequester = if (isFirstCardInRow && index == 0) firstCardFocusRequester else null,
                    gridController = gridController,
                    gridRow = rowIndex,
                    gridCol = index,
                )
            }
        }
    }
}
