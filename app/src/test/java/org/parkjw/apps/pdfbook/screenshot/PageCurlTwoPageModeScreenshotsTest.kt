package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Two-page curl captures in the inverted and sepia page modes at p=0.75 (#145 follow-up).
 * Pages are generated synthetic colour pages; no real book, screenshot or private file is
 * used. Both captures live in one method: in this Robolectric setup a second
 * capture-bearing method renders blank.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageCurlTwoPageModeScreenshotsTest {
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
    fun twoPageCurlModesAtThreeQuarters() {
        val currentLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt())
        val currentRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val targetLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt())
        val targetRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt())

        var mode by mutableStateOf(DisplayMode.Invert)

        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                TwoPageCurlPreview(
                    side = CurlSide.Right,
                    progress = 0.75f,
                    currentLeft = currentLeft,
                    currentRight = currentRight,
                    targetLeft = targetLeft,
                    targetRight = targetRight,
                    mode = mode,
                )
            }
        }
        composeRule.waitForIdle()

        for (m in listOf(DisplayMode.Invert, DisplayMode.Sepia)) {
            composeRule.runOnIdle { mode = m }
            composeRule.waitForIdle()
            val name = when (m) {
                DisplayMode.Invert -> "inverted"
                DisplayMode.Sepia -> "sepia"
                DisplayMode.Default -> "default"
            }
            composeRule.onRoot().captureRoboImage("reader-page-curl-two-$name-p75.png")
        }
    }
}
