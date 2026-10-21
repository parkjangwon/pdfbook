package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.collectLatest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.settings.TwoPageMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w841dp-h701dp-xxhdpi")
class FoldPostureComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pageCount = 40

    @Test
    fun zf1_tabletopPageTopPadsBottomNoClickInFold() {
        setReader(
            page = 12,
            twoPage = false,
            direction = ReadingDirection.LTR,
            posture = { w, h -> FoldPosture.Tabletop(horizontalFold(w, h)) },
        )
        composeRule.onNodeWithTag("tabletop_pads").assertIsDisplayed()
        composeRule.onNodeWithTag("tabletop_pad_Previous").assertIsDisplayed()
        composeRule.onNodeWithTag("tabletop_pad_Menu").assertIsDisplayed()
        composeRule.onNodeWithTag("tabletop_pad_Next").assertIsDisplayed()
        composeRule.onNodeWithTag("tabletop_page_number").assertIsDisplayed()

        val pageArea = composeRule.onNodeWithTag("reader_page_area").getBoundsInRoot()
        val pads = composeRule.onNodeWithTag("tabletop_pads").getBoundsInRoot()
        assertTrue(pageArea.bottom <= pads.top + 1.dp)
        listOf("tabletop_pad_Previous", "tabletop_pad_Menu", "tabletop_pad_Next", "tabletop_page_number")
            .map { composeRule.onNodeWithTag(it).getBoundsInRoot() }
            .forEach { bounds ->
                assertTrue(
                    "control too close to fold: $bounds pageBottom=${pageArea.bottom}",
                    bounds.top >= pageArea.bottom + 12.dp,
                )
            }
    }

    @Test
    fun zf2_rtlNextPadOnLeftAndAdvances() {
        var page by mutableIntStateOf(12)
        setReader(
            pageState = { page },
            onPage = { page = it },
            twoPage = false,
            direction = ReadingDirection.RTL,
            posture = { w, h -> FoldPosture.Tabletop(horizontalFold(w, h)) },
        )
        val next = composeRule.onNodeWithTag("tabletop_pad_Next").getBoundsInRoot()
        val prev = composeRule.onNodeWithTag("tabletop_pad_Previous").getBoundsInRoot()
        assertTrue(next.left < prev.left)
        composeRule.onNodeWithTag("tabletop_pad_Next").performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        assertEquals(13, page)
    }

    @Test
    fun zf3_anchorSurvivesTwoPageAndPostureChanges() {
        var mode by mutableStateOf(TwoPageMode.Auto)
        var widthDp by mutableIntStateOf(841)
        var postureKind by mutableStateOf("flat")
        var page by mutableIntStateOf(37)

        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val posture = when (postureKind) {
                        "tabletop" -> FoldPosture.Tabletop(horizontalFold(constraints.maxWidth, constraints.maxHeight))
                        "book" -> FoldPosture.Book(verticalFold(constraints.maxWidth, constraints.maxHeight))
                        else -> FoldPosture.Flat
                    }
                    val twoPage = shouldUseTwoPage(mode, widthDp, posture)
                    val pageSpreads = remember(pageCount, twoPage) {
                        spreads(pageCount, twoPage = twoPage, coverAlone = true)
                    }
                    val pagerState = rememberPagerState(
                        initialPage = spreadIndexOf(pageSpreads, page),
                        pageCount = { pageSpreads.size },
                    )
                    LaunchedEffect(pageSpreads) {
                        var sync = readingAnchorBeginSync(ReadingAnchor(page), pageSpreads)
                        val index = readingAnchorTargetIndex(pageSpreads, sync)
                        if (pagerState.currentPage != index) pagerState.scrollToPage(index)
                        else sync = sync.copy(pendingIndex = null)
                        snapshotFlow { pagerState.settledPage }.collectLatest { settled ->
                            sync = readingAnchorAfterSettle(sync, pageSpreads, settled)
                            if (sync.page != page) page = sync.page
                        }
                    }
                    ReaderScreen(
                        renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                        pagerState = pagerState,
                        spreads = pageSpreads,
                        title = "책",
                        pageCount = pageCount,
                        currentPage = page,
                        twoPage = twoPage,
                        posture = posture,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.LTR,
                        showTapZoneGuide = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(37, page)

        widthDp = 360
        postureKind = "tabletop"
        mode = TwoPageMode.Auto
        composeRule.waitForIdle()
        assertEquals(37, page)

        widthDp = 841
        postureKind = "book"
        composeRule.waitForIdle()
        assertEquals(37, page)

        postureKind = "flat"
        composeRule.waitForIdle()
        assertEquals(37, page)
    }

    @Test
    fun zf4_bookGapAtFoldSliderInRightHalf() {
        setReader(
            page = 12,
            twoPage = true,
            direction = ReadingDirection.LTR,
            overlay = true,
            posture = { w, h -> FoldPosture.Book(verticalFold(w, h)) },
        )
        val gap = composeRule.onNodeWithTag("book_posture_fold_gap").getBoundsInRoot()
        val leftPage = composeRule.onNodeWithTag("reader_page_12").getBoundsInRoot()
        // Cover-alone two-page: page 12 is in [12,13] → LTR layout [12,13], left is 12.
        assertTrue(leftPage.right <= gap.left + 1.dp)
        val slider = composeRule.onNodeWithTag("reader_slider").getBoundsInRoot()
        assertTrue(slider.left >= gap.right - 1.dp)
    }

    @Test
    fun zf5_bookSingleDoesNotCrossFold() {
        setReader(
            page = 12,
            twoPage = false,
            direction = ReadingDirection.LTR,
            posture = { w, h -> FoldPosture.Book(verticalFold(w, h)) },
        )
        val page = composeRule.onNodeWithTag("reader_page_12").getBoundsInRoot()
        val gap = composeRule.onNodeWithTag("book_posture_fold_gap").getBoundsInRoot()
        assertTrue(page.right <= gap.left + 1.dp)
    }

    @Test
    fun zf1_tabletopHidesPageModeToggleOnly() {
        var postureKind by mutableStateOf("tabletop")
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val posture = when (postureKind) {
                        "tabletop" -> FoldPosture.Tabletop(horizontalFold(constraints.maxWidth, constraints.maxHeight))
                        "book" -> FoldPosture.Book(verticalFold(constraints.maxWidth, constraints.maxHeight))
                        else -> FoldPosture.Flat
                    }
                    val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
                    val pagerState = rememberPagerState(
                        initialPage = spreadIndexOf(pageSpreads, 12),
                        pageCount = { pageSpreads.size },
                    )
                    ReaderScreen(
                        renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                        pagerState = pagerState,
                        spreads = pageSpreads,
                        title = "책",
                        pageCount = pageCount,
                        currentPage = 12,
                        twoPage = false,
                        posture = posture,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.LTR,
                        overlayVisible = true,
                        showTapZoneGuide = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("reader_direction_toggle").assertCountEquals(1)
        composeRule.onAllNodesWithTag("reader_page_mode", useUnmergedTree = true).assertCountEquals(0)

        postureKind = "flat"
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("reader_direction_toggle").assertCountEquals(1)
        composeRule.onAllNodesWithTag("reader_page_mode", useUnmergedTree = true).assertCountEquals(1)

        postureKind = "book"
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("reader_page_mode", useUnmergedTree = true).assertCountEquals(1)
    }

    private fun setReader(
        page: Int = 12,
        pageState: (() -> Int)? = null,
        onPage: ((Int) -> Unit)? = null,
        twoPage: Boolean,
        direction: ReadingDirection,
        overlay: Boolean = false,
        posture: (width: Int, height: Int) -> FoldPosture,
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val foldPosture = posture(constraints.maxWidth, constraints.maxHeight)
                    val current = pageState?.invoke() ?: page
                    val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
                    val pagerState = rememberPagerState(
                        initialPage = spreadIndexOf(pageSpreads, current),
                        pageCount = { pageSpreads.size },
                    )
                    if (onPage != null) {
                        LaunchedEffect(pagerState.settledPage) {
                            pageSpreads.getOrNull(pagerState.settledPage)?.firstOrNull()?.let(onPage)
                        }
                    }
                    ReaderScreen(
                        renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                        pagerState = pagerState,
                        spreads = pageSpreads,
                        title = "책",
                        pageCount = pageCount,
                        currentPage = current,
                        twoPage = twoPage,
                        posture = foldPosture,
                        onBack = {},
                        onDirectionChange = {},
                        direction = direction,
                        overlayVisible = overlay,
                        showTapZoneGuide = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun horizontalFold(width: Int, height: Int): IntRect {
        val mid = height / 2
        return IntRect(0, mid - 8, width, mid + 8)
    }

    private fun verticalFold(width: Int, height: Int): IntRect {
        val mid = width / 2
        return IntRect(mid - 8, 0, mid + 8, height)
    }

}
