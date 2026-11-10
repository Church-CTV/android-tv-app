package com.dominickp.firetvandroidtv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dominickp.firetvandroidtv.FireTVApplication
import com.dominickp.firetvandroidtv.analytics.AnalyticsEvent
import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.data.network.FeedRepository
import com.dominickp.firetvandroidtv.data.network.createFeedApi
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

/**
 * FeedUiState — mirrors Roku's feed loading states.
 * State machine: [Loading] → [Success] | [Error]
 *                [Error] → retry() → [Loading] → ...
 */
sealed interface FeedUiState {
    object Loading : FeedUiState

    data class Success(
        val groups: Map<String, List<VideoItem>>,
    ) : FeedUiState

    data class Error(
        val message: String,
    ) : FeedUiState
}

/**
 * FeedViewModel — mirrors Roku's MainLoaderTask.
 *
 * Handles feed loading, retry, and error states.
 * Exposes StateFlow<FeedUiState> to Compose.
 */
class FeedViewModel(
    private val configLoader: ConfigLoader,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val logger = Logger
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // AnalyticsTracker is a singleton initialized in FireTVApplication.
    // getInstance() is double-checked-locked — calling it directly is safe and idempotent.
    // No need for local caching or config reloading.
    private val analyticsTracker: AnalyticsTracker
        get() = AnalyticsTracker.getInstance()

    init {
        loadFeed()
    }

    /**
     * Get FeedRepository: uses shared instance from FireTVApplication (production),
     * or creates a new one from config (tests / standalone use).
     */
    private suspend fun getRepository(): FeedRepository {
        // Try shared instance first (production path)
        try {
            FireTVApplication.application.feedRepository?.let { return it }
        } catch (_: IllegalStateException) {
            // FireTVApplication not initialized (e.g., unit tests)
        }

        // Fallback: create from config (test path)
        val config = configLoader.loadConfig()
        return FeedRepository(
            feedApi = createFeedApi(config.apiBaseUrl, config.content),
            contentConfig = config.content,
        )
    }

    fun loadFeed() {
        logger.d("FeedViewModel", "loadFeed()")
        _uiState.value = FeedUiState.Loading
        viewModelScope.launch(dispatcher) {
            try {
                val repository = getRepository()

                repository
                    .fetchFeed()
                    .onSuccess { groups: Map<String, List<VideoItem>> ->
                        logger.i("FeedViewModel", "Feed loaded: ${groups.size} groups")
                        _uiState.value = FeedUiState.Success(groups)
                    }.onFailure { throwable: Throwable ->
                        logger.e("FeedViewModel", throwable, "Feed load failed: ${throwable.message}")
                        _uiState.value = FeedUiState.Error(throwable.message ?: "Failed to load feed")
                    }
            } catch (e: java.io.IOException) {
                logger.e("FeedViewModel", e, "Network/feed read error: ${e.message}")
                _uiState.value = FeedUiState.Error(e.message ?: "Failed to load feed")
            } catch (e: SerializationException) {
                logger.e("FeedViewModel", e, "Invalid feed JSON: ${e.message}")
                _uiState.value = FeedUiState.Error(e.message ?: "Failed to parse feed")
            } catch (e: RuntimeException) {
                logger.e("FeedViewModel", e, "Runtime error in feed load: ${e.message}")
                _uiState.value = FeedUiState.Error(e.message ?: "Failed to load feed")
            }
        }
    }

    /**
     * Retry the last feed fetch.
     */
    fun retry() {
        logger.d("FeedViewModel", "retry()")
        loadFeed()
    }

    /**
     * Find a video by ID in the current feed groups.
     */
    private fun findVideoByIdInGroups(
        videoId: String,
        groups: Map<String, List<com.dominickp.firetvandroidtv.data.model.VideoItem>>,
    ): com.dominickp.firetvandroidtv.data.model.VideoItem? = groups.values.flatten().find { it.id == videoId }

    /**
     * Resolve a deep link video ID against the loaded feed.
     *
     * Returns null if the feed hasn't loaded successfully or the video ID isn't found.
     *
     * @param videoId The content_id from the deep link URL
     * @return The VideoItem if found, null otherwise
     */
    fun resolveDeepLink(videoId: String): com.dominickp.firetvandroidtv.data.model.VideoItem? =
        when (val state = _uiState.value) {
            is FeedUiState.Success -> findVideoByIdInGroups(videoId, state.groups)
            else -> null
        }

    /**
     * Check if the feed is currently loading.
     */
    val isLoading: Boolean
        get() = _uiState.value is FeedUiState.Loading

    /**
     * Get the current feed data if available.
     */
    val feedData: Map<String, List<VideoItem>>?
        get() =
            when (val state = _uiState.value) {
                is FeedUiState.Success -> state.groups
                else -> null
            }

    /**
     * Track an impression for a video card.
     * Test-only convenience method — production impressions are tracked in AppNavigation.
     */
    @Suppress("unused") // used by FeedViewModelAnalyticsTest
    fun trackImpression(videoItem: VideoItem) {
        analyticsTracker.track(
            AnalyticsEvent.Impression(
                videoId = videoItem.id,
                rowLabel = "",
                position = 0,
            ),
        )
    }
}
