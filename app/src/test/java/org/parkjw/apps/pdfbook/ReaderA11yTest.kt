package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
class ReaderA11yTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapZonesHaveAccessibleNamesAndButtonRole() {
        setReader()
        val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onAllNodesWithTag("reader_zone_Previous")[0]
            .assertContentDescriptionEquals("이전 쪽으로 넘겨요")
            .assert(button)
        composeRule.onAllNodesWithTag("reader_zone_Menu")[0]
            .assertContentDescriptionEquals("메뉴를 열거나 닫아요")
            .assert(button)
        composeRule.onAllNodesWithTag("reader_zone_Next")[0]
            .assertContentDescriptionEquals("다음 쪽으로 넘겨요")
            .assert(button)
    }

    @Test
    fun overlayBarsAreNotUnlabeledButtons() {
        setReader(overlayVisible = true)
        val notClickable = SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("reader_overlay_top").assert(notClickable)
        composeRule.onNodeWithTag("reader_overlay_bottom").assert(notClickable)
    }

    private fun setReader(overlayVisible: Boolean = false) {
        val pageCount = 4
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
                    showTapZoneGuide = false,
                )
            }
        }
    }
}
