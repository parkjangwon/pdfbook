package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class PageRendererCloseTest {
    @Test(timeout = 10_000)
    fun closeReturnsWhileAPageDrawIsStillRunning() {
        val drawStarted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val source = BlockingSource(drawStarted, release)
        val renderer = PageRenderer(source)
        val draw = thread { runBlocking { renderer.render(1, 20, 20) } }
        assertTrue(drawStarted.await(5, TimeUnit.SECONDS))

        renderer.close()

        assertFalse("close waited for the in-flight draw", source.drawFinished.get())
        assertNull("a closed renderer still draws", runBlocking { renderer.render(1, 20, 20) })
        release.countDown()
        runBlocking { renderer.awaitClosed() }
        assertTrue("source was never closed", source.closed.get())
        assertFalse("source closed on the main thread", source.closedOnMain.get())
        draw.join(5_000)
    }

    @Test(timeout = 10_000)
    fun closeIsIdempotentAndReleasesTheSourceOnce() {
        val source = BlockingSource(CountDownLatch(0), CountDownLatch(0))
        val renderer = PageRenderer(source)
        runBlocking { renderer.render(1, 20, 20) }
        assertEquals(setOf(1), renderer.cache.pages())
        renderer.close()
        renderer.close()
        runBlocking { renderer.awaitClosed() }
        assertTrue(source.closed.get())
        assertEquals(1, source.closes.get())
        assertTrue(renderer.cache.pages().isEmpty())
        assertNull(renderer.cached(1, 20, 20))
    }

    private class BlockingSource(
        private val drawStarted: CountDownLatch,
        private val release: CountDownLatch,
    ) : PageSource {
        override val pageCount: Int = 3
        val drawFinished = AtomicBoolean(false)
        val closed = AtomicBoolean(false)
        val closedOnMain = AtomicBoolean(false)
        val closes = AtomicInteger(0)

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            drawStarted.countDown()
            release.await(5, TimeUnit.SECONDS)
            drawFinished.set(true)
            return Bitmap.createBitmap(maxWidth.coerceAtLeast(1), maxHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        }

        override fun close() {
            closed.set(true)
            closes.incrementAndGet()
            closedOnMain.set(Looper.getMainLooper().isCurrentThread)
        }
    }
}
