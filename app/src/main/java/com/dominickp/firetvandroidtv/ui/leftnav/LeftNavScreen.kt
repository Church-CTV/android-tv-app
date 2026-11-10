package com.dominickp.firetvandroidtv.ui.leftnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.config.CardStyleConfig
import com.dominickp.firetvandroidtv.config.NavItem
import com.dominickp.firetvandroidtv.config.PageType
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.LayoutConstants
import com.dominickp.firetvandroidtv.ui.components.MonthGroupedRows
import com.dominickp.firetvandroidtv.ui.components.OverhangHeader
import com.dominickp.firetvandroidtv.ui.components.FocusedInfoBar
import com.dominickp.firetvandroidtv.ui.focus.FocusManager
import com.dominickp.firetvandroidtv.ui.focus.FocusZone
import com.dominickp.firetvandroidtv.ui.focus.GridFocusController
import com.dominickp.firetvandroidtv.ui.focus.trackFocus
import com.dominickp.firetvandroidtv.util.Logger

private const val TAG = "LeftNavScreen"

/**
 * LeftNavScreen — the main content layout with left sidebar + right content area.
 *
 * Matches Roku LeftNavScreen.xml exactly:
 *   - Sidebar: 80dp wide, black background (Roku: width="80")
 *   - Sidebar title: channel name at top (Roku: translation="[10, 10]")
 *   - Nav items: 40x40 icons, centered vertically (Roku: 40x40 icons at [10, 10] in 60x60 containers)
 *   - Item spacing: 10dp between 60dp items (Roku: 50px between icon centers)
 *   - Focus: white bg + black icon (focused), transparent bg + white icon (unfocused)
 *
 * Focus behavior:
 *   - Initial focus: first card (0,0) in the grid — nav has NO focus on boot
 *   - Left from first card: routes focus to Home nav item
 *   - Nav items: white icon on transparent bg (unfocused), black icon on white bg (focused)
 *   - Up/down in sidebar: switches focused nav item (stays within sidebar)
 *   - Right from sidebar: transfers focus back to grid first card (0,0)
 *   - Nav items do NOT change route on focus — only on select (OK/Enter)
 *
 * Focus zone tracking:
 *   - FocusManager.currentZone = FocusZone.CONTENT on boot
 *   - FocusManager.currentZone = FocusZone.SIDEBAR when nav item focused
 *   - Zone updates on every focus transfer
 *
 * Focus gate pattern: a hidden no-op FocusRequester clears focus from all groups
 * before transferring to the target element. This prevents dual-focus bugs where
 * both sidebar and grid show focus indicators simultaneously.
 */
