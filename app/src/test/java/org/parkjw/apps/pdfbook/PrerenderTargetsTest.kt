package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class PrerenderTargetsTest {
    @Test
    fun singlePageTargetsNextThenPrevious() {
        val pages = spreads(10, twoPage = false, coverAlone = true)
        assertEquals(
            listOf(PageRequest(6, 1080, 2400), PageRequest(4, 1080, 2400)),
            prerenderTargets(pages, 4, 1080, 2400),
        )
    }

    @Test
    fun twoPageTargetsWholeNextSpreadThenPreviousSpread() {
        val pages = spreads(9, twoPage = true, coverAlone = true)
        assertEquals(
            listOf(
                PageRequest(4, 1280, 1600),
                PageRequest(5, 1280, 1600),
                PageRequest(1, 2560, 1600),
            ),
            prerenderTargets(pages, 1, 2560, 1600),
        )
    }

    @Test
    fun edgesHaveOnlyOneNeighbor() {
        val pages = spreads(3, twoPage = false, coverAlone = true)
        assertEquals(listOf(PageRequest(2, 10, 20)), prerenderTargets(pages, 0, 10, 20))
        assertEquals(listOf(PageRequest(2, 10, 20)), prerenderTargets(pages, 2, 10, 20))
    }

    @Test
    fun noTargetsBeforeLayout() {
        val pages = spreads(3, twoPage = false, coverAlone = true)
        assertEquals(emptyList<PageRequest>(), prerenderTargets(pages, 1, 0, 0))
    }
}
