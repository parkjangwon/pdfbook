package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class PageFitReaderComposeTest {
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
    fun tapScrollsOneFitScreenThenTurnsThePage() {
        setReader(ReadingDirection.RTL, PageFit.Width)
        val pageHeight = pageViewportHeight()
        val contentHeight = pageHeight * 1.2f
        val maxOffset = (contentHeight - pageHeight) / 2f
        assertEquals(maxOffset, zoom().offsetY, 0.5f)

        tapNext(ReadingDirection.RTL)
        assertEquals(0, pagerState.currentPage)
        assertEquals(-maxOffset, zoom().offsetY, 0.5f)

        tapNext(ReadingDirection.RTL)
        assertEquals(1, pagerState.currentPage)
        assertEquals(maxOffset, zoom().offsetY, 0.5f)
    }

    @Test
    fun previousTurnOpensThePreviousPageAtItsEnd() {
        setReader(ReadingDirection.RTL, PageFit.Width)
        tapNext(ReadingDirection.RTL)
        tapNext(ReadingDirection.RTL)
        assertEquals(1, pagerState.currentPage)

        tapPrevious(ReadingDirection.RTL)
        assertEquals(0, pagerState.currentPage)
        assertEquals(-pageFitMaxOffsetY(), zoom().offsetY, 0.5f)
    }

    @Test
    fun swipePansWithinThePageAndNeverTurnsIt() {
        setReader(ReadingDirection.RTL, PageFit.Width)
        val maxOffset = pageFitMaxOffsetY()
        assertEquals(maxOffset, zoom().offsetY, 0.5f)
        // A small drag from the start must move by the drag delta, not snap back to the centre.
        composeRule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(0f, -100f))
            up()
        }
        settle()
        assertEquals(0, pagerState.currentPage)
        assertEquals(maxOffset - 100f, zoom().offsetY, 1.5f)
    }

    @Test
    fun volumeKeysScrollAndThenTurnLikeTapZones() {
        setReader(ReadingDirection.RTL, PageFit.Width)
        runBlocking { volume.emit(1) }
        settle()
        assertEquals(0, pagerState.currentPage)
        runBlocking { volume.emit(1) }
        settle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun defaultFitStillTurnsOnTheFirstTap() {
        setReader(ReadingDirection.RTL, PageFit.Screen)
        tapNext(ReadingDirection.RTL)
        assertEquals(1, pagerState.currentPage)
        assertEquals(ZoomState(), zoom())
    }

    @Test
    fun tapOntoAnOverflowingPageSlidesAndLandsAtItsStart() {
        setReader(
            direction = ReadingDirection.RTL,
            pageFit = PageFit.Width,
            renderer = MixedPageBitmaps(shortPages = setOf(2)),
            initialPage = FITTING_PAGE_INDEX,
            animation = PageTurnAnimation.Curl,
        )
        assertEquals(ZoomState(), zoom())

        tapNext(ReadingDirection.RTL)
        assertEquals(FITTING_PAGE_INDEX + 1, pagerState.currentPage)
        assertEquals("Slide", turnMode())
        assertEquals(pageFitMaxOffsetY(), zoom().offsetY, 0.5f)
    }

    @Test
    fun backwardTapOntoAnOverflowingPageLandsAtItsEnd() {
        setReader(
            direction = ReadingDirection.RTL,
            pageFit = PageFit.Width,
            renderer = MixedPageBitmaps(shortPages = setOf(2)),
            initialPage = FITTING_PAGE_INDEX,
            animation = PageTurnAnimation.Curl,
        )

        tapPrevious(ReadingDirection.RTL)
        assertEquals(FITTING_PAGE_INDEX - 1, pagerState.currentPage)
        assertEquals("Slide", turnMode())
        assertEquals(-pageFitMaxOffsetY(), zoom().offsetY, 0.5f)
    }

    @Test
    fun curlDragBackOntoAnOverflowingPageSlidesToItsEnd() {
        setReader(
            direction = ReadingDirection.RTL,
            pageFit = PageFit.Width,
            renderer = MixedPageBitmaps(shortPages = setOf(2)),
            initialPage = FITTING_PAGE_INDEX,
            animation = PageTurnAnimation.Curl,
        )

        drag(0.9f, 0.3f)
        assertEquals(FITTING_PAGE_INDEX - 1, pagerState.currentPage)
        assertEquals("Slide", turnMode())
        assertEquals(-pageFitMaxOffsetY(), zoom().offsetY, 0.5f)
    }

    @Test
    fun aNoOpBackwardTapLeavesTheNextSwipeAtTheStart() {
        setReader(
            direction = ReadingDirection.LTR,
            pageFit = PageFit.Width,
            renderer = MixedPageBitmaps(shortPages = setOf(1)),
            animation = PageTurnAnimation.Slide,
        )
        // Nothing before the first page: the anchor must not be left armed for the next turn.
        tapPrevious(ReadingDirection.LTR)
        assertEquals(0, pagerState.currentPage)

        composeRule.onRoot().performTouchInput { swipeLeft() }
        settle()
        assertEquals(1, pagerState.currentPage)
        assertEquals(pageFitMaxOffsetY(), zoom().offsetY, 0.5f)
    }

    private fun tapNext(direction: ReadingDirection) {
        tap(if (direction == ReadingDirection.RTL) 0.1f else 0.9f)
    }

    private fun tapPrevious(direction: ReadingDirection) {
        tap(if (direction == ReadingDirection.RTL) 0.9f else 0.1f)
    }

    private fun tap(fraction: Float) {
        composeRule.onRoot().performTouchInput { click(Offset(width * fraction, height / 2f)) }
        settle()
    }

    private fun drag(fromFraction: Float, toFraction: Float) {
        composeRule.onRoot().performTouchInput {
            down(Offset(width * fromFraction, height / 2f))
            // Slow drag: below the fling speed, so the distance rule decides.
            advanceEventTime(500)
            moveTo(Offset(width * toFraction, height / 2f))
            advanceEventTime(500)
            up()
        }
        settle()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun zoom(): ZoomState =
        composeRule.onNodeWithTag("reader_screen").fetchSemanticsNode().config[ReaderZoomKey]

    private fun turnMode(): String =
        composeRule.onNodeWithTag("reader_screen").fetchSemanticsNode().config[ReaderTurnKey]

    private fun viewportSize() = composeRule.onNodeWithTag("reader_screen").fetchSemanticsNode().size

    private fun pageViewportHeight(): Float = viewportSize().height.toFloat()

    private fun pageFitMaxOffsetY(): Float {
        val size = viewportSize()
        val contentHeight = size.width * TallPageBitmaps.ASPECT
        return (contentHeight - size.height) / 2f
    }

    private fun setAnimatorScale(scale: Float) {
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    private fun setReader(
        direction: ReadingDirection,
        pageFit: PageFit,
        renderer: PageBitmaps = TallPageBitmaps(PAGE_COUNT),
        initialPage: Int = 0,
        animation: PageTurnAnimation = PageTurnAnimation.Slide,
    ) {
        val pageSpreads = spreads(PAGE_COUNT, twoPage = false, coverAlone = true)
        composeRule.setContent {
            pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageSpreads.size })
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = renderer,
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "t",
                    pageCount = PAGE_COUNT,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    volumeDelta = volume,
                    pageFit = pageFit,
                    pageTurnAnimation = animation,
                )
            }
        }
        settle()
    }

    /** Page 2 is a square page that fits a width fit; the rest overflow like [TallPageBitmaps]. */
    private class MixedPageBitmaps(private val shortPages: Set<Int>) : PageBitmaps {
        override val pageCount: Int get() = PAGE_COUNT

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
            if (page < 1 || page > PAGE_COUNT || maxWidth <= 0 || maxHeight <= 0) return null
            val width = maxWidth.coerceAtLeast(1)
            val aspect = if (page in shortPages) 1f else TallPageBitmaps.ASPECT
            val height = (width * aspect).toInt().coerceAtMost(maxHeight).coerceAtLeast(1)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }

    private class TallPageBitmaps(override val pageCount: Int) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
            if (page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
            val width = maxWidth.coerceAtLeast(1)
            val height = (width * ASPECT).toInt().coerceAtMost(maxHeight).coerceAtLeast(1)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        companion object {
            // 1.2x the 400x800 test pane, so a width fit overflows by 80px and one tap reaches the end.
            const val ASPECT = 2.4f
        }
    }

    private companion object {
        const val PAGE_COUNT = 4

        /** The square page 2 in [MixedPageBitmaps], between the overflowing pages 1 and 3. */
        const val FITTING_PAGE_INDEX = 1
    }
}