@androidx.media3.common.util.UnstableApi
@Composable
fun LeftNavScreen(
    navItems: List<NavItem>,
    groupedVideos: Map<String, List<VideoItem>>,
    modifier: Modifier = Modifier,
    onCardSelected: (VideoItem) -> Unit = {},
    onNavRouteChange: (String) -> Unit = {},
    config: AppConfig? = null,
    /**
     * The page currently displayed in the content slot. Drives where the
     * sidebar focuses when the user presses LEFT from the content's left edge:
     * the sidebar item whose `page` matches this string is highlighted (the
     * "you are here" affordance). Defaults to "home".
     */
    page: String = "home",
    /**
     * Optional content slot rendered in the right area instead of the home
     * grid. When non-null this screen is a *shell*: the sidebar stays, the
     * slot renders its own screen (search/settings), and the root key handler
     * delegates all content-mode keys to the slot (LEFT at the slot's left
     * edge calls `onFocusSidebar` to re-focus the sidebar on [page]).
     *
     * The slot receives two things: `onFocusSidebar` (call it when the user
     * presses LEFT at the slot's leftmost element) and `contentAnchor` (a
     * FocusRequester the slot attaches to its initial-focus element so the
     * shell can hand focus back to the slot on RIGHT from the sidebar).
     */
    content: (@Composable (onFocusSidebar: () -> Unit, contentAnchor: FocusRequester) -> Unit)? = null,
) {
    val focusManager = FocusManager.getInstance()
    val gridController = GridFocusController.getInstance()

    // Selected index: which nav item is active for content routing
    var selectedIndex by remember { mutableIntStateOf(0) }
    val effectiveNavItems = if (navItems.isEmpty()) defaultNavItems else navItems

    // Focused index: which nav item currently has keyboard focus
    // Starts at -1 (no nav item focused) — focus goes to grid on boot
    var focusedIndex by remember { mutableIntStateOf(-1) }

    // Per-item FocusRequesters — one for each nav item
    val itemFocusRequesters =
        remember {
            effectiveNavItems.map { _ -> FocusRequester() }.toMutableList()
        }

    // FocusRequester for the first card in the grid — used to transfer focus from sidebar
    val firstCardFocusRequester = remember { FocusRequester() }

    // Focus gate: hidden no-op element used to clear all focus before cross-group transfer.
    val focusGateRequester = remember { FocusRequester() }

    // ── Content-slot (shell) support ─────────────────────────────────────────
    // When `content` is provided this screen hosts another screen (search/
    // settings) in the right area instead of the home grid. The sidebar stays,
    // so the user can press LEFT from the content's left edge to return here
    // (highlighting the current page), move up/down to switch pages, or OK to
    // re-enter. `contentAnchor` is the slot's initial-focus element — the
    // sidebar's RIGHT targets it. `focusPageInSidebar()` focuses the sidebar
    // item whose `page` matches the active `page` (the "you are here" affordance).
    val inSlotMode = content != null
    val contentAnchor = remember { FocusRequester() }
    val pageFocusIndex = effectiveNavItems.indexOfFirst { it.page == page }.let {
        if (it < 0) 0 else it
    }

    fun focusPageInSidebar() {
        focusManager.currentZone = FocusZone.SIDEBAR
        focusedIndex = pageFocusIndex
        itemFocusRequesters[pageFocusIndex].requestFocus()
        focusManager.lastFocusedItemId = "nav_item_$pageFocusIndex"
    }

    // Resolve the active item's page target
    val activeNav = effectiveNavItems.getOrNull(selectedIndex) ?: defaultNavItems.first()

    // Update current screen in FocusManager
    LaunchedEffect(activeNav.page) {
        focusManager.currentScreen = activeNav.page
    }

    // Currently focused video — drives the info bar above the grid
    var focusedVideo by remember { mutableStateOf<VideoItem?>(null) }

    // Channel name for the overhang header
    val channelName = config?.channel?.name ?: "Navigation"

    // Shared handler: D-pad routing for both sidebar and content (see LeftNavDpadRouter).
    val onDpadKey: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
        // Slot mode (search/settings hosted as a shell): the slot's screen owns
        // its own root onPreviewKeyEvent, which fires FIRST (inner preview) and
        // handles the content's D-pad + its LEFT-at-left-edge → onFocusSidebar
        // callback. Here we only own the SIDEBAR zone and the sidebar's RIGHT
        // (→ hand focus back to the slot's anchor). If the slot is focused
        // (focusedIndex < 0) we must NOT consume, so the slot's handler runs.
        if (inSlotMode && focusedIndex !in effectiveNavItems.indices) {
            false
        } else {
            handleLeftNavDpad(
                keyEvent = keyEvent,
                focusedIndex = focusedIndex,
                onFocusedIndexChange = { focusedIndex = it },
                effectiveNavItems = effectiveNavItems,
                itemFocusRequesters = itemFocusRequesters,
                firstCardFocusRequester = if (inSlotMode) contentAnchor else firstCardFocusRequester,
                gridController = gridController,
                focusManager = focusManager,
                onSelectNav = { index ->
                    selectedIndex = index
                    val item = effectiveNavItems[index]
                    onNavRouteChange(item.page)
                    focusManager.currentScreen = item.page
                },
            )
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { onDpadKey(it) },
    ) {
        // ===== FOCUS GATE — hidden element to clear focus =====
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(1.dp)
                    .focusRequester(focusGateRequester),
        )

        // ===== LEFT SIDEBAR (full height, extending to top of screen) =====
        // Sidebar spans the entire screen height. The right content area contains
        // the overhang header at top and content below, so the sidebar visually
        // extends from top to bottom of the screen.
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            NavigationSidebar(
                items = effectiveNavItems,
                focusedIndex = focusedIndex,
                onFocusedIndexChange = { newIndex ->
                    focusedIndex = newIndex
                },
                onSelectNavIndex = { index ->
                    selectedIndex = index
                    val item = effectiveNavItems[index]
                    onNavRouteChange(item.page)
                    focusManager.currentScreen = item.page
                },
                focusRequesters = itemFocusRequesters,
                modifier = Modifier.width(NAV_BAR_WIDTH).fillMaxHeight(),
            )

            // ===== RIGHT CONTENT AREA (overhang + content, or a hosted slot) =====
            Column(modifier = Modifier.weight(1f)) {
                if (content != null) {
                    // Shell mode: host another screen (search/settings) in this
                    // area. It fills the whole column and owns its own header.
                    // LEFT at its left edge calls focusPageInSidebar() to return
                    // focus to this sidebar (highlighting the current page).
                    content({ focusPageInSidebar() }, contentAnchor)
                } else {
                    // Overhang bar (spans right content area width)
                    OverhangHeader(
                        channelName = channelName,
                        showClock = config?.features?.overhangClock ?: false,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Content rows below overhang
                    val target = activeNav.page.ifBlank { "home" }
                    if (target == "home" || target == "videos") {
                        if (groupedVideos.isNotEmpty()) {
                            FocusedInfoBar(video = focusedVideo)
                            MonthGroupedRows(
                                groupedVideos = groupedVideos,
                                onCardSelected = onCardSelected,
                                onCardFocused = { video ->
                                    if (video != null) focusedVideo = video
                                },
                                cardStyle = config?.cardStyle ?: CardStyleConfig(),
                                firstCardFocusRequester = firstCardFocusRequester,
                                gridController = gridController,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            EmptyState("Loading feed...")
                        }
                    }
                }
            }
        }
    }
}

