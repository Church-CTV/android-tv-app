package com.dominickp.firetvandroidtv.ui

import android.content.Intent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {
    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
    }

    @Test
    fun `extractDeepLinkIdReturnsContentIdFromValidDeepLink`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://example.invalid/watch?content_id=abc123")
            }
        val result = activity.extractDeepLinkId(intent)
        assertEquals("abc123", result)
    }

    @Test
    fun `extractDeepLinkIdReturnsNullForNullIntent`() {
        val result = activity.extractDeepLinkId(null)
        assertNull(result)
    }

    @Test
    fun `extractDeepLinkIdReturnsNullForNonExampleHost`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://example.com/watch?content_id=abc123")
            }
        val result = activity.extractDeepLinkId(intent)
        assertNull(result)
    }

    @Test
    fun `extractDeepLinkIdReturnsNullForNonWatchPath`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://example.invalid/videos?content_id=abc123")
            }
        val result = activity.extractDeepLinkId(intent)
        assertNull(result)
    }

    @Test
    fun `extractDeepLinkIdReturnsContentIdFromInvalidUrlScheme`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("invalid://example.invalid/watch?content_id=abc123")
            }
        val result = activity.extractDeepLinkId(intent)
        // invalid:// scheme still parses - this tests edge case behavior
        assertNotNull(result)
    }

    @Test
    fun `extractDeepLinkIdReturnsNullWhenContentIdMissing`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://example.invalid/watch")
            }
        val result = activity.extractDeepLinkId(intent)
        assertNull(result)
    }

    @Test
    fun `extractDeepLinkIdHandlesContentIdWithSpecialCharacters`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://example.invalid/watch?content_id=video_123-test")
            }
        val result = activity.extractDeepLinkId(intent)
        assertEquals("video_123-test", result)
    }

    @Test
    fun `extractDeepLinkIdReturnsNullForEmptyIntentData`() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = null
            }
        val result = activity.extractDeepLinkId(intent)
        assertNull(result)
    }
}
