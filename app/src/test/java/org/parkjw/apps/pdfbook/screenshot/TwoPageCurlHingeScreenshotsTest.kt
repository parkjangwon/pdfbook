package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
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
 * Folded Book posture captures for #172: the two-page sheet turns on the virtual spread and the
 * overlay cuts it at the hinge. LTR and RTL at p=0.5 and 0.95; the hinge columns must stay
 * ReaderBackground (the fold gap), so no page or flap paper crosses it. Synthetic pages only.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = FOLDABLE_QUALIFIERS)
class TwoPageCurlHingeScreenshotsTest {
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
    fun foldedBookHingeGapTurnsBothWays() {
        val currentLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt())
        val currentRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val targetLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt())
        val targetRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt())

        var progress by mutableStateOf(0.5f)
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
                    hingeGap = HINGE_GAP,
                )
            }
        }
        composeRule.waitForIdle()

        val window = composeRule.onRoot().captureToImage().toPixelMap()
        val spine = window.width / 2
        val gapLeft = spine - HINGE_GAP / 2
        val gapRight = spine + HINGE_GAP / 2

        for (turningSide in listOf(CurlSide.Right, CurlSide.Left)) {
            for (p in listOf(0.5f, 0.95f)) {
                composeRule.runOnIdle {
                    side = turningSide
                    progress = p
                }
                composeRule.waitForIdle()
                val tag = if (turningSide == CurlSide.Right) "ltr" else "rtl"
                val pixels = composeRule.onRoot().captureToImage().toPixelMap()
                for (y in 0 until pixels.height) {
                    for (x in gapLeft until gapRight) {
                        assertEquals(
                            "$tag p=$p: the hinge gap is painted at ($x, $y)",
                            ReaderBackground.value,
                            pixels[x, y].value,
                        )
                    }
                }
                composeRule.onRoot()
                    .captureRoboImage("reader-page-curl-hinge-$tag-p${(p * 100).roundToInt()}.png")
            }
        }
    }

    private companion object {
        const val HINGE_GAP = 16
    }
}
