package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w360dp-h800dp")
class ReaderToolRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun brightnessToolShownAndOpensSheet() {
        setReader()
        composeRule.onNodeWithTag("reader_tool_row").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_brightness").assertIsDisplayed()
        composeRule.onNodeWithText("밝기").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_brightness").performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun autoTurnToolShownAndOpensSheet() {
        setReader()
        composeRule.onNodeWithTag("reader_auto_turn").assertIsDisplayed()
        composeRule.onNodeWithText("자동 넘김").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_auto_turn").performClick()
        composeRule.onNodeWithTag(AUTO_TURN_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun brightnessToolTouchTargetAtLeast48dp() {
        setReader()
        val bounds = composeRule.onNodeWithTag("reader_brightness").getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        val width = bounds.right - bounds.left
        assertTrue("height is $height", height >= 48.dp)
        assertTrue("width is $width", width >= 48.dp)
    }

    @Test
    fun toolsKeepEnumOrderRegardlessOfInputOrder() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                Column(Modifier.fillMaxWidth().background(ReaderOverlay)) {
                    ReaderToolRow(
                        items = listOf(
                            ReaderToolItem.Action(
                                tool = ReaderTool.AutoCrop,
                                iconRes = R.drawable.ic_brightness,
                                label = "자르기",
                                testTag = "tool_crop",
                                onClick = {},
                            ),
                            ReaderToolItem.Action(
                                tool = ReaderTool.Brightness,
                                iconRes = R.drawable.ic_brightness,
                                label = "밝기",
                                testTag = "tool_brightness",
                                onClick = {},
                            ),
                            ReaderToolItem.Toggle(
                                tool = ReaderTool.Bookmarks,
                                iconRes = R.drawable.ic_brightness,
                                label = "책갈피",
                                testTag = "tool_bookmarks",
                                checked = false,
                                onCheckedChange = {},
                            ),
                            ReaderToolItem.Action(
                                tool = ReaderTool.AutoTurn,
                                iconRes = R.drawable.ic_brightness,
                                label = "넘김",
                                testTag = "tool_turn",
                                onClick = {},
                            ),
                        ),
                    )
                }
            }
        }
        val brightness = composeRule.onNodeWithTag("tool_brightness").getUnclippedBoundsInRoot()
        val turn = composeRule.onNodeWithTag("tool_turn").getUnclippedBoundsInRoot()
        val bookmarks = composeRule.onNodeWithTag("tool_bookmarks").getUnclippedBoundsInRoot()
        val crop = composeRule.onNodeWithTag("tool_crop").getUnclippedBoundsInRoot()
        assertTrue(brightness.left < turn.left)
        assertTrue(turn.left < bookmarks.left)
        assertTrue(bookmarks.left < crop.left)
    }

    @Test
    fun brightnessSwitchRowTogglesWithSwitchRole() {
        var follow by mutableStateOf(true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderBrightnessSheet(
                    followSystem = follow,
                    percent = 50,
                    onFollowSystemChange = { follow = it },
                    onPercentChange = {},
                )
            }
        }
        val switchRow = composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG)
        switchRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        switchRow.assertIsOn()
        composeRule.onNodeWithText("시스템 밝기 따르기").performClick()
        composeRule.runOnIdle { assertTrue(!follow) }
        switchRow.assertIsOff()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SLIDER_TAG).assertIsDisplayed()
    }

    private fun setReader() {
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
                    overlayVisible = true,
                )
            }
        }
    }
}
