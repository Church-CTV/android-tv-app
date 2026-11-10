package com.dominickp.firetvandroidtv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.data.repository.PlayerRepository
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PlayerState — represents the current state of the video player.
 *
 * Mirrors Roku's VideoScreen state machine:
 *   - Idle: No video loaded
 *   - Loading: Video URL is being prepared
 *   - Playing: Video is playing
 *   - Paused: Video is paused
 *   - Error: Playback error
 *   - Completed: Video finished playing
 */
sealed interface PlayerState {
    object Idle : PlayerState

    object Loading : PlayerState

    data class Playing(
        val progress: Float = 0f,
        val isPlaying: Boolean = true,
    ) : PlayerState

    data class Paused(
        val progress: Float = 0f,
    ) : PlayerState

    data class Error(
        val message: String,
    ) : PlayerState

    object Completed : PlayerState
}

/**
 * PlayerViewModel — manages video playback state and continue-watching integration.
 *
 * Mirrors Roku's VideoScreen ViewModel behavior:
 *   - Tracks progress during playback
 *   - Saves playback position to Room DB (continue-watching) every 10s
 *   - Handles errors and provides retry mechanism
 *
 * In production, this holds a Media3 ExoPlayer instance.
 * The composable controls the actual player; this ViewModel manages
 * state transitions and DB persistence.
 */
class PlayerViewModel(
    private val playerRepository: PlayerRepository? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var currentVideo: VideoItem? = null

    /**
     * Internal accessor for test access.
     */
    internal val internalState: MutableStateFlow<PlayerState> = _state

    /**
     * Prepare to play a video. Loads the stream URL and starts preparation.
     */
    fun prepare(video: VideoItem) {
        currentVideo = video
        _state.value = PlayerState.Loading

        viewModelScope.launch {
            try {
                val streamUrl = video.playableUrl
                if (streamUrl.isNotBlank()) {
                    _state.value = PlayerState.Playing()
                } else {
                    _state.value = PlayerState.Error("No playable URL for this video")
                }
            } catch (e: Throwable) {
                _state.value = PlayerState.Error(e.message ?: "Failed to prepare video")
            }
        }
    }

    /**
     * Toggle play/pause.
     */
    fun togglePlay() {
        when (val s = _state.value) {
            is PlayerState.Playing -> {
                _state.value = PlayerState.Paused(s.progress)
            }

            is PlayerState.Paused -> {
                _state.value = PlayerState.Playing(s.progress)
            }

            else -> {}
        }
    }

    /**
     * Update playback progress (called periodically from player).
     */
    fun updateProgress(progress: Float) {
        when (val s = _state.value) {
            is PlayerState.Playing -> {
                _state.value = PlayerState.Playing(progress)
            }

            is PlayerState.Paused -> {
                _state.value = PlayerState.Paused(progress)
            }

            else -> {}
        }
    }

    /**
     * Update playing state — call when player starts playing.
     * Only allows transitions from Loading or Paused to Playing.
     * Error and Completed states are immutable unless explicitly reset via prepare().
     */
    fun updatePlaying() {
        _state.value =
            when (_state.value) {
                is PlayerState.Loading,
                is PlayerState.Paused,
                -> PlayerState.Playing()

                is PlayerState.Playing -> _state.value

                // stay playing
                else -> _state.value // Error/Completed — stay as-is (immutable)
            }
    }

    /**
     * Update paused state — call when player pauses.
     * Only allows transitions from Playing to Paused.
     * Error and Completed states are immutable unless explicitly reset via prepare().
     */
    fun updatePaused() {
        val progress =
            when (val s = _state.value) {
                is PlayerState.Playing -> s.progress
                is PlayerState.Paused -> s.progress
                else -> return // Don't transition from Error/Completed
            }
        _state.value = PlayerState.Paused(progress)
    }

    /**
     * Update completed state — call when video reaches 80%+.
     */
    fun updateCompleted() {
        _state.value = PlayerState.Completed
        currentVideo?.let { video ->
            savePlaybackPosition(
                videoId = video.id,
                title = video.title,
                thumbnail = video.thumbnail,
                playedPercentage = 100,
                contentUrl = video.playableUrl,
            )
            markAsWatched(video.id)
        }
    }

    /**
     * Update error state.
     */
    fun updateError(message: String) {
        _state.value = PlayerState.Error(message)
    }

    /**
     * Save playback position to Room DB for continue-watching.
     * Called every 10 seconds during playback.
     */
    fun savePlaybackPosition(
        videoId: String,
        title: String,
        thumbnail: String,
        playedPercentage: Int,
        contentUrl: String,
    ) {
        playerRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    repo.savePlaybackPosition(videoId, title, thumbnail, playedPercentage, contentUrl)
                } catch (e: Throwable) {
                    Logger.e("PlayerViewModel", e, "Failed to save playback position")
                }
            }
        }
    }

    /**
     * Mark the current video as fully watched.
     * Used for ContinueWatching integration.
     */
    fun markAsWatched(videoId: String) {
        playerRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    repo.markAsWatched(videoId)
                } catch (e: Throwable) {
                    Logger.e("PlayerViewModel", e, "Failed to mark as watched")
                }
            }
        }
    }

    /**
     * Reset player to idle state.
     */
    fun reset() {
        currentVideo = null
        _state.value = PlayerState.Idle
    }

    /**
     * Retry playback after an error (#4.8).
     * Resets state to Loading and re-triggers prepare if video is available.
     */
    fun retry() {
        val video = currentVideo ?: return
        _state.value = PlayerState.Loading
        viewModelScope.launch {
            try {
                val streamUrl = video.playableUrl
                if (streamUrl.isNotBlank()) {
                    _state.value = PlayerState.Playing()
                } else {
                    _state.value = PlayerState.Error("No playable URL for this video")
                }
            } catch (e: Throwable) {
                _state.value = PlayerState.Error(e.message ?: "Failed to retry video")
            }
        }
    }

    /**
     * Set the current video context for retry support.
     */
    fun setCurrentVideo(video: VideoItem) {
        currentVideo = video
    }
}