/**
 * D-pad routing for LeftNavScreen. Owns ALL key handling at the root:
 *  - focusedIndex >= 0  → sidebar item has focus (ENTER/UP/DOWN/RIGHT)
 *  - focusedIndex < 0   → content (grid) has focus (LEFT → sidebar,
 *    DOWN/UP/LEFT/RIGHT → GridFocusController)
 *
 * Returns true if the key was consumed, false to pass through.
 */
@Suppress("LongParameterList")
private fun handleLeftNavDpad(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    focusedIndex: Int,
    onFocusedIndexChange: (Int) -> Unit,
    effectiveNavItems: List<NavItem>,
    itemFocusRequesters: MutableList<FocusRequester>,
    firstCardFocusRequester: FocusRequester,
    gridController: GridFocusController,
    focusManager: FocusManager,
    onSelectNav: (Int) -> Unit,
): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) return false

    val inSidebar = focusedIndex in effectiveNavItems.indices
    return if (inSidebar) {
        handleSidebarKey(
            keyEvent,
            focusedIndex,
            onFocusedIndexChange,
            effectiveNavItems,
            itemFocusRequesters,
            firstCardFocusRequester,
            focusManager,
            onSelectNav,
        )
    } else {
        handleContentKey(keyEvent, gridController, focusManager, onFocusedIndexChange, itemFocusRequesters)
    }
}

@Suppress("LongParameterList")
private fun handleSidebarKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    focusedIndex: Int,
    onFocusedIndexChange: (Int) -> Unit,
    effectiveNavItems: List<NavItem>,
    itemFocusRequesters: MutableList<FocusRequester>,
    firstCardFocusRequester: FocusRequester,
    focusManager: FocusManager,
    onSelectNav: (Int) -> Unit,
): Boolean {
    return when (keyEvent.key) {
        // Remote OK = Key.DirectionCenter; hardware Enter = Key.Enter. Both select.
        Key.Enter, Key.DirectionCenter -> {
            val item = effectiveNavItems[focusedIndex]
            Logger.d(TAG, "ENTER on sidebar item: ${item.page}")
            onSelectNav(focusedIndex)
            true
        }

        Key.DirectionUp -> {
            if (focusedIndex > 0) {
                onFocusedIndexChange(focusedIndex - 1)
                itemFocusRequesters[focusedIndex - 1].requestFocus()
                focusManager.lastFocusedItemId = "nav_item_${focusedIndex - 1}"
                focusManager.currentZone = FocusZone.SIDEBAR
            }
            true
        }

        Key.DirectionDown -> {
            if (focusedIndex < effectiveNavItems.size - 1) {
                onFocusedIndexChange(focusedIndex + 1)
                itemFocusRequesters[focusedIndex + 1].requestFocus()
                focusManager.lastFocusedItemId = "nav_item_${focusedIndex + 1}"
                focusManager.currentZone = FocusZone.SIDEBAR
            }
            true
        }

        Key.DirectionRight -> {
            Logger.d(TAG, "RIGHT from sidebar: focus to content")
            focusManager.currentZone = FocusZone.CONTENT
            onFocusedIndexChange(-1)
            firstCardFocusRequester.requestFocus()
            true
        }

        else -> false
    }
}

private fun handleContentKey(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    gridController: GridFocusController,
    focusManager: FocusManager,
    onFocusedIndexChange: (Int) -> Unit,
    itemFocusRequesters: MutableList<FocusRequester>,
): Boolean {
    return when (keyEvent.key) {
        Key.DirectionDown -> moveAndMark(gridController.moveDown(), focusManager)

        Key.DirectionUp -> moveAndMark(gridController.moveUp(), focusManager)

        Key.DirectionRight -> moveAndMark(gridController.moveRight(), focusManager)

        Key.DirectionLeft -> {
            // Within-row left move; if we're at col 0, route to sidebar.
            if (gridController.moveLeft()) {
                focusManager.currentZone = FocusZone.CONTENT
                true
            } else {
                // At left edge -> sidebar home.
                focusManager.currentZone = FocusZone.SIDEBAR
                onFocusedIndexChange(0)
                itemFocusRequesters[0].requestFocus()
                true
            }
        }

        else -> false
    }
}

