package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableSharedFlow
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
class JumpBackComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pageCount = 120
    private var kept by mutableIntStateOf(12)
    private var open by mutableStateOf(true)
    private var bookId by mutableIntStateOf(1)
    private val keys = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    private val renderers = mutableMapOf<Int, PageBitmaps>()

    private fun setReader(
        start: Int = 12,
        twoPage: Boolean = false,
        volumes: ReaderVolumes = ReaderVolumes(),
    ) {
        kept = start
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                if (open) Reader(pageSpreads, twoPage, volumes)
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Reader(pageSpreads: List<List<Int>>, twoPage: Boolean, volumes: ReaderVolumes) {
        val pagerState = rememberPagerState(
            initialPage = spreadIndexOf(pageSpreads, kept),
            pageCount = { pageSpreads.size },
        )
        LaunchedEffect(pagerState.settledPage) {
            keptPageFromSettle(pageSpreads, pagerState.settledPage)?.let { kept = it }
        }
        val renderer = renderers.getOrPut(bookId) {
            SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        }
        ReaderScreen(
            renderer = renderer,
            pagerState = pagerState,
            spreads = pageSpreads,
            title = "책 $bookId",
            pageCount = pageCount,
            currentPage = kept,
            twoPage = twoPage,
            onBack = {},
            onDirectionChange = {},
            direction = ReadingDirection.LTR,
            volumeDelta = keys,
            overlayVisible = true,
            showTapZoneGuide = false,
            volumes = volumes,
            modifier = Modifier.fillMaxSize(),
        )
    }

    private fun slideTo(page: Int) {
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it((page - 1).toFloat()) }
        composeRule.waitForIdle()
    }

    private fun tapNext() {
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
        composeRule.waitForIdle()
    }

    private fun advance(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        composeRule.waitForIdle()
    }

    private fun pill(page: Int) = composeRule.onNodeWithText("${page}쪽으로 돌아가기")

    private fun assertNoPill(page: Int) = pill(page).assertDoesNotExist()

    @Test
    fun jb1SliderJumpShowsButtonThatReturns() {
        setReader()
        slideTo(80)
        assertEquals(80, kept)
        pill(12).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(12, kept)
    }

    @Test
    fun jb2ReturnOffersRoundTrip() {
        setReader()
        slideTo(80)
        pill(12).performClick()
        composeRule.waitForIdle()
        assertNoPill(12)
        pill(80).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(80, kept)
        pill(12).assertIsDisplayed()
    }

    @Test
    fun jb3TapPageTurnHidesButton() {
        setReader()
        slideTo(80)
        pill(12).assertIsDisplayed()
        tapNext()
        advance(500)
        assertEquals(81, kept)
        assertNoPill(12)
    }

    @Test
    fun jb3FiveSecondsHideButton() {
        setReader()
        composeRule.mainClock.autoAdvance = false
        slideTo(80)
        advance(4_700)
        pill(12).assertIsDisplayed()
        advance(800)
        assertNoPill(12)
    }

    @Test
    fun jb3VolumeKeyAndSwipeHideButton() {
        setReader()
        slideTo(80)
        keys.tryEmit(1)
        composeRule.waitForIdle()
        advance(500)
        assertNoPill(12)
        slideTo(30)
        pill(81).assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        advance(500)
        assertNoPill(81)
    }

    @Test
    fun jb4ShortJumpShowsNothing() {
        setReader()
        slideTo(14)
        assertEquals(14, kept)
        assertNoPill(12)
        slideTo(17)
        pill(14).assertIsDisplayed()
    }

    @Test
    fun singlePageTurnsNeverShowButton() {
        setReader()
        repeat(4) { tapNext() }
        keys.tryEmit(1)
        composeRule.waitForIdle()
        advance(500)
        assertEquals(17, kept)
        assertNoPill(12)
        assertNoPill(13)
    }

    @Test
    fun jb5CloseAndReopenForgetsButtonAndKeepsLastPage() {
        setReader()
        slideTo(80)
        pill(12).assertIsDisplayed()
        open = false
        composeRule.waitForIdle()
        open = true
        composeRule.waitForIdle()
        advance(500)
        assertEquals(80, kept)
        assertNoPill(12)
    }

    @Test
    fun otherBookShowsNoButton() {
        setReader()
        slideTo(80)
        pill(12).assertIsDisplayed()
        bookId = 2
        composeRule.waitForIdle()
        advance(500)
        assertNoPill(12)
    }

    @Test
    fun twoPageUsesSpreadFrontPage() {
        setReader(start = 13, twoPage = true)
        slideTo(81)
        assertEquals(80, kept)
        pill(12).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(12, kept)
        pill(80).assertIsDisplayed()
    }

    @Test
    fun nextBookCardFirstThenButtonReturns() {
        val next = BookInfo("doc:next", "책 2.pdf", 10)
        setReader(volumes = ReaderVolumes(onNext = {}, nextBook = next))
        composeRule.mainClock.autoAdvance = false
        slideTo(120)
        advance(300)
        pill(12).assertIsDisplayed()
        tapNext()
        advance(300)
        composeRule.onNodeWithText("다음 책: 책 2").assertIsDisplayed()
        assertNoPill(12)
        advance(NEXT_BOOK_NOTICE_MILLIS + 300)
        composeRule.onNodeWithText("다음 책: 책 2").assertDoesNotExist()
        pill(12).assertIsDisplayed()
        advance(4_500)
        pill(12).assertIsDisplayed()
        advance(800)
        assertNoPill(12)
    }

    @Test
    fun buttonIsAnnouncedButtonWithVisibleName() {
        setReader()
        slideTo(80)
        pill(12)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
    }

    @Test
    fun buttonSitsAboveBarOrAtBottomInset() {
        setReader()
        slideTo(80)
        val rootHeight = composeRule.onRoot().getBoundsInRoot().bottom
        val openGap = rootHeight - pill(12).getBoundsInRoot().bottom
        composeRule.onRoot().performTouchInput { click(Offset(width / 2f, height / 2f)) }
        composeRule.waitForIdle()
        advance(500)
        val closedGap = rootHeight - pill(12).getBoundsInRoot().bottom
        assertEquals(END_NOTICE_BOTTOM.value, closedGap.value, 0.5f)
        assertTrue("open $openGap closed $closedGap", openGap > closedGap + 40.dp)
    }
}
