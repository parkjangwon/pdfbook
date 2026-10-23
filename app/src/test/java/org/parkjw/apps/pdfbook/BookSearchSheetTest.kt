package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class BookSearchSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val jumped = mutableListOf<Int>()

    private class FakeSearcher(
        private val hits: List<SearchPageHits> = emptyList(),
        private val hang: Boolean = false,
    ) : PageTextSearch {
        val queries = mutableListOf<String>()
        var cancelled = 0

        override suspend fun search(query: String, onPageHits: (SearchPageHits) -> Unit) {
            queries += query
            try {
                if (hang) awaitCancellation()
                hits.forEach { onPageHits(it) }
            } catch (e: CancellationException) {
                cancelled++
                throw e
            }
        }
    }

    private fun setSheet(searcher: PageTextSearch) {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                if (visible) {
                    BookSearchSheet(
                        searcher = searcher,
                        onJump = {
                            jumped += it
                            visible = false
                        },
                        onDismiss = { visible = false },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun search(query: String) {
        composeRule.onNodeWithTag(READER_SEARCH_FIELD_TAG).performTextReplacement(query)
        composeRule.waitForIdle()
    }

    @Test
    fun resultsShowPageRowsWithCountsInOrder() {
        val searcher = FakeSearcher(listOf(SearchPageHits(3, 2), SearchPageHits(12, 1)))
        setSheet(searcher)
        search("바다")
        composeRule.onNodeWithText("3쪽").assertIsDisplayed()
        composeRule.onNodeWithText("12쪽").assertIsDisplayed()
        composeRule.onNodeWithText("2곳").assertIsDisplayed()
        val tops = listOf("3쪽", "12쪽").map {
            composeRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun tappingResultCallsJumpWithPage() {
        val searcher = FakeSearcher(listOf(SearchPageHits(3, 2), SearchPageHits(12, 1)))
        setSheet(searcher)
        search("바다")
        composeRule.onNode(hasText("12쪽") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(12), jumped)
        composeRule.onAllNodesWithTag(READER_SEARCH_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun noTextLayerShowsMessage() {
        setSheet(FakeSearcher(emptyList()))
        search("바다")
        composeRule.onNodeWithText("이 책에서는 글자를 찾을 수 없어요.").assertIsDisplayed()
        composeRule.onAllNodesWithTag("reader_search_row").assertCountEquals(0)
    }

    @Test
    fun emptyQueryShowsNothingAndSkipsSearch() {
        val searcher = FakeSearcher(listOf(SearchPageHits(1, 1)))
        setSheet(searcher)
        search("바다")
        search("  ")
        composeRule.onAllNodesWithTag("reader_search_row").assertCountEquals(0)
        composeRule.onAllNodesWithText("이 책에서는 글자를 찾을 수 없어요.").assertCountEquals(0)
        assertEquals(1, searcher.queries.size)
    }

    @Test
    fun searchingResultCancelsPreviousRun() {
        val searcher = FakeSearcher(hang = true)
        setSheet(searcher)
        search("첫")
        search("둘")
        composeRule.waitForIdle()
        assertEquals(1, searcher.cancelled)
    }

    @Test
    fun searchResultJumpsAndShowsJumpBackPill() {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        val searcher = FakeSearcher(listOf(SearchPageHits(80, 4)))
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = rememberPagerState(
                        initialPage = spreadIndexOf(pageSpreads, 12),
                        pageCount = { pageSpreads.size },
                    ),
                    spreads = pageSpreads,
                    title = "책",
                    pageCount = pageCount,
                    currentPage = 12,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.LTR,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    searcher = searcher,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_search").performClick()
        composeRule.onNodeWithTag(READER_SEARCH_SHEET_TAG).assertIsDisplayed()
        search("바다")
        composeRule.onNode(hasText("80쪽") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(READER_SEARCH_SHEET_TAG).assertCountEquals(0)
        composeRule.onNodeWithTag("jump_back_pill").assertIsDisplayed()
        composeRule.onNodeWithText("12쪽으로 돌아가기").assertIsDisplayed()
    }
}
