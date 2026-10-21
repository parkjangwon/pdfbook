package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingAnchorTest {
    @Test
    fun singleToTwoPageKeepsPageThree() {
        val single = spreads(5, twoPage = false, coverAlone = true)
        var anchor = readingAnchorOf(3)
        anchor = readingAnchorAfterSettle(anchor, single, spreadIndexOf(single, 3))
        assertEquals(3, anchor.page)

        val double = spreads(5, twoPage = true, coverAlone = true)
        anchor = readingAnchorBeginSync(anchor, double)
        assertEquals(spreadIndexOf(double, 3), anchor.pendingIndex)
        // Stale settle on old index must not move the anchor.
        anchor = readingAnchorAfterSettle(anchor, double, 0)
        assertEquals(3, anchor.page)
        assertEquals(spreadIndexOf(double, 3), anchor.pendingIndex)

        val target = spreadIndexOf(double, 3)
        anchor = readingAnchorAfterSettle(anchor, double, target)
        assertEquals(3, anchor.page)
        assertNull(anchor.pendingIndex)
        assertEquals(listOf(2, 3), double[target])
    }

    @Test
    fun page37RoundTrip() {
        val unfolded = spreads(40, twoPage = true, coverAlone = true)
        var anchor = readingAnchorOf(37)
        anchor = readingAnchorAfterSettle(anchor, unfolded, spreadIndexOf(unfolded, 37))
        assertEquals(37, anchor.page)
        assertEquals(listOf(36, 37), unfolded[spreadIndexOf(unfolded, 37)])

        val folded = spreads(40, twoPage = false, coverAlone = true)
        anchor = readingAnchorBeginSync(anchor, folded)
        anchor = readingAnchorAfterSettle(anchor, folded, spreadIndexOf(folded, 37))
        assertEquals(37, anchor.page)

        val book = spreads(40, twoPage = true, coverAlone = true)
        anchor = readingAnchorBeginSync(anchor, book)
        anchor = readingAnchorAfterSettle(anchor, book, spreadIndexOf(book, 37))
        assertEquals(37, anchor.page)
        assertEquals(listOf(36, 37), book[spreadIndexOf(book, 37)])
    }

    @Test
    fun userNavigateUpdatesToFirstOfNewSpread() {
        val spreads = spreads(10, twoPage = true, coverAlone = true)
        var anchor = readingAnchorOf(3)
        anchor = readingAnchorAfterSettle(anchor, spreads, spreadIndexOf(spreads, 3))
        val nextIndex = spreadIndexOf(spreads, 5)
        anchor = readingAnchorAfterSettle(anchor, spreads, nextIndex)
        assertEquals(4, anchor.page) // spread [4,5]
    }

    @Test
    fun lastOddPageAlone() {
        // cover alone + even total → last page sits alone: [1], [2,3], [4]
        val spreads = spreads(4, twoPage = true, coverAlone = true)
        var anchor = readingAnchorOf(4)
        anchor = readingAnchorBeginSync(anchor, spreads)
        anchor = readingAnchorAfterSettle(anchor, spreads, spreadIndexOf(spreads, 4))
        assertEquals(4, anchor.page)
        assertEquals(listOf(4), spreads[spreadIndexOf(spreads, 4)])
    }

    @Test
    fun coverAloneSpread() {
        val spreads = spreads(5, twoPage = true, coverAlone = true)
        assertEquals(listOf(1), spreads[0])
        var anchor = readingAnchorOf(1)
        anchor = readingAnchorAfterSettle(anchor, spreads, 0)
        assertEquals(1, anchor.page)
    }

    @Test
    fun rtlSpreadStillContainsAnchor() {
        val spreads = spreads(10, twoPage = true, coverAlone = true)
        val index = spreadIndexOf(spreads, 7)
        val layout = spreadLayout(spreads[index], ReadingDirection.RTL)
        assertEquals(listOf(7, 6), layout)
        var anchor = readingAnchorOf(7)
        anchor = readingAnchorAfterSettle(anchor, spreads, index)
        assertEquals(7, anchor.page)
    }
}
