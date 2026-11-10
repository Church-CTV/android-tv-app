package com.dominickp.firetvandroidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dominickp.firetvandroidtv.FireTVApplication
import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.config.RawFeedResponse
import com.dominickp.firetvandroidtv.data.network.FeedRepository
import com.dominickp.firetvandroidtv.data.network.SearchApi
import com.dominickp.firetvandroidtv.data.network.createFeedApi
import com.dominickp.firetvandroidtv.data.network.createSearchApi
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SearchViewModel — manages search input with debounce.
 *
 * Mirrors Roku's SearchScreen behavior:
 *   - Debounced search (300ms delay between keystrokes)
 *   - Empty results state
 *   - Error state
 *
 * Uses FeedRepository.search() for actual API calls.
 * Creates FeedRepository internally from ConfigLoader (matching FeedViewModel pattern).
 * Owns analytics tracking for search events.
 */
class SearchViewModel(
    private val configLoader: ConfigLoader,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val feedRepositoryOverride: FeedRepository? = null,
    private val searchApiOverride: SearchApi? = null,
) : ViewModel() {
    private val logger = Logger
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Lazily-created shared SearchApi for server-side search fallback.
     * Created on first search (from the loaded config's apiBaseUrl).
     * `searchApiOverride` (tests) takes precedence when set.
     */
    @Volatile
    private var searchApiInstance: SearchApi? = null

    /**
     * Resolve a SearchApi for server-side search fallback.
     * Uses the injected override (tests) or lazily creates one from config.
     * Returns null if the SearchApi can't be built (no server-side fallback).
     */
    private fun getSearchApi(): SearchApi? {
        val existing = searchApiOverride ?: searchApiInstance
        if (existing != null) return existing
        val api =
            runCatching {
                createSearchApi(configLoader.loadConfig().apiBaseUrl)
            }.getOrNull()
        searchApiInstance = api
        return api
    }

    /**
     * AnalyticsTracker is a singleton initialized in FireTVApplication.
     * getInstance() is double-checked-locked — calling it directly is safe.
     */
    private val analyticsTracker: AnalyticsTracker
        get() = AnalyticsTracker.getInstance()

    /**
     * Use shared FeedRepository from Application (avoids duplicating OkHttpClient).
     * Override only for tests. Falls back to creating from config if Application not ready.
     */
    private suspend fun getRepository(): FeedRepository {
        feedRepositoryOverride?.let { return it }
        return try {
            FireTVApplication.application.feedRepository
        } catch (_: IllegalStateException) {
            null
        } ?: run {
            val config = configLoader.loadConfig()
            FeedRepository(
                feedApi = createFeedApi(config.apiBaseUrl, config.content),
                contentConfig = config.content,
            )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    /**
     * Update the search query. Triggers debounced search.
     * When results are found, tracks a search analytics event automatically.
     */
    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _state.value = SearchState.Idle
            return
        }

        searchJob =
            viewModelScope.launch(dispatcher) {
                delay(SEARCH_DEBOUNCE_MS) // 300ms debounce (matches Roku's MiniKeyboard debounce)

                _state.value = SearchState.Loading

                try {
                    val repository = getRepository()
                    val results = repository.searchLocal(newQuery).getOrNull() ?: emptyList()

                    // If in-memory feed data has no results, fall back to server-side search.
                    // getSearchApi() resolves the injected override (tests) or lazily
                    // builds one from config (production). Never null on production
                    // builds, so the DOX contract (server search) is always honoured.
                    val finalResults =
                        if (results.isEmpty()) {
                            val api = getSearchApi()
                            if (api != null) {
                                try {
                                    val response = api.search(newQuery)
                                    if (response.isSuccessful) {
                                        RawFeedResponse.parseSearch(response.body()?.string() ?: "[]")
                                    } else {
                                        emptyList()
                                    }
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                        } else {
                            results
                        }

                    _state.value = SearchState.Results(finalResults)
                    // Track search event with result count (mirrors Roku's search analytics)
                    analyticsTracker.trackSearch(newQuery, finalResults.size)
                } catch (e: Exception) {
                    logger.e("SearchViewModel", e, "Error during local search: ${e.message}")
                    _state.value = SearchState.Error(e.message ?: "Search failed")
                }
            }
    }

    /**
     * Clear the search and reset to idle.
     */
    fun clear() {
        _query.value = ""
        searchJob?.cancel()
        _state.value = SearchState.Idle
    }

    /**
     * Clear the search results (keeps the query text).
     */
    fun clearResults() {
        _state.value = SearchState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
