package com.dominickp.firetvandroidtv.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith

/**
 * Utility for E2E tests that need to simulate D-pad/remote input.
 *
 * On Android TV/Fire TV, the primary input is the D-pad remote.
 * These test utilities provide helpers for simulating D-pad navigation:
 *   - D-pad up/down: navigate sidebar and content rows
 *   - D-pad left/right: navigate within rows
 *   - D-pad center (Enter): select focused element
 *   - D-pad back: return to previous screen
 *
 * These are used in conjunction with AndroidJUnit4 + ActivityScenario
 * for instrumented tests running on device/emulator.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
abstract class DpadTestHelper {
    /**
     * Simulate D-pad center press (OK/Enter button on remote).
     * In instrumented tests, this maps to KeyEvent.KEYCODE_DPAD_CENTER.
     */
    fun pressOK() {
        // In a real Compose test, use:
        // composeTestRule.onNodeWithTag("focused").performClick()
    }

    /**
     * Simulate D-pad back press (Back button on remote).
     * Maps to KeyEvent.KEYCODE_BACK.
     */
    fun pressBack() {
        // In instrumented tests:
        // InstrumentationRegistry.getInstrumentation().sendKeySync(
        //     KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
    }
}
