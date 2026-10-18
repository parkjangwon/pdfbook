package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.DarkInkButton
import org.parkjw.apps.pdfbook.ui.theme.DarkPrimary
import org.parkjw.apps.pdfbook.ui.theme.DarkSurfaceContainer
import org.parkjw.apps.pdfbook.ui.theme.LightInkButton
import org.parkjw.apps.pdfbook.ui.theme.LightPrimary
import org.parkjw.apps.pdfbook.ui.theme.LightSurfaceContainerLow
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class TapZoneWidthSheetThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    @Test
    fun tw1SettingsSheetFollowsLightTheme() {
        setSettings(ThemeMode.Light)
        assertSheet(container = LightSurfaceContainerLow, done = LightInkButton, accent = LightPrimary)
    }

    @Test
    fun tw2SettingsSheetFollowsDarkTheme() {
        setSettings(ThemeMode.Dark)
        assertSheet(container = DarkSurfaceContainer, done = DarkInkButton, accent = DarkPrimary)
    }

    @Test
    fun previewZonesFollowSliderAndDefaultDirection() {
        var settings by mutableStateOf(
            AppSettings(tapZoneSidePercent = DEFAULT_TAP_ZONE_SIDE_PERCENT, defaultReadingDirection = ReadingDirection.RTL),
        )
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.3.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowTapWidthSheet = true,
                )
            }
        }
        val page = composeRule.onNodeWithTag(TAP_ZONE_WIDTH_PREVIEW_TAG, useUnmergedTree = true).getBoundsInRoot()
        assertEquals(120f, (page.right - page.left).value, 0.5f)
        assertEquals(168f, (page.bottom - page.top).value, 0.5f)
        val next = composeRule.onNodeWithTag(tapZonePreviewTag(TapZone.Next), useUnmergedTree = true).getBoundsInRoot()
        val previous = composeRule.onNodeWithTag(tapZonePreviewTag(TapZone.Previous), useUnmergedTree = true).getBoundsInRoot()
        assertTrue(next.left < previous.left)
        assertEquals(40f, (next.right - next.left).value, 1f)
        composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(45f) }
        composeRule.waitForIdle()
        val wider = composeRule.onNodeWithTag(tapZonePreviewTag(TapZone.Next), useUnmergedTree = true).getBoundsInRoot()
        assertEquals(54f, (wider.right - wider.left).value, 1f)
        settings = settings.copy(defaultReadingDirection = ReadingDirection.LTR)
        composeRule.waitForIdle()
        val ltrNext = composeRule.onNodeWithTag(tapZonePreviewTag(TapZone.Next), useUnmergedTree = true).getBoundsInRoot()
        val ltrPrevious = composeRule.onNodeWithTag(tapZonePreviewTag(TapZone.Previous), useUnmergedTree = true).getBoundsInRoot()
        assertTrue(ltrNext.left > ltrPrevious.left)
    }

    @Test
    fun readerSheetHasNoPreview() {
        setReader(ThemeMode.Light, percent = DEFAULT_TAP_ZONE_SIDE_PERCENT)
        composeRule.onNodeWithText("폭 조절").performClick()
        composeRule.onAllNodesWithTag(TAP_ZONE_WIDTH_PREVIEW_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun tw3ReaderSheetKeepsReaderColorsInLightTheme() {
        setReader(ThemeMode.Light, percent = DEFAULT_TAP_ZONE_SIDE_PERCENT)
        composeRule.onNodeWithText("폭 조절").performClick()
        val pixels = sheetPixels()
        assertClose(ReaderOverlay, pixels[4, 30])
        val done = composeRule.onNodeWithText("완료").getBoundsInRoot()
        val sheet = composeRule.onNodeWithTag(TAP_ZONE_WIDTH_SHEET_TAG).getBoundsInRoot()
        assertClose(
            ReaderOnOverlay,
            pixels[(done.left - sheet.left).value.toInt() + 6, ((done.top + done.bottom) / 2 - sheet.top).value.toInt()],
        )
    }

    @Test
    fun tw4WidthChangedInSettingsShowsInReaderSheet() {
        var settings by mutableStateOf(AppSettings(tapZoneSidePercent = DEFAULT_TAP_ZONE_SIDE_PERCENT))
        var inReader by mutableStateOf(false)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                if (inReader) {
                    ReaderHost(percent = settings.tapZoneSidePercent)
                } else {
                    SettingsScreen(
                        settings = settings,
                        onSettingsChange = { settings = it(settings) },
                        versionName = "0.3.0",
                        initialSubScreen = SettingsSubScreen.Reading,
                        initiallyShowTapWidthSheet = true,
                    )
                }
            }
        }
        composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        composeRule.onNodeWithText("완료").performClick()
        composeRule.waitForIdle()
        assertEquals(40, settings.tapZoneSidePercent)
        inReader = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("폭 조절").performClick()
        composeRule.onNodeWithText("40% · 기본 33%").assertIsDisplayed()
    }

    @Test
    fun appColorsUseOrangeOnlyOnSlider() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                Box(Modifier.width(360.dp).testTag("host")) {
                    TapZoneWidthSheet(
                        percent = 40,
                        onPercentChange = {},
                        onDone = {},
                        colors = TapZoneWidthSheetDefaults.appColors(),
                    )
                }
            }
        }
        val pixels = composeRule.onNodeWithTag("host").captureToImage().toPixelMap()
        val bounds = composeRule.onNode(slider).getBoundsInRoot()
        var orange = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                if (distance(pixels[x, y], LightPrimary) < 0.08f) {
                    val inside = x >= bounds.left.value && x <= bounds.right.value &&
                        y >= bounds.top.value && y <= bounds.bottom.value
                    assertTrue("orange outside slider at $x,$y", inside)
                    orange++
                }
            }
        }
        assertTrue(orange > 0)
    }

    private fun assertSheet(container: Color, done: Color, accent: Color) {
        val pixels = sheetPixels()
        assertClose(container, pixels[4, 30])
        val sheet = composeRule.onNodeWithTag(TAP_ZONE_WIDTH_SHEET_TAG).getBoundsInRoot()
        val doneBounds = composeRule.onNodeWithText("완료").getBoundsInRoot()
        assertClose(
            done,
            pixels[(doneBounds.left - sheet.left).value.toInt() + 6, ((doneBounds.top + doneBounds.bottom) / 2 - sheet.top).value.toInt()],
        )
        var accentPixels = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                if (distance(pixels[x, y], accent) < 0.08f) accentPixels++
            }
        }
        assertTrue(accentPixels > 0)
    }

    private fun sheetPixels(): PixelMap {
        composeRule.waitForIdle()
        return composeRule.onNodeWithTag(TAP_ZONE_WIDTH_SHEET_TAG).captureToImage().toPixelMap()
    }

    private fun setSettings(theme: ThemeMode) {
        composeRule.setContent {
            PdfBookTheme(themeMode = theme) {
                SettingsScreen(
                    settings = AppSettings(themeMode = theme, tapZoneSidePercent = 40),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowTapWidthSheet = true,
                )
            }
        }
    }

    private fun setReader(theme: ThemeMode, percent: Int) {
        composeRule.setContent {
            PdfBookTheme(themeMode = theme) {
                ReaderHost(percent = percent)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ReaderHost(percent: Int) {
        val pageCount = 5
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
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
            showTapZoneGuide = true,
            tapZoneSidePercent = percent,
            onTapZoneSidePercentChange = {},
        )
    }

    private fun assertClose(expected: Color, actual: Color) {
        assertTrue("expected $expected got $actual", distance(expected, actual) < 0.02f)
    }

    private fun distance(a: Color, b: Color): Float =
        maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue))
}
