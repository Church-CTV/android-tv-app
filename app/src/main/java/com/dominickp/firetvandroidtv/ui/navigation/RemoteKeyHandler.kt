package com.dominickp.firetvandroidtv.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Actions that can be triggered by hardware remote key presses.
 *
 * Mirrors Roku's hardware key handling. On Fire TV remotes, the voice button
 * (KEYCODE_SEARCH / KEYCODE_VOICE_ASSIST) should open the search screen.
 * Channel up/down buttons can be used for quick navigation between rows.
 */
sealed interface RemoteKeyAction {
    /** Voice/search button pressed — navigate to search screen. */
    data object VoiceSearch : RemoteKeyAction

    /** Channel up button — navigate to next row/section. */
    data object NextChannel : RemoteKeyAction

    /** Channel down button — navigate to previous row/section. */
    data object PreviousChannel : RemoteKeyAction
}

/**
 * RemoteKeyHandler — interprets hardware key events for TV-specific navigation.
 *
 * Pure logic object with no Android framework dependencies.
 * All keycode and action constants mirror `android.view.KeyEvent` values
 * so the handler can be tested on the JVM.
 *
 * Usage: call [handleKey] in `MainActivity.dispatchKeyEvent` and pass the
 * resulting [RemoteKeyAction] to `AppNavigation` via the event bus.
 */
object RemoteKeyHandler {
    // android.view.KeyEvent action constants
    const val ACTION_DOWN = 0
    const val ACTION_UP = 1

    // android.view.KeyEvent keycode constants (Fire TV / Android TV relevant)
    const val KEYCODE_SEARCH = 84
    const val KEYCODE_VOICE_ASSIST = 235
    const val KEYCODE_CHANNEL_UP = 165
    const val KEYCODE_CHANNEL_DOWN = 166

    /**
     * Interpret a key event and return the corresponding action, or null
     * if the key should not be intercepted by the app.
     *
     * Only ACTION_DOWN events produce actions — ACTION_UP is always null
     * to prevent double-firing on key release.
     *
     * @param keyCode The event's keyCode (matches android.view.KeyEvent.keyCode)
     * @param action The event's action (matches android.view.KeyEvent.action)
     * @return The [RemoteKeyAction] to handle, or null to pass through.
     */
    fun handleKey(keyCode: Int, action: Int): RemoteKeyAction? =
        when {
            action != ACTION_DOWN -> null
            keyCode == KEYCODE_SEARCH || keyCode == KEYCODE_VOICE_ASSIST ->
                RemoteKeyAction.VoiceSearch
            keyCode == KEYCODE_CHANNEL_UP -> RemoteKeyAction.NextChannel
            keyCode == KEYCODE_CHANNEL_DOWN -> RemoteKeyAction.PreviousChannel
            else -> null
        }

    /**
     * Check whether this key event should be consumed by the app.
     * Returns true if [handleKey] would produce a non-null action.
     */
    fun shouldIntercept(keyCode: Int, action: Int): Boolean =
        handleKey(keyCode, action) != null
}

/**
 * RemoteKeyEventBus — holds the SharedFlow of remote key events.
 *
 * Created once in MainActivity, passed to AppNavigation.
 * AppNavigation collects from [events] to react to remote key presses.
 *
 * Uses extraBufferCapacity=1 to prevent tryEmit from failing when
 * AppNavigation hasn't started collecting yet (e.g., during splash screen).
 */
class RemoteKeyEventBus {
    private val _events =
        MutableSharedFlow<RemoteKeyAction>(
            replay = 0,
            extraBufferCapacity = 1,
        )

    /** The shared flow of remote key events. Collect in AppNavigation. */
    val events: SharedFlow<RemoteKeyAction> = _events.asSharedFlow()

    /** Emit a remote key action. Non-blocking (tryEmit). */
    fun emit(action: RemoteKeyAction) {
        _events.tryEmit(action)
    }
}
