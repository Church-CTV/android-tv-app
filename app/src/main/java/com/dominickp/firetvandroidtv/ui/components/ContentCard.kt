package com.dominickp.firetvandroidtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dominickp.firetvandroidtv.config.CardStyleConfig
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.focus.GridFocusController
import com.dominickp.firetvandroidtv.ui.focus.trackFocus
import com.dominickp.firetvandroidtv.ui.theme.BrandColors
import com.dominickp.firetvandroidtv.util.Logger

private const val TAG = "ContentCard"

/**
 * ContentCard — poster-only card matching Roku's ContentCard 1:1.
 *
 * Roku layout (ContentCard.xml):
 *   - MaskGroup with 8px rounded corners, 480×270 poster
 *   - Poster fills entire card (scaleToFit)
 *   - Focus ring: white overlay clipped to rounded corners
 *   - Title overlay at bottom: y=216, width=450, height=54
 *   - Description overlay: y=150, width=330, height=135, 3 lines
 *   - Focus: poster scale 1.05, white ring visible
 *   - Unfocus: poster scale 1.0, ring hidden
 *
 * cardStyle config (default.json) controls visibility:
 *   - showTitle: false → hide title overlay
 *   - showDescription: false → hide description overlay
 *   - When both false: poster-only card (default for this channel)
 *
 * Rendered via LazyRow in MonthGroupedRows for horizontal scrolling.
 */
@Composable
fun ContentCard(
    videoItem: VideoItem,
    modifier: Modifier = Modifier,
    cardStyle: CardStyleConfig = CardStyleConfig(),
    focusScale: Float = 1.05f,
    onCardSelected: ((VideoItem) -> Unit)? = null,
    onCardFocused: ((VideoItem?) -> Unit)? = null,
    isFirstCard: Boolean = false,
    onLeftFromFirstCard: (() -> Unit)? = null,
    firstCardFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    gridController: GridFocusController? = null,
    gridRow: Int = -1,
    gridCol: Int = -1,
) {
    val context = LocalContext.current

    // Track focus state for scale animation and white ring
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "card_scale",
    )

    // Use the passed-in FocusRequester or create a local one
    val cardFocusRequester = firstCardFocusRequester ?: remember { FocusRequester() }

    // Request focus for the first card after a delay to ensure composable is ready.
    // LaunchedEffect must NOT be inside a conditional block — that's a Compose anti-pattern
    // causing unexpected effect restarts. Instead, key on isFirstCard and guard inside.
    androidx.compose.runtime.LaunchedEffect(isFirstCard, cardFocusRequester) {
        if (isFirstCard) {
            kotlinx.coroutines.delay(100L)
            Logger.d(TAG, "Requesting focus for first card")
            cardFocusRequester.requestFocus()
        }
    }

    // Register with GridFocusController so the root key handler can navigate by position.
    // Cards self-manage lifecycle: register on composition, unregister on dispose.
    androidx.compose.runtime.DisposableEffect(gridController, gridRow, gridCol, cardFocusRequester) {
        registerGridCard(gridController, gridRow, gridCol, cardFocusRequester)
        onDispose {
            unregisterGridCard(gridController, gridRow, gridCol)
        }
    }

    // Tell the grid controller where focus is, so subsequent D-pad moves start from here.
    val notifyGridFocus: (Boolean) -> Unit = { nowFocused ->
        reportGridFocus(gridController, gridRow, gridCol, nowFocused)
    }

    // Focus is communicated ONLY by the white border ring + scale (see below) — the
    // standard TV poster-card pattern. Background and text stay the same in both states
    // (dark Surface + light text).
    //
    // Previously the focused card flipped to a white containerColor ("white bg + dark
    // content"). Because the poster is clipped to RoundedCornerShape and drawn OVER that
    // background, its anti-aliased rounded-corner pixels are semi-transparent and the WHITE
    // background bled through as a small white-opacity square at each thumbnail corner.
    // Keeping the background dark in both states removes that corner artifact entirely.
    val cardBgColor = BrandColors.Surface
    val cardTitleColor = BrandColors.TextPrimary
    val cardDescColor = BrandColors.TextSecondary

    Card(
        modifier =
            modifier
                .width(CARD_WIDTH)
                .height(CARD_HEIGHT)
                .focusable()
                .trackFocus(videoItem.id)
                .focusRequester(cardFocusRequester)
                .scale(scale)
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused || focusState.hasFocus
                    if (isFocused != wasFocused) {
                        Logger.d(TAG, "Card focus changed: ${videoItem.id}, focused=$isFocused")
                        onCardFocused?.invoke(if (isFocused) videoItem else null)
                        notifyGridFocus(isFocused)
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    // Handle left-key press on the first card
                    if (shouldRouteLeftToSidebar(isFirstCard, isFocused, keyEvent.key, onLeftFromFirstCard)) {
                        onLeftFromFirstCard?.invoke()
                        true // consume the key event
                    } else {
                        false
                    }
                }
                .then(
                    if (isFocused) {
                        // Roku focus: white border ring
                        Modifier.border(
                            width = 3.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(CARD_CORNER_RADIUS),
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (onCardSelected != null) {
                        // indication = null removes Compose's default clickable focus
                        // highlight — the translucent white box (lightened ripple) that
                        // draws over the focused thumbnail, most visible as a white-opacity
                        // square at the rounded corners. The explicit 3dp border ring +
                        // scale above is the only focus signal we keep.
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onCardSelected(videoItem) }
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors =
            CardDefaults.cardColors(
                containerColor = cardBgColor,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Poster fills entire card — matches Roku Poster node.
            // Focus is indicated by the white ring border + scale (see Card modifier)
            // and the title/description text switching to black (cardTitleColor).
            // We intentionally keep the poster visible on focus rather than painting
            // it white — the ring + scale is the standard TV poster-card focus signal.
            AsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(videoItem.thumbnail)
                        .build(),
                contentDescription = videoItem.title,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(CARD_CORNER_RADIUS)),
            )

            // Text overlays at bottom — only shown when cardStyle enables them
            if (cardStyle.showTitle || cardStyle.showDescription) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(8.dp),
                ) {
                    // Title overlay — matches Roku titleLabel position
                    if (cardStyle.showTitle) {
                        Text(
                            text = videoItem.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cardTitleColor,
                            maxLines = cardStyle.titleNumLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = if (cardStyle.showDescription) 4.dp else 0.dp),
                        )
                    }

                    // Description overlay — matches Roku descLabel position
                    if (cardStyle.showDescription && videoItem.shortDescription.isNotBlank()) {
                        Text(
                            text = videoItem.shortDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = cardDescColor,
                            maxLines = cardStyle.descriptionNumLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // LIVE badge — top-right corner
            if (videoItem.isLive) {
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandColors.Live,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                )
            }
        }
    }
}

