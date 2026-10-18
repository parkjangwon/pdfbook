package org.parkjw.apps.pdfbook

import kotlin.math.abs

data class PageRequest(val page: Int, val maxWidth: Int, val maxHeight: Int)

const val CacheHeapDivisor = 4L

fun cacheLimitBytes(maxHeapBytes: Long): Long = (maxHeapBytes / CacheHeapDivisor).coerceAtLeast(1L)

fun spreadRequests(spread: List<Int>, viewportWidth: Int, viewportHeight: Int): List<PageRequest> {
    if (viewportWidth <= 0 || viewportHeight <= 0) return emptyList()
    val width = (viewportWidth / spread.size.coerceAtLeast(1)).coerceAtLeast(1)
    return spread.sorted().map { PageRequest(it, width, viewportHeight) }
}

fun prerenderTargets(
    spreads: List<List<Int>>,
    spreadIndex: Int,
    viewportWidth: Int,
    viewportHeight: Int,
): List<PageRequest> =
    listOf(spreadIndex + 1, spreadIndex - 1)
        .mapNotNull { spreads.getOrNull(it) }
        .flatMap { spreadRequests(it, viewportWidth, viewportHeight) }

class PageCache<V : Any>(
    val limitBytes: Long,
    private val sizeOf: (V) -> Long,
) {
    private val entries = LinkedHashMap<PageRequest, V>()
    private var current: List<Int> = emptyList()

    var sizeBytes: Long = 0L
        private set

    var peakBytes: Long = 0L
        private set

    @Synchronized
    fun get(key: PageRequest): V? = entries[key]

    @Synchronized
    fun pages(): Set<Int> = entries.keys.mapTo(sortedSetOf()) { it.page }

    @Synchronized
    fun put(key: PageRequest, value: V) {
        val size = sizeOf(value).coerceAtLeast(1L)
        entries.remove(key)?.let { sizeBytes -= sizeOf(it).coerceAtLeast(1L) }
        if (size > limitBytes) return
        entries[key] = value
        sizeBytes += size
        while (sizeBytes > limitBytes) {
            evict(victim())
        }
        peakBytes = maxOf(peakBytes, sizeBytes)
    }

    @Synchronized
    fun focus(pages: List<Int>) {
        current = pages
    }

    @Synchronized
    fun trimToCurrent() {
        entries.keys.filter { it.page !in current }.forEach { evict(it) }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        sizeBytes = 0L
    }

    private fun evict(key: PageRequest) {
        entries.remove(key)?.let { sizeBytes -= sizeOf(it).coerceAtLeast(1L) }
    }

    private fun victim(): PageRequest {
        val first = current.minOrNull() ?: 0
        return entries.keys.maxWith(
            compareBy<PageRequest> { distance(it.page) }.thenBy { if (it.page < first) 1 else 0 },
        )
    }

    private fun distance(page: Int): Int = current.minOfOrNull { abs(page - it) } ?: 0
}
