package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The two-page curl needs a real back face on the flap. One test keeps every capture in a
 * single composition: in this Robolectric setup a second capture-bearing test method in the
 * same class renders a blank frame.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageCurlFlapBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val paper = solidBitmap(0xFFF7F2E8.toInt())
    private val red = solidBitmap(0xFFD9534F.toInt())
    private val green = solidBitmap(0xFF2E7D32.toInt())
    private val black = solidBitmap(0xFF000000.toInt())
    private val white = solidBitmap(0xFFFFFFFF.toInt())
    private val splitRedGreen = splitBitmap(0xFFD9534F.toInt(), 0xFF2E7D32.toInt(), split = 0.6f)

    @Test
    fun flapBackBitmapDrivesTheFlap() {
        var front by mutableStateOf(red)
        var back by mutableStateOf(paper)
        var flapBack by mutableStateOf<Bitmap?>(green)
        var flapCrop by mutableStateOf<Rect?>(null)
        var filter by mutableStateOf<ColorFilter?>(null)
        composeRule.setContent {
            PageCurlOverlay(
                front = front,
                back = back,
                progress = { 0.5f },
                side = CurlSide.Right,
                modifier = Modifier.fillMaxSize(),
                colorFilter = filter,
                flapBack = flapBack,
                flapContentCrop = flapCrop,
            )
        }

        // 1. A supplied back face replaces the paper back + 8% mirrored front.
        val supplied = capture()
        assertTrue("flap pixel was ${supplied[20, 600]}", isGreen(supplied[20, 600]))
        assertTrue("green pixels: ${greenCount(supplied)}", greenCount(supplied) > 100)

        // 2. Null keeps the v0.7.0 paper back.
        composeRule.runOnIdle { flapBack = null }
        assertEquals("paper back must not be green", 0, greenCount(capture()))

        // 3. The flap crop is its own hook: cropping away the green edge hides it.
        composeRule.runOnIdle {
            flapBack = splitRedGreen
            flapCrop = Rect(0f, 0f, 0.4f, 1f)
        }
        assertEquals("cropped flap back must not show green", 0, greenCount(capture()))

        // 4. The caller's color filter reaches the flap bitmap too.
        // shortcut: Robolectric's native canvas ignores SrcIn on drawBitmap, so this uses
        // SrcOver; it still proves the filter is applied to the flap paint.
        composeRule.runOnIdle {
            front = black
            back = black
            flapBack = white
            flapCrop = null
            filter = ColorFilter.tint(Color.Red, BlendMode.SrcOver)
        }
        val tinted = capture()
        assertTrue("flap pixel was ${tinted[20, 600]}", tinted[20, 600].red > 0.8f && tinted[20, 600].green < 0.2f)
    }

    private fun capture(): PixelMap {
        composeRule.waitForIdle()
        return composeRule.onRoot().captureToImage().toPixelMap()
    }

    private fun isGreen(pixel: Color): Boolean =
        pixel.green > 0.3f && pixel.red < 0.3f && pixel.blue < 0.3f

    private fun greenCount(pixels: PixelMap): Int {
        var count = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (isGreen(pixels[x, y])) count++
            }
        }
        return count
    }

    private fun solidBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        return bitmap
    }

    private fun splitBitmap(leftColor: Int, rightColor: Int, split: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(leftColor)
        canvas.drawRect(
            PAGE_WIDTH * split,
            0f,
            PAGE_WIDTH.toFloat(),
            PAGE_HEIGHT.toFloat(),
            Paint().apply { color = rightColor },
        )
        return bitmap
    }

    private companion object {
        const val PAGE_WIDTH = 300
        const val PAGE_HEIGHT = 500
    }
}
