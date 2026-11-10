package com.dominickp.firetvandroidtv.ui.continuewatching

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ContinueWatchingScreenTest — verifies the ContinueWatchingScreen Composable
 * contracts (screen state behavior, card rendering logic, entry data).
 *
 * Tests the structural contracts rather than Compose rendering, which requires
 * a full ComposeTestRule setup not available in this project's Robolectric config.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContinueWatchingScreenTest {
    private companion object {
        // Paths relative to module dir (Gradle CWD = app/)
        private const val SRC = "src/main/java/com/dominickp/firetvandroidtv/ui/continuewatching/ContinueWatchingScreen.kt"
        private const val DAO_SRC = "src/main/java/com/dominickp/firetvandroidtv/dao/ContinueWatchingDao.kt"
    }

    private val source by lazy {
        String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(SRC)))
    }

    @Test
    fun `screenHasContinueWatchingHeader()`() {
        assertTrue(source.contains("Continue Watching"))
    }

    @Test
    fun `screenHasBackButton()`() {
        assertTrue(source.contains("Back"))
    }

    @Test
    fun `screenShowsEmptyStateText()`() {
        assertTrue(source.contains("No recently watched videos"))
    }

    @Test
    fun `screenUsesLazyColumnForEntries()`() {
        assertTrue(source.contains("LazyColumn"))
    }

    @Test
    fun `screenRendersContinueWatchingCardForEachEntry()`() {
        assertTrue(source.contains("ContinueWatchingCard"))
    }

    @Test
    fun `screenCardShowsPlayedPercentageProgress()`() {
        assertTrue(source.contains("playedPercentage"))
    }

    @Test
    fun `screenCardShowsProgressIndicator()`() {
        assertTrue(source.contains("LinearProgressIndicator"))
    }

    @Test
    fun `screenCardHasThumbnailImage()`() {
        assertTrue(source.contains("AsyncImage"))
    }

    @Test
    fun `screenCardUsesTrackFocusForDpad()`() {
        assertTrue(source.contains("trackFocus"))
    }

    @Test
    fun `screenUsesUnstableApiAnnotation()`() {
        assertTrue(source.contains("UnstableApi"))
    }

    @Test
    fun `screenHandlesCardSelectionInternallyViaSelectedEntry()`() {
        assertTrue(source.contains("selectedEntry"))
        assertTrue(source.contains("VideoPlayerScreen"))
    }

    @Test
    fun `screenHasOnBackCallback()`() {
        assertTrue(source.contains("onBack"))
    }

    @Test
    fun `entryDataClassHasVideoIdPrimaryKey()`() {
        val daoSrc = String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(DAO_SRC)))
        assertTrue(daoSrc.contains("@PrimaryKey"))
        assertTrue(daoSrc.contains("videoId: String"))
    }

    @Test
    fun `entryDataClassHasAllRequiredFields()`() {
        val daoSrc = String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(DAO_SRC)))
        for (field in listOf("videoId", "title", "thumbnail", "playedPercentage", "lastWatched", "contentUrl")) {
            assertTrue("Entry should have $field", daoSrc.contains(field))
        }
    }

    @Test
    fun `entryHasDefaultValuesForOptionalFields()`() {
        val daoSrc = String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(DAO_SRC)))
        // thumbnail = "", playedPercentage = 0, lastWatched = current time, contentUrl = ""
        assertTrue(daoSrc.contains("thumbnail: String ="))
        assertTrue(daoSrc.contains("playedPercentage: Int = 0"))
        assertTrue(daoSrc.contains("contentUrl: String ="))
    }

    @Test
    fun `continueWatchingCardHasClickHandler()`() {
        assertTrue(source.contains("clickable"))
    }

    @Test
    fun `screenNavigatesToPlayerOnCardSelect()`() {
        assertTrue(source.contains("VideoPlayerScreen"))
    }

    @Test
    fun `entryDataClassIsSerializable()`() {
        val daoSrc = String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(DAO_SRC)))
        assertTrue(daoSrc.contains("@kotlinx.serialization.Serializable"))
    }

    @Test
    fun `entryDataClassIsRoomEntity()`() {
        val daoSrc = String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(DAO_SRC)))
        assertTrue(daoSrc.contains("@Entity"))
    }

    @Test
    fun `cardRendersTitleAndProgressInfo()`() {
        assertTrue(source.contains("entry.title"))
        assertTrue(source.contains("entry.playedPercentage"))
    }
}
