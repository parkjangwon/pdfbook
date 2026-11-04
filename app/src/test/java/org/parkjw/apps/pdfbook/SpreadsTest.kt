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

    @Test
    fun shiftWithCoverAlonePutsOneMorePageAloneAtTheFront() {
        assertEquals(
            listOf(listOf(1), listOf(2), listOf(3, 4), listOf(5, 6), listOf(7)),
            spreads(7, twoPage = true, coverAlone = true, shift = 1),
        )
    }

    @Test
    fun shiftWithoutCoverAlonePutsOnePageAloneAtTheFront() {
        assertEquals(
            listOf(listOf(1), listOf(2, 3), listOf(4, 5), listOf(6)),
            spreads(6, twoPage = true, coverAlone = false, shift = 1),
        )
    }

    @Test
    fun shiftMovesEveryPairByOneAndKeepsEveryPage() {
        val plain = spreads(6, twoPage = true, coverAlone = true)
        val shifted = spreads(6, twoPage = true, coverAlone = true, shift = 1)
        assertEquals(listOf(listOf(1), listOf(2, 3), listOf(4, 5), listOf(6)), plain)
        assertEquals(listOf(listOf(1), listOf(2), listOf(3, 4), listOf(5, 6)), shifted)
        assertEquals((1..6).toList(), shifted.flatten())
    }

    @Test
    fun shiftIgnoresSinglePageView() {
        assertEquals(
            listOf(listOf(1), listOf(2), listOf(3)),
            spreads(3, twoPage = false, coverAlone = true, shift = 1),
        )
    }

    @Test
    fun shiftedPairsDifferOnlyInsideTheSpreadForBothDirections() {
        val pair = spreads(6, twoPage = true, coverAlone = true, shift = 1)[2]
        assertEquals(listOf(3, 4), spreadLayout(pair, ReadingDirection.LTR))
        assertEquals(listOf(4, 3), spreadLayout(pair, ReadingDirection.RTL))
    }
}
