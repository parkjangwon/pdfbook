package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The lone-page sheet is the page, not the pane (#173). Landscape/letterbox margins stay black,
 * and at progress 1 the frame is the target page at the settled rect. All captures live in one
 * test method: in this Robolectric setup a second capture-bearing method renders blank.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class SingleSheetCurlOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loneSheetStaysInsideThePageRectAndEndsOnTheTarget() {
        val narrowFront = solid(NARROW_PAGE_WIDTH, PAGE_HEIGHT, 0xFF4A90D9.toInt())
        val front = solid(PAGE_WIDTH, PAGE_HEIGHT, 0xFF4A90D9.toInt())
        val target = accented(PAGE_WIDTH, PAGE_HEIGHT, 0xFF2E7D32.toInt())
        val narrowTarget = solid(NARROW_PAGE_WIDTH, PAGE_HEIGHT, 0xFF2E7D32.toInt())

        var narrow by mutableStateOf(true)
        var progress by mutableStateOf(0.5f)
        var showCurl by mutableStateOf(true)
        var pageRect: Rect? = null

        composeRule.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val pane = IntRect(0, 0, constraints.maxWidth, constraints.maxHeight)
                val bitmapWidth = if (narrow) NARROW_PAGE_WIDTH else PAGE_WIDTH
                val rect = pageDrawRect(bitmapWidth, PAGE_HEIGHT, CropMargins(), pane, SlotAlignment.Center, null)
                pageRect = rect
                if (showCurl) {
                    PageCurlOverlay(
                        front = if (narrow) narrowFront else front,
                        back = if (narrow) narrowTarget else target,
                        progress = { progress },
                        side = CurlSide.Right,
                        modifier = Modifier.fillMaxSize(),
                        frontRect = rect,
                        underlay = listOf(CurlUnderlay(if (narrow) narrowTarget else target, null, rect)),
                    )
                } else {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawRect(ReaderBackground)
                        drawSettled(target, rect)
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // 1. A narrow page in a wide pane: the flap must not reach the black margin beside it.
        val narrowRect = requireNotNull(pageRect)
        assertEquals("narrow page left", 80f, narrowRect.left, 0f)
        assertEquals("narrow page right", 320f, narrowRect.right, 0f)
        var outsideDrawn = 0
        var insideDrawn = 0
        val atHalf = composeRule.onRoot().captureToImage().toPixelMap()
        for (y in 0 until atHalf.height) {
            for (x in 0 until atHalf.width) {
                val same = sameAsBackground(atHalf[x, y])
                when {
                    x < narrowRect.left - 1f || x > narrowRect.right + 1f -> if (!same) outsideDrawn++
                    x > narrowRect.left + 1f && x < narrowRect.right - 1f -> if (!same) insideDrawn++
                }
            }
        }
        assertEquals("flap or fold pixels outside the page rect", 0, outsideDrawn)
        assertTrue("the curl itself must draw inside the page rect", insideDrawn > 1000)

        // 2. Progress 1 is the target page at the settled rect, at the same measured position.
        composeRule.runOnIdle {
            narrow = false
            progress = 1f
        }
        composeRule.waitForIdle()
        val last = composeRule.onRoot().captureToImage().toPixelMap()
        composeRule.runOnIdle { showCurl = false }
        composeRule.waitForIdle()
        val settled = composeRule.onRoot().captureToImage().toPixelMap()

        val inLast = colourBounds(last) { it.green > 0.25f && it.red < 0.6f && it.blue < 0.5f }
        val inSettled = colourBounds(settled) { it.green > 0.25f && it.red < 0.6f && it.blue < 0.5f }
        assertTrue("the target must show in the last curl frame", inLast != null)
        assertTrue("the target must show in the settled page", inSettled != null)
        assertEquals("target left", inSettled!!.left, inLast!!.left, 2f)
        assertEquals("target top", inSettled.top, inLast.top, 2f)
        assertEquals("target right", inSettled.right, inLast.right, 2f)
        assertEquals("target bottom", inSettled.bottom, inLast.bottom, 2f)
        // The front page is fully peeled away: none of its blue accent is left.
        assertTrue(
            "no front page at progress 1",
            colourBounds(last) { it.blue > 0.6f && it.red < 0.5f } == null,
        )
    }

    private fun sameAsBackground(pixel: Color): Boolean =
        kotlin.math.abs(pixel.red - ReaderBackground.red) < 0.01f &&
            kotlin.math.abs(pixel.green - ReaderBackground.green) < 0.01f &&
            kotlin.math.abs(pixel.blue - ReaderBackground.blue) < 0.01f

    private fun DrawScope.drawSettled(bitmap: Bitmap, dest: Rect) {
        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset(dest.left.roundToInt(), dest.top.roundToInt()),
            dstSize = IntSize(dest.width.roundToInt(), dest.height.roundToInt()),
        )
    }

    /** Bounds of the pixels matching [test], or null when none do. */
    private fun colourBounds(pixels: PixelMap, test: (Color) -> Boolean): Rect? {
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = -1
        var bottom = -1
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (test(pixels[x, y])) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < 0) return null
        return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    private fun solid(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    /** Cream page with a green block inset from every edge, like a text page. */
    private fun accented(width: Int, height: Int, accent: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFFF7F2E8.toInt())
            val canvas = android.graphics.Canvas(this)
            canvas.drawRect(
                width * 0.1f,
                height * 0.08f,
                width * 0.9f,
                height * 0.92f,
                android.graphics.Paint().apply { color = accent },
            )
        }

    private companion object {
        const val PAGE_WIDTH = 300
        const val PAGE_HEIGHT = 500
        const val NARROW_PAGE_WIDTH = 150
    }
}
