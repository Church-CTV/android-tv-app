package com.dominickp.firetvandroidtv.ui.navigation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RemoteKeyHandlerTest {

    // === VoiceSearch key handling ===

    @Test
    fun `search key down triggers VoiceSearch`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = RemoteKeyHandler.KEYCODE_SEARCH,
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertEquals(RemoteKeyAction.VoiceSearch, action)
    }

    @Test
    fun `voice assistant key down triggers VoiceSearch`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = RemoteKeyHandler.KEYCODE_VOICE_ASSIST,
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertEquals(RemoteKeyAction.VoiceSearch, action)
    }

    @Test
    fun `search key up returns null`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = RemoteKeyHandler.KEYCODE_SEARCH,
            action = RemoteKeyHandler.ACTION_UP,
        )
        assertNull(action)
    }

    @Test
    fun `unmapped key returns null`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = 1, // KEYCODE_UNKNOWN
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertNull(action)
    }

    @Test
    fun `D-pad up is not intercepted`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = 19, // KEYCODE_DPAD_UP
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertNull(action)
    }

    @Test
    fun `D-pad down is not intercepted`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = 20, // KEYCODE_DPAD_DOWN
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertNull(action)
    }

    @Test
    fun `OK key is not intercepted`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = 23, // KEYCODE_DPAD_CENTER
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertNull(action)
    }

    // === Channel key handling ===

    @Test
    fun `channel up key down triggers NextChannel`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = RemoteKeyHandler.KEYCODE_CHANNEL_UP,
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertEquals(RemoteKeyAction.NextChannel, action)
    }

    @Test
    fun `channel down key down triggers PreviousChannel`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = RemoteKeyHandler.KEYCODE_CHANNEL_DOWN,
            action = RemoteKeyHandler.ACTION_DOWN,
        )
        assertEquals(RemoteKeyAction.PreviousChannel, action)
    }

    @Test
    fun `channel up key up returns null`() {
        val action = RemoteKeyHandler.handleKey(
            keyCode = RemoteKeyHandler.KEYCODE_CHANNEL_UP,
            action = RemoteKeyHandler.ACTION_UP,
        )
        assertNull(action)
    }

    // === shouldIntercept ===

    @Test
    fun `shouldIntercept true for voice search key`() {
        assertTrue(
            RemoteKeyHandler.shouldIntercept(
                keyCode = RemoteKeyHandler.KEYCODE_VOICE_ASSIST,
                action = RemoteKeyHandler.ACTION_DOWN,
            ),
        )
    }

    @Test
    fun `shouldIntercept false for D-pad key`() {
        assertFalse(
            RemoteKeyHandler.shouldIntercept(
                keyCode = 19,
                action = RemoteKeyHandler.ACTION_DOWN,
            ),
        )
    }

    @Test
    fun `shouldIntercept false for ACTION_UP even on voice key`() {
        assertFalse(
            RemoteKeyHandler.shouldIntercept(
                keyCode = RemoteKeyHandler.KEYCODE_VOICE_ASSIST,
                action = RemoteKeyHandler.ACTION_UP,
            ),
        )
    }

    // === RemoteKeyEventBus ===

    @Test
    fun `event bus delivers emitted action to collector`() = runBlocking {
        val bus = RemoteKeyEventBus()
        val received = CompletableDeferred<RemoteKeyAction>()

        // Unconfined dispatcher: collector subscribes synchronously before returning
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            bus.events.first { action ->
                received.complete(action)
                true
            }
        }
        bus.emit(RemoteKeyAction.VoiceSearch)
        val result = withTimeout(1000) { received.await() }
        job.cancel()
        assertEquals(RemoteKeyAction.VoiceSearch, result)
    }

    @Test
    fun `event bus collects multiple actions`() = runBlocking {
        val bus = RemoteKeyEventBus()
        val received = mutableListOf<RemoteKeyAction>()
        val done = CompletableDeferred<Unit>()

        // Unconfined dispatcher: collector subscribes synchronously before returning
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            bus.events.collect { action ->
                received.add(action)
                if (received.size >= 2) {
                    done.complete(Unit)
                    return@collect
                }
            }
        }
        bus.emit(RemoteKeyAction.VoiceSearch)
        bus.emit(RemoteKeyAction.NextChannel)
        withTimeout(1000) { done.await() }
        job.cancel()
        assertEquals(2, received.size)
        assertTrue(received.contains(RemoteKeyAction.VoiceSearch))
        assertTrue(received.contains(RemoteKeyAction.NextChannel))
    }
}
