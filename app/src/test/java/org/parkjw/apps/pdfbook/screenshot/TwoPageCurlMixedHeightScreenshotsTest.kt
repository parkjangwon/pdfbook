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
 * End-frame evidence for the v0.7.2 flat-fold hotfix: pages that differ in height by 2-4px, like
 * [6,7] -> [8,9] in 장원's book, at p=0.5, 0.95 and 1, in LTR and RTL. At p=1 each page must sit
 * exactly on its settled rect, with no flap-color hem or clipped rows. Synthetic pages only; no
 * real book, screenshot or private file.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class TwoPageCurlMixedHeightScreenshotsTest {
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
    fun mixedHeightPairLandsEachPageOnItsOwnRect() {
        // Heights scaled from [6,7] -> [8,9] of 장원's book (1337 / 1333 / 1330 px): the turning
        // page, the revealed page and the back face are each a few pixels apart.
        val currentLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt(), height = 851)
        val currentRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt(), height = 853)
        val targetLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt(), height = 846)
        val targetRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt(), height = 849)

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
                )
            }
        }
        composeRule.waitForIdle()

        // p=1 is the settled target spread: the frame must equal it exactly.
        for (p in listOf(0.5f, 0.95f, 1f)) {
            for (turningSide in listOf(CurlSide.Right, CurlSide.Left)) {
                composeRule.runOnIdle {
                    side = turningSide
                    progress = p
                }
                composeRule.waitForIdle()
                val tag = if (turningSide == CurlSide.Right) "ltr" else "rtl"
                composeRule.onRoot()
                    .captureRoboImage("reader-page-curl-mixed-$tag-p${(p * 100).roundToInt()}.png")
            }
        }
    }
}
