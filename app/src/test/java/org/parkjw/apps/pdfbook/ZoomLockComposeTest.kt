package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class ZoomLockComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var pagerState: PagerState
    private val volume = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    private var opened: Any? = null

    @Test
    fun zl1_ltrSwipePastRightEdgeWhileZoomedDoesNotTurn() {
        setReader(ReadingDirection.LTR)
        zoomIn()
        swipeTowardNext(ReadingDirection.LTR)
        val atEdge = zoom()
        assertEquals(2f, atEdge.scale, 0f)
        assertEquals(-maxOffsetX(), atEdge.offsetX, 0.5f)
        swipeTowardNext(ReadingDirection.LTR)
        swipeTowardNext(ReadingDirection.LTR)
        assertEquals(0, pagerState.currentPage)
        assertEquals(2f, zoom().scale, 0f)
    }

    @Test
    fun zl1_rtlSwipePastLeftEdgeWhileZoomedDoesNotTurn() {
        setReader(ReadingDirection.RTL)
        zoomIn()
        swipeTowardNext(ReadingDirection.RTL)
        assertEquals(maxOffsetX(), zoom().offsetX, 0.5f)
        swipeTowardNext(ReadingDirection.RTL)
        assertEquals(0, pagerState.currentPage)
        assertEquals(2f, zoom().scale, 0f)
    }

    @Test
    fun zl1_swipePastEndOnLastPageWhileZoomedShowsNoNextBookNotice() {
        setReader(ReadingDirection.LTR, initialPage = PAGE_COUNT - 1, nextBook = true)
        zoomIn()
        repeat(3) { swipeTowardNext(ReadingDirection.LTR) }
        assertEquals(PAGE_COUNT - 1, pagerState.currentPage)
        assertNull(opened)
        composeRule.onNodeWithTag("end_notice").assertDoesNotExist()
    }

    @Test
    fun zl2_tapZoneTurnsPageWhileZoomed() {
        setReader(ReadingDirection.LTR)
        zoomIn()
        tap(0.9f)
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun zl2_rtlTapZoneTurnsPageWhileZoomed() {
        setReader(ReadingDirection.RTL)
        zoomIn()
        tap(0.1f)
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun zl2_volumeKeyTurnsPageWhileZoomed() {
        setReader(ReadingDirection.LTR)
        zoomIn()
        runBlocking { volume.emit(1) }
        settle()
        assertEquals(1, pagerState.currentPage)
        runBlocking { volume.emit(-1) }
        settle()
        assertEquals(0, pagerState.currentPage)
    }

    @Test
    fun zl3_keepZoomRtlOpensNextPageAtScaleTwoTopRight() {
        setReader(ReadingDirection.RTL, keepZoom = true)
        zoomIn()
        tap(0.1f)
        assertEquals(1, pagerState.currentPage)
        val after = zoom()
        assertEquals(2f, after.scale, 0f)
        assertEquals(-maxOffsetX(), after.offsetX, 0.5f)
        assertEquals(maxOffsetY(), after.offsetY, 0.5f)
    }

    @Test
    fun zl3_keepZoomLtrOpensNextPageAtScaleTwoTopLeft() {
        setReader(ReadingDirection.LTR, keepZoom = true)
        zoomIn()
        tap(0.9f)
        assertEquals(1, pagerState.currentPage)
        val after = zoom()
        assertEquals(2f, after.scale, 0f)
        assertEquals(maxOffsetX(), after.offsetX, 0.5f)
        assertEquals(maxOffsetY(), after.offsetY, 0.5f)
    }

    @Test
    fun zl3_keepZoomTwoPageSpreadOpensNextSpreadAtTopRight() {
        setReader(ReadingDirection.RTL, keepZoom = true, twoPage = true, initialPage = 1)
        zoomIn()
        runBlocking { volume.emit(1) }
        settle()
        assertEquals(2, pagerState.currentPage)
        val after = zoom()
        assertEquals(2f, after.scale, 0f)
        assertEquals(-maxOffsetX(), after.offsetX, 0.5f)
        assertEquals(maxOffsetY(), after.offsetY, 0.5f)
    }

    @Test
    fun zl4_keepZoomOffByDefaultOpensNextPageAtScaleOne() {
        setReader(ReadingDirection.RTL)
        zoomIn()
        tap(0.1f)
        assertEquals(1, pagerState.currentPage)
        assertEquals(ZoomState(), zoom())
    }

    @Test
    fun zl5_swipeAtScaleOneTurnsPage() {
        setReader(ReadingDirection.LTR)
        swipeTowardNext(ReadingDirection.LTR)
        assertEquals(1, pagerState.currentPage)
        assertEquals(ZoomState(), zoom())
    }

    @Test
    fun zl5_rtlSwipeAtScaleOneTurnsPage() {
        setReader(ReadingDirection.RTL)
        swipeTowardNext(ReadingDirection.RTL)
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun doubleTapOnSideWhileZoomedZoomsOutWithoutTurning() {
        setReader(ReadingDirection.LTR)
        zoomIn()
        composeRule.onRoot().performTouchInput { doubleClick(Offset(width * 0.9f, height / 2f)) }
        settle()
        assertEquals(0, pagerState.currentPage)
        assertEquals(ZoomState(), zoom())
    }

    private fun zoomIn() {
        composeRule.onRoot().performTouchInput { doubleClick(Offset(width * 0.5f, height / 2f)) }
        settle()
        assertEquals(2f, zoom().scale, 0f)
    }

    private fun swipeTowardNext(direction: ReadingDirection) {
        composeRule.onRoot().performTouchInput {
            if (direction == ReadingDirection.RTL) swipeRight() else swipeLeft()
        }
        settle()
    }

    private fun tap(fraction: Float) {
        composeRule.onRoot().performTouchInput { click(Offset(width * fraction, height / 2f)) }
        settle()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun zoom(): ZoomState =
        composeRule.onNodeWithTag("reader_screen").fetchSemanticsNode().config[ReaderZoomKey]

    private fun viewportSize() = composeRule.onNodeWithTag("reader_screen").fetchSemanticsNode().size

    private fun maxOffsetX(): Float = viewportSize().width * (zoom().scale - 1f) / 2f

    private fun maxOffsetY(): Float = viewportSize().height * (zoom().scale - 1f) / 2f

    private fun setReader(
        direction: ReadingDirection,
        keepZoom: Boolean = false,
        twoPage: Boolean = false,
        initialPage: Int = 0,
        nextBook: Boolean = false,
    ) {
        val pageSpreads = spreads(PAGE_COUNT, twoPage = twoPage, coverAlone = true)
        composeRule.setContent {
            pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageSpreads.size })
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(PAGE_COUNT, 0, 0),
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "t",
                    pageCount = PAGE_COUNT,
                    currentPage = 1,
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    volumeDelta = volume,
                    keepZoom = keepZoom,
                    volumes = if (nextBook) ReaderVolumes(onNext = { opened = Unit }, nextBook = BookInfo("doc:2", "책 2권.pdf", 10)) else ReaderVolumes(),
                )
            }
        }
    }

    private companion object {
        const val PAGE_COUNT = 6
    }
}
