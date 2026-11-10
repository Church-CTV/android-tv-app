package com.dominickp.firetvandroidtv.ui.search

/**
 * SearchState — mirrors Roku's search loading states.
 * State machine: [Idle] → [Loading] → [Results] | [Error]
 */
sealed interface SearchState {
    object Idle : SearchState

    object Loading : SearchState

    data class Results(val items: List<com.dominickp.firetvandroidtv.data.model.VideoItem>) : SearchState

    data class Error(val message: String) : SearchState
}
