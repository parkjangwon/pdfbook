package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpreadIndexOfTest {
    @Test
    fun findsSpreadForPage() {
        val s = spreads(7, twoPage = true, coverAlone = true)
        assertEquals(0, spreadIndexOf(s, 1))
        assertEquals(1, spreadIndexOf(s, 2))
        assertEquals(1, spreadIndexOf(s, 3))
        assertEquals(3, spreadIndexOf(s, 6))
        assertEquals(3, spreadIndexOf(s, 7))
    }

    @Test
    fun singlePageMode() {
        val s = spreads(3, twoPage = false, coverAlone = true)
        assertEquals(0, spreadIndexOf(s, 1))
        assertEquals(2, spreadIndexOf(s, 3))
    }

    @Test
    fun missingPageReturnsZero() {
        assertEquals(0, spreadIndexOf(spreads(2, twoPage = true, coverAlone = false), 99))
        assertEquals(0, spreadIndexOf(emptyList(), 1))
    }

    @Test
    fun modeSwitchKeepsPageViaIndex() {
        val two = spreads(7, twoPage = true, coverAlone = true)
        val one = spreads(7, twoPage = false, coverAlone = true)
        val kept = 5
        assertEquals(2, spreadIndexOf(two, kept))
        assertEquals(4, spreadIndexOf(one, kept))
        assertEquals(kept, one[spreadIndexOf(one, kept)].first())
    }

    @Test
    fun settleDoesNotReadPastEmpty() {
        assertNull(keptPageFromSettle(emptyList(), 0))
        assertEquals(1, keptPageFromSettle(spreads(1, twoPage = false, coverAlone = true), 0))
    }
}