// Roku ContentCard.xml dimensions: 480×270 on 1920×1080 display
// Scaled for Android TV: ~25% of content area width
private val CARD_WIDTH = 220.dp
private val CARD_HEIGHT = 124.dp
private val CARD_CORNER_RADIUS = 8.dp

// ─── GridFocusController helpers (extracted to keep ContentCard cyclomatic complexity in range) ───

private fun registerGridCard(
    ctrl: GridFocusController?,
    row: Int,
    col: Int,
    requester: FocusRequester,
) {
    if (ctrl != null && row >= 0 && col >= 0) {
        ctrl.register(row, col, requester)
    }
}

private fun unregisterGridCard(ctrl: GridFocusController?, row: Int, col: Int) {
    if (ctrl != null && row >= 0 && col >= 0) {
        ctrl.unregister(row, col)
    }
}

private fun reportGridFocus(ctrl: GridFocusController?, row: Int, col: Int, isFocused: Boolean) {
    if (isFocused && ctrl != null && row >= 0 && col >= 0) {
        ctrl.reportFocus(row, col)
    }
}

private fun shouldRouteLeftToSidebar(
    isFirstCard: Boolean,
    isFocused: Boolean,
    key: Key,
    onLeftFromFirstCard: (() -> Unit)?,
): Boolean {
    return isFirstCard && isFocused && key == Key.DirectionLeft && onLeftFromFirstCard != null
}
