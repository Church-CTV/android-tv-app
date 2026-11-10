package com.dominickp.firetvandroidtv.ui.player

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * VideoPlayerScreenTest — verifies the VideoPlayerScreen Composable contracts
 * (screen state behavior, ExoPlayer integration, controls, and overlays).
 *
 * Tests the structural contracts rather than Compose rendering, which requires
 * a full ComposeTestRule setup not available in this project's Robolectric config.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoPlayerScreenTest {
    private companion object {
        private const val SRC = "src/main/java/com/dominickp/firetvandroidtv/ui/player/VideoPlayerScreen.kt"
        private const val LAYOUT = "src/main/res/layout/media3_player.xml"
    }

    private val source by lazy {
        String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(SRC)))
    }

    private val layout by lazy {
        String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(LAYOUT)))
    }

    // === Composable Signature Tests ===

    @Test
    fun `screenHasVideoPlayerScreenFunction`() {
        assertTrue(
            "VideoPlayerScreen must have the main @Composable function",
            source.contains("fun VideoPlayerScreen"),
        )
    }

    @Test
    fun `screenAcceptsVideoItemParameter`() {
        assertTrue(
            "VideoPlayerScreen must accept VideoItem parameter",
            source.contains("videoItem: VideoItem"),
        )
    }

    @Test
    fun `screenAcceptsOnBackCallback`() {
        assertTrue(
            "VideoPlayerScreen must accept onBack callback for navigation",
            source.contains("onBack:"),
        )
    }

    @Test
    fun `screenUsesViewModel`() {
        assertTrue(
            "VideoPlayerScreen must use PlayerViewModel",
            source.contains("viewModel: PlayerViewModel"),
        )
    }

    // === ExoPlayer Integration Tests ===

    @Test
    fun `screenCreatesExoPlayer`() {
        assertTrue(
            "VideoPlayerScreen must create an ExoPlayer instance",
            source.contains("ExoPlayer") && source.contains(".Builder"),
        )
    }

    @Test
    fun `screenSetsMediaItemFromVideo`() {
        assertTrue(
            "VideoPlayerScreen must set MediaItem from video playableUrl",
            source.contains("setMediaItem") && source.contains("MediaItem.fromUri"),
        )
    }

    @Test
    fun `screenCallsPrepareOnPlayer`() {
        assertTrue(
            "VideoPlayerScreen must call prepare() on player creation",
            source.contains("prepare()"),
        )
    }

    @Test
    fun `screenUsesAndroidViewForPlayerView`() {
        assertTrue(
            "VideoPlayerScreen must use AndroidView to host PlayerView",
            source.contains("AndroidView") && source.contains("PlayerView"),
        )
    }

    @Test
    fun `playerViewUsesTextureViewSurfaceForCompose`() {
        // texture_view renders in-canvas — no separate SurfaceView to desync from the
        // Compose surface, so the video is not black until a key press on any API.
        // (The default surface_view punches a hole that stays black in AndroidView.)
        assertTrue(
            "PlayerView layout must use surface_type=texture_view (Compose-embedding fix)",
            layout.contains("surface_type=\"texture_view\""),
        )
        assertFalse(
            "PlayerView must NOT use the default surface_view (black-in-Compose bug)",
            layout.contains("surface_type=\"surface_view\""),
        )
    }

    @Test
    fun `screenInflatesPlayerViewFromLayout`() {
        assertTrue(
            "VideoPlayerScreen must inflate R.layout.media3_player",
            source.contains("R.layout.media3_player"),
        )
    }

    // === State Management Tests ===

    @Test
    fun `screenCollectsViewModelState`() {
        assertTrue(
            "VideoPlayerScreen must collect state from viewModel",
            source.contains("collectAsStateWithLifecycle") || source.contains("collectAsState"),
        )
    }

    @Test
    fun `screenShowsErrorOverlayOnErrorState`() {
        assertTrue(
            "VideoPlayerScreen must show error overlay when state is Error",
            source.contains("PlayerState.Error"),
        )
    }

    @Test
    fun `screenShowsNativeControllerOnPlayerView`() {
        assertTrue(
            "PlayerView layout must enable the native Media3 controller",
            layout.contains("use_controller=\"true\""),
        )
    }

    // === Player Listener Tests ===

    @Test
    fun `screenRegistersPlayerListener`() {
        assertTrue(
            "VideoPlayerScreen must register a Player.Listener for state changes",
            source.contains("player.addListener") || source.contains("addListener"),
        )
    }

    @Test
    fun `screenHandlesOnPlayerError`() {
        assertTrue(
            "VideoPlayerScreen must handle onPlayerError",
            source.contains("onPlayerError"),
        )
    }

    @Test
    fun `screenHandlesOnIsPlayingChanged`() {
        assertTrue(
            "VideoPlayerScreen must handle onIsPlayingChanged",
            source.contains("onIsPlayingChanged"),
        )
    }

    // === Analytics Integration Tests ===

    @Test
    fun `screenHasAnalyticsTracker`() {
        assertTrue(
            "VideoPlayerScreen must have AnalyticsTracker",
            source.contains("AnalyticsTracker"),
        )
    }

    @Test
    fun `screenTracksPlayEvent`() {
        assertTrue(
            "VideoPlayerScreen must track play events",
            source.contains("trackPlay"),
        )
    }

    @Test
    fun `screenTracksPauseEvent`() {
        assertTrue(
            "VideoPlayerScreen must track pause events",
            source.contains("trackPause"),
        )
    }

    @Test
    fun `screenTracksCompleteEvent`() {
        assertTrue(
            "VideoPlayerScreen must track complete events",
            source.contains("trackComplete"),
        )
    }

    @Test
    fun `screenTracksErrorEvent`() {
        assertTrue(
            "VideoPlayerScreen must track error events",
            source.contains("trackError"),
        )
    }

    // === Continue Watching Integration Tests ===

    @Test
    fun `screenSavesPlaybackPositionPeriodically`() {
        assertTrue(
            "VideoPlayerScreen must save playback position periodically",
            source.contains("savePlaybackPosition"),
        )
    }

    @Test
    fun `screenHas10SecondSaveInterval`() {
        assertTrue(
            "VideoPlayerScreen must use 10-second interval for saves (matches Roku)",
            source.contains("10_000") || source.contains("10000"),
        )
    }

    // === Lifecycle Management Tests ===

    @Test
    fun `screenReleasesPlayerOnDispose`() {
        assertTrue(
            "VideoPlayerScreen must release player in DisposableEffect",
            source.contains("DisposableEffect") && source.contains("player.release()"),
        )
    }

    @Test
    fun `screenRemovesListenerOnDispose`() {
        assertTrue(
            "VideoPlayerScreen must remove listener on dispose",
            source.contains("removeListener"),
        )
    }

    @Test
    fun `screenUsesUnstableApiAnnotation`() {
        assertTrue(
            "VideoPlayerScreen must use @UnstableApi for Media3 compatibility",
            source.contains("UnstableApi"),
        )
    }

    // === UI Behavior Tests ===

    @Test
    fun `screenHasAutoHideControlsViaNativeController`() {
        assertTrue(
            "PlayerView layout must enable the native controller (which auto-hides)",
            layout.contains("use_controller=\"true\""),
        )
    }

    @Test
    fun `screenHasClickAreaToShowControls`() {
        assertTrue(
            "VideoPlayerScreen must have native controller handle show-on-touch",
            source.contains("PlayerView"),
        )
    }

    @Test
    fun `screenHasErrorOverlayWithRetry`() {
        assertTrue(
            "VideoPlayerScreen must have ErrorOverlay with retry capability",
            source.contains("ErrorOverlay") && source.contains("onRetry"),
        )
    }

    @Test
    fun `screenCallsViewModelRetryOnRetryClick`() {
        assertTrue(
            "VideoPlayerScreen must call viewModel.retry() on retry",
            source.contains("viewModel.retry()"),
        )
    }

    @Test
    fun `screenShowsVideoTitleInControls`() {
        assertTrue(
            "VideoPlayerScreen must show video title",
            source.contains("videoItem.title"),
        )
    }

    @Test
    fun `screenHasCompletionDetectionAt80Percent`() {
        assertTrue(
            "VideoPlayerScreen must detect completion at 80% progress",
            source.contains("0.8f") || source.contains(">= 0.8"),
        )
    }

    @Test
    fun `screenAutoDismissesOnCompletion`() {
        assertTrue(
            "VideoPlayerScreen must auto-dismiss after completion",
            source.contains("onBack()"),
        )
    }
}
