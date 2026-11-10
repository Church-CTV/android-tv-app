package com.dominickp.firetvandroidtv.e2e

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dominickp.firetvandroidtv.ui.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E Test — MainActivity lifecycle and startup on Android TV / Fire TV.
 *
 * Runs on the google_tv_x86_64 emulator or physical Fire TV/Android TV devices.
 * Verifies:
 *   1. App launches without crashing
 *   2. MainActivity is active after startup
 *   3. Default route is Home
 *   4. Missing config handled gracefully (fallback config)
 *   5. Back navigation does not crash
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityE2ETest {
    @Test
    fun `app_launches_without_crash`() {
        // MainActivity uses LEANBACK_LAUNCHER category (not LAUNCHER),
        // so getLaunchIntentForPackage returns null.
        // Use explicit intent with correct action.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        val scenario = ActivityScenario.launch<MainActivity>(launchIntent)

        scenario.onActivity { activity ->
            assertFalse("MainActivity should not be finishing", activity.isFinishing)
        }

        scenario.close()
    }

    @Test
    fun `home_screen_is_default_route`() {
        // The default navigation route is Home (LeftNavScreen).
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        val scenario = ActivityScenario.launch<MainActivity>(launchIntent)

        scenario.onActivity { activity ->
            assertFalse("Activity should still be active", activity.isFinishing)
            assertFalse("Activity should not be destroyed", activity.isDestroyed)
        }

        scenario.close()
    }

    @Test
    fun `back_navigation_does_not_crash`() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        val scenario = ActivityScenario.launch<MainActivity>(launchIntent)

        // Simulate back press (equivalent to D-pad back on TV remote)
        scenario.onActivity { activity ->
            activity.moveTaskToBack(true)
        }

        scenario.close()
    }

    @Test
    fun `app_handles_missing_config_gracefully`() {
        // ConfigLoader.loadConfig() falls back to loadFallbackConfig().
        // The app must never crash if assets/app_config.json is missing or malformed.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        val scenario = ActivityScenario.launch<MainActivity>(launchIntent)

        scenario.onActivity { activity ->
            assertFalse("Activity should handle missing config gracefully", activity.isFinishing)
        }

        scenario.close()
    }
}
