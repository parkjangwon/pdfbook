package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class ReaderVolumesComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun book(n: Int) = BookInfo("doc:$n", "책 ${n}권.pdf", 10)

    private fun setReader(
        volumes: ReaderVolumes,
        title: String = "책 1권",
        direction: ReadingDirection = ReadingDirection.RTL,
    ) {
        val pageCount = 4
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = title,
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    volumes = volumes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun openBookList() {
        composeRule.onNodeWithTag("reader_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_more_book_list").performClick()
        composeRule.waitForIdle()
    }

    private fun list(books: List<BookInfo>, current: BookInfo) =
        BookListUi("시리즈", bookListEntries(books, current, emptyMap()))

    @Test
    fun nb3FirstBookDimsPreviousAndNextMoves() {
        var next = 0
        setReader(ReaderVolumes(onPrevious = null, onNext = { next++ }))
        composeRule.onNodeWithContentDescription("이전 권").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("다음 권").assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("이전 권").performClick()
        assertEquals(1, next)
    }

    @Test
    fun nb3LastBookDimsNextAndPreviousMoves() {
        var previous = 0
        setReader(ReaderVolumes(onPrevious = { previous++ }, onNext = null))
        composeRule.onNodeWithContentDescription("다음 권").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("이전 권").assertIsEnabled().performClick()
        assertEquals(1, previous)
    }

    @Test
    fun nb3SingleBookDimsBoth() {
        setReader(ReaderVolumes())
        composeRule.onNodeWithContentDescription("이전 권").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("다음 권").assertIsNotEnabled()
    }

    @Test
    fun bl1Bl2BookListShowsFolderBooksInOrderWithCurrentMarked() {
        val books = listOf(book(1), book(2), book(10))
        setReader(ReaderVolumes(bookList = list(books, book(2))))
        composeRule.onAllNodesWithTag(BOOK_LIST_SHEET_TAG).assertCountEquals(0)
        openBookList()
        composeRule.onNodeWithTag(BOOK_LIST_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("책 3권").assertIsDisplayed()
        val tops = listOf("책 1권", "책 2권", "책 10권").map {
            composeRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(tops.sorted(), tops)
        composeRule.onAllNodesWithText("읽는 중").assertCountEquals(1)
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true) and hasText("책 2권"))
            .assertExists()
    }

    @Test
    fun bl2CurrentRowIsScrolledIntoViewInLongList() {
        val books = (1..40).map { book(it) }
        setReader(ReaderVolumes(bookList = list(books, book(33))))
        openBookList()
        composeRule.onNodeWithText("책 33권").assertIsDisplayed()
        composeRule.onNodeWithText("읽는 중").assertIsDisplayed()
    }

    @Test
    fun bl3TappingOtherBookClosesSheetAndPicksIt() {
        val picked = mutableListOf<BookInfo>()
        val books = listOf(book(1), book(2))
        setReader(ReaderVolumes(bookList = list(books, book(1)), onPick = { picked += it }))
        openBookList()
        composeRule.onNode(hasText("책 2권") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(book(2)), picked)
        composeRule.onAllNodesWithTag(BOOK_LIST_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun bl4TappingCurrentBookOnlyClosesAndSingleBookStillOpens() {
        val picked = mutableListOf<BookInfo>()
        setReader(ReaderVolumes(bookList = list(listOf(book(1)), book(1)), onPick = { picked += it }))
        openBookList()
        composeRule.onNodeWithTag(BOOK_LIST_SHEET_TAG).assertIsDisplayed()
        composeRule.onNode(hasText("책 1권") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertTrue(picked.isEmpty())
        composeRule.onAllNodesWithTag(BOOK_LIST_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun backClosesSheet() {
        setReader(ReaderVolumes(bookList = list(listOf(book(1), book(2)), book(1))))
        openBookList()
        composeRule.onNodeWithTag(BOOK_LIST_SHEET_TAG).assertIsDisplayed()
        Espresso.pressBack()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(BOOK_LIST_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun scrimTapClosesSheet() {
        setReader(ReaderVolumes(bookList = list(listOf(book(1), book(2)), book(1))))
        openBookList()
        composeRule.onNodeWithTag(BOOK_LIST_SHEET_TAG).assertIsDisplayed()
        composeRule.onAllNodes(isRoot())[1].performTouchInput { click(Offset(width / 2f, 10f)) }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(BOOK_LIST_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun bl2RepeatedRowTapsPickOnce() {
        val picked = mutableListOf<String>()
        setReader(
            ReaderVolumes(
                bookList = list(listOf(book(1), book(2), book(3)), book(1)),
                onPick = { picked += it.name },
            ),
        )
        openBookList()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasText("책 2권") and hasClickAction()).performClick()
        composeRule.onNode(hasText("책 3권") and hasClickAction()).performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf("책 2권.pdf"), picked)
    }

    @Test
    fun nb3RightToLeftBookPutsNextOnTheLeft() {
        setReader(ReaderVolumes(onPrevious = {}, onNext = {}), direction = ReadingDirection.RTL)
        val next = composeRule.onNodeWithContentDescription("다음 권").fetchSemanticsNode().boundsInRoot
        val previous = composeRule.onNodeWithContentDescription("이전 권").fetchSemanticsNode().boundsInRoot
        assertTrue(next.right <= previous.left)
    }

    @Test
    fun nb3LeftToRightBookPutsNextOnTheRight() {
        setReader(ReaderVolumes(onPrevious = {}, onNext = {}), direction = ReadingDirection.LTR)
        val next = composeRule.onNodeWithContentDescription("다음 권").fetchSemanticsNode().boundsInRoot
        val previous = composeRule.onNodeWithContentDescription("이전 권").fetchSemanticsNode().boundsInRoot
        assertTrue(previous.right <= next.left)
    }

    @Test
    fun titleToastShowsNewTitleThenHides() {
        var shown = 0
        composeRule.mainClock.autoAdvance = false
        val pageCount = 2
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "책 2권",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    showTapZoneGuide = false,
                    showTitleToast = true,
                    onTitleToastShown = { shown++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithText("책 2권").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(BOOK_CHANGED_TOAST_MILLIS + 100)
        assertEquals(1, shown)
    }
}
