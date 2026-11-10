package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnalyticsTrackerRokuParityTest — verifies that AnalyticsTracker only contains
 * event types that exist in the Roku event set.
 *
 * Roku event set (from roku-channel-template):
 *   - Impression
 *   - Play
 *   - Pause
 *   - Complete
 *   - Search
 *   - Subscribe
 *   - Error
 *
 * Any additional event type is a violation of the Roku parity requirement:
 * "Event types must match Roku's event set exactly. No additions, no deletions."
 *
 * RED: This test failed because SettingsChange was an unauthorized 8th event type.
 * GREEN: Removing SettingsChange makes this test pass with exactly 7 event types.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalyticsTrackerRokuParityTest {
    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
    }

    @Test
    fun `exactlySevenRokuEventTypes_queued()`() {
        val tracker =
            AnalyticsTracker.getInstance(
                AnalyticsConfig(enabled = false, exportUrl = ""),
            )

        // Queue one of each authorized event type
        tracker.trackImpression("v", "r", 0)
        tracker.trackPlay("v", "u", false, "h")
        tracker.trackPause("v", 0.5f)
        tracker.trackComplete("v", 1.0f, 1000L)
        tracker.trackSearch("q", 5)
        tracker.trackSubscribe("plan")
        tracker.trackError("type", "msg")

        assertEquals(
            "Exactly 7 Roku event types should be queued — " +
                "any more means an unauthorized event type was added",
            7,
            tracker.queueSize(),
        )
    }
}
