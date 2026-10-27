package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

interface PageBitmaps {
    val pageCount: Int
    suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap?
    fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? = null
    fun prerender(current: List<PageRequest>, targets: List<PageRequest>) {}
    fun trimToCurrent() {}
    suspend fun outline(): List<OutlineEntry> = emptyList()
    suspend fun thumbnail(request: PageRequest): Bitmap? = null
}

interface PageSource : Closeable {
    val pageCount: Int
    fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap

    // PdfRenderer has no outline API on minSdk 35, so PdfPageSource keeps this empty and the contents tab stays hidden.
    fun outline(): List<OutlineNode> = emptyList()
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
            try {
                bitmap.eraseColor(Color.WHITE)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            } catch (t: Throwable) {
                bitmap.recycle()
                throw t
            }
            return bitmap
        }
    }

    override fun close() {
        pdf.close()
        pfd.close()
    }
}

class PageRenderer(
    private val source: PageSource,
    limitBytes: Long = cacheLimitBytes(Runtime.getRuntime().maxMemory()),
    thumbLimitBytes: Long = THUMB_CACHE_BYTES,
) : PageBitmaps, Closeable {
    private val mutex = Mutex()
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + renderDispatcher)
    private val jobLock = Any()
    private var prerenderJob: Job? = null
    private var pending: List<PageRequest> = emptyList()
    @Volatile private var closed = false
    @Volatile private var closeJob: Job? = null
    private val failedPages = ConcurrentHashMap.newKeySet<Int>()

    val cache = PageCache<Bitmap>(limitBytes) { it.byteCount.toLong() }

    private val thumbs = object : LruCache<PageRequest, Bitmap>(thumbLimitBytes.toInt()) {
        override fun sizeOf(key: PageRequest, value: Bitmap) = value.byteCount
    }

    override val pageCount: Int = source.pageCount

    // Thumbnails queue behind the page mutex without cancelling prerender, so the reader's own page never waits on a strip.
    override suspend fun thumbnail(request: PageRequest): Bitmap? {
        thumbs.get(request)?.let { return it }
        if (closed || request.page < 1 || request.page > pageCount) return null
        return withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock null
                thumbs.get(request)?.let { return@withLock it }
                source.draw(request.page, request.maxWidth, request.maxHeight).also { thumbs.put(request, it) }
            }
        }
    }

    override suspend fun outline(): List<OutlineEntry> = withContext(renderDispatcher) {
        mutex.withLock {
            if (closed) {
                emptyList()
            } else {
                outlineEntries(runCatching { source.outline() }.getOrDefault(emptyList()), pageCount)
            }
        }
    }

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
                    if (target.page in failedPages) continue
                    renderLocked(target)
                }
            }
        }
    }

    private suspend fun renderLocked(key: PageRequest): Bitmap? {
        if (closed || key.page < 1 || key.page > pageCount || key.maxWidth <= 0 || key.maxHeight <= 0) {
            return null
        }
        if (key.page in failedPages) return null
        cache.get(key)?.let { return it }
        return withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock null
                if (key.page in failedPages) return@withLock null
                cache.get(key)?.let { return@withLock it }
                try {
                    source.draw(key.page, key.maxWidth, key.maxHeight).also { cache.put(key, it) }
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    failedPages += key.page
                    Log.w(TAG, "draw failed for page ${key.page} (${t.javaClass.simpleName})")
                    null
                }
            }
        }
    }

    override fun close() {
        synchronized(jobLock) {
            if (closed) return
            closed = true
            prerenderJob?.cancel()
            closeJob = scope.launch {
                mutex.withLock {
                    cache.clear()
                    thumbs.evictAll()
                    source.close()
                }
            }
        }
    }

    suspend fun awaitClosed() {
        closeJob?.join()
    }

    companion object {
        private const val TAG = "PageRenderer"

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
