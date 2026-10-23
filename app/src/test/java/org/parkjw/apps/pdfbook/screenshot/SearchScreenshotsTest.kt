package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.BookSearchSheet
import org.parkjw.apps.pdfbook.PageTextSearch
import org.parkjw.apps.pdfbook.READER_SEARCH_FIELD_TAG
import org.parkjw.apps.pdfbook.SearchPageHits
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class SearchScreenshotsTest {
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
    fun searchResultsLightFolded() = capture(
        hits = listOf(SearchPageHits(3, 2), SearchPageHits(12, 1), SearchPageHits(41, 5)),
        dark = false,
        file = "reader-search-results-light-folded.png",
    )

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun searchResultsDarkFolded() = capture(
        hits = listOf(SearchPageHits(3, 2), SearchPageHits(12, 1), SearchPageHits(41, 5)),
        dark = true,
        file = "reader-search-results-dark-folded.png",
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun searchNoTextLightFolded() = capture(
        hits = emptyList(),
        dark = false,
        file = "reader-search-no-text-light-folded.png",
    )

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(hits: List<SearchPageHits>, dark: Boolean, file: String) {
        val searcher = object : PageTextSearch {
            override suspend fun search(query: String, onPageHits: (SearchPageHits) -> Unit) {
                hits.forEach { onPageHits(it) }
            }
        }
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                BookSearchSheet(searcher = searcher, onJump = {}, onDismiss = {})
            }
        }
        composeRule.onNodeWithTag(READER_SEARCH_FIELD_TAG).performTextReplacement("바다")
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }
}
