package com.dominickp.firetvandroidtv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominickp.firetvandroidtv.ui.focus.trackFocus
import com.dominickp.firetvandroidtv.ui.theme.BrandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OverhangHeader — full-width top bar matching Roku's Overhang component 1:1.
 *
 * Roku Overhang.xml specs:
 *   - Rectangle bg: 1920×100, color=0x1A1A2E (background color)
 *   - Logo Poster: translation=[30,20], 60×60 display (120×120 load)
 *   - Options Label: translation=[110,30], font:MediumSystemFont
 *   - Clock Label: translation=[1000,30], right-aligned, 180×40
 *
 * Android TV adaptation:
 *   - Full-width bar at top, ~5% of screen height (Roku: 100/1080 ≈ 9%)
 *   - Background matches brand background color
 *   - Channel name as text (no logo asset on Android)
 *   - Clock on the right side
 */
@Composable
fun OverhangHeader(
    channelName: String,
    modifier: Modifier = Modifier,
    logoResId: Int? = null,
    showClock: Boolean = true,
) {
    var currentTime by remember { mutableStateOf("") }

    // Update clock every second. Formatter is remembered to avoid allocating
    // a new SimpleDateFormat on every tick (memory churn in tight loop).
    // Key on showClock so the effect restarts when the flag changes:
    // - false→true: clock starts
    // - true→false: effect cancels, clock stops
    // Uses isActive guard to cancel the coroutine when the composable leaves the tree.
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    LaunchedEffect(showClock) {
        while (isActive && showClock) {
            currentTime = formatter.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(OVERHANG_HEIGHT)
                .background(BrandColors.Background),
    ) {
        // Left side — logo/channel name
        Row(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (logoResId != null) {
                Image(
                    painter =
                        androidx.compose.ui.res
                            .painterResource(id = logoResId),
                    contentDescription = "$channelName logo",
                    modifier = Modifier.size(60.dp).trackFocus("overhang_logo"),
                )
                // Channel name beside the logo (Roku: sidebarTitle shows channelName)
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandColors.TextPrimary,
                    modifier =
                        Modifier
                            .padding(start = 12.dp)
                            .trackFocus("overhang_channel"),
                )
            } else {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandColors.TextPrimary,
                    modifier = Modifier.trackFocus("overhang_channel"),
                )
            }
        }

        // Right side — clock
        if (showClock) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandColors.TextPrimary,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 30.dp)
                        .trackFocus("overhang_clock"),
            )
        }
    }
}

// Roku Overhang.xml: height="100" on 1080p (~9.3%)
private val OVERHANG_HEIGHT = 93.dp