private fun moveAndMark(moved: Boolean, focusManager: FocusManager): Boolean {
    if (moved) focusManager.currentZone = FocusZone.CONTENT
    return moved
}

// Layout constants from LayoutConstants (density-corrected: Roku px / 2)
// Roku LeftNavScreen width="80" px → 40dp at density 320
private val NAV_BAR_WIDTH = LayoutConstants.NAV_BAR_WIDTH

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

/**
 * NavigationSidebar — the left panel with navigation items.
 *
 * Matches Roku LeftNavScreen.xml:
 *   - Width: 80dp (Roku: width="80")
 *   - Background: black (Roku: color="0x000000")
 *   - Title: channel name at top (Roku: sidebarTitle at translation="[10, 10]")
 *   - Icons: 40x40dp centered (Roku: 40x40 at [10, 10] in 60x60 container)
 *   - Items: vertically centered, 10dp spacing (Roku: 50px between icon centers)
 *   - Focus highlight: white bg + black icon (Roku: opacity 1.0 on focus)
 *
 * Focus behavior:
 *   - Items start unfocusable — grid wins initial focus
 *   - Left from first card routes focus to sidebar (via navFocusKey)
 *   - Up/down moves focus between items (stays within sidebar)
 *   - Right arrow transfers focus back to grid first card
 *   - Route change happens only on select (Enter/OK), not on focus entry
 *
 * Uses LazyColumn with itemsIndexed for proper Compose semantics.
 * Each item has its own FocusRequester for manual D-pad focus control.
 */
@Composable
private fun NavigationSidebar(
    items: List<NavItem>,
    focusedIndex: Int,
    modifier: Modifier = Modifier,
    onFocusedIndexChange: (Int) -> Unit = {},
    onSelectNavIndex: (Int) -> Unit = {},
    focusRequesters: MutableList<FocusRequester> = mutableListOf(),
    focusManager: FocusManager = FocusManager.getInstance(),
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .background(Color.Black),
    ) {
        // No sidebar title here — channel name renders in OverhangHeader (logo + text).
        // Roku's 180px sidebarTitle overflowed the 80px bar; on Android we consolidate it.

        // Spacer to center nav items vertically (matching Roku: items at y≈438-598, centered on 1080p)
        Spacer(modifier = Modifier.weight(1f))

        // Nav items — matching Roku layout: 60px tall items, 50px between icon centers
        items.forEachIndexed { index, navItem ->
            val isFocused = index == focusedIndex
            val itemFocusId = "nav_item_$index"

            // Each nav item gets its own FocusRequester for manual D-pad focus control
            val requester =
                if (index < focusRequesters.size) {
                    focusRequesters[index]
                } else {
                    FocusRequester().also { focusRequesters.add(it) }
                }

            // Nav item container (matching Roku: 60x60 container)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(LayoutConstants.NAV_ITEM_HEIGHT)
                        .padding(horizontal = 5.dp)
                        .trackFocus(itemFocusId)
                        .focusRequester(requester)
                        .onFocusChanged { focusState ->
                            val hasFocus = focusState.isFocused || focusState.hasFocus
                            if (hasFocus && index != focusedIndex) {
                                Logger.d(TAG, "Sidebar item focused: index=$index, id=$itemFocusId")
                                focusManager.lastFocusedItemId = itemFocusId
                                focusManager.currentZone = FocusZone.SIDEBAR
                                onFocusedIndexChange(index)
                            }
                        }
                        .background(
                            if (isFocused) Color.White else Color.Transparent,
                        )
                        .focusProperties { canFocus = true }
                        .clickable(
                            onClick = {
                                onSelectNavIndex(index)
                                requester.requestFocus()
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                // Icon (matching Roku: 40x40 at [10, 10] in 60x60 container)
                Icon(
                    imageVector = pageToIcon(navItem.page),
                    contentDescription = navItem.label,
                    tint = if (isFocused) Color.Black else Color.White,
                    modifier = Modifier.size(LayoutConstants.NAV_ICON_SIZE),
                )
            }

            // Spacing between items (matching Roku: 50px between icon centers, items are 60px)
            if (index < items.size - 1) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Bottom spacer to center items vertically
        Spacer(modifier = Modifier.weight(1f))
    }
}

fun pageToIcon(page: String): ImageVector = PageType.fromStringToIcon(page)

private val defaultNavItems: List<NavItem> =
    listOf(
        NavItem(id = "home", label = "Home", page = "home", visible = true),
        NavItem(id = "search", label = "Search", page = "search", visible = true),
        NavItem(id = "settings", label = "Settings", page = "settings", visible = true),
    )
