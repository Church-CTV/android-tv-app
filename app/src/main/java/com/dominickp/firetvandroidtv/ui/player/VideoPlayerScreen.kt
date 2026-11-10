package com.dominickp.firetvandroidtv.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.dominickp.firetvandroidtv.R
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.theme.BrandColors
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * VideoPlayerScreen — full-screen video player using Media3 ExoPlayer.
 *
 * Mirrors Roku's VideoPlayer scene from roku-channel-template:
 *   - Full-screen video with native Media3 ExoPlayer controls
 *   - Plays video from VideoItem.playableUrl
 *   - D-pad navigation (play/pause on center, back on exit)
 *
 * Playback controls are handled by Media3's built-in PlayerView controller
 * (no custom overlay). The controller auto-hides after inactivity.
 *
 * Features:
 *   - Periodic continue-watching save (every 10 seconds)
 *   - Analytics: play on start, pause on pause, complete at 80%+ (VOD analytics only)
 *   - Auto-exit ONLY on STATE_ENDED (true end of content). The 80% analytics
 *     gate does not exit — live items skip it entirely and play indefinitely
 *     until the broadcast actually ends.
 *   - DisposableEffect for proper listener cleanup
 */
@Composable
@androidx.media3.common.util.UnstableApi
fun VideoPlayerScreen(
    videoItem: VideoItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(),
    referrer: String = "video",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // AnalyticsTracker singleton is initialized in FireTVApplication with the correct config.
    // Using getInstance() is safe — returns the existing singleton instance.
    val analyticsTracker = com.dominickp.firetvandroidtv.analytics.AnalyticsTracker.getInstance()

    // Set currentVideo in ViewModel so updateCompleted() can save to continue-watching
    // and retry() has the video context. Without this, completed videos are never marked
    // as watched and ViewModel.retry() silently does nothing.
    LaunchedEffect(videoItem.id) {
        viewModel.setCurrentVideo(videoItem)
    }

    // ExoPlayer — configured for TV playback (#2.5)
    // Remembered with videoItem.id as key so a new player is created for each video.
    val player =
        remember(videoItem.id) {
            @androidx.media3.common.util.UnstableApi
            ExoPlayer
                .Builder(context)
                .apply {
                    setHandleAudioBecomingNoisy(false) // Don't pause on headset disconnect (TV)
                    setTrackSelector(
                        androidx.media3.exoplayer.trackselection
                            .DefaultTrackSelector(context),
                    )
                }.build()
                .apply {
                    val streamUrl = videoItem.playableUrl
                    if (streamUrl.isNotBlank()) {
                        setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                        prepare()
                        play()
                    }
                }
        }

    // Release player when video changes or composable leaves composition.
    // Key on videoItem.id to ensure old player is released before new one is created.
    DisposableEffect(videoItem.id) {
        onDispose {
            player.release()
        }
    }

    // Player listener: error handling, play/pause analytics, completion tracking.
    // Media3 callbacks run on player's internal thread — dispatch all state/analytics
    // updates to the main thread via scope.launch.
    // completionTracked flag prevents duplicate completion events from repeated onEvents calls.
    // initialPlayTracked flag ensures play event fires only on first playback start, not on resume.
    var completionTracked by remember { mutableStateOf(false) }
    var initialPlayTracked by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener =
            object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Logger.e("VideoPlayerScreen", error, "Playback error: ${error.message}")
                    scope.launch(Dispatchers.Main) {
                        viewModel.updateError(error.message ?: "Playback error")
                        analyticsTracker.trackError(
                            "player",
                            error.message ?: "Unknown player error",
                            videoItem.id,
                        )
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    // True end of content — the ONLY auto-exit trigger.
                    // VOD: reached the last second. Live: the broadcast ended
                    // (resi.media converts live -> VOD, the playlist stops growing
                    // and reaches its end). A live stream that is STILL live never
                    // reaches STATE_ENDED, so it plays indefinitely — no more
                    // instant/early exit.
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        val dur = player.duration
                        scope.launch(Dispatchers.Main) {
                            if (!completionTracked) {
                                completionTracked = true
                                viewModel.updateCompleted()
                                analyticsTracker.trackComplete(
                                    videoItem.id,
                                    1.0f,
                                    dur,
                                )
                            }
                            if (isActive) {
                                delay(2000)
                                if (isActive) {
                                    onBack()
                                }
                            }
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    // Media3 callbacks run on player's internal thread — dispatch ALL state mutations
                    // to the main thread via scope.launch.
                    scope.launch(Dispatchers.Main) {
                        if (isPlaying) {
                            viewModel.updatePlaying()
                            // Track play event only on initial start, not on resume after pause.
                            // Without this guard, every pause→resume cycle generates a new play event.
                            if (!initialPlayTracked) {
                                initialPlayTracked = true
                                analyticsTracker.trackPlay(
                                    videoItem.id,
                                    videoItem.playableUrl,
                                    videoItem.isLive,
                                    referrer,
                                )
                            }
                        } else {
                            viewModel.updatePaused()
                            analyticsTracker.trackPause(
                                videoItem.id,
                                getProgress(player),
                            )
                        }
                    }
                }

                override fun onEvents(
                    player: androidx.media3.common.Player,
                    events: androidx.media3.common.Player.Events,
                ) {
                    // Analytics only — do NOT exit here. The auto-exit lives in
                    // onPlaybackStateChanged(STATE_ENDED) above. Firing onBack()
                    // from this 80% progress gate is what made VOD exit at 80%
                    // and live exit instantly (live position/duration ratio is ~1
                    // at the live edge).
                    // Live items: skip the progress gate entirely — for a live
                    // event playlist the ratio is the accumulated-stream position
                    // divided by total accumulated duration, not "how much of this
                    // sermon is watched". The complete event for live fires from
                    // STATE_ENDED when the broadcast actually ends.
                    if (videoItem.isLive) return
                    if (player.duration > 0) {
                        val progress = player.currentPosition.toFloat() / player.duration.toFloat()
                        if (progress >= 0.8f) {
                            scope.launch(Dispatchers.Main) {
                                if (!completionTracked) {
                                    completionTracked = true
                                    analyticsTracker.trackComplete(
                                        videoItem.id,
                                        progress,
                                        player.duration.toLong(),
                                    )
                                    // No updateCompleted() and no onBack() here —
                                    // 80% is an analytics threshold (Roku parity),
                                    // not the end of the video.
                                }
                            }
                        }
                    }
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Periodic continue-watching save (every 10 seconds during playback)
    // Uses DisposableEffect to ensure only one coroutine runs per video/player combo.
    // The coroutine is cancelled when the composable leaves composition or video changes.
    DisposableEffect(player, videoItem.id) {
        val job =
            scope.launch {
                while (isActive && player.isPlaying && player.duration > 0) {
                    delay(10_000) // 10 seconds — matches Roku heartbeat
                    if (isActive && player.isPlaying && player.duration > 0) {
                        val progress = (player.currentPosition.toFloat() / player.duration.toFloat() * 100).toInt()
                        viewModel.savePlaybackPosition(
                            videoId = videoItem.id,
                            title = videoItem.title,
                            thumbnail = videoItem.thumbnail,
                            playedPercentage = progress,
                            contentUrl = videoItem.playableUrl,
                        )
                    }
                }
            }
        onDispose { job.cancel() }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Video player with native Media3 ExoPlayer controls only.
        // The PlayerView is inflated from res/layout/media3_player.xml because
        // surface_type="texture_view" is XML-only (no programmatic setter).
        // TextureView renders in-canvas — no separate SurfaceView to desync from
        // the Compose surface, so the video is not black until a key press on any
        // API level. See player/AGENTS.md "Player Surface in Compose".
        AndroidView(
            factory = { ctx ->
                // Inflate the XML PlayerView (root of media3_player.xml). Passing a
                // null root returns the inflated root view (the PlayerView) without
                // attaching it — AndroidView then sets its layout params + attaches.
                val playerView =
                    android.view.View.inflate(ctx, R.layout.media3_player, null)
                        as androidx.media3.ui.PlayerView
                playerView.apply {
                    this.player = player
                    // No-op with texture_view on every API; on API 34 it would
                    // additionally group a SurfaceView with the Compose surface.
                    setEnableComposeSurfaceSyncWorkaround(true)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Collect player state for UI rendering (error overlay only)
        val playerState by viewModel.state.collectAsStateWithLifecycle()

        // Error overlay only — playback controls are handled by native PlayerView controller
        if (playerState is PlayerState.Error) {
            ErrorOverlay(
                errorMessage = (playerState as PlayerState.Error).message,
                onRetry = {
                    // Re-prepare and play the stream — retry() alone only updates state
                    // without actually restarting the ExoPlayer.
                    val streamUrl = videoItem.playableUrl
                    if (streamUrl.isNotBlank()) {
                        player.seekTo(0)
                        player.prepare()
                        player.play()
                    }
                    viewModel.retry()
                },
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Get current playback progress as a 0.0-1.0 float.
 */
private fun getProgress(player: ExoPlayer): Float =
    if (player.duration > 0) {
        player.currentPosition.toFloat() / player.duration.toFloat()
    } else {
        0f
    }

/**
 * Error overlay with Retry button — shown when playback fails (#4.8).
 */
@androidx.media3.common.util.UnstableApi
@Composable
private fun ErrorOverlay(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Playback Error",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Accent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Retry")
                }
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
    }
}
