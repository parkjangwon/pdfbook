package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageCurlOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun colorFilterTintsThePageBitmaps() {
        val pages = SolidPageBitmaps(1, 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        val page = runBlocking { pages.render(1, 360, 640)!! }
        composeRule.setContent {
            PageCurlOverlay(
                front = page,
                back = page,
                progress = { 0f },
                side = CurlSide.Right,
                modifier = Modifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(Color.Red),
            )
        }
        composeRule.waitForIdle()
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val pixel = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue("center pixel was $pixel", pixel.red > 0.8f && pixel.green < 0.2f)
    }

    /**
     * The revealed page arrives on its own page, whose margins can differ from the page the turn
     * starts from. The last curl frame must draw the back page with its own crop, so it matches the
     * settled page instead of jumping when the overlay disappears.
     */
    @Test
    fun finalFrameDrawsTheBackPageWithItsOwnCrop() {
        val front = pageBitmap(accent = 0xFF4A90D9.toInt(), left = 0.20f, right = 0.05f)
        val back = pageBitmap(accent = 0xFFD9534F.toInt(), left = 0.04f, right = 0.20f)
        val frontCrop = CropMargins(left = 0.20f, top = 0.08f, right = 0.05f, bottom = 0.08f)
        val backCrop = CropMargins(left = 0.04f, top = 0.08f, right = 0.20f, bottom = 0.08f)

        var showCurl by mutableStateOf(true)
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showCurl) {
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = { 1f },
                        side = CurlSide.Right,
                        modifier = Modifier.fillMaxSize(),
                        contentCrop = frontCrop.toContentRect(),
                        backContentCrop = backCrop.toContentRect(),
                    )
                } else {
                    PageBitmap(
                        renderer = CachedPage(back, backCrop),
                        page = 1,
                        maxWidth = PAGE_WIDTH,
                        maxHeight = PAGE_HEIGHT,
                        modifier = Modifier.fillMaxSize(),
                        autoCrop = true,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val curlFrame = redBounds(composeRule.onRoot().captureToImage().toPixelMap())
        composeRule.runOnIdle { showCurl = false }
        composeRule.waitForIdle()
        val settled = redBounds(composeRule.onRoot().captureToImage().toPixelMap())

        assertTrue("no red content in the curl frame or the settled page", curlFrame != null && settled != null)
        assertEquals("left edge", settled!!.left, curlFrame!!.left, 2f)
        assertEquals("top edge", settled.top, curlFrame.top, 2f)
        assertEquals("right edge", settled.right, curlFrame.right, 2f)
        assertEquals("bottom edge", settled.bottom, curlFrame.bottom, 2f)
    }

    private fun pageBitmap(accent: Int, left: Float, right: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFF7F2E8.toInt())
        canvas.drawRect(
            PAGE_WIDTH * left,
            PAGE_HEIGHT * 0.08f,
            PAGE_WIDTH * (1f - right),
            PAGE_HEIGHT * 0.92f,
            Paint().apply { color = accent },
        )
        return bitmap
    }

    private fun CropMargins.toContentRect(): Rect =
        Rect(left, top, 1f - right, 1f - bottom)

    private fun redBounds(pixels: PixelMap): Rect? {
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = -1
        var bottom = -1
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val pixel = pixels[x, y]
                if (pixel.red > 0.6f && pixel.green < 0.5f && pixel.blue < 0.5f) {
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

    private class CachedPage(
        private val bitmap: Bitmap,
        private val crop: CropMargins,
    ) : PageBitmaps {
        override val pageCount: Int = 1
        override val cropUpdates = emptyFlow<Int>()

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap = bitmap

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap = bitmap

        override fun cropMargins(page: Int): CropMargins = crop
    }

    private companion object {
        const val PAGE_WIDTH = 300
        const val PAGE_HEIGHT = 500
    }
}
