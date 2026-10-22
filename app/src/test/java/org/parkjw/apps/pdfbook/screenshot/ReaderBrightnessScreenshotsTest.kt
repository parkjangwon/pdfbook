package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderBrightnessScreenshotsTest {
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
    fun brightnessSheetLightFolded() {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(false, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(initialPage = 36, pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "샘플 시리즈 5권",
                    pageCount = pageCount,
                    currentPage = 37,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    followSystemBrightness = false,
                    brightnessPercent = 62,
                    brightnessSheetVisible = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage("reader-brightness-light-folded.png")
    }
}
