package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A two-page turn shows the back face of the peeled sheet. It must turn with the sheet, not
 * stay mirrored across the fold: at the end of the turn its outer edge keeps the bitmap's
 * left side and its spine edge the right, so the last frame equals the still spread.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w800dp-h400dp-mdpi")
class PageCurlFlapBackTurnTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theTurnedBackFaceKeepsItsLeftAndRightSides() {
        val paper = solidBitmap(0xFFF7F2E8.toInt())
        val flapBack = splitBitmap(0xFFD9534F.toInt(), 0xFF2E7D32.toInt(), split = 0.6f)
        composeRule.setContent {
            PageCurlOverlay(
                front = paper,
                back = paper,
                progress = { 0.98f },
                side = CurlSide.Right,
                modifier = Modifier.fillMaxSize(),
                flapBack = flapBack,
                spreadFlap = true,
            )
        }

        // 800x400 root: the 300x500 page fits at 240x400, leaving the left page visible at
        // x 40..280. At p=0.98 the turned flap covers it.
        val pixels = capture()
        val outer = pixels[70, 340]
        val spine = pixels[250, 340]
        assertTrue("outer edge pixel was $outer", isRed(outer))
        assertTrue("spine edge pixel was $spine", isGreen(spine))
    }

    private fun capture(): PixelMap {
        composeRule.waitForIdle()
        return composeRule.onRoot().captureToImage().toPixelMap()
    }

    private fun isRed(pixel: Color): Boolean =
        pixel.red > 0.6f && pixel.green < 0.5f && pixel.blue < 0.5f

    private fun isGreen(pixel: Color): Boolean =
        pixel.green > 0.3f && pixel.red < 0.3f && pixel.blue < 0.3f

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
