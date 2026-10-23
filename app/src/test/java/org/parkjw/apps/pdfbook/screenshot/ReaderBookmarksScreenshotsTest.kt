package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ReaderBookmarks
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderBookmarksScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun onlyWhenRecordingOrVerifying() {
        assumeTrue(
            System.getProperty("roborazzi.test.record") == "true" ||
                System.getProperty("roborazzi.test.verify") == "true" ||
                System.getProperty("roborazzi.test.compare") == "true",
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun bookmarkOffLightFolded() = captureOverlay(
        file = "reader-bookmark-off-light-folded.png",
        bookmarkedPages = emptyList(),
        title = "샘플 시리즈 5권",
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun bookmarkOnLightFolded() = captureOverlay(
        file = "reader-bookmark-on-light-folded.png",
        bookmarkedPages = listOf(1),
        title = "샘플 시리즈 5권",
    )

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun bookmarkOnLightFoldedEn() = captureOverlay(
        file = "reader-bookmark-on-light-folded-en.png",
        bookmarkedPages = listOf(1),
        title = "Sample series vol. 5",
    )

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun bookmarkOnLightUnfolded() = captureOverlay(
        file = "reader-bookmark-on-light-unfolded.png",
        bookmarkedPages = listOf(1),
        title = "샘플 시리즈 5권",
    )

    // Review-only capture: brightness, auto turn and bookmarks together in the shared tool row.
    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun bottomBarCombinedLightFolded() = captureOverlay(
        file = "reader-bottom-bar-combined-light-folded.png",
        bookmarkedPages = listOf(1),
        title = "샘플 시리즈 5권",
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun bookmarksSheetLightFolded() = captureSheet(
        file = "reader-bookmarks-sheet-light-folded.png",
        pages = listOf(3, 12, 41),
        title = "샘플 시리즈 5권",
    )

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun bookmarksSheetLightFoldedEn() = captureSheet(
        file = "reader-bookmarks-sheet-light-folded-en.png",
        pages = listOf(3, 12, 41),
        title = "Sample series vol. 5",
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun bookmarksSheetEmptyLightFolded() = captureSheet(
        file = "reader-bookmarks-sheet-empty-light-folded.png",
        pages = emptyList(),
        title = "샘플 시리즈 5권",
    )

    private fun captureOverlay(
        file: String,
        bookmarkedPages: List<Int>,
        title: String,
    ) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(false, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = title,
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    bookmarks = ReaderBookmarks(bookmarkedPages) {},
                    volumes = sampleReaderVolumes(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage(file)
    }

    private fun captureSheet(
        file: String,
        pages: List<Int>,
        title: String,
    ) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(false, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = title,
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    bookmarkSheetVisible = true,
                    bookmarks = ReaderBookmarks(pages) {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage(file)
    }
}
