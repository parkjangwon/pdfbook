package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class PageRendererWidePagesTest {
    @Test
    fun onlyPagesWiderThanTallAreWideAndTheScanRunsOnce() = runBlocking {
        val source = SizedSource(
            mapOf(
                1 to PageSize(600, 800),
                2 to PageSize(900, 700),
                3 to PageSize(500, 500),
                4 to null,
            ),
        )
        val renderer = PageRenderer(source)
        try {
            assertEquals(setOf(2), renderer.widePages())
            assertEquals(4, source.sizeCalls)

            assertEquals(setOf(2), renderer.widePages())
            assertEquals("wide pages were rescanned", 4, source.sizeCalls)
        } finally {
            renderer.close()
            renderer.awaitClosed()
        }
    }

    private class SizedSource(private val sizes: Map<Int, PageSize?>) : PageSource {
        override val pageCount: Int = sizes.size
        var sizeCalls = 0

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap =
            Bitmap.createBitmap(maxWidth.coerceAtLeast(1), maxHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)

        override fun pageSize(page: Int): PageSize? {
            sizeCalls += 1
            return sizes[page]
        }

        override fun close() = Unit
    }
}
