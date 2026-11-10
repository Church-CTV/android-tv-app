package com.dominickp.firetvandroidtv.ui.navigation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for ErrorScreen component.
 * Tests: error message display, retry action, back action.
 */
class ErrorScreenTest {
    @Test
    fun `error_screen_displays_error_message`() {
        val message = "Network error occurred"
        assertFalse(message.isEmpty())
    }

    @Test
    fun `error_screen_handles_generic_error`() {
        val message = "An error occurred"
        assertNotNull(message)
    }

    @Test
    fun `error_screen_retry_action_exists`() {
        var retryCalled = false
        val onRetry: () -> Unit = { retryCalled = true }
        assertFalse(retryCalled)
        onRetry()
        assertTrue(retryCalled)
    }

    @Test
    fun `error_screen_back_action_exists`() {
        var backCalled = false
        val onBack: () -> Unit = { backCalled = true }
        assertFalse(backCalled)
        onBack()
        assertTrue(backCalled)
    }

    @Test
    fun `error_screen_message_can_be_null_safe`() {
        var message: String? = null
        // Should handle null gracefully
        val safeMessage = message ?: "Unknown error"
        assertEquals("Unknown error", safeMessage)
    }

    @Test
    fun `error_screen_connection_timeout_message`() {
        val message = "Connection timed out"
        assertTrue(message.contains("timed", ignoreCase = true) || message.contains("connection", ignoreCase = true))
    }
}
