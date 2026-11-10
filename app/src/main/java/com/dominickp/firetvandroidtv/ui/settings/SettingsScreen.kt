package com.dominickp.firetvandroidtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.ui.theme.BrandColors

/**
 * SettingsScreen — minimal app settings with DataStore persistence.
 *
 * Scope (2026-09-11, user-requested reduction): Back, an **Autoplay** toggle,
 * an optional gold **Donate** CTA, and the **About** panel (channel name /
 * description / copyright). Quality, Show Descriptions, Language & Audio,
 * Subtitles, Layout, and the entire Features section (Search / Analytics /
 * Crash Reporting) are intentionally removed from this screen.
 *
 * Focus routing: single synchronous root `onPreviewKeyEvent` handler (the
 * proven Fire TV pattern). OK on the remote is `KEYCODE_DPAD_CENTER` →
 * `Key.DirectionCenter`; Enter on a hardware keyboard is `Key.Enter`. BOTH
 * must activate, so the handler treats them identically.
 *
 * Contract: [config], [viewModel], [onBack], [onOpenUrl].
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    config: AppConfig? = null,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit = { /* no-op */ },
    /**
     * Slot mode: called when the user presses LEFT from any settings row
     * (all rows are single-cell). The shell re-focuses its sidebar on the
     * "settings" item. No-op when standalone.
     */
    onFocusSidebar: () -> Unit = {},
    /**
     * Slot mode: the shell's focus anchor, attached to the first focusable row
     * (Autoplay) so pressing RIGHT from the sidebar re-enters settings there.
     * When null (standalone) a private requester is created.
     */
    contentAnchor: FocusRequester? = null,
) {
    // Channel info from config (About panel only — no server/feed/endpoint data).
    val channelName = config?.channel?.name ?: "FireTV App"
    val channelDescription = config?.channel?.description ?: ""
    val channelCopyright = config?.channel?.copyright ?: ""
    val donationUrl = config?.giving?.donationUrl.orEmpty()

    val autoplay by viewModel.autoplay.collectAsStateWithLifecycle(true)
    val keepAwake by viewModel.keepScreenAwake.collectAsStateWithLifecycle(true)

    val rows = remember(donationUrl) {
        SettingsLayout.rows(hasGive = donationUrl.isNotBlank())
    }

    val listState = rememberLazyListState()
    // Row 1 = Autoplay (row 0 is the Back pill). Land focus on it on entry.
    var focusedRow by remember { mutableIntStateOf(1) }
    var focusedCol by remember { mutableIntStateOf(0) }
    val requesters = remember {
        mutableStateMapOf<Pair<Int, Int>, FocusRequester>().apply {
            // In slot mode the shell's anchor IS the Autoplay cell (row 1), so
            // RIGHT from the sidebar re-enters settings on the first row.
            contentAnchor?.let { this[1 to 0] = it }
        }
    }

    fun focusCell(row: Int, col: Int) {
        focusedRow = row
        focusedCol = col
        requesters[row to col]?.requestFocus()
    }

    fun activate(row: Int) {
        when (rows[row].id) {
            "back" -> onBack()
            "autoplay" -> viewModel.saveAutoplay(!autoplay)
            "keep_awake" -> viewModel.saveKeepScreenAwake(!keepAwake)
            "give" -> onOpenUrl(donationUrl)
        }
    }

    LaunchedEffect(focusedRow, focusedCol) {
        if (focusedRow in rows.indices) listState.animateScrollToItem(focusedRow)
    }
    LaunchedEffect(Unit) {
        requesters[1 to 0]?.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandColors.Background)
            .onPreviewKeyEvent { ev ->
                handleSettingsKey(
                    ev,
                    rows,
                    focusedRow,
                    focusedCol,
                    ::focusCell,
                    ::activate,
                    onFocusSidebar,
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp, end = 80.dp, top = 56.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = BrandColors.Focus,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 80.dp, end = 80.dp, top = 12.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(rows) { rowIndex, row ->
                SectionFrame(header = row.header) {
                    when (row.id) {
                        "back" -> BackPill(rowIndex, requesters, onBack)
                        "autoplay" -> ToggleRow(
                            "Autoplay", "Automatically play the next video", autoplay, rowIndex, requesters,
                        ) { viewModel.saveAutoplay(!autoplay) }
                        "keep_awake" -> ToggleRow(
                            "Keep screen awake",
                            "Keep the TV on while the app is open",
                            keepAwake, rowIndex, requesters,
                        ) { viewModel.saveKeepScreenAwake(!keepAwake) }
                        "give" -> GiveCard(rowIndex, requesters) { onOpenUrl(donationUrl) }
                        "about" -> AboutPanel(
                            channelName, channelDescription, channelCopyright,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Synchronous D-pad/OK routing for the settings list. Returns true when the
 * key is consumed. OK on the remote (KEYCODE_DPAD_CENTER) and Enter on a
 * hardware keyboard both activate the focused row. Top-level so its own
 * complexity is counted separately from the [SettingsScreen] composable.
 */
@Suppress("LongParameterList")
private fun handleSettingsKey(
    ev: androidx.compose.ui.input.key.KeyEvent,
    rows: List<SettingsRow>,
    focusedRow: Int,
    focusedCol: Int,
    focusCell: (Int, Int) -> Unit,
    activate: (Int) -> Unit,
    onFocusSidebar: () -> Unit,
): Boolean {
    if (ev.type != KeyEventType.KeyDown) return false
    return when (ev.key) {
        Key.DirectionUp -> {
            val nr = SettingsLayout.nextFocusRow(rows, focusedRow, -1)
            if (nr >= 0) focusCell(nr, SettingsLayout.clampCol(focusedCol, rows, nr))
            true
        }
        Key.DirectionDown -> {
            val nr = SettingsLayout.nextFocusRow(rows, focusedRow, 1)
            if (nr >= 0) focusCell(nr, SettingsLayout.clampCol(focusedCol, rows, nr))
            true
        }
        Key.DirectionLeft -> {
            // Every remaining control is a single-cell row — there is no column
            // to the left, so LEFT leaves the content and focuses the
            // sidebar's "settings" item.
            onFocusSidebar()
            true
        }
        Key.DirectionRight -> {
            // No column to the right — consume to keep focus stable.
            true
        }
        Key.DirectionCenter, Key.Enter -> {
            activate(focusedRow)
            true
        }
        else -> false
    }
}

/** Section label (uppercase, letterspaced) above a row's content, plus rhythm. */
@Composable
private fun SectionFrame(header: String?, content: @Composable () -> Unit) {
    Column {
        if (header != null) {
            Text(
                text = header.uppercase(),
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandColors.TextSecondary,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
        }
        content()
    }
}

/** Registers a single (row,0) focusable pill (Back). */
@Composable
private fun BackPill(
    row: Int,
    requesters: MutableMap<Pair<Int, Int>, FocusRequester>,
    onClick: () -> Unit,
) {
    val requester = requesters[row to 0] ?: remember(row) {
        FocusRequester().also { requesters[row to 0] = it }
    }
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) BrandColors.Focus else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Back",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) BrandColors.Background else BrandColors.TextPrimary,
        )
    }
}

/** (row,0) focusable toggle row with a custom TV switch. */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    row: Int,
    requesters: MutableMap<Pair<Int, Int>, FocusRequester>,
    onToggle: () -> Unit,
) {
    val requester = requesters[row to 0] ?: remember(row) {
        FocusRequester().also { requesters[row to 0] = it }
    }
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isFocused) BrandColors.Focus.copy(alpha = 0.10f) else Color.Transparent)
                .clickable(onClick = onToggle)
                .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isFocused) BrandColors.Focus else Color.Transparent),
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                    color = BrandColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = BrandColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TvSwitch(checked)
        }
    }
}

@Composable
private fun TvSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) BrandColors.Focus else Color.White.copy(alpha = 0.16f)),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) BrandColors.Background else Color.White.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun GiveCard(
    row: Int,
    requesters: MutableMap<Pair<Int, Int>, FocusRequester>,
    onClick: () -> Unit,
) {
    val requester = requesters[row to 0] ?: remember(row) {
        FocusRequester().also { requesters[row to 0] = it }
    }
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.Focus)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Donate Now", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandColors.Background)
            Text(
                text = "Support this ministry",
                fontSize = 14.sp,
                color = BrandColors.Background.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(text = "Open  →", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BrandColors.Background)
    }
}

@Composable
private fun AboutPanel(
    channelName: String,
    channelDescription: String,
    channelCopyright: String,
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(text = channelName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandColors.TextPrimary)
        if (channelDescription.isNotBlank()) {
            Text(
                text = channelDescription,
                fontSize = 15.sp,
                color = BrandColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (channelCopyright.isNotBlank()) {
            Text(
                text = channelCopyright,
                fontSize = 13.sp,
                color = BrandColors.TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
