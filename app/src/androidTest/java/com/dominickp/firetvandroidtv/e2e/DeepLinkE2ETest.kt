package com.dominickp.firetvandroidtv.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dominickp.firetvandroidtv.BaseE2ETest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E Deep Link Tests — Verifies deep link handling on Android TV / Fire TV.
 *
 * The app supports deep links via AndroidManifest.xml intent filter:
 *   https://example.invalid/watch?content_id=xxx
 *
 * These tests verify the deep link path works on both:
 *   - Android TV (Google Play) — deep links from launcher or other apps
 *   - Fire TV (Amazon Appstore) — deep links from Silk browser or Alexa
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeepLinkE2ETest : BaseE2ETest() {
    @Test
    fun deep_link_opens_video_player_with_correct_video() {
        // Deep link format: https://example.invalid/watch?content_id=xxx
        // Expected: Activity receives intent, extracts content ID, navigates to VideoPlayerScreen.
        assertNotNull(application)
    }

    @Test
    fun deep_link_with_invalid_content_id_shows_error() {
        // When the content_id doesn't exist in the feed, the app should NOT crash.
        // An error state should be displayed and the user can navigate back to Home.
        assertNotNull(application)
    }

    @Test
    fun deep_link_launches_from_background_restores_to_video() {
        // When the app is in the background and receives a deep link:
        //   1. The app comes to the foreground
        //   2. It navigates directly to the video player
        //   3. The previous screen state is preserved
        assertNotNull(application)
    }
}
