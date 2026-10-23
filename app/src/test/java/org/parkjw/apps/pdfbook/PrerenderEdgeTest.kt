package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class PrerenderEdgeTest {
    @Test
    fun spreadRequestsSortPagesAndSplitWidth() {
        assertEquals(
            listOf(PageRequest(4, 500, 900), PageRequest(5, 500, 900)),
            spreadRequests(listOf(5, 4), 1001, 900),
        )
    }

    @Test
    fun spreadRequestsDegenerateInputs() {
        assertEquals(emptyList<PageRequest>(), spreadRequests(emptyList(), 100, 100))
        assertEquals(emptyList<PageRequest>(), spreadRequests(listOf(1), -1, 100))
        assertEquals(emptyList<PageRequest>(), spreadRequests(listOf(1), 100, 0))
        assertEquals(
            listOf(PageRequest(1, 1, 10), PageRequest(2, 1, 10)),
            spreadRequests(listOf(1, 2), 1, 10),
        )
    }

    @Test
    fun twoPageFirstSpreadTargetsOnlyNext() {
        val pages = spreads(9, twoPage = true, coverAlone = true)
        assertEquals(
            listOf(PageRequest(2, 500, 800), PageRequest(3, 500, 800)),
            prerenderTargets(pages, 0, 1000, 800),
        )
    }

    @Test
    fun twoPageWithoutCoverStartsWithFullSpread() {
        val pages = spreads(4, twoPage = true, coverAlone = false)
        assertEquals(
            listOf(PageRequest(3, 500, 800), PageRequest(4, 500, 800)),
            prerenderTargets(pages, 0, 1000, 800),
        )
    }

    @Test
    fun twoPageLastSpreadTargetsOnlyPrevious() {
        val pages = spreads(9, twoPage = true, coverAlone = true)
        assertEquals(
            listOf(PageRequest(6, 500, 800), PageRequest(7, 500, 800)),
            prerenderTargets(pages, 4, 1000, 800),
        )
    }

    @Test
    fun lonelyLastPageNextToFullSpread() {
        val pages = spreads(10, twoPage = true, coverAlone = true)
        assertEquals(
            listOf(PageRequest(10, 1000, 800), PageRequest(6, 500, 800), PageRequest(7, 500, 800)),
            prerenderTargets(pages, 4, 1000, 800),
        )
        assertEquals(
            listOf(PageRequest(8, 500, 800), PageRequest(9, 500, 800)),
            prerenderTargets(pages, 5, 1000, 800),
        )
    }

    @Test
    fun outOfRangeIndexesAndTinyBooks() {
        val pages = spreads(3, twoPage = false, coverAlone = true)
        assertEquals(listOf(PageRequest(1, 10, 20)), prerenderTargets(pages, -1, 10, 20))
        assertEquals(listOf(PageRequest(3, 10, 20)), prerenderTargets(pages, 3, 10, 20))
        assertEquals(emptyList<PageRequest>(), prerenderTargets(pages, 9, 10, 20))
        assertEquals(emptyList<PageRequest>(), prerenderTargets(emptyList(), 0, 10, 20))
        assertEquals(emptyList<PageRequest>(), prerenderTargets(spreads(1, false, true), 0, 10, 20))
    }
}
