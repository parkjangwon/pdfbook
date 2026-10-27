package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderAutoCropScreenshotsTest {
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
    fun readerOverlayWithAutoCropOn() {
        composeRule.setContent { ReaderContent() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("reader-auto-crop-light-folded.png")
    }

    @Composable
    private fun ReaderContent() {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
            ReaderScreen(
                renderer = SolidPageBitmaps(
                    pageCount = pageCount,
                    pageColor = 0xFFF7F2E8.toInt(),
                    accentColor = 0xFF3D5A80.toInt(),
                ),
                pagerState = rememberPagerState(initialPage = 36, pageCount = { pageSpreads.size }),
                spreads = pageSpreads,
                title = "샘플 시리즈 1권",
                pageCount = pageCount,
                currentPage = 37,
                twoPage = false,
                onBack = {},
                onDirectionChange = {},
                direction = ReadingDirection.RTL,
                overlayVisible = true,
                showTapZoneGuide = false,
                autoCrop = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
