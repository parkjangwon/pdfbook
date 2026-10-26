package org.parkjw.apps.pdfbook

import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageCurlComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var pagerState: PagerState
    private val volume = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun animationsOn() {
        setAnimatorScale(1f)
    }

    @After
    fun resetAnimations() {
        setAnimatorScale(1f)
    }

    @Test
    fun tapNextPlaysCurlOverlayThenSettlesOnNextPage() {
        setReader()
        freezeClock()
        tapNext(ReadingDirection.LTR)
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun swipePastThresholdFollowsFingerThenCompletes() {
        setReader()
        freezeClock()
        startDrag(ReadingDirection.LTR, fromFraction = 0.9f, toFraction = 0.5f)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        endDrag()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun swipeBeforeThresholdSpringsBackToCurrentPage() {
        setReader()
        freezeClock()
        startDrag(ReadingDirection.LTR, fromFraction = 0.9f, toFraction = 0.75f)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        endDrag()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(0, pagerState.currentPage)
    }

    @Test
    fun rtlSwipeTowardNextCompletesCurl() {
        setReader(direction = ReadingDirection.RTL)
        freezeClock()
        startDrag(ReadingDirection.RTL, fromFraction = 0.1f, toFraction = 0.5f)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        endDrag()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun volumeKeyPlaysCurlThenSettles() {
        setReader()
        freezeClock()
        runBlocking { volume.emit(1) }
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun newTapDuringCurlFinishesItAndStartsTheNextTurn() {
        setReader()
        freezeClock()
        tapNext(ReadingDirection.LTR)
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        tapNext(ReadingDirection.LTR)
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(2, pagerState.currentPage)
    }

    @Test
    fun uncachedNextPageUsesSlideWithoutCurl() {
        setReader(renderer = UncachedPages(PAGE_COUNT))
        freezeClock()
        swipeNext(ReadingDirection.LTR)
        repeat(30) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun uncachedDragFollowsFingerBeforeSliding() {
        setReader(renderer = UncachedPages(PAGE_COUNT))
        freezeClock()
        startDrag(ReadingDirection.LTR, fromFraction = 0.9f, toFraction = 0.5f)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertTrue(pagerState.currentPageOffsetFraction > 0f)
        endDrag()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun uncachedDragBeforeThresholdSpringsBackAnimated() {
        setReader(renderer = UncachedPages(PAGE_COUNT))
        freezeClock()
        startDrag(ReadingDirection.LTR, fromFraction = 0.9f, toFraction = 0.75f)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertTrue(pagerState.currentPageOffsetFraction > 0f)
        endDrag()
        composeRule.mainClock.advanceTimeByFrame()
        // Animated spring-back, not an instant snap to the resting offset.
        assertTrue(pagerState.currentPageOffsetFraction > 0f)
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(0f, pagerState.currentPageOffsetFraction, 0.001f)
        assertEquals(0, pagerState.currentPage)
    }

    @Test
    fun volumeKeyDuringCurlDragKeepsTheInterruptedTurn() {
        setReader()
        freezeClock()
        startDrag(ReadingDirection.LTR, fromFraction = 0.9f, toFraction = 0.5f)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        runBlocking { volume.emit(1) }
        composeRule.mainClock.advanceTimeByFrame()
        // The stale drag release must not settle back to its original target (page 1).
        endDrag()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(2, pagerState.currentPage)
    }

    @Test
    fun twoPageViewUsesSlideWithoutCurl() {
        setReader(twoPage = true)
        freezeClock()
        tapNext(ReadingDirection.LTR)
        repeat(20) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun zoomedViewUsesSlideWithoutCurl() {
        setReader()
        zoomIn()
        freezeClock()
        tapNext(ReadingDirection.LTR)
        repeat(20) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun animatorScaleZeroTurnsInstantly() {
        setAnimatorScale(0f)
        setReader()
        freezeClock()
        tapNext(ReadingDirection.LTR)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun noneOptionSwipesInstantlyWithoutSlideOrCurl() {
        setReader(animation = PageTurnAnimation.None)
        freezeClock()
        swipeNext(ReadingDirection.LTR)
        repeat(5) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        }
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun noneOptionSmallDragDoesNotTurn() {
        setReader(animation = PageTurnAnimation.None)
        freezeClock()
        drag(fromFraction = 0.9f, toFraction = 0.8f)
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(0, pagerState.currentPage)
    }

    @Test
    fun lastPageTapShowsNoticeWithoutCurl() {
        setReader(
            initialPage = PAGE_COUNT - 1,
            volumes = ReaderVolumes(nextBook = BookInfo("doc:2", "책 2권.pdf", 10)),
        )
        freezeClock()
        tapNext(ReadingDirection.LTR)
        repeat(10) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        }
        composeRule.onNodeWithTag("end_notice").assertExists()
    }

    private fun setReader(
        renderer: PageBitmaps = CachedPages(PAGE_COUNT),
        direction: ReadingDirection = ReadingDirection.LTR,
        twoPage: Boolean = false,
        initialPage: Int = 0,
        animation: PageTurnAnimation = PageTurnAnimation.Curl,
        volumes: ReaderVolumes = ReaderVolumes(),
    ) {
        val pageSpreads = spreads(PAGE_COUNT, twoPage = twoPage, coverAlone = true)
        composeRule.setContent {
            pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageSpreads.size })
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = renderer,
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "책 1권",
                    pageCount = PAGE_COUNT,
                    currentPage = 1,
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    volumeDelta = volume,
                    showTapZoneGuide = false,
                    volumes = volumes,
                    pageTurnAnimation = animation,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun freezeClock() {
        composeRule.mainClock.autoAdvance = false
    }

    private fun tapNext(direction: ReadingDirection) {
        composeRule.onRoot().performTouchInput {
            val x = if (direction == ReadingDirection.RTL) width * 0.1f else width * 0.9f
            click(Offset(x, height / 2f))
        }
    }

    private fun swipeNext(direction: ReadingDirection) {
        composeRule.onRoot().performTouchInput {
            if (direction == ReadingDirection.RTL) swipeRight() else swipeLeft()
        }
    }

    private fun drag(fromFraction: Float, toFraction: Float) {
        composeRule.onRoot().performTouchInput {
            down(Offset(width * fromFraction, height / 2f))
            advanceEventTime(16)
            moveTo(Offset(width * toFraction, height / 2f))
            advanceEventTime(16)
            up()
        }
    }

    private fun startDrag(direction: ReadingDirection, fromFraction: Float, toFraction: Float) {
        composeRule.onRoot().performTouchInput {
            down(Offset(width * fromFraction, height / 2f))
            advanceEventTime(16)
            moveTo(Offset(width * toFraction, height / 2f))
        }
    }

    private fun endDrag() {
        composeRule.onRoot().performTouchInput { up() }
    }

    private fun zoomIn() {
        composeRule.onRoot().performTouchInput {
            doubleClick(Offset(width / 2f, height / 2f))
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun setAnimatorScale(scale: Float) {
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    private class CachedPages(override val pageCount: Int) : PageBitmaps {
        private val pages = (1..pageCount).associateWith { page ->
            Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (page % 2 == 0) 0xFFE8F0F7.toInt() else 0xFFF7F2E8.toInt())
            }
        }

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            cached(page, maxWidth, maxHeight)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? = pages[page]
    }

    private class UncachedPages(override val pageCount: Int) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? = null
    }

    private companion object {
        const val PAGE_COUNT = 5
    }
}
