package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.AUTO_TURN_DEFAULT_SECONDS
import org.parkjw.apps.pdfbook.AUTO_TURN_START_TAG
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.autoTurnIntervalTag
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class AutoTurnScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun onlyWhenRecordingOrVerifying() {
        assumeTrue(
            System.getProperty("roborazzi.test.record") == "true" ||
                System.getProperty("roborazzi.test.verify") == "true" ||
                System.getProperty("roborazzi.test.compare") == "true",
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun intervalSheetLightFolded() {
        setReader(dark = false)
        tap("reader_auto_turn")
        composeRule.mainClock.advanceTimeBy(300)
        capture("reader-auto-turn-sheet-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun intervalSheetLightFoldedEn() {
        setReader(dark = false)
        tap("reader_auto_turn")
        composeRule.mainClock.advanceTimeBy(300)
        capture("reader-auto-turn-sheet-light-folded-en.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun runningStatusLightFolded() {
        setReader(dark = false)
        tap("reader_auto_turn")
        tap(autoTurnIntervalTag(10))
        tap(AUTO_TURN_START_TAG)
        composeRule.mainClock.advanceTimeBy(300)
        capture("reader-auto-turn-running-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun runningStatusDarkFolded() {
        setReader(dark = true)
        tap("reader_auto_turn")
        tap(AUTO_TURN_START_TAG)
        composeRule.mainClock.advanceTimeBy(300)
        capture("reader-auto-turn-running-dark-folded.png")
    }

    private fun tap(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun setReader(dark: Boolean) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(initialPage = 30, pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "샘플 시리즈 5권",
                    pageCount = pageCount,
                    currentPage = pageSpreads[30].first(),
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    autoTurnSeconds = AUTO_TURN_DEFAULT_SECONDS,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
    }

    private fun capture(file: String) {
        composeRule.onRoot().captureRoboImage(file)
    }
}
