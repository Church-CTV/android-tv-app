package com.dominickp.firetvandroidtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.theme.BrandColors

/**
 * FocusedInfoBar — shows title, description, and duration of the focused card.
 *
 * Sits above the grid in the home page. Content updates live as focus moves
 * between cards.
 *
 * LAYOUT STABILITY (the "rows jump" fix): the bar reserves a FIXED minimum
 * height (title + 2-line description) and top-aligns its content. Every card
 * therefore produces a bar of identical height, so switching the focused card
 * never resizes the bar and never pushes the video rows up or down.
 *
 * Before this fix the bar was a natural-height Column: cards WITHOUT a
 * description were 1 line tall, cards WITH a 2-line description were 3 lines
 * tall, and the whole row grid jumped on every focus change.
 *
 * @param video The currently focused video, or null to show a placeholder.
 */
@Composable
fun FocusedInfoBar(
    video: VideoItem?,
    modifier: Modifier = Modifier,
) {
    val title = video?.title ?: "Select a video"
    val description = video?.shortDescription?.takeIf { it.isNotBlank() }
    val durationText = formatDuration(video?.duration ?: 0)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = INFO_BAR_MIN_HEIGHT)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Title row: title (left) + duration (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = BrandColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (durationText != null && video != null && !video.isLive) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextSecondary,
                )
            }
        }

        // Description — space is always reserved (fixed bar height) so its
        // presence/absence never shifts the rows below.
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Fixed reserved height: title line (titleLarge ~32dp) + 4dp spacing +
// 2-line description (bodyMedium 2x ~22dp) + 16dp vertical padding.
// Slightly padded to 108dp for safety margin. Every card uses the same
// reserved height so the bar never resizes on focus change.
private val INFO_BAR_MIN_HEIGHT = 108.dp

/**
 * Format seconds as "H:MM:SS" or "M:SS" for display.
 */
private fun formatDuration(seconds: Int): String? {
    if (seconds <= 0) return null
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m}:${s.toString().padStart(2, '0')}"
    }
}
