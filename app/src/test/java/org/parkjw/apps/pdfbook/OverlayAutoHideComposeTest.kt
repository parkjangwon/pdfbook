package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class OverlayAutoHideComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setReader() {
        val pageCount = 4
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "책 1권",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun overlayHidesAfterInactivityTimeout() {
        composeRule.mainClock.autoAdvance = false
        setReader()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_overlay_top").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(OVERLAY_AUTO_HIDE_MILLIS)
        composeRule.mainClock.advanceTimeBy(END_NOTICE_FADE_MILLIS + 500L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("reader_overlay_top").assertCountEquals(0)
        composeRule.onAllNodesWithTag("reader_overlay_bottom").assertCountEquals(0)
    }

    @Test
    fun overlayStaysWhileInteractingThenHidesAfterIdle() {
        composeRule.mainClock.autoAdvance = false
        setReader()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_overlay_top").performTouchInput {
            down(center)
            up()
        }
        composeRule.mainClock.advanceTimeBy(OVERLAY_AUTO_HIDE_MILLIS - 1_000)
        composeRule.onNodeWithTag("reader_overlay_top").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(1_000 + END_NOTICE_FADE_MILLIS + 500L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("reader_overlay_top").assertCountEquals(0)
    }

    @Test
    fun overlayStaysWhileSliderDragContinues() {
        composeRule.mainClock.autoAdvance = false
        setReader()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_slider").performTouchInput {
            down(center)
            moveBy(Offset(width / 4f, 0f))
        }
        composeRule.mainClock.advanceTimeBy(OVERLAY_AUTO_HIDE_MILLIS + END_NOTICE_FADE_MILLIS + 500L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_overlay_top").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_slider").performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(END_NOTICE_FADE_MILLIS + 500L)
        composeRule.mainClock.advanceTimeBy(OVERLAY_AUTO_HIDE_MILLIS + END_NOTICE_FADE_MILLIS + 500L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("reader_overlay_top").assertCountEquals(0)
    }
}
