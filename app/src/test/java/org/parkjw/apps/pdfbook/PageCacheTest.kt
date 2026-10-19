package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PageCacheTest {
    private fun cache(limit: Long) = PageCache<Long>(limit) { it }

    private fun key(page: Int) = PageRequest(page, 100, 200)

    @Test
    fun evictsFarthestFromCurrentFirst() {
        val cache = cache(30)
        cache.focus(listOf(10))
        cache.put(key(10), 10)
        cache.put(key(11), 10)
        cache.put(key(15), 10)
        cache.put(key(9), 10)
        assertEquals(setOf(9, 10, 11), cache.pages())
        assertTrue(cache.sizeBytes <= 30)
    }

    @Test
    fun keepsPageAheadInReadingOrderOnTie() {
        val cache = cache(15)
        cache.focus(listOf(5))
        cache.put(key(5), 10)
        cache.put(key(4), 5)
        cache.put(key(6), 5)
        assertEquals(setOf(5, 6), cache.pages())
    }

    @Test
    fun followsFocusWhenCurrentPageMoves() {
        val cache = cache(30)
        cache.focus(listOf(1))
        listOf(1, 2, 3).forEach { cache.put(key(it), 10) }
        cache.focus(listOf(4))
        cache.put(key(4), 10)
        assertEquals(setOf(2, 3, 4), cache.pages())
    }

    @Test
    fun neverExceedsLimitUnderRandomLoad() {
        val cache = cache(1_000)
        val random = Random(7)
        repeat(5_000) {
            val current = random.nextInt(1, 600)
            cache.focus(listOf(current))
            cache.put(key((current + random.nextInt(-3, 4)).coerceAtLeast(1)), random.nextLong(1, 400))
            assertTrue(cache.sizeBytes <= cache.limitBytes)
        }
        assertTrue(cache.peakBytes <= cache.limitBytes)
    }

    @Test
    fun entryLargerThanLimitIsNotCached() {
        val cache = cache(10)
        cache.put(key(1), 11)
        assertNull(cache.get(key(1)))
        assertEquals(0L, cache.sizeBytes)
    }

    @Test
    fun trimKeepsOnlyCurrentSpread() {
        val cache = cache(1_000)
        cache.focus(listOf(4, 5))
        listOf(2, 3, 4, 5, 6, 7).forEach { cache.put(key(it), 10) }
        cache.trimToCurrent()
        assertEquals(setOf(4, 5), cache.pages())
        assertEquals(20L, cache.sizeBytes)
    }

    @Test
    fun limitIsQuarterOfMaxHeap() {
        assertEquals(64L * MiB, cacheLimitBytes(256L * MiB))
        assertEquals(128L * MiB, cacheLimitBytes(512L * MiB))
    }

    @Test
    fun quarterBudgetHoldsWorkingSetOnCommonScreens() {
        val heap = 256L * MiB
        val screens = listOf(
            Triple(1080, 2400, false),
            Triple(1440, 3200, false),
            Triple(2560, 1600, true),
            Triple(2208, 1840, true),
        )
        screens.forEach { (width, height, twoPage) ->
            val pages = if (twoPage) 6 else 3
            val slotWidth = if (twoPage) width / 2 else width
            val workingSet = pages * slotWidth.toLong() * height * 4
            assertTrue("$width x $height", workingSet <= cacheLimitBytes(heap))
        }
        val tallPhone = 3 * 1440L * 3200 * 4
        assertTrue(tallPhone > heap / 8)
    }

    private companion object {
        const val MiB = 1024L * 1024L
    }
}
