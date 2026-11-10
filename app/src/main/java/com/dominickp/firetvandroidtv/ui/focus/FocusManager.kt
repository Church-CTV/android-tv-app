package com.dominickp.firetvandroidtv.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * FocusZone — tracks which UI zone currently has focus.
 */
enum class FocusZone {
    /** Sidebar navigation has focus. */
    SIDEBAR,

    /** Content area (cards, rows) has focus. */
    CONTENT,

    /** No zone has focus (transient state during transfer). */
    NONE,
}

/**
 * FocusManager — coordinates focus state and restoration for Android TV / Fire TV.
 *
 * Thread-safe via synchronized blocks on a single mutex. Mirrors Roku FocusManager.xml:
 *   - On screen change: save last focused element, restore on return
 *   - On video stop: restore focus to the previously focused element
 *   - Focus is always constrained to visible elements (no lost focus)
 */
@Stable
class FocusManager {
    companion object {
        @Volatile
        private var instance: FocusManager? = null

        fun getInstance(): FocusManager {
            return instance ?: synchronized(this) {
                instance ?: FocusManager().also { instance = it }
            }
        }
    }

    // Thread-safe state — all accesses guarded by _mutex via synchronized blocks.
    // Plain fields under synchronized are sufficient; AtomicReference was redundant.
    private val _mutex = Any()
    private var _requesters: MutableMap<String, FocusRequester> = mutableMapOf()
        @Synchronized get
    private var _currentScreen: String? = null
        @Synchronized get
    private var _lastFocusedItemId: String? = null
        @Synchronized get
    private var _videoFocusRestoration: String? = null
        @Synchronized get
    private var _currentZone: FocusZone = FocusZone.CONTENT
        @Synchronized get

    /** The current focus zone: SIDEBAR, CONTENT, or NONE. */
    var currentZone: FocusZone
        get() = synchronized(_mutex) { _currentZone }
        set(value) {
            synchronized(_mutex) { _currentZone = value }
        }

    /** The current active screen name/ID. */
    var currentScreen: String?
        get() = synchronized(_mutex) { _currentScreen }
        set(value) {
            synchronized(_mutex) {
                val oldScreen = _currentScreen
                val focusedId = _lastFocusedItemId
                // Only save restoration state when actually switching screens with a focused element
                if (oldScreen != null && focusedId != null && oldScreen != value) {
                    _videoFocusRestoration = focusedId
                }
                _currentScreen = value
            }
        }

    /** The ID of the element that currently has focus. */
    var lastFocusedItemId: String?
        get() = synchronized(_mutex) { _lastFocusedItemId }
        set(value) {
            synchronized(_mutex) { _lastFocusedItemId = value }
        }

    /**
     * Save the focus state for later restoration (e.g., before entering video playback).
     */
    fun saveFocusState(
        screen: String,
        focusedItemId: String?,
    ) {
        synchronized(_mutex) {
            _currentScreen = screen
            focusedItemId?.let { _lastFocusedItemId = it }
        }
    }

    /**
     * Restore the focus state after video stops or screen return.
     * Only clears restoration target if focus was actually restored.
     * Thread-safe: lookup is within the synchronized block; requestFocus()
     * is called outside to avoid holding the lock while Compose recomposes.
     */
    fun restoreFocus(): Boolean {
        val id: String?
        val requester: FocusRequester?
        synchronized(_mutex) {
            id = _videoFocusRestoration ?: return false
            requester = _requesters[id]
        }
        if (requester != null) {
            requester.requestFocus()
            synchronized(_mutex) { _videoFocusRestoration = null }
        }
        return requester != null
    }

    /**
     * Clear the focus restoration state after it's been used.
     */
    fun clearRestoration() {
        synchronized(_mutex) { _videoFocusRestoration = null }
    }

    /**
     * Register a FocusRequester with a unique ID.
     * Thread-safe: reads and mutates the map within the synchronized block.
     */
    fun register(
        id: String,
        requester: FocusRequester,
    ) {
        synchronized(_mutex) { _requesters[id] = requester }
    }

    /**
     * Unregister a FocusRequester from the registry.
     * Thread-safe: reads and removes within the synchronized block.
     */
    fun unregister(id: String) {
        synchronized(_mutex) { _requesters.remove(id) }
    }

    /**
     * Request focus for the element associated with [id].
     * Returns true if a requester was found and focus was requested.
     * Thread-safe: lookup is within the synchronized block.
     */
    fun requestFocus(id: String): Boolean {
        val requester: FocusRequester?
        synchronized(_mutex) { requester = _requesters[id] }
        return if (requester != null) {
            requester.requestFocus()
            true
        } else {
            false
        }
    }

    /**
     * Restore focus to the last known focused item if it is still registered.
     * Thread-safe: lookup is within the synchronized block.
     */
    fun restoreLastFocus(): Boolean {
        val id: String?
        val requester: FocusRequester?
        synchronized(_mutex) {
            id = _lastFocusedItemId ?: return false
            requester = _requesters[id]
        }
        return if (requester != null) {
            requester.requestFocus()
            true
        } else {
            false
        }
    }

    /**
     * Clear all registered requesters (useful during major navigation changes).
     * Also clears focus restoration state to prevent restoring to stale elements.
     */
    fun clearRegistry() {
        synchronized(_mutex) {
            _requesters.clear()
            _lastFocusedItemId = null
            _videoFocusRestoration = null
        }
    }

    /**
     * Get the number of registered requesters (for testing).
     */
    internal fun registeredRequesterCount(): Int = synchronized(_mutex) { _requesters.size }

    /**
     * Get the current video focus restoration target ID (for testing).
     */
    internal val videoFocusRestorationForTesting: String?
        get() = synchronized(_mutex) { _videoFocusRestoration }
}

/**
 * Compose-aware FocusManager extension functions for Compose UI.
 */

/**
 * A composable Modifier that:
 * 1. Creates a FocusRequester
 * 2. Registers it with the FocusManager
 * 3. Tracks focus state changes (updates lastFocusedItemId)
 * 4. Unregisters on dispose to prevent memory leaks
 */
@Composable
fun Modifier.trackFocus(
    elementId: String,
    focusManager: FocusManager = FocusManager.getInstance(),
): Modifier =
    composed {
        val requester = remember { FocusRequester() }

        // Register on create, unregister on dispose
        DisposableEffect(elementId, requester) {
            focusManager.register(elementId, requester)
            onDispose {
                focusManager.unregister(elementId)
            }
        }

        this
            .focusRequester(requester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused || focusState.hasFocus) {
                    focusManager.lastFocusedItemId = elementId
                }
            }
    }
