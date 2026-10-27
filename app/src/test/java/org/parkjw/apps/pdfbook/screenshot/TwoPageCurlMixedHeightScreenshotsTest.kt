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
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 * [6,7] -> [8,9] in 장원's book, at p=0.5, 0.95 and 1, in LTR and RTL. Mid-turn no background may
 * show between the rows the four pages share (the synthetic ink is #1B1B1B, so pure black is
 * unambiguous), and at p=1 each page must sit on its settled rect within 1px per edge. Synthetic
 * pages only; no real book, screenshot or private file.
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
        var showOverlay by mutableStateOf(true)

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
                )
            }
        }
        composeRule.waitForIdle()

        // The shared rows come from the frames themselves: p=0 shows the current spread, the pager
        // alone shows the settled target spread. Their intersection is the rows all four pages
        // share, which is where a filled flap used to leave a black hem across paper.
        composeRule.runOnIdle { showOverlay = false }
        composeRule.waitForIdle()
        val settledPixels = composeRule.onRoot().captureToImage().toPixelMap()
        val settled = arrayOf(
            pageBounds(settledPixels, 0, settledPixels.width / 2),
            pageBounds(settledPixels, settledPixels.width / 2, settledPixels.width),
        )
        composeRule.runOnIdle {
            showOverlay = true
            progress = 0f
        }
        composeRule.waitForIdle()
        val currentPixels = composeRule.onRoot().captureToImage().toPixelMap()
        val currentLeftBounds = pageBounds(currentPixels, 0, currentPixels.width / 2)
        val currentRightBounds = pageBounds(currentPixels, currentPixels.width / 2, currentPixels.width)
        val commonTop = maxOf(
            currentLeftBounds[1],
            currentRightBounds[1],
            settled[0][1],
            settled[1][1],
        )
        val commonBottom = minOf(
            currentLeftBounds[3],
            currentRightBounds[3],
            settled[0][3],
            settled[1][3],
        )
        assertTrue("the four pages must share rows", commonBottom > commonTop)

        for (p in listOf(0.5f, 0.95f)) {
            for (turningSide in listOf(CurlSide.Right, CurlSide.Left)) {
                composeRule.runOnIdle {
                    side = turningSide
                    progress = p
                }
                composeRule.waitForIdle()
                val tag = if (turningSide == CurlSide.Right) "ltr" else "rtl"
                val pixels = composeRule.onRoot().captureToImage().toPixelMap()
                assertEquals(
                    "$tag p=$p: background pixels between the shared page rows",
                    0,
                    backgroundPixels(pixels, commonTop, commonBottom),
                )
                composeRule.onRoot()
                    .captureRoboImage("reader-page-curl-mixed-$tag-p${(p * 100).roundToInt()}.png")
            }
        }

        // p=1 is the settled target spread: the frame must equal it within 1px per edge.
        for (turningSide in listOf(CurlSide.Right, CurlSide.Left)) {
            composeRule.runOnIdle {
                side = turningSide
                progress = 1f
            }
            composeRule.waitForIdle()
            val tag = if (turningSide == CurlSide.Right) "ltr" else "rtl"
            val frame = composeRule.onRoot().captureToImage().toPixelMap()
            composeRule.onRoot().captureRoboImage("reader-page-curl-mixed-$tag-p100.png")
            assertBoundsClose(tag, settled[0], pageBounds(frame, 0, frame.width / 2))
            assertBoundsClose(tag, settled[1], pageBounds(frame, frame.width / 2, frame.width))
        }
    }

    private fun backgroundPixels(pixels: PixelMap, top: Int, bottom: Int): Int {
        var count = 0
        for (y in top until bottom) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y] == ReaderBackground) count++
            }
        }
        return count
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

    private fun assertBoundsClose(tag: String, settled: IntArray, frame: IntArray) {
        val edges = listOf("left", "top", "right", "bottom")
        for (i in edges.indices) {
            assertTrue(
                "$tag p=1 ${edges[i]} edge is ${frame[i]}, settled ${settled[i]}",
                abs(frame[i] - settled[i]) <= 1,
            )
        }
    }
}
