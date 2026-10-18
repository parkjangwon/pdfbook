package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getBoundsInRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class TapZoneWidthSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    @Test
    fun entryOpensSheetInsideReader() {
        setReader()
        composeRule.onAllNodesWithTag(TAP_ZONE_WIDTH_SHEET_TAG).assertCountEquals(0)
        composeRule.onNodeWithText("폭 조절").performClick()
        composeRule.onNodeWithTag(TAP_ZONE_WIDTH_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TAP_ZONE_GUIDE_TAG).assertIsDisplayed()
    }

    @Test
    fun sliderSnapsSavesAndMovesGuide() {
        val saved = mutableListOf<Int>()
        setReader(onChange = { saved += it })
        composeRule.onNodeWithText("폭 조절").performClick()
        val before = composeRule.onNodeWithText("이전").getBoundsInRoot()
        composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(41.2f) }
        composeRule.waitForIdle()
        assertEquals(listOf(40), saved)
        composeRule.onNodeWithText("40% · 기본 33%").assertIsDisplayed()
        val after = composeRule.onNodeWithText("이전").getBoundsInRoot()
        assertTrue(after.left != before.left)
    }

    @Test
    fun resetRestoresDefault() {
        val saved = mutableListOf<Int>()
        setReader(percent = 40, onChange = { saved += it })
        composeRule.onNodeWithText("폭 조절").performClick()
        composeRule.onNodeWithText("기본값으로").performClick()
        assertEquals(listOf(33), saved)
        composeRule.onNodeWithText("33% · 기본 33%").assertIsDisplayed()
    }

    @Test
    fun doneClosesSheetAndKeepsMenu() {
        setReader()
        composeRule.onNodeWithText("폭 조절").performClick()
        composeRule.onNodeWithText("완료").performClick()
        composeRule.onAllNodesWithTag(TAP_ZONE_WIDTH_SHEET_TAG).assertCountEquals(0)
        composeRule.onNodeWithText("폭 조절").assertIsDisplayed()
    }

    @Test
    fun entryStaysWhenGuideSettingOffAndSheetPreviewsGuide() {
        setReader(showGuide = false)
        composeRule.onAllNodesWithTag(TAP_ZONE_GUIDE_TAG).assertCountEquals(0)
        composeRule.onNodeWithText("폭 조절").performClick()
        composeRule.onNodeWithTag(TAP_ZONE_GUIDE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("완료").performClick()
        composeRule.onAllNodesWithTag(TAP_ZONE_GUIDE_TAG).assertCountEquals(0)
    }

    @Test
    fun defaultWidthTreatsFortyTwoPercentAsMenu() {
        setReader(overlayVisible = false)
        tapAt(0.42f)
        composeRule.onNodeWithText("폭 조절").assertIsDisplayed()
    }

    @Test
    fun widestSidesTreatFortyTwoPercentAsPageTurn() {
        setReader(percent = 45, overlayVisible = false)
        tapAt(0.42f)
        composeRule.onAllNodesWithTag(TAP_ZONE_WIDTH_SHEET_TAG).assertCountEquals(0)
        composeRule.onAllNodesWithText("폭 조절").assertCountEquals(0)
        tapAt(0.5f)
        composeRule.onNodeWithText("폭 조절").assertIsDisplayed()
    }

    private fun tapAt(fraction: Float) {
        composeRule.onRoot().performTouchInput {
            click(Offset(width * fraction, height / 2f))
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun setReader(
        percent: Int = DEFAULT_TAP_ZONE_SIDE_PERCENT,
        overlayVisible: Boolean = true,
        showGuide: Boolean = true,
        onChange: (Int) -> Unit = {},
    ) {
        val pageCount = 5
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0, 0),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "t",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.LTR,
                    overlayVisible = overlayVisible,
                    showTapZoneGuide = showGuide,
                    tapZoneSidePercent = percent,
                    onTapZoneSidePercentChange = onChange,
                )
            }
        }
    }
}
