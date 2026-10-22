package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
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
class GoToPageComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pageCount = 120
    private var kept by mutableIntStateOf(12)

    private fun setReader(start: Int = 12, twoPage: Boolean = false) {
        kept = start
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        val renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                val pagerState = rememberPagerState(
                    initialPage = spreadIndexOf(pageSpreads, start),
                    pageCount = { pageSpreads.size },
                )
                LaunchedEffect(pagerState.settledPage) {
                    keptPageFromSettle(pageSpreads, pagerState.settledPage)?.let { kept = it }
                }
                ReaderScreen(
                    renderer = renderer,
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "책",
                    pageCount = pageCount,
                    currentPage = kept,
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.LTR,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun openDialog() {
        composeRule.onNodeWithTag("reader_progress").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(GO_TO_PAGE_DIALOG_TAG).assertIsDisplayed()
    }

    private fun typePage(value: String) {
        composeRule.onNodeWithTag(GO_TO_PAGE_FIELD_TAG).performTextClearance()
        composeRule.onNodeWithTag(GO_TO_PAGE_FIELD_TAG).performTextInput(value)
        composeRule.waitForIdle()
    }

    private fun pill(page: Int) = composeRule.onNodeWithText("${page}쪽으로 돌아가기")

    @Test
    fun tappingPageNumberOpensDialogWithRangeAndError() {
        setReader()
        val progress = composeRule.onNodeWithTag("reader_progress")
        progress.assertTextEquals("12 / 120")
        assertEquals(
            "쪽 번호로 이동",
            progress.fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
        val progressBounds = progress.getUnclippedBoundsInRoot()
        val progressHeight = progressBounds.bottom - progressBounds.top
        assertTrue("progress touch target is $progressHeight", progressHeight >= 48.dp)
        openDialog()
        composeRule.onNodeWithTag(GO_TO_PAGE_FIELD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("1~120").assertIsDisplayed()
        composeRule.onNodeWithText("1~120 사이의 쪽 번호를 넣어 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("이동").assertIsNotEnabled()
    }

    @Test
    fun invalidInputKeepsGoDisabledAndValidInputEnablesIt() {
        setReader()
        openDialog()
        typePage("0")
        composeRule.onNodeWithText("이동").assertIsNotEnabled()
        typePage("121")
        composeRule.onNodeWithText("이동").assertIsNotEnabled()
        composeRule.onNodeWithText("1~120 사이의 쪽 번호를 넣어 주세요.").assertIsDisplayed()
        typePage("120")
        composeRule.onNodeWithText("이동").assertIsEnabled()
        composeRule.onNodeWithText("1~120 사이의 쪽 번호를 넣어 주세요.").assertDoesNotExist()
    }

    @Test
    fun goJumpsToPageAndShowsJumpBackPill() {
        setReader()
        openDialog()
        typePage("80")
        composeRule.onNodeWithText("이동").performClick()
        composeRule.waitForIdle()
        assertEquals(80, kept)
        composeRule.onAllNodesWithTag(GO_TO_PAGE_DIALOG_TAG).assertCountEquals(0)
        pill(12).assertIsDisplayed()
    }

    @Test
    fun goInTwoPageViewLandsOnTheSpreadContainingThePage() {
        setReader(twoPage = true)
        openDialog()
        typePage("81")
        composeRule.onNodeWithText("이동").performClick()
        composeRule.waitForIdle()
        assertEquals(80, kept)
        pill(12).assertIsDisplayed()
    }

    @Test
    fun cancelClosesWithoutJumping() {
        setReader()
        openDialog()
        typePage("80")
        composeRule.onNodeWithText("취소").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(GO_TO_PAGE_DIALOG_TAG).assertCountEquals(0)
        // Also guards the reading position against focus falling back into an
        // offscreen page's tap zone and scrolling the pager.
        assertEquals(12, kept)
        composeRule.onNodeWithText("12쪽으로 돌아가기").assertDoesNotExist()
    }
}
