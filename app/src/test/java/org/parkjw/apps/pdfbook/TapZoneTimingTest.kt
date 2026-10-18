package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class TapZoneTimingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var pagerState: PagerState

    @Test
    fun singleTapOnSideTurnsPageWithoutDoubleTapWait() {
        setReader()
        composeRule.mainClock.autoAdvance = false
        tap(0.9f)
        composeRule.mainClock.advanceTimeBy(48)
        assertEquals(1, pagerState.targetPage)
    }

    @Test
    fun rtlSingleTapOnLeftTurnsPageWithoutDoubleTapWait() {
        setReader(direction = ReadingDirection.RTL)
        composeRule.mainClock.autoAdvance = false
        tap(0.1f)
        composeRule.mainClock.advanceTimeBy(48)
        assertEquals(1, pagerState.targetPage)
    }

    @Test
    fun doubleTapOnSideAtBaseScaleTurnsTwoPages() {
        setReader()
        composeRule.onRoot().performTouchInput { doubleClick(Offset(width * 0.9f, height / 2f)) }
        settle()
        assertEquals(2, pagerState.currentPage)
    }

    @Test
    fun doubleTapWorksWhileZoomed() {
        setReader()
        composeRule.onRoot().performTouchInput { doubleClick(Offset(width * 0.5f, height / 2f)) }
        settle()
        tap(0.9f)
        settle()
        assertEquals(0, pagerState.currentPage)
        composeRule.onRoot().performTouchInput { doubleClick(Offset(width * 0.9f, height / 2f)) }
        settle()
        tap(0.9f)
        settle()
        assertEquals(1, pagerState.currentPage)
    }

    private fun tap(fraction: Float) {
        composeRule.onRoot().performTouchInput { click(Offset(width * fraction, height / 2f)) }
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun setReader(direction: ReadingDirection = ReadingDirection.LTR) {
        val pageCount = 6
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            pagerState = rememberPagerState(pageCount = { pageSpreads.size })
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0, 0),
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "t",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                )
            }
        }
    }
}
