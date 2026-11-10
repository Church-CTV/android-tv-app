package com.dominickp.firetvandroidtv.ui.continuewatching

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dominickp.firetvandroidtv.dao.ContinueWatchingEntry
import com.dominickp.firetvandroidtv.data.model.VideoContent
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.focus.trackFocus
import com.dominickp.firetvandroidtv.ui.player.VideoPlayerScreen
import com.dominickp.firetvandroidtv.ui.theme.BrandColors

/**
 * ContinueWatchingScreen — displays previously watched content with resume position.
 *
 * Mirrors Roku's ContinueWatching scene:
 *   - Horizontal row of cards with progress indicators
 *   - Shows last watched timestamp
 *   - Tap to resume playback from saved position
 *
 * Data comes from the ContinueWatching Room database.
 *
 * Navigation: selecting a card opens VideoPlayerScreen full-screen.
 * Back button returns to the previous screen (Home).
 */
@Composable
@androidx.media3.common.util.UnstableApi
fun ContinueWatchingScreen(
    entries: List<ContinueWatchingEntry>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    // Track which entry is being watched (for video player overlay)
    var selectedEntry by remember { mutableStateOf<ContinueWatchingEntry?>(null) }

    // VideoPlayerScreen must render at top level to fill the screen.
    // When an entry is selected, show full-screen player; otherwise show the list.
    val entry = selectedEntry
    if (entry != null) {
        val video =
            VideoItem(
                id = entry.videoId,
                title = entry.title,
                shortDescription = "",
                thumbnail = entry.thumbnail,
                url = entry.contentUrl,
                releaseDate = "",
                content =
                    VideoContent(
                        videos =
                            listOf(
                                com.dominickp.firetvandroidtv.data.model.Video(
                                    url = entry.contentUrl,
                                ),
                            ),
                    ),
                isLive = false,
            )
        VideoPlayerScreen(
            videoItem = video,
            onBack = {
                selectedEntry = null
                onBack()
            },
            modifier = modifier.fillMaxSize(),
        )
    } else {
        ContinueWatchingList(
            entries = entries,
            onSelectEntry = { selectedEntry = it },
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ContinueWatchingList(
    entries: List<ContinueWatchingEntry>,
    onSelectEntry: (ContinueWatchingEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        // Header — Roku: title at [0, 20], ExtraBoldSystemFont, centered
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp, top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "← Back",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .trackFocus("continue_watching_back")
                        .clickable(onClick = onBack),
            )
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(80.dp))
        }

        // Content
        if (entries.isEmpty()) {
            // Roku: empty message at [0, 400], Bold, #A0A0B0
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No recently watched videos",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandColors.TextSecondary,
                )
            }
        } else {
            // Roku: RowList at [80, 120]
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 80.dp, end = 80.dp, top = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { it.videoId }) { entry ->
                    ContinueWatchingCard(
                        entry = entry,
                        onSelectEntry = onSelectEntry,
                    )
                }
            }
        }
    }
}

/**
 * ContinueWatchingCard — single entry with progress indicator.
 */
@Composable
private fun ContinueWatchingCard(
    entry: ContinueWatchingEntry,
    onSelectEntry: (ContinueWatchingEntry) -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .trackFocus("continue_card_${entry.videoId}")
                .clickable(onClick = { onSelectEntry(entry) }),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Thumbnail
            AsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(entry.thumbnail)
                        .build(),
                contentDescription = entry.title,
                modifier =
                    Modifier
                        .width(160.dp)
                        .fillMaxHeight(),
            )

            // Details
            Column(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { entry.playedPercentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                    Text(
                        text = "Watched ${entry.playedPercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
