package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class ReadingInfoComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everythingOffShowsNoChip() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReadingInfoChip(page = null, clock = null, battery = null, maxWidth = 304.dp)
            }
        }
        composeRule.onAllNodesWithTag("reader_info_chip").assertCountEquals(0)
    }

    @Test
    fun enabledItemsShowInTheFixedOrder() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReadingInfoChip(
                    page = ReadingInfoPage.Spread(12, 13, 180),
                    clock = ReadingInfoClock(display = "22:52", spoken = "오후 10:52"),
                    battery = 87,
                    maxWidth = 304.dp,
                )
            }
        }
        composeRule.onNodeWithTag("reader_info_chip").assertIsDisplayed()
        composeRule.onNodeWithText("12–13/180").assertIsDisplayed()
        composeRule.onNodeWithText("22:52").assertIsDisplayed()
        composeRule.onNodeWithText("87%").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_info_chip").assertContentDescriptionEquals(
            "12~13쪽, 전체 180쪽, 오후 10:52, 배터리 87%",
        )
    }

    @Test
    fun onlyTheEnabledItemIsShown() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                ReadingInfoChip(
                    page = ReadingInfoPage.Single(12, 180),
                    clock = null,
                    battery = 87,
                    maxWidth = 304.dp,
                )
            }
        }
        composeRule.onNodeWithText("12/180").assertIsDisplayed()
        composeRule.onNodeWithText("87%").assertIsDisplayed()
        composeRule.onAllNodesWithTag("reader_info_chip").assertCountEquals(1)
        composeRule.onNodeWithTag("reader_info_chip").assertContentDescriptionEquals(
            "12쪽, 전체 180쪽, 배터리 87%",
        )
    }

    // The pager counts spreads, so a two-page book has half as many pager pages as pages.
    @Test
    fun twoPageViewDividesByTheRealPageCount() {
        val spreads = twoPageSpreads(180)
        val index = spreads.indexOf(listOf(12, 13))
        assertEquals("pager pages must differ from pages", 91, spreads.size)
        hostOn(spreads, pagerCount = spreads.size, pageIndex = index, pageCount = 180)
        composeRule.onNodeWithText("12–13/180").assertIsDisplayed()
    }

    @Test
    fun twoPageViewStillNumbersTheLoneLastPage() {
        val spreads = twoPageSpreads(180)
        hostOn(spreads, pagerCount = spreads.size, pageIndex = spreads.lastIndex, pageCount = 180)
        composeRule.onNodeWithText("180/180").assertIsDisplayed()
    }

    @Test
    fun twoPageViewKeepsPagesPastTheOldPagerLimit() {
        // A 30-page book has 16 pager pages in two-page view; pages 16 and up used to vanish.
        val spreads = twoPageSpreads(30)
        val index = spreads.indexOf(listOf(16, 17))
        hostOn(spreads, pagerCount = spreads.size, pageIndex = index, pageCount = 30)
        composeRule.onNodeWithText("16–17/30").assertIsDisplayed()
    }

    @Test
    fun singlePageViewKeepsItsPageNumber() {
        val spreads = (1..180).map { listOf(it) }
        hostOn(spreads, pagerCount = spreads.size, pageIndex = 11, pageCount = 180)
        composeRule.onNodeWithText("12/180").assertIsDisplayed()
    }

    private fun twoPageSpreads(pageCount: Int): List<List<Int>> {
        val spreads = mutableListOf(listOf(1))
        var page = 2
        while (page <= pageCount) {
            spreads += if (page + 1 <= pageCount) listOf(page, page + 1) else listOf(page)
            page += 2
        }
        return spreads
    }

    private fun hostOn(
        spreads: List<List<Int>>,
        pagerCount: Int,
        pageIndex: Int,
        pageCount: Int,
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                val pagerState = rememberPagerState(initialPage = pageIndex) { pagerCount }
                ReadingInfoHost(
                    spreads = spreads,
                    pagerState = pagerState,
                    pageCount = pageCount,
                    maxWidth = 304.dp,
                    showPage = true,
                    showTime = false,
                    showBattery = false,
                    omitPage = false,
                    atFold = false,
                    visible = true,
                )
            }
        }
    }
}
