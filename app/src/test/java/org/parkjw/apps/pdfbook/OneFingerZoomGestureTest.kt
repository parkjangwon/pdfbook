package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class OneFingerZoomGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var pagerState: PagerState
    private var opened: Any? = null
    private var sidePercent by mutableIntStateOf(DEFAULT_TAP_ZONE_SIDE_PERCENT)
    private var direction by mutableStateOf(ReadingDirection.LTR)

    @Test
    fun offQuickDoubleTapStillZooms() {
        setReader(oneFingerZoom = false)
        doubleTap()
        settle()
        assertEquals(DoubleTapZoomScale, zoom().scale, 0f)
    }

    @Test
    fun offDoubleTapHoldDoesNotZoomWhileHeld() {
        setReader(oneFingerZoom = false)
        holdSecondTap()
        assertEquals(MinZoomScale, zoom().scale, 0f)
        releaseHold()
    }

    @Test
    fun offSwipeStillTurnsPage() {
        setReader(oneFingerZoom = false)
        swipe()
        settle()
        assertEquals(1, pagerState.currentPage)
        assertEquals(MinZoomScale, zoom().scale, 0f)
    }

    @Test
    fun onTapZoneStillTurnsPage() {
        setReader(oneFingerZoom = true)
        tap(0.9f)
        settle()
        assertEquals(1, pagerState.currentPage)
        assertEquals(MinZoomScale, zoom().scale, 0f)
    }

    @Test
    fun onQuickDoubleTapZoomsAndZoomsBackOut() {
        setReader(oneFingerZoom = true)
        doubleTap()
        settle()
        assertEquals(DoubleTapZoomScale, zoom().scale, 0f)
        doubleTap()
        settle()
        assertEquals(MinZoomScale, zoom().scale, 0f)
    }

    @Test
    fun onSecondTapHeldZoomsPansAndRestoresOnRelease() {
        setReader(oneFingerZoom = true)
        holdSecondTap()
        assertEquals(DoubleTapZoomScale, zoom().scale, 0f)

        composeRule.onRoot().performTouchInput { moveBy(Offset(150f, 0f)) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(150f, zoom().offsetX, 1f)

        releaseHold()
        assertEquals(MinZoomScale, zoom().scale, 0f)
        assertEquals(0, pagerState.currentPage)
    }

    @Test
    fun onHoldAtLastPageDoesNotTurnOrOpenNextBook() {
        setReader(oneFingerZoom = true, initialPage = PAGE_COUNT - 1, nextBook = true)
        holdSecondTap()
        composeRule.onRoot().performTouchInput { moveBy(Offset(-150f, 0f)) }
        composeRule.mainClock.advanceTimeByFrame()
        releaseHold()
        assertEquals(MinZoomScale, zoom().scale, 0f)
        assertEquals(PAGE_COUNT - 1, pagerState.currentPage)
        assertNull(opened)
    }

    @Test
    fun onHoldUsesUpdatedZoneWidth() {
        setReader(oneFingerZoom = true)
        sidePercent = TAP_ZONE_SIDE_MAX_PERCENT
        composeRule.waitForIdle()

        holdSecondTap()

        // The held point is still the page centre; a stale middle-zone start would
        // anchor the zoom about 0.12 x width away from it.
        assertEquals(DoubleTapZoomScale, zoom().scale, 0f)
        assertEquals(0f, zoom().offsetX, 20f)
        releaseHold()
    }

    @Test
    fun onSideZoneTapUsesUpdatedDirectionWhileZoomed() {
        setReader(oneFingerZoom = true)
        doubleTap()
        settle()
        assertEquals(DoubleTapZoomScale, zoom().scale, 0f)

        direction = ReadingDirection.RTL
        composeRule.waitForIdle()

        tap(0.1f)
        settle()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun offNoneModeDragStillPansWhileZoomed() {
        setReader(oneFingerZoom = false, pageTurnAnimation = PageTurnAnimation.None)
        doubleTap()
        settle()
        assertEquals(DoubleTapZoomScale, zoom().scale, 0f)

        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.5f, height / 2f))
            advanceEventTime(16)
            moveTo(Offset(width * 0.5f - 120f, height / 2f))
            advanceEventTime(16)
        }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(-120f, zoom().offsetX, 5f)
        composeRule.onRoot().performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun onSwipeWithCurlStillTurnsPage() {
        setReader(oneFingerZoom = true, pageTurnAnimation = PageTurnAnimation.Curl)
        swipe()
        assertEquals(1, pagerState.currentPage)
        assertEquals(MinZoomScale, zoom().scale, 0f)
    }

    private fun holdSecondTap() {
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { down(Offset(width * 0.5f, height / 2f)) }
        composeRule.mainClock.advanceTimeBy(30)
        composeRule.onRoot().performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onRoot().performTouchInput { down(Offset(width * 0.5f, height / 2f)) }
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun releaseHold() {
        composeRule.onRoot().performTouchInput { up() }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(1_000)
    }

    private fun swipe() {
        // Starts in the middle zone, where the hold detector lives when the switch is on.
        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.6f, height / 2f))
            advanceEventTime(16)
            moveTo(Offset(width * 0.5f, height / 2f))
            advanceEventTime(16)
            moveTo(Offset(width * 0.3f, height / 2f))
            advanceEventTime(16)
            moveTo(Offset(width * 0.1f, height / 2f))
            advanceEventTime(16)
            up()
        }
        settle()
    }

    private fun tap(fraction: Float) {
        composeRule.onRoot().performTouchInput { click(Offset(width * fraction, height / 2f)) }
    }

    private fun doubleTap() {
        composeRule.onRoot().performTouchInput { doubleClick(Offset(width * 0.5f, height / 2f)) }
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun zoom(): ZoomState =
        composeRule.onNodeWithTag("reader_screen").fetchSemanticsNode().config[ReaderZoomKey]

    private fun setReader(
        oneFingerZoom: Boolean,
        pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.Slide,
        initialPage: Int = 0,
        nextBook: Boolean = false,
    ) {
        val pageSpreads = spreads(PAGE_COUNT, twoPage = false, coverAlone = true)
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
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    tapZoneSidePercent = sidePercent,
                    oneFingerZoom = oneFingerZoom,
                    pageTurnAnimation = pageTurnAnimation,
                    volumes = if (nextBook) {
                        ReaderVolumes(onNext = { opened = Unit }, nextBook = BookInfo("doc:2", "책 2권.pdf", 10))
                    } else {
                        ReaderVolumes()
                    },
                )
            }
        }
    }

    private companion object {
        const val PAGE_COUNT = 6
    }
}
