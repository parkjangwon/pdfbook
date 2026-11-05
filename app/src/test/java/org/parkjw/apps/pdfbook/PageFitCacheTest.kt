package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class PageFitCacheTest {
    @Test
    fun fitPagesAreCachedAtTheFitRenderSize() = runBlocking {
        val renderer = PageRenderer(Pages(4))
        val slotWidth = 400
        val slotHeight = 800
        val (fitWidth, fitHeight) = pageFitRenderSize(PageFit.Width, slotWidth, slotHeight)
        renderer.render(1, fitWidth, fitHeight)
        // Curl looks pages up with the slot size at Fit screen; a fit page must use the fit size.
        assertNull(renderer.cached(1, slotWidth, slotHeight))
        assertTrue(isSpreadCached(renderer, listOf(1), fitWidth, fitHeight))
        assertTrue(cachedSpreadBitmaps(renderer, listOf(1), fitWidth, fitHeight) != null)
    }

    private class Pages(override val pageCount: Int) : PageSource {
        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            val scale = minOf(maxWidth / 600f, maxHeight / 900f)
            val width = (600 * scale).toInt().coerceAtLeast(1)
            val height = (900 * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(Color.WHITE)
            return bitmap
        }

        override fun close() {}
    }
}
