package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
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
    fun setAutoCrop(enabled: Boolean) {}
    fun cropMargins(page: Int): CropMargins? = null
    val cropUpdates: Flow<Int> get() = emptyFlow()
}

interface PageSource : Closeable {
    val pageCount: Int
    fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap
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

    override fun close() {
        pdf.close()
        pfd.close()
    }
}

class PageRenderer(
    private val source: PageSource,
    limitBytes: Long = cacheLimitBytes(Runtime.getRuntime().maxMemory()),
) : PageBitmaps, Closeable {
    private val mutex = Mutex()
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + renderDispatcher)
    private val jobLock = Any()
    private var prerenderJob: Job? = null
    private var pending: List<PageRequest> = emptyList()
    @Volatile private var closed = false

    val cache = PageCache<Bitmap>(limitBytes) { it.byteCount.toLong() }

    private val cropLock = Any()
    private val crops = HashMap<Int, CropMargins>()
    private val cropUpdatesFlow = MutableStateFlow(0)
    @Volatile private var autoCrop = false

    override val pageCount: Int = source.pageCount

    override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
        if (closed) null else cache.get(PageRequest(page, maxWidth, maxHeight))

    override fun setAutoCrop(enabled: Boolean) {
        if (autoCrop == enabled) return
        autoCrop = enabled
        if (enabled) {
            // The crop is applied while drawing the same cached bitmap, so the cache key does
            // not carry crop state. Pages cached before the toggle were never scanned; clear
            // them so they are re-rendered and detected.
            cache.clear()
        }
        cropUpdatesFlow.update { it + 1 }
    }

    override fun cropMargins(page: Int): CropMargins? {
        if (!autoCrop) return null
        return synchronized(cropLock) { crops[page] }
    }

    override val cropUpdates: Flow<Int> get() = cropUpdatesFlow

    override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        val key = PageRequest(page, maxWidth, maxHeight)
        cache.get(key)?.let {
            ensureCropDetected(page, it)
            return it
        }
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
        cache.get(key)?.let {
            ensureCropDetected(key.page, it)
            return it
        }
        val bitmap = withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) return@withLock null
                cache.get(key)?.let { return@withLock it }
                val drawn = source.draw(key.page, key.maxWidth, key.maxHeight)
                // Cache first: crop detection runs outside the render mutex so the next page
                // is not blocked. A cache hit with no crop yet detects it lazily.
                cache.put(key, drawn)
                drawn
            }
        }
        if (bitmap != null) ensureCropDetected(key.page, bitmap)
        return bitmap
    }

    private suspend fun ensureCropDetected(page: Int, bitmap: Bitmap) {
        if (!autoCrop || bitmap.isRecycled) return
        if (synchronized(cropLock) { crops.containsKey(page) }) return
        withContext(renderDispatcher) {
            if (synchronized(cropLock) { crops.containsKey(page) }) return@withContext
            val margins = detectCropMargins(bitmap)
            synchronized(cropLock) { crops[page] = margins }
            cropUpdatesFlow.update { it + 1 }
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
