package com.dominickp.firetvandroidtv.ui.player

import androidx.media3.common.util.UnstableApi
import com.dominickp.firetvandroidtv.data.model.VideoItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PlayerViewModelTest — tests the PlayerViewModel state machine transitions.
 *
 * Verifies that the ViewModel correctly handles:
 * - prepare() transitions Loading → Playing/Error
 * - togglePlay() transitions Playing ↔ Paused
 * - updatePlaying() transitions Loading/Paused → Playing
 * - updatePaused() transitions Playing → Paused
 * - updateCompleted() transitions to Completed
 * - updateError() transitions to Error
 * - retry() transitions Error/Loading → Playing
 * - reset() transitions to Idle
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayerViewModelTest {
    private lateinit var viewModel: PlayerViewModel
    private val testVideo =
        VideoItem(
            id = "test-1",
            title = "Test Video",
            shortDescription = "A test video",
            thumbnail = "",
            url = "https://example.com/video.mp4",
            isLive = false,
            contentRating = "",
        )

    @Before
    fun setup() {
        viewModel = PlayerViewModel()
    }

    @Test
    fun `initialState_is_Idle`() {
        assertEquals(PlayerState.Idle, viewModel.state.value)
    }

    @Test
    fun `prepare_withValidUrl_transitionsToPlaying`() =
        runTest {
            viewModel.prepare(testVideo)
            // Give coroutine time to run
            Thread.sleep(50)
            assertTrue(
                "Expected Playing, got ${viewModel.state.value}",
                viewModel.state.value is PlayerState.Playing,
            )
        }

    @Test
    fun `prepare_withEmptyUrl_transitionsToError`() =
        runTest {
            val emptyUrlVideo = testVideo.copy(url = "")
            viewModel.prepare(emptyUrlVideo)
            Thread.sleep(50)
            assertTrue(
                "Expected Error, got ${viewModel.state.value}",
                viewModel.state.value is PlayerState.Error,
            )
        }

    @Test
    fun `togglePlay_fromPlaying_transitionsToPaused`() =
        runTest {
            viewModel.prepare(testVideo)
            Thread.sleep(50)
            viewModel.togglePlay()
            assertEquals(PlayerState.Paused::class, viewModel.state.value::class)
        }

    @Test
    fun `togglePlay_fromPaused_transitionsToPlaying`() =
        runTest {
            viewModel.prepare(testVideo)
            Thread.sleep(50)
            viewModel.togglePlay() // Playing → Paused
            viewModel.togglePlay() // Paused → Playing
            assertEquals(PlayerState.Playing::class, viewModel.state.value::class)
        }

    @Test
    fun `updatePlaying_fromLoading_transitionsToPlaying`() {
        viewModel.internalState.value = PlayerState.Loading
        viewModel.updatePlaying()
        assertEquals(PlayerState.Playing::class, viewModel.state.value::class)
    }

    @Test
    fun `updatePlaying_fromPaused_transitionsToPlaying`() {
        viewModel.internalState.value = PlayerState.Paused(0.5f)
        viewModel.updatePlaying()
        assertEquals(PlayerState.Playing::class, viewModel.state.value::class)
    }

    @Test
    fun `updatePlaying_fromPlaying_remainsPlaying`() {
        viewModel.internalState.value = PlayerState.Playing(0.3f)
        viewModel.updatePlaying()
        assertEquals(PlayerState.Playing::class, viewModel.state.value::class)
    }

    @Test
    fun `updatePlaying_fromError_staysError`() {
        viewModel.internalState.value = PlayerState.Error("test error")
        viewModel.updatePlaying()
        assertEquals(PlayerState.Error::class, viewModel.state.value::class)
    }

    @Test
    fun `updatePlaying_fromCompleted_staysCompleted`() {
        viewModel.internalState.value = PlayerState.Completed
        viewModel.updatePlaying()
        assertEquals(PlayerState.Completed, viewModel.state.value)
    }

    @Test
    fun `updatePaused_fromPlaying_transitionsToPaused`() {
        viewModel.internalState.value = PlayerState.Playing(0.7f)
        viewModel.updatePaused()
        assertEquals(PlayerState.Paused::class, viewModel.state.value::class)
    }

    @Test
    fun `updatePaused_preservesProgressFromPlaying`() {
        viewModel.internalState.value = PlayerState.Playing(0.75f)
        viewModel.updatePaused()
        val paused = viewModel.state.value as PlayerState.Paused
        assertEquals(0.75f, paused.progress, 0.001f)
    }

    @Test
    fun `updatePaused_fromError_doesNotTransition`() {
        viewModel.internalState.value = PlayerState.Error("error")
        viewModel.updatePaused()
        assertEquals(PlayerState.Error::class, viewModel.state.value::class)
    }

    @Test
    fun `updateCompleted_transitionsToCompleted`() =
        runTest {
            viewModel.prepare(testVideo)
            Thread.sleep(50)
            viewModel.updateCompleted()
            assertEquals(PlayerState.Completed, viewModel.state.value)
        }

    @Test
    fun `updateError_transitionsToError`() {
        viewModel.internalState.value = PlayerState.Playing()
        viewModel.updateError("Test error")
        val error = viewModel.state.value as PlayerState.Error
        assertEquals("Test error", error.message)
    }

    @Test
    fun `retry_fromError_withValidUrl_transitionsToPlaying`() =
        runTest {
            viewModel.internalState.value = PlayerState.Error("test error")
            viewModel.setCurrentVideo(testVideo)
            viewModel.retry()
            Thread.sleep(50)
            assertEquals(PlayerState.Playing::class, viewModel.state.value::class)
        }

    @Test
    fun `retry_fromError_withEmptyUrl_staysError`() =
        runTest {
            val emptyUrlVideo = testVideo.copy(url = "")
            viewModel.internalState.value = PlayerState.Error("test error")
            viewModel.setCurrentVideo(emptyUrlVideo)
            viewModel.retry()
            Thread.sleep(50)
            assertEquals(PlayerState.Error::class, viewModel.state.value::class)
        }

    @Test
    fun `reset_transitionsToIdle`() {
        viewModel.internalState.value = PlayerState.Playing()
        viewModel.reset()
        assertEquals(PlayerState.Idle, viewModel.state.value)
    }

    @Test
    fun `updateProgress_fromPlaying_updatesProgress`() {
        viewModel.internalState.value = PlayerState.Playing(0f)
        viewModel.updateProgress(0.5f)
        val playing = viewModel.state.value as PlayerState.Playing
        assertEquals(0.5f, playing.progress, 0.001f)
    }

    @Test
    fun `updateProgress_fromPaused_updatesProgress`() {
        viewModel.internalState.value = PlayerState.Paused(0f)
        viewModel.updateProgress(0.25f)
        val paused = viewModel.state.value as PlayerState.Paused
        assertEquals(0.25f, paused.progress, 0.001f)
    }

    @Test
    fun `updateProgress_fromIdle_doesNotTransition`() {
        viewModel.internalState.value = PlayerState.Idle
        viewModel.updateProgress(0.5f)
        assertEquals(PlayerState.Idle, viewModel.state.value)
    }
}
