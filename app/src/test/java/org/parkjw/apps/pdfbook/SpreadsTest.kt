package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadsTest {
    @Test
    fun singlePage() {
        assertEquals(listOf(listOf(1), listOf(2), listOf(3)), spreads(3, twoPage = false, coverAlone = true))
    }

    @Test
    fun coverAloneOddCount() {
        assertEquals(listOf(listOf(1), listOf(2, 3), listOf(4, 5), listOf(6, 7)), spreads(7, twoPage = true, coverAlone = true))
    }

    @Test
    fun noCoverEvenCount() {
        assertEquals(listOf(listOf(1, 2), listOf(3, 4)), spreads(4, twoPage = true, coverAlone = false))
    }

    @Test
    fun trailingSinglePage() {
        assertEquals(listOf(listOf(1), listOf(2, 3), listOf(4)), spreads(4, twoPage = true, coverAlone = true))
    }

    @Test
    fun empty() {
        assertEquals(emptyList<List<Int>>(), spreads(0, twoPage = true, coverAlone = true))
        assertFalse(hasReadableSpreads(spreads(0, twoPage = true, coverAlone = true)))
        assertTrue(hasReadableSpreads(spreads(1, twoPage = false, coverAlone = true)))
    }
}
