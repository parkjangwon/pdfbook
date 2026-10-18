package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class TapZoneGuideTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun guideShowsWhenOverlayVisibleAndSettingOn() {
        setReader(overlayVisible = true, showGuide = true)
        composeRule.onNodeWithTag(TAP_ZONE_GUIDE_TAG).assertIsDisplayed()
    }

    @Test
    fun guideHiddenWhenOverlayHidden() {
        setReader(overlayVisible = false, showGuide = true)
        composeRule.onAllNodesWithTag(TAP_ZONE_GUIDE_TAG).assertCountEquals(0)
    }

    @Test
    fun guideHiddenWhenSettingOff() {
        setReader(overlayVisible = true, showGuide = false)
        composeRule.onAllNodesWithTag(TAP_ZONE_GUIDE_TAG).assertCountEquals(0)
    }

    @Test
    fun rtlPutsNextOnTheLeft() {
        setReader(overlayVisible = true, showGuide = true, direction = ReadingDirection.RTL)
        val next = composeRule.onNodeWithText("다음").getBoundsInRoot()
        val previous = composeRule.onNodeWithText("이전").getBoundsInRoot()
        assertTrue(next.left < previous.left)
    }

    @Test
    fun ltrPutsPreviousOnTheLeft() {
        setReader(overlayVisible = true, showGuide = true, direction = ReadingDirection.LTR)
        val next = composeRule.onNodeWithText("다음").getBoundsInRoot()
        val previous = composeRule.onNodeWithText("이전").getBoundsInRoot()
        assertTrue(previous.left < next.left)
    }

    private fun setReader(
        overlayVisible: Boolean,
        showGuide: Boolean,
        direction: ReadingDirection = ReadingDirection.RTL,
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
                    direction = direction,
                    overlayVisible = overlayVisible,
                    showTapZoneGuide = showGuide,
                )
            }
        }
    }
}
