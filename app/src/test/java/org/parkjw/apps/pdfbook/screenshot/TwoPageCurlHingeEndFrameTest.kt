package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #172 acceptance criteria 1 and 5, ungated: a hinge turn starts and ends on the settled spreads.
 *
 * No Roborazzi gate: this runs in CI. For a 24px hinge (about 8dp at xxhdpi) and an odd 17px one,
 * both turn directions, p=0 must equal the current spread drawn by the pager alone and p=1 the
 * target spread, within 1px per page edge. The hinge columns must be ReaderBackground for the
 * whole turn, so no paper and no flap is ever painted over the fold gap. Synthetic pages only.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = FOLDABLE_QUALIFIERS)
class TwoPageCurlHingeEndFrameTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hingeTurnStartsAndEndsOnTheSettledSpreads() {
        val currentLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt())
        val currentRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val targetLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt())
        val targetRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt())

        var gap by mutableStateOf(HINGE_GAP)
        var side by mutableStateOf(CurlSide.Right)
        var progress by mutableStateOf(0f)
        var showOverlay by mutableStateOf(false)
        var pagerPage by mutableStateOf(0)

        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                TwoPageCurlPreview(
                    side = side,
                    progress = progress,
                    currentLeft = currentLeft,
                    currentRight = currentRight,
                    targetLeft = targetLeft,
                    targetRight = targetRight,
                    showOverlay = showOverlay,
                    hingeGap = gap,
                    pagerPage = pagerPage,
                )
            }
        }
        composeRule.waitForIdle()

        fun staticSpread(page: Int): Array<IntArray> {
            composeRule.runOnIdle {
                showOverlay = false
                pagerPage = page
            }
            composeRule.waitForIdle()
            val pixels = capture()
            return arrayOf(
                pageBounds(pixels, 0, pixels.width / 2),
                pageBounds(pixels, pixels.width / 2, pixels.width),
            )
        }

        for (hingeGap in listOf(HINGE_GAP, ODD_HINGE_GAP)) {
            composeRule.runOnIdle { gap = hingeGap }
            composeRule.waitForIdle()
            // The settled spreads for this gap: the pager alone on the current and target page.
            val current = staticSpread(page = 0)
            val target = staticSpread(page = 1)

            for (turningSide in listOf(CurlSide.Right, CurlSide.Left)) {
                val tag = "${if (turningSide == CurlSide.Right) "ltr" else "rtl"} gap=$hingeGap"
                composeRule.runOnIdle {
                    gap = hingeGap
                    side = turningSide
                    showOverlay = true
                    pagerPage = 1
                    progress = 0f
                }
                composeRule.waitForIdle()
                val first = capture()
                assertSpread(tag, "p=0", current, first)
                assertGapBackground(tag, hingeGap, first)

                for (p in listOf(0.25f, 0.5f, 0.75f, 0.95f)) {
                    composeRule.runOnIdle { progress = p }
                    composeRule.waitForIdle()
                    assertGapBackground("$tag p=$p", hingeGap, capture())
                }

                composeRule.runOnIdle { progress = 1f }
                composeRule.waitForIdle()
                val last = capture()
                assertSpread(tag, "p=1", target, last)
                assertGapBackground("$tag p=1", hingeGap, last)
            }
        }
    }

    private fun capture(): PixelMap = composeRule.onRoot().captureToImage().toPixelMap()

    /** Each half's page must sit on the settled spread's page within 1px per edge. */
    private fun assertSpread(tag: String, frame: String, settled: Array<IntArray>, pixels: PixelMap) {
        val edges = listOf("left", "top", "right", "bottom")
        for (half in 0..1) {
            val from = if (half == 0) 0 else pixels.width / 2
            val to = if (half == 0) pixels.width / 2 else pixels.width
            val bounds = pageBounds(pixels, from, to)
            for (edge in edges.indices) {
                assertTrue(
                    "$tag $frame half $half ${edges[edge]} is ${bounds[edge]}, " +
                        "settled ${settled[half][edge]}",
                    abs(bounds[edge] - settled[half][edge]) <= 1,
                )
            }
        }
    }

    private fun assertGapBackground(tag: String, gap: Int, pixels: PixelMap) {
        val gapLeft = pixels.width / 2 - gap / 2
        for (y in 0 until pixels.height) {
            for (x in gapLeft until gapLeft + gap) {
                assertEquals(
                    "$tag: the hinge gap is painted at ($x, $y)",
                    ReaderBackground.value,
                    pixels[x, y].value,
                )
            }
        }
    }

    /** Non-background bounds of the page in the columns [left, right): left, top, right, bottom. */
    private fun pageBounds(pixels: PixelMap, left: Int, right: Int): IntArray {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        for (y in 0 until pixels.height) {
            for (x in left until right) {
                if (pixels[x, y] != ReaderBackground) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return intArrayOf(minX, minY, maxX, maxY)
    }

    private companion object {
        const val HINGE_GAP = 24
        const val ODD_HINGE_GAP = 17
    }
}
