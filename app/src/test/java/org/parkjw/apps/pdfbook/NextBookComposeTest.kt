package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
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
class NextBookComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val next = BookInfo("doc:2", "책 2권.pdf", 10)

    private fun setReader(
        volumes: ReaderVolumes,
        direction: ReadingDirection = ReadingDirection.LTR,
        twoPage: Boolean = false,
        keys: MutableSharedFlow<Int>? = null,
    ) {
        val pageCount = 5
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    pagerState = rememberPagerState(initialPage = pageSpreads.lastIndex, pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "책 1권",
                    pageCount = pageCount,
                    currentPage = pageCount,
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    volumeDelta = keys,
                    showTapZoneGuide = false,
                    volumes = volumes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun tapNextZone(direction: ReadingDirection) {
        composeRule.onRoot().performTouchInput {
            val x = if (direction == ReadingDirection.RTL) width * 0.1f else width * 0.9f
            click(Offset(x, height / 2f))
        }
        composeRule.waitForIdle()
    }

    private fun tapPreviousZone(direction: ReadingDirection) =
        tapNextZone(if (direction == ReadingDirection.RTL) ReadingDirection.LTR else ReadingDirection.RTL)

    @Test
    fun nb1FirstNextShowsNoticeSecondNextOpens() {
        var opened = 0
        setReader(ReaderVolumes(onNext = { opened++ }, nextBook = next))
        tapNextZone(ReadingDirection.LTR)
        composeRule.onNodeWithText("다음 책: 책 2권").assertIsDisplayed()
        assertEquals(0, opened)
        tapNextZone(ReadingDirection.LTR)
        assertEquals(1, opened)
    }

    @Test
    fun openButtonOpensNextBook() {
        var opened = 0
        setReader(ReaderVolumes(onNext = { opened++ }, nextBook = next))
        tapNextZone(ReadingDirection.LTR)
        composeRule.onNodeWithContentDescription("다음 책 열기: 책 2권").performClick()
        composeRule.waitForIdle()
        assertEquals(1, opened)
    }

    @Test
    fun nb3NoNextBookShowsNoticeOnly() {
        setReader(ReaderVolumes())
        tapNextZone(ReadingDirection.LTR)
        composeRule.onNodeWithText("다음 책이 없어요").assertIsDisplayed()
    }

    @Test
    fun nb4RtlTwoPageNextZoneOpensAfterSecondTap() {
        var opened = 0
        setReader(ReaderVolumes(onNext = { opened++ }, nextBook = next), ReadingDirection.RTL, twoPage = true)
        tapNextZone(ReadingDirection.RTL)
        composeRule.onNodeWithText("다음 책: 책 2권").assertIsDisplayed()
        tapNextZone(ReadingDirection.RTL)
        assertEquals(1, opened)
    }

    @Test
    fun previousClosesNoticeSoNextShowsItAgain() {
        var opened = 0
        setReader(ReaderVolumes(onNext = { opened++ }, nextBook = next))
        tapNextZone(ReadingDirection.LTR)
        tapPreviousZone(ReadingDirection.LTR)
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        tapNextZone(ReadingDirection.LTR)
        assertEquals(0, opened)
    }

    @Test
    fun volumeKeyAndSwipeCountAsNext() {
        var opened = 0
        val keys = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        setReader(ReaderVolumes(onNext = { opened++ }, nextBook = next), ReadingDirection.RTL, keys = keys)
        composeRule.waitForIdle()
        keys.tryEmit(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("다음 책: 책 2권").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertEquals(1, opened)
    }

    @Test
    fun ltrSwipeLeftAtEndShowsNotice() {
        setReader(ReaderVolumes(onNext = {}, nextBook = next))
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("다음 책: 책 2권").assertIsDisplayed()
    }
}
