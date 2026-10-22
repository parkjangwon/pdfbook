package org.parkjw.apps.pdfbook

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AutoTurnComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pageCount = 10
    private var kept by mutableIntStateOf(1)
    private var savedSeconds: Int? = null
    private var viewRef: View? = null

    private fun setReader(
        start: Int = 1,
        direction: ReadingDirection = ReadingDirection.LTR,
        twoPage: Boolean = false,
        volumes: ReaderVolumes = ReaderVolumes(),
    ) {
        kept = start
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                Reader(pageSpreads, direction, twoPage, volumes)
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Reader(
        pageSpreads: List<List<Int>>,
        direction: ReadingDirection,
        twoPage: Boolean,
        volumes: ReaderVolumes,
    ) {
        val pagerState = rememberPagerState(
            initialPage = spreadIndexOf(pageSpreads, kept),
            pageCount = { pageSpreads.size },
        )
        LaunchedEffect(pagerState.settledPage) {
            keptPageFromSettle(pageSpreads, pagerState.settledPage)?.let { kept = it }
        }
        var seconds by remember { mutableIntStateOf(AUTO_TURN_DEFAULT_SECONDS) }
        val view = LocalView.current
        SideEffect { viewRef = view }
        ReaderScreen(
            renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
            pagerState = pagerState,
            spreads = pageSpreads,
            title = "책 1권",
            pageCount = pageCount,
            currentPage = kept,
            twoPage = twoPage,
            onBack = {},
            onDirectionChange = {},
            direction = direction,
            overlayVisible = true,
            showTapZoneGuide = false,
            volumes = volumes,
            autoTurnSeconds = seconds,
            onAutoTurnSecondsChange = {
                seconds = it
                savedSeconds = it
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    private fun clickTag(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun openSheet() {
        clickTag("reader_auto_turn")
    }

    private fun startAutoTurn(seconds: Int) {
        openSheet()
        clickTag(autoTurnIntervalTag(seconds))
        clickTag(AUTO_TURN_START_TAG)
    }

    private fun advance(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        composeRule.waitForIdle()
    }

    private fun tapNext() {
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun tapMenu() {
        composeRule.onRoot().performTouchInput { click(Offset(width / 2f, height / 2f)) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    @Test
    fun sheetOffersItsIntervalsAndKeepsTheDefault() {
        setReader()
        openSheet()
        composeRule.onNodeWithTag(AUTO_TURN_SHEET_TAG).assertIsDisplayed()
        for (seconds in AUTO_TURN_INTERVAL_SECONDS) {
            composeRule.onNodeWithTag(autoTurnIntervalTag(seconds)).assertIsDisplayed()
        }
        composeRule.onNodeWithTag(autoTurnIntervalTag(15)).assertIsSelected()
    }

    @Test
    fun startShowsStatusAndKeepsTheScreenAwake() {
        setReader()
        startAutoTurn(15)
        advance(100)
        composeRule.onNodeWithText("자동 넘김 15초").assertIsDisplayed()
        assertTrue(viewRef!!.keepScreenOn)
    }

    @Test
    fun chosenIntervalIsRememberedThroughTheCallback() {
        setReader()
        openSheet()
        clickTag(autoTurnIntervalTag(30))
        composeRule.onNodeWithTag(autoTurnIntervalTag(30)).assertIsSelected()
        assertEquals(30, savedSeconds)
    }

    @Test
    fun turnsAfterTheChosenInterval() {
        setReader()
        startAutoTurn(5)
        assertEquals(1, kept)
        advance(4_000)
        assertEquals(1, kept)
        advance(2_000)
        advance(1_000)
        assertEquals(2, kept)
    }

    @Test
    fun stopsWhenThePageIsTouched() {
        setReader()
        startAutoTurn(5)
        advance(5_000)
        advance(1_000)
        assertEquals(2, kept)
        tapNext()
        advance(1_000)
        assertEquals(3, kept)
        composeRule.onNodeWithText("자동 넘김을 멈췄어요").assertIsDisplayed()
        assertFalse(viewRef!!.keepScreenOn)
        advance(10_000)
        assertEquals(3, kept)
    }

    @Test
    fun stopsWhenTheOverlayOpens() {
        setReader()
        startAutoTurn(5)
        tapMenu()
        advance(500)
        composeRule.onNodeWithTag("reader_overlay_top").assertIsDisplayed()
        advance(10_000)
        assertEquals(1, kept)
    }

    @Test
    fun stopsAtTheLastPageAndShowsTheNextBookNoticeWithoutOpeningIt() {
        var opened = 0
        val next = BookInfo("doc:next", "다음 책.pdf", 10)
        setReader(
            start = pageCount,
            volumes = ReaderVolumes(onNext = { opened++ }, nextBook = next),
        )
        startAutoTurn(5)
        advance(5_000)
        advance(500)
        composeRule.onNodeWithText("다음 책: 다음 책").assertIsDisplayed()
        assertEquals(0, opened)
        advance(10_000)
        assertEquals(0, opened)
        assertEquals(pageCount, kept)
    }

    @Test
    fun rtlTurnsToTheNextSpread() {
        setReader(direction = ReadingDirection.RTL)
        startAutoTurn(5)
        advance(5_000)
        advance(1_000)
        assertEquals(2, kept)
    }

    @Test
    fun twoPageTurnsBySpread() {
        setReader(twoPage = true)
        startAutoTurn(5)
        advance(5_000)
        advance(1_000)
        assertEquals(2, kept)
    }
}
