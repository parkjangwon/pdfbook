package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.roundToInt
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi captures for the two-page curl (#145 PR2). Pages are generated synthetic
 * colour pages; no real book, screenshot or private file is used. All captures live in one
 * test method: in this Robolectric setup a second capture-bearing method renders blank.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageCurlTwoPageScreenshotsTest {
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
    fun twoPageCurlLtrAndRtl() {
        val currentLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt())
        val currentRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val targetLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt())
        val targetRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt())

        var progress by mutableStateOf(0.25f)
        var side by mutableStateOf(CurlSide.Right)

        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                TwoPageCurlPreview(
                    side = side,
                    progress = progress,
                    currentLeft = currentLeft,
                    currentRight = currentRight,
                    targetLeft = targetLeft,
                    targetRight = targetRight,
                )
            }
        }
        composeRule.waitForIdle()

        // LTR forward: the right sheet turns; the flap back face is the target's left page.
        for (p in listOf(0.25f, 0.5f, 0.75f, 0.95f)) {
            composeRule.runOnIdle {
                side = CurlSide.Right
                progress = p
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-two-ltr-p${percent(p)}.png")
        }

        // RTL forward: mirrored, the left sheet turns; the flap back face is the target's right page.
        for (p in listOf(0.25f, 0.5f, 0.75f, 0.95f)) {
            composeRule.runOnIdle {
                side = CurlSide.Left
                progress = p
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-two-rtl-p${percent(p)}.png")
        }
    }

    private fun percent(p: Float): Int = (p * 100).roundToInt()
}
