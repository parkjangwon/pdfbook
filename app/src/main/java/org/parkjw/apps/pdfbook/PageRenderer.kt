package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
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
    fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? = null
    fun prerender(current: List<PageRequest>, targets: List<PageRequest>) {}
    fun trimToCurrent() {}
}

interface PageSource : Closeable {
    val pageCount: Int
    fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap
    fun searchText(page: Int, query: String): Int = 0
}

data class SearchPageHits(val page: Int, val count: Int)

fun interface PageTextSearch {
    suspend fun search(query: String, onPageHits: (SearchPageHits) -> Unit)
}

class PdfPageSource(
    private val pfd: ParcelFileDescriptor,
    private val pdf: PdfRenderer,
) : PageSource {
    override val pageCount: Int get() = pdf.pageCount

    override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
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

    override fun searchText(page: Int, query: String): Int =
        pdf.openPage(page - 1).use { pdfPage -> pdfPage.searchText(query).size }

    override fun close() {
        pdf.close()
        pfd.close()
    }
}

class PageRenderer(
    private val source: PageSource,
    limitBytes: Long = cacheLimitBytes(Runtime.getRuntime().maxMemory()),
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : PageBitmaps, PageTextSearch, Closeable {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + renderDispatcher)
    private val jobLock = Any()
    private var prerenderJob: Job? = null
    private var pending: List<PageRequest> = emptyList()
    @Volatile private var closed = false

    val cache = PageCache<Bitmap>(limitBytes) { it.byteCount.toLong() }

    override val pageCount: Int = source.pageCount

    override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
        if (closed) null else cache.get(PageRequest(page, maxWidth, maxHeight))

    override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        val key = PageRequest(page, maxWidth, maxHeight)
        cache.get(key)?.let { return it }
        val interrupted = synchronized(jobLock) {
            val active = prerenderJob?.isActive == true
            prerenderJob?.cancel()
            active
        }
        try {
            return renderLocked(key)
        } finally {
            if (interrupted) startPrerender()
        }
    }

    override fun prerender(current: List<PageRequest>, targets: List<PageRequest>) {
        if (closed) return
        cache.focus(current.map { it.page })
        synchronized(jobLock) { pending = current + targets }
        startPrerender()
    }

    override fun trimToCurrent() {
        synchronized(jobLock) {
            prerenderJob?.cancel()
            pending = emptyList()
        }
        cache.trimToCurrent()
    }

    suspend fun awaitPrerender() {
        synchronized(jobLock) { prerenderJob }?.join()
    }

    private fun startPrerender() {
        synchronized(jobLock) {
            prerenderJob?.cancel()
            val targets = pending
            if (closed || targets.isEmpty()) return
            prerenderJob = scope.launch {
                for (target in targets) {
                    ensureActive()
                    renderLocked(target)
                }
            }
        }
    }

    private suspend fun renderLocked(key: PageRequest): Bitmap? {
        if (closed || key.page < 1 || key.page > pageCount || key.maxWidth <= 0 || key.maxHeight <= 0) {
            return null
        }
        cache.get(key)?.let { return it }
        return withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock null
                cache.get(key)?.let { return@withLock it }
                source.draw(key.page, key.maxWidth, key.maxHeight).also { cache.put(key, it) }
            }
        }
    }

    override suspend fun search(query: String, onPageHits: (SearchPageHits) -> Unit) {
        for (page in 1..pageCount) {
            coroutineContext.ensureActive()
            val count = withContext(renderDispatcher) {
                mutex.withLock {
                    if (closed) return@withLock 0
                    source.searchText(page, query)
                }
            }
            if (count > 0) onPageHits(SearchPageHits(page, count))
        }
    }

    override fun close() {
        synchronized(jobLock) { prerenderJob?.cancel() }
        scope.cancel()
        runBlocking(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock
                closed = true
                cache.clear()
                source.close()
            }
        }
    }

    companion object {
        fun open(pfd: ParcelFileDescriptor): PageRenderer {
            val pdf = try {
                PdfRenderer(pfd)
            } catch (t: Throwable) {
                pfd.close()
                throw t
            }
            return PageRenderer(PdfPageSource(pfd, pdf))
        }
    }
}
