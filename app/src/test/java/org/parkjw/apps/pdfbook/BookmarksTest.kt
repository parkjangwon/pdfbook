package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class BookmarksTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun toggleAddsRemovesAndKeepsPageOrder() = runTest {
        val store = BookStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "a.preferences_pb") })
        store.toggleBookmark("a", 9)
        store.toggleBookmark("a", 2)
        store.toggleBookmark("a", 5)
        assertEquals(listOf(2, 5, 9), store.bookmarks("a").first())
        store.toggleBookmark("a", 5)
        assertEquals(listOf(2, 9), store.bookmarks("a").first())
    }

    @Test
    fun bookmarksAreIsolatedPerBook() = runTest {
        val store = BookStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "b.preferences_pb") })
        store.toggleBookmark(bookKey("content://one", 10), 3)
        assertEquals(emptyList<Int>(), store.bookmarks(bookKey("content://two", 10)).first())
        assertEquals(emptyList<Int>(), store.bookmarks(bookKey("content://one", 11)).first())
        assertEquals(listOf(3), store.bookmarks(bookKey("content://one", 10)).first())
    }

    @Test
    fun overlayToggleListJumpAndDelete() {
        val pageCount = 4
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        var pages by mutableStateOf(listOf(3))
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                val pagerState = rememberPagerState(pageCount = { pageSpreads.size })
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "책",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    bookmarks = ReaderBookmarks(pages) { pages = toggledBookmark(pages, it) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithContentDescription("책갈피 추가").performClick()
        assertEquals(listOf(1, 3), pages)
        composeRule.onNodeWithContentDescription("책갈피 해제").performClick()
        assertEquals(listOf(3), pages)

        composeRule.onNodeWithContentDescription("책갈피 목록").performClick()
        composeRule.onNodeWithTag(BOOKMARK_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("3쪽").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("3쪽 책갈피 삭제").performClick()
        assertEquals(emptyList<Int>(), pages)
        composeRule.onNodeWithText("아직 책갈피가 없어요").assertIsDisplayed()
    }

    @Test
    fun listEntryJumpsAndClosesSheet() {
        val pageSpreads = spreads(4, twoPage = false, coverAlone = true)
        lateinit var settled: () -> Int
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                val pagerState = rememberPagerState(pageCount = { pageSpreads.size })
                settled = { pagerState.currentPage }
                ReaderScreen(
                    renderer = SolidPageBitmaps(4, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "책",
                    pageCount = 4,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    bookmarks = ReaderBookmarks(listOf(3)) {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithContentDescription("책갈피 목록").performClick()
        composeRule.onNodeWithText("3쪽").performClick()
        composeRule.waitForIdle()
        assertEquals(spreadIndexOf(pageSpreads, 3), settled())
        composeRule.onAllNodesWithTag(BOOKMARK_SHEET_TAG).assertCountEquals(0)
    }
}
