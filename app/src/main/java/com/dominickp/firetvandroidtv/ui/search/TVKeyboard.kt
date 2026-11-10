package com.dominickp.firetvandroidtv.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dominickp.firetvandroidtv.ui.theme.BrandColors

/** Key cell size for the compact keyboard. */
internal val KEY_SIZE = 36.dp

/** Spacing between keys. */
internal val KEY_GAP = 6.dp

/** Special key labels (last row). */
internal const val KEY_SPACE = "SPACE"
internal const val KEY_MIC = "MIC"
internal const val KEY_BACKSPACE = "BACKSPACE"
internal const val KEY_SEARCH = "SEARCH"

/**
 * Uniform 10-column grid. Letters run in reading order (A→Z), then digits 1–9.
 * Row 3 has 9 logical keys; SEARCH spans the last two cells (cols 8–9).
 */
internal val KEY_ROWS: List<List<String>> =
    listOf(
        listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
        listOf("K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"),
        listOf("U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", KEY_SPACE, KEY_MIC, KEY_BACKSPACE, KEY_SEARCH),
    )

internal const val KEY_COLS = 10
internal const val KEY_ROW_COUNT = 4
internal const val SEARCH_COL = 8

/**
 * TVKeyboard — compact D-pad-navigable keyboard for the LEFT search zone.
 *
 * Replaces the old full-width 3-row layout (10/9/3 rows that consumed ~60% of
 * screen height and left no room for results). Matches Roku's split:
 * MiniKeyboard (left) + results (right).
 *
 * Navigation is driven by the screen-level root key handler (SearchScreen's
 * onPreviewKeyEvent) — the synchronous routing pattern this app uses for the
 * home grid (GridFocusController's docblock explains why Compose
 * auto-traversal freezes inside nested lazy layouts on Fire TV).
 *
 * Keys are visual surfaces only; the (row, col) pair renders the highlight.
 */
@Composable
fun TVKeyboard(
    focusedRow: Int,
    focusedCol: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        KEY_ROWS.forEachIndexed { rowIdx, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
            ) {
                for (colIdx in 0 until KEY_COLS) {
                    if (rowIdx == 3 && colIdx == SEARCH_COL) {
                        // SEARCH CTA spans cols 8–9.
                        KeySearchButton(isFocused = rowIdx == focusedRow && colIdx == SEARCH_COL)
                    } else if (colIdx < rowKeys.size) {
                        KeySurface(
                            label = rowKeys[colIdx],
                            isFocused = rowIdx == focusedRow && colIdx == focusedCol,
                        )
                    }
                }
            }
            if (rowIdx < KEY_ROWS.lastIndex) {
                Spacer(modifier = Modifier.height(KEY_GAP))
            }
        }
    }
}

/**
 * One key surface. Letters/digits render their glyph; special keys render an
 * icon or a word. Focused = brand gold fill + dark glyph (the app's standard
 * focus treatment).
 */
@Composable
internal fun KeySurface(
    label: String,
    isFocused: Boolean,
) {
    val bg = if (isFocused) BrandColors.Focus else Color.White.copy(alpha = 0.10f)
    val fg = if (isFocused) BrandColors.Background else BrandColors.TextPrimary.copy(alpha = 0.85f)

    Box(
        modifier =
            Modifier
                .size(KEY_SIZE)
                .clip(RoundedCornerShape(6.dp))
                .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        when (label) {
            KEY_MIC ->
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint = fg,
                    modifier = Modifier.size(18.dp),
                )
            KEY_BACKSPACE ->
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = fg,
                    modifier = Modifier.size(18.dp),
                )
            KEY_SPACE ->
                Text(
                    text = "SPACE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                    textAlign = TextAlign.Center,
                )
            else ->
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                    textAlign = TextAlign.Center,
                )
        }
    }
}

/** Gold Search CTA — spans the last two columns of the bottom row. */
@Composable
internal fun KeySearchButton(isFocused: Boolean) {
    Box(
        modifier =
            Modifier
                .width(KEY_SIZE * 2 + KEY_GAP)
                .height(KEY_SIZE)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isFocused) BrandColors.Focus else Color.White.copy(alpha = 0.18f),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) BrandColors.Background else BrandColors.TextPrimary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SEARCH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) BrandColors.Background else BrandColors.TextPrimary,
            )
        }
    }
}

/**
 * SearchField — full-width input bar with a blinking cursor.
 *
 * Visual only (not focusable): the keyboard anchor holds Compose focus while
 * the keyboard zone is active, so D-pad typing shows up here immediately.
 */
@Composable
fun SearchField(
    query: String,
    modifier: Modifier = Modifier,
) {
    // Blinking cursor — 1s cycle (500ms on / 500ms off). The cursor element
    // is ALWAYS in the layout (fixed 3dp + 12dp spacer); only its alpha
    // animates. Conditional insert/remove (the old `if (showCursor)`) made the
    // placeholder text re-layout every blink, so "Search videos..." jumped
    // right when the bar appeared and back left when it dimmed.
    val transition = rememberInfiniteTransition(label = "cursor_blink")
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cursor_alpha",
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = BrandColors.Focus.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp),
                )
                .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .alpha(blink)
                    .background(BrandColors.Focus),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (query.isEmpty()) "Search videos..." else query,
            fontSize = 24.sp,
            color = if (query.isEmpty()) BrandColors.TextSecondary else BrandColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}
