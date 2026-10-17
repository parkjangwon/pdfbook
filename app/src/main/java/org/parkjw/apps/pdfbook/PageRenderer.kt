package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.min

interface PageBitmaps {
    val pageCount: Int
    suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap?
    fun prefetch(spreads: List<List<Int>>, spreadIndex: Int, maxWidth: Int, maxHeight: Int) {}
}

class PageRenderer private constructor(
    private val pfd: ParcelFileDescriptor,
    private val pdf: PdfRenderer,
) : PageBitmaps, Closeable {
    private val mutex = Mutex()
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + renderDispatcher)
    private var prefetchJob: Job? = null
    @Volatile private var closed = false

    private val cache = object : LruCache<CacheKey, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt().coerceAtLeast(1),
    ) {
        override fun sizeOf(key: CacheKey, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    override val pageCount: Int get() = pdf.pageCount

    override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        prefetchJob?.cancel()
        return renderLocked(page, maxWidth, maxHeight)
    }

    override fun prefetch(spreads: List<List<Int>>, spreadIndex: Int, maxWidth: Int, maxHeight: Int) {
        if (closed || maxWidth <= 0 || maxHeight <= 0 || spreads.isEmpty()) return
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val from = (spreadIndex - 2).coerceAtLeast(0)
            val to = (spreadIndex + 2).coerceAtMost(spreads.lastIndex)
            for (i in from..to) {
                for (page in spreads[i]) {
                    renderLocked(page, maxWidth, maxHeight)
                }
            }
        }
    }

    private suspend fun renderLocked(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (closed || page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
        val key = CacheKey(page, maxWidth, maxHeight)
        cache.get(key)?.let { return it }
        return withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock null
                cache.get(key)?.let { return@withLock it }
                draw(page, maxWidth, maxHeight).also { cache.put(key, it) }
            }
        }
    }

    private fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
        pdf.openPage(page - 1).use { pdfPage ->
            val scale = min(
                maxWidth.toFloat() / pdfPage.width,
                maxHeight.toFloat() / pdfPage.height,
            )
            val width = (pdfPage.width * scale).toInt().coerceAtLeast(1)
            val height = (pdfPage.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    override fun close() {
        prefetchJob?.cancel()
        scope.cancel()
        runBlocking(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock
                closed = true
                cache.evictAll()
                pdf.close()
                pfd.close()
            }
        }
    }

    private data class CacheKey(val page: Int, val maxWidth: Int, val maxHeight: Int)

    companion object {
        fun open(pfd: ParcelFileDescriptor): PageRenderer {
            val pdf = try {
                PdfRenderer(pfd)
            } catch (t: Throwable) {
                pfd.close()
                throw t
            }
            return PageRenderer(pfd, pdf)
        }
    }
}
