package dev.parkjw.pdfbook

import org.junit.Assert.assertEquals
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
}
