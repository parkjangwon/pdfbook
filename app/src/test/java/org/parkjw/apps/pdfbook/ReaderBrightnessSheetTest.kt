package org.parkjw.apps.pdfbook

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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
class ReaderBrightnessSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var followSystem: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var percent: androidx.compose.runtime.MutableIntState

    @Test
    fun entryOpensSheetWithSystemBrightnessOn() {
        setReader()
        composeRule.onAllNodesWithTag(READER_BRIGHTNESS_SHEET_TAG).assertCountEquals(0)
        composeRule.onNodeWithTag("reader_brightness").performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("밝기").assertIsDisplayed()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG).assertIsOn()
        composeRule.onAllNodesWithTag(READER_BRIGHTNESS_SLIDER_TAG).assertCountEquals(0)
    }

    @Test
    fun turningOffShowsSliderAndSavesLevel() {
        setReader()
        composeRule.onNodeWithTag("reader_brightness").performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG).performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG).assertIsOff()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SLIDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SLIDER_TAG).performSemanticsAction(
            SemanticsActions.SetProgress,
        ) { it(41.2f) }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(false, followSystem.value)
            assertEquals(41, percent.intValue)
        }
        composeRule.onNodeWithText("41%").assertIsDisplayed()
    }

    @Test
    fun turningSystemBrightnessBackOnHidesSlider() {
        setReader()
        composeRule.onNodeWithTag("reader_brightness").performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG).performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SLIDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG).performClick()
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SWITCH_TAG).assertIsOn()
        composeRule.onAllNodesWithTag(READER_BRIGHTNESS_SLIDER_TAG).assertCountEquals(0)
        composeRule.runOnIdle { assertEquals(true, followSystem.value) }
    }

    @Test
    fun sliderDragPreviewsButPersistsOnceOnRelease() {
        var previews = 0
        var lastPreview = -1
        val persisted = mutableListOf<Int>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderBrightnessSheet(
                    followSystem = false,
                    percent = 40,
                    onFollowSystemChange = {},
                    onPercentChange = { persisted += it },
                    onPercentPreview = {
                        previews++
                        lastPreview = it
                    },
                )
            }
        }
        composeRule.onNodeWithTag(READER_BRIGHTNESS_SLIDER_TAG).performTouchInput {
            down(Offset(width * 0.4f, center.y))
            advanceEventTime(16)
            moveTo(Offset(width * 0.55f, center.y))
            advanceEventTime(16)
            moveTo(Offset(width * 0.75f, center.y))
            advanceEventTime(16)
            moveTo(Offset(width * 0.9f, center.y))
            advanceEventTime(16)
            up()
        }
        composeRule.waitForIdle()
        assertTrue("previews=$previews persisted=$persisted", previews >= 2)
        assertEquals("release must persist exactly once", 1, persisted.size)
        assertEquals(lastPreview, persisted.single())
    }

    private fun setReader() {
        followSystem = mutableStateOf(true)
        percent = mutableIntStateOf(50)
        val follow = followSystem
        val level = percent
        val pageCount = 5
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            var followSystemOn by follow
            var brightnessPercent by level
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
                    followSystemBrightness = followSystemOn,
                    brightnessPercent = brightnessPercent,
                    onFollowSystemBrightnessChange = { followSystemOn = it },
                    onBrightnessChange = { brightnessPercent = it },
                )
            }
        }
    }
}
