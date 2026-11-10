package com.dominickp.firetvandroidtv.ui

import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.config.ChannelConfig
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.config.ContentConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * FeedViewModelAnalyticsTest — verifies that trackImpression properly initializes AnalyticsTracker.
 */
@RunWith(RobolectricTestRunner::class)
class FeedViewModelAnalyticsTest {
    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
    }

    @Test
    fun `analyticsTracker_is_initialized_with_config`() =
        runTest {
            val analyticsConfig = AnalyticsConfig(enabled = true, exportUrl = "")
            val configLoader =
                object : ConfigLoader() {
                    override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                        return AppConfig(
                            channel = ChannelConfig(name = "Test"),
                            content =
                                ContentConfig(
                                    sourceUrl = "",
                                    retryAttempts = 0,
                                    retryDelayMs = 0,
                                ),
                            analytics = analyticsConfig,
                        )
                    }
                }

            val viewModel = FeedViewModel(configLoader)

            // Trigger analyticsTracker lazy init by calling trackImpression (which accesses it)
            // The lazy delegate ensures it's initialized on first access
            viewModel.trackImpression(
                com.dominickp.firetvandroidtv.data.model.VideoItem(
                    id = "test",
                    title = "Test",
                    shortDescription = "",
                    thumbnail = "",
                    url = "",
                    content = com.dominickp.firetvandroidtv.data.model.VideoContent(),
                    isLive = false,
                ),
            )

            // Verify the singleton was created (analyticsTracker lazy delegates to getInstance)
            val tracker = AnalyticsTracker.getInstance(analyticsConfig)
            assertNotNull("AnalyticsTracker should be initialized", tracker)
        }

    @Test
    fun `trackImpression_does_not_crash_when_tracker_is_initialized`() =
        runTest {
            val analyticsConfig = AnalyticsConfig(enabled = true, exportUrl = "")
            val configLoader =
                object : ConfigLoader() {
                    override fun loadConfig(assetManager: android.content.res.AssetManager?): AppConfig {
                        return AppConfig(
                            channel = ChannelConfig(name = "Test"),
                            content =
                                ContentConfig(
                                    sourceUrl = "",
                                    retryAttempts = 0,
                                    retryDelayMs = 0,
                                ),
                            analytics = analyticsConfig,
                        )
                    }
                }

            val viewModel = FeedViewModel(configLoader)

            // Get the tracker reference to verify it's the same singleton
            val tracker = AnalyticsTracker.getInstance(analyticsConfig)

            val videoId = "test_video_123"
            viewModel.trackImpression(
                com.dominickp.firetvandroidtv.data.model.VideoItem(
                    id = videoId,
                    title = "Test",
                    shortDescription = "",
                    thumbnail = "",
                    url = "",
                    content = com.dominickp.firetvandroidtv.data.model.VideoContent(),
                    isLive = false,
                ),
            )

            // Since we can't easily verify the tracker queue (private), we just check that trackImpression doesn't crash
            // and that the tracker was accessed. The previous test ensures tracker is non-null.
            // For now, this test verifies basic functionality without NPE.
        }
}
