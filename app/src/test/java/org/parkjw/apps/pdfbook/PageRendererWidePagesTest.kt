package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Test
    fun scanStartsAtTheAnchorAndSweepsOutward() = runBlocking {
        val source = SizedSource((1..40).associateWith { PageSize(600, 800) })
        val renderer = PageRenderer(source)
        try {
            renderer.widePages(anchor = 20)
            assertEquals(listOf(20, 21, 19, 22, 18), source.order.take(5))
            assertEquals((1..40).toSet(), source.order.toSet())
        } finally {
            renderer.close()
            renderer.awaitClosed()
        }
    }

    @Test
    fun firstPageRendersWhileTheScanIsStillRunning() = runBlocking {
        val source = SizedSource(
            (1..1000).associateWith { PageSize(600, 800) },
            delayMillis = 5,
        )
        val renderer = PageRenderer(source)
        try {
            val scan = launch(Dispatchers.Default) { renderer.widePages(anchor = 500) }
            assertTrue("scan did not start", source.started.await(5, TimeUnit.SECONDS))

            val bitmap = withTimeout(2_000) { renderer.render(500, 300, 400) }

            assertNotNull("the anchor page did not render", bitmap)
            assertTrue("the scan finished before the first page rendered", scan.isActive)
            scan.cancel()
        } finally {
            renderer.close()
            renderer.awaitClosed()
        }
    }

    @Test
    fun closeStopsTheScanAndClosesTheSource() = runBlocking {
        val source = SizedSource(
            (1..1000).associateWith { PageSize(600, 800) },
            delayMillis = 5,
        )
        val renderer = PageRenderer(source)
        val scan = launch(Dispatchers.Default) { renderer.widePages(anchor = 1) }
        assertTrue("scan did not start", source.started.await(5, TimeUnit.SECONDS))

        renderer.close()
        withTimeout(2_000) { renderer.awaitClosed() }
        scan.cancel()

        assertTrue("the source closed before the scan stopped", source.closed)
        assertTrue("the scan read the whole book", source.sizeCalls < 1000)
    }

    private class SizedSource(
        private val sizes: Map<Int, PageSize?>,
        private val delayMillis: Long = 0,
    ) : PageSource {
        override val pageCount: Int = sizes.keys.max()
        var sizeCalls = 0
        val order = ArrayList<Int>()
        val started = CountDownLatch(1)
        @Volatile var closed = false

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap =
            Bitmap.createBitmap(maxWidth.coerceAtLeast(1), maxHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)

        override fun pageSize(page: Int): PageSize? {
            sizeCalls += 1
            order += page
            started.countDown()
            if (delayMillis > 0) Thread.sleep(delayMillis)
            return sizes[page]
        }

        override fun close() {
            closed = true
        }
    }
}
