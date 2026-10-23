package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PageCacheEdgeTest {
    private fun cache(limit: Long) = PageCache<Long>(limit) { it }

    private fun key(page: Int, width: Int = 100) = PageRequest(page, width, 200)

    @Test
    fun replacingKeyAdjustsSize() {
        val cache = cache(100)
        cache.put(key(1), 10)
        cache.put(key(1), 4)
        assertEquals(4L, cache.sizeBytes)
        assertEquals(setOf(1), cache.pages())
    }

    @Test
    fun replacingWithOversizeValueDropsOldEntry() {
        val cache = cache(10)
        cache.put(key(1), 5)
        cache.put(key(1), 11)
        assertNull(cache.get(key(1)))
        assertEquals(0L, cache.sizeBytes)
    }

    @Test
    fun zeroSizedValuesCountAsOneByte() {
        val cache = cache(3)
        cache.focus(listOf(1))
        (1..4).forEach { cache.put(key(it), 0) }
        assertEquals(setOf(1, 2, 3), cache.pages())
        assertEquals(3L, cache.sizeBytes)
    }

    @Test
    fun zeroLimitCachesNothing() {
        val cache = cache(0)
        cache.put(key(1), 1)
        assertEquals(emptySet<Int>(), cache.pages())
        assertEquals(0L, cache.sizeBytes)
    }

    @Test
    fun sameBitmapAtTwoSizesAreSeparateEntriesTrimmedTogether() {
        val cache = cache(100)
        cache.focus(listOf(2))
        cache.put(key(1), 10)
        cache.put(key(1, width = 50), 10)
        assertEquals(20L, cache.sizeBytes)
        assertNotNull(cache.get(key(1, width = 50)))
        cache.trimToCurrent()
        assertEquals(0L, cache.sizeBytes)
    }

    @Test
    fun newEntryFarFromFocusIsEvictedItself() {
        val cache = cache(20)
        cache.focus(listOf(1))
        cache.put(key(1), 10)
        cache.put(key(2), 10)
        cache.put(key(9), 10)
        assertEquals(setOf(1, 2), cache.pages())
    }

    @Test
    fun emptyFocusEvictsOldestFirst() {
        val cache = cache(20)
        (1..3).forEach { cache.put(key(it), 10) }
        assertEquals(setOf(2, 3), cache.pages())
    }

    @Test
    fun trimWithEmptyFocusClearsEverything() {
        val cache = cache(100)
        cache.put(key(1), 10)
        cache.trimToCurrent()
        assertEquals(0L, cache.sizeBytes)
    }

    @Test
    fun spreadFocusTieDropsPageBehindSpread() {
        val cache = cache(30)
        cache.focus(listOf(4, 5))
        listOf(4, 5, 3, 6).forEach { cache.put(key(it), 10) }
        assertEquals(setOf(4, 5, 6), cache.pages())
    }

    @Test
    fun largeEntryEvictsSeveralUntilItFits() {
        val cache = cache(30)
        cache.focus(listOf(5))
        listOf(4, 5, 6).forEach { cache.put(key(it), 10) }
        cache.put(key(5, width = 50), 25)
        assertEquals(25L, cache.sizeBytes)
        assertNotNull(cache.get(key(5, width = 50)))
    }

    @Test
    fun clearKeepsPeak() {
        val cache = cache(30)
        cache.focus(listOf(1))
        (1..3).forEach { cache.put(key(it), 10) }
        cache.clear()
        cache.put(key(1), 10)
        assertEquals(10L, cache.sizeBytes)
        assertEquals(30L, cache.peakBytes)
    }

    @Test
    fun limitIsAtLeastOneByte() {
        assertEquals(1L, cacheLimitBytes(0))
        assertEquals(1L, cacheLimitBytes(-5))
        assertEquals(1L, cacheLimitBytes(3))
        assertEquals(2L, cacheLimitBytes(8))
    }
}
