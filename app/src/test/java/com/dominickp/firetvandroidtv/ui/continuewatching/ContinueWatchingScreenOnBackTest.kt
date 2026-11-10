package com.dominickp.firetvandroidtv.ui.continuewatching

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ContinueWatchingScreenOnBackTest — verifies that when the video player
 * exits from ContinueWatchingScreen, the parent's onBack callback is invoked.
 *
 * Bug: previously onBack was { selectedEntry = null } which only cleared
 * local state without calling the parent's onBack, leaving users stuck
 * on the Continue Watching screen after video playback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContinueWatchingScreenOnBackTest {
    private companion object {
        private const val SRC =
            "src/main/java/com/dominickp/firetvandroidtv/ui/continuewatching/ContinueWatchingScreen.kt"
    }

    private val source by lazy {
        String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(SRC)))
    }

    @Test
    fun `videoPlayerOnBackCallsParentOnBack()`() {
        // The VideoPlayerScreen call's onBack handler must invoke the parent's
        // onBack callback (not just clear selectedEntry).
        //
        // The buggy pattern is: onBack = { selectedEntry = null }
        // The correct pattern is: onBack = { selectedEntry = null; onBack() }

        // Find the VideoPlayerScreen call
        val videoPlayerIndex = source.indexOf("VideoPlayerScreen(")
        assertTrue("Source must contain VideoPlayerScreen call", videoPlayerIndex >= 0)

        // Extract the onBack argument: from "onBack =" to the closing "),"
        val onBackArgStart = source.indexOf("onBack =", videoPlayerIndex - 200)
        assertTrue("Source must contain onBack parameter near VideoPlayerScreen", onBackArgStart >= 0)

        // Find the closing of this argument — look for the next ");" or "),\n"
        val blockEnd = source.indexOf(")", onBackArgStart)
        val onBackBlock = source.substring(onBackArgStart, blockEnd + 1)

        // The onBack block must contain a call to the parent's onBack()
        // NOT just { selectedEntry = null }
        assertTrue(
            "VideoPlayerScreen's onBack must call the parent's onBack callback. " +
                "Found: $onBackBlock",
            onBackBlock.contains("onBack()") || onBackBlock.contains("onBack.invoke()"),
        )
    }

    @Test
    fun `videoPlayerOnBackClearsSelectedEntry()`() {
        // The onBack handler must also clear selectedEntry to hide the player
        val videoPlayerIndex = source.indexOf("VideoPlayerScreen(")
        assertTrue("Source must contain VideoPlayerScreen call", videoPlayerIndex >= 0)

        val onBackArgStart = source.indexOf("onBack =", videoPlayerIndex - 200)
        assertTrue("Source must contain onBack parameter near VideoPlayerScreen", onBackArgStart >= 0)

        val blockEnd = source.indexOf(")", onBackArgStart)
        val onBackBlock = source.substring(onBackArgStart, blockEnd + 1)

        assertTrue(
            "VideoPlayerScreen's onBack must clear selectedEntry",
            onBackBlock.contains("selectedEntry = null") || onBackBlock.contains("selectedEntry=null"),
        )
    }
}
