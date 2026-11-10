package com.dominickp.firetvandroidtv.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * FocusedInfoBarTest — verifies the FocusedInfoBar Composable contracts.
 *
 * Tests the structural contracts (composable signature, content, and behavior)
 * rather than Compose rendering, matching the established pattern for screen
 * tests in this project's Robolectric config.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FocusedInfoBarTest {
    private companion object {
        private const val SRC = "src/main/java/com/dominickp/firetvandroidtv/ui/components/FocusedInfoBar.kt"
    }

    private val source by lazy {
        String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(SRC)))
    }

    // === Composable Signature Tests ===

    @Test
    fun `hasFocusedInfoBarComposableFunction`() {
        assertTrue(
            "FocusedInfoBar must have the main @Composable function",
            source.contains("fun FocusedInfoBar("),
        )
    }

    @Test
    fun `acceptsVideoParameter`() {
        assertTrue(
            "FocusedInfoBar must accept a VideoItem parameter (nullable for placeholder state)",
            source.contains("video: VideoItem?"),
        )
    }

    @Test
    fun `acceptsModifierParameterWithDefault`() {
        assertTrue(
            "FocusedInfoBar must accept a modifier parameter with a default value",
            source.contains("modifier: Modifier = Modifier"),
        )
    }

    @Test
    fun `isAnnotatedComposable`() {
        assertTrue(
            "FocusedInfoBar function must be @Composable",
            source.contains("@Composable"),
        )
    }

    // === Content Tests ===

    @Test
    fun `displaysVideoTitle`() {
        assertTrue(
            "FocusedInfoBar must display the video title",
            source.contains("video?.title"),
        )
    }

    @Test
    fun `displaysShortDescription`() {
        assertTrue(
            "FocusedInfoBar must display the short description when present",
            source.contains("video?.shortDescription"),
        )
    }

    @Test
    fun `showsPlaceholderWhenNoVideoFocused`() {
        assertTrue(
            "FocusedInfoBar must show a placeholder title when no video is focused",
            source.contains("Select a video"),
        )
    }

    @Test
    fun `formatsDurationFromVideoSeconds`() {
        assertTrue(
            "FocusedInfoBar must derive duration text from video.duration via formatDuration",
            source.contains("formatDuration(") && source.contains("video?.duration"),
        )
    }

    // === Duration Badge Behavior ===

    @Test
    fun `durationBadgeOnlyShownForNonLiveVideo`() {
        assertTrue(
            "The duration badge must only render for non-live videos",
            source.contains("!video.isLive"),
        )
    }

    @Test
    fun `formatDurationReturnsNullForNonPositiveSeconds`() {
        assertTrue(
            "formatDuration must return null when seconds <= 0 (no badge rendered)",
            source.contains("if (seconds <= 0) return null"),
        )
    }

    // === Theming Tests ===

    @Test
    fun `usesBrandTextColors`() {
        assertTrue(
            "FocusedInfoBar must use BrandColors for text",
            source.contains("BrandColors.TextPrimary") && source.contains("BrandColors.TextSecondary"),
        )
    }

    @Test
    fun `titleUsesBoldTitleLargeStyle`() {
        assertTrue(
            "The title must use the bold titleLarge typography style",
            source.contains("titleLarge") && source.contains("FontWeight.Bold"),
        )
    }

    @Test
    fun `hasHorizontalPadding`() {
        assertTrue(
            "FocusedInfoBar must have horizontal padding",
            source.contains("padding("),
        )
    }

    // === Anti-regression Tests ===

    @Test
    fun `isPresentationalOnlyNoViewModelReference`() {
        assertFalse(
            "FocusedInfoBar is a presentational component and must not reference a ViewModel",
            source.contains("ViewModel"),
        )
    }

    @Test
    fun `isPresentationalOnlyNoNavigationReference`() {
        assertFalse(
            "FocusedInfoBar is a presentational component and must not reference Navigation",
            source.contains("Navigation"),
        )
    }

    @Test
    fun `titleTruncatesWithEllipsisSingleLine`() {
        assertTrue(
            "The title must truncate to a single line with ellipsis overflow",
            source.contains("maxLines = 1") && source.contains("TextOverflow.Ellipsis"),
        )
    }
}
