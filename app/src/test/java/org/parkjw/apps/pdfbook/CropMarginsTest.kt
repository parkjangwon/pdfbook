package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class CropMarginsTest {
    @Test
    fun findsSymmetricMargins() {
        val margins = detectCropMargins(page(left = 20, top = 30, right = 40, bottom = 10), paddingFraction = 0f)
        assertClose(0.10f, margins.left)
        assertClose(0.10f, margins.top)
        assertClose(0.20f, margins.right)
        assertClose(0.10f / 3f, margins.bottom)
    }

    @Test
    fun findsUnevenMargins() {
        val margins = detectCropMargins(page(left = 10, top = 5, right = 50, bottom = 60), paddingFraction = 0f)
        assertClose(0.05f, margins.left)
        assertClose(0.05f / 3f, margins.top)
        assertClose(0.25f, margins.right)
        assertClose(0.20f, margins.bottom)
    }

    @Test
    fun blankPageIsNotCropped() {
        assertEquals(CropMargins(), detectCropMargins(blankPage()))
    }

    @Test
    fun fullBleedPageIsNotCropped() {
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        assertEquals(CropMargins(), detectCropMargins(bitmap))
    }

    @Test
    fun lowContentPageIsNotCropped() {
        val bitmap = blankPage()
        bitmap.setPixel(100, 150, Color.BLACK)
        bitmap.setPixel(101, 150, Color.BLACK)
        assertEquals(CropMargins(), detectCropMargins(bitmap))
    }

    @Test
    fun neverCropsMoreThanTheCapPerSide() {
        val bitmap = blankPage()
        for (y in 0 until 300) {
            for (x in 0 until 11) bitmap.setPixel(x, y, Color.BLACK)
        }
        val margins = detectCropMargins(bitmap, paddingFraction = 0f)
        assertEquals(0f, margins.left)
        assertEquals(CROP_MAX_SIDE_FRACTION, margins.right)
    }

    @Test
    fun keepsSafetyPaddingInsideTheContentEdge() {
        val margins = detectCropMargins(page(left = 20, top = 20, right = 20, bottom = 20))
        assertTrue(margins.left < 0.10f)
        assertTrue(margins.top < 0.10f)
    }

    @Test
    fun nearWhitePaperCountsAsBackground() {
        val margins = detectCropMargins(page(background = 0xFFF7F7F4.toInt()), paddingFraction = 0f)
        assertClose(0.10f, margins.left)
    }

    @Test
    fun twoPageRuleSharesVerticalCropAndKeepsTheSpine() {
        val leftPage = CropMargins(left = 0.10f, top = 0.10f, right = 0.05f, bottom = 0.05f)
        val rightPage = CropMargins(left = 0.02f, top = 0.20f, right = 0.10f, bottom = 0.10f)
        val (left, right) = alignedTwoPageMargins(leftPage, rightPage)
        assertClose(0.10f, left.left)
        assertEquals(0f, left.right)
        assertClose(0.10f, left.top)
        assertClose(0.05f, left.bottom)
        assertEquals(0f, right.left)
        assertClose(0.10f, right.right)
        assertClose(0.10f, right.top)
        assertClose(0.05f, right.bottom)
    }

    @Test
    fun creamPaperIsMeasuredAgainstItsOwnBackground() {
        val margins = detectCropMargins(
            page(background = 0xFFF7F2E8.toInt()),
            paddingFraction = 0f,
        )
        assertClose(0.10f, margins.left)
        assertClose(0.10f, margins.top)
        assertClose(0.20f, margins.right)
        assertClose(0.10f / 3f, margins.bottom)
    }

    @Test
    fun oneStrayPixelDoesNotStretchTheBoundingBox() {
        val bitmap = page(left = 20, top = 30, right = 40, bottom = 10)
        bitmap.setPixel(2, 2, Color.BLACK)
        val margins = detectCropMargins(bitmap, paddingFraction = 0f)
        assertClose(0.10f, margins.left)
        assertClose(0.10f, margins.top)
    }

    @Test
    fun aThinFaintLinePartlyAcrossTheMarginIsKept() {
        val bitmap = page(left = 20, top = 30, right = 40, bottom = 10)
        for (x in 50 until 150) bitmap.setPixel(x, 5, 0xFF999999.toInt())
        val margins = detectCropMargins(bitmap, paddingFraction = 0f)
        assertTrue("line should be treated as content (top=${margins.top})", margins.top < 0.02f)
        assertClose(0.10f, margins.left)
    }

    @Test
    fun darkScansStayUncropped() {
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF202020.toInt())
        for (y in 30 until 270) {
            for (x in 20 until 160) bitmap.setPixel(x, y, 0xFF606060.toInt())
        }
        assertEquals(CropMargins(), detectCropMargins(bitmap))
    }

    @Test
    fun rendererDetectsCropOffTheMainThreadAndCachesIt() {
        val source = MarginSource()
        val renderer = PageRenderer(source)
        try {
            renderer.setAutoCrop(true)
            runBlocking { renderer.render(1, 100, 150) }
            assertFalse("crop detection ran on the main thread", source.drewOnMain)
            assertNotNull(renderer.cropMargins(1))
        } finally {
            renderer.close()
        }
    }

    private fun blankPage(): Bitmap =
        Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }

    private fun page(
        left: Int = 20,
        top: Int = 30,
        right: Int = 40,
        bottom: Int = 10,
        background: Int = Color.WHITE,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(background)
        for (y in top until 300 - bottom) {
            for (x in left until 200 - right) bitmap.setPixel(x, y, Color.BLACK)
        }
        return bitmap
    }

    private fun assertClose(expected: Float, actual: Float, delta: Float = 0.002f) {
        assertEquals(expected, actual, delta)
    }

    private class MarginSource : PageSource {
        @Volatile var drewOnMain = false
        override val pageCount: Int = 1

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            drewOnMain = Looper.getMainLooper().isCurrentThread
            val bitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            for (y in 10 until maxHeight - 10) {
                for (x in 10 until maxWidth - 10) bitmap.setPixel(x, y, Color.BLACK)
            }
            return bitmap
        }

        override fun close() {}
    }
}
