package dev.parkjw.pdfbook

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.min

class PageRenderer(
    private val pfd: ParcelFileDescriptor,
) : Closeable {
    private val pdf = PdfRenderer(pfd)
    private val mutex = Mutex()
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + renderDispatcher)
    private var prefetchJob: Job? = null

    private val cache = object : LruCache<CacheKey, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt().coerceAtLeast(1),
    ) {
        override fun sizeOf(key: CacheKey, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    val pageCount: Int get() = pdf.pageCount

    suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
        val key = CacheKey(page, maxWidth, maxHeight)
        cache.get(key)?.let { return it }
        return withContext(renderDispatcher) {
            mutex.withLock {
                cache.get(key)?.let { return@withLock it }
                draw(page, maxWidth, maxHeight).also { cache.put(key, it) }
            }
        }
    }

    fun prefetch(spreads: List<List<Int>>, spreadIndex: Int, maxWidth: Int, maxHeight: Int) {
        if (maxWidth <= 0 || maxHeight <= 0 || spreads.isEmpty()) return
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val from = (spreadIndex - 2).coerceAtLeast(0)
            val to = (spreadIndex + 2).coerceAtMost(spreads.lastIndex)
            for (i in from..to) {
                for (page in spreads[i]) {
                    render(page, maxWidth, maxHeight)
                }
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
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    override fun close() {
        prefetchJob?.cancel()
        scope.cancel()
        cache.evictAll()
        pdf.close()
        pfd.close()
    }

    private data class CacheKey(val page: Int, val maxWidth: Int, val maxHeight: Int)
}
