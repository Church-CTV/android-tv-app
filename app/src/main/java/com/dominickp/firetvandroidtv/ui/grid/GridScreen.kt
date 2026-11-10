package com.dominickp.firetvandroidtv.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.config.CardStyleConfig
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.components.MonthGroupedRows
import com.dominickp.firetvandroidtv.ui.components.OverhangHeader
import com.dominickp.firetvandroidtv.util.Logger

private const val TAG = "GridScreen"

/**
 * GridScreen — full-screen grid layout without the left sidebar.
 *
 * Mirrors Roku's GridScreen.xml (GridScreen/ directory):
 *   - OverhangHeader at top (Roku: Overhang component)
 *   - Full-screen MonthGroupedRows (Roku: RowList with numRows=2 at translation=[150,300])
 *   - No sidebar navigation — layout preference is switched in Settings
 *
 * Back behavior: the Activity-level exit dialog (MainActivity) handles back,
 * matching Roku's GridScreen exit dialog flow (back from (0,0) → exit dialog).
 *
 * @param groupedVideos Map of month label -> video list
 * @param onCardSelected Callback when a card is selected
 * @param config App config for overhang branding
 * @param modifier Modifier for the composable
 */
@androidx.media3.common.util.UnstableApi
@Composable
fun GridScreen(
    groupedVideos: Map<String, List<VideoItem>>,
    modifier: Modifier = Modifier,
    onCardSelected: (VideoItem) -> Unit = {},
    config: AppConfig? = null,
) {
    Logger.d(TAG, "GridScreen rendered: ${groupedVideos.size} groups")

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        // Overhang header (matching Roku's Overhang component)
        OverhangHeader(
            channelName = config?.channel?.name ?: "FireTV App",
            showClock = config?.features?.overhangClock ?: false,
            modifier = Modifier.fillMaxWidth(),
        )

        // Full-screen content rows (matching Roku's RowList in GridScreen.xml)
        // Roku specs: translation=[150,300], numRows=2, rowItemSpacing=[[30,0]]
        // onLeftFromFirstCard = null since there's no sidebar to route to
        MonthGroupedRows(
            groupedVideos = groupedVideos,
            onCardSelected = onCardSelected,
            cardStyle = config?.cardStyle ?: CardStyleConfig(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
