package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.settings.TwoPageMode

class SpreadsEdgeTest {
    @Test
    fun negativeAndZeroPageCountsAreEmptyInEveryMode() {
        for (count in listOf(-5, -1, 0)) for (two in listOf(true, false)) for (cover in listOf(true, false)) {
            assertEquals(emptyList<List<Int>>(), spreads(count, two, cover))
        }
    }

    @Test
    fun tinyCounts() {
        assertEquals(listOf(listOf(1)), spreads(1, twoPage = true, coverAlone = true))
        assertEquals(listOf(listOf(1)), spreads(1, twoPage = true, coverAlone = false))
        assertEquals(listOf(listOf(1), listOf(2)), spreads(2, twoPage = true, coverAlone = true))
        assertEquals(listOf(listOf(1, 2)), spreads(2, twoPage = true, coverAlone = false))
        assertEquals(listOf(listOf(1, 2), listOf(3)), spreads(3, twoPage = true, coverAlone = false))
        assertEquals(listOf(listOf(1), listOf(2, 3)), spreads(3, twoPage = true, coverAlone = true))
    }

    @Test
    fun singlePageModeIgnoresCoverAlone() {
        assertEquals(spreads(5, false, true), spreads(5, false, false))
    }

    @Test
    fun everyPageAppearsOnceInOrderForOddAndEvenCounts() {
        for (count in 1..25) for (cover in listOf(true, false)) {
            val result = spreads(count, twoPage = true, coverAlone = cover)
            assertEquals("count=$count cover=$cover", (1..count).toList(), result.flatten())
            assertTrue(result.all { it.size in 1..2 })
            assertTrue("only the last spread may be short", result.dropLast(1).drop(if (cover) 1 else 0).all { it.size == 2 })
        }
    }

    @Test
    fun largeBookStaysLinear() {
        val result = spreads(10_000, twoPage = true, coverAlone = true)
        assertEquals(5_001, result.size)
        assertEquals(listOf(10_000), result.last())
        assertEquals(listOf(9_998, 9_999), result[result.size - 2])
    }

    @Test
    fun rtlLayoutPutsLowerPageRightAndLeavesSinglesAlone() {
        val laidOut = spreads(5, twoPage = true, coverAlone = true).map { spreadLayout(it, ReadingDirection.RTL) }
        assertEquals(listOf(listOf(1), listOf(3, 2), listOf(5, 4)), laidOut)
    }

    @Test
    fun rtlTrailingSingleOnOddCountWithoutCoverStaysSingle() {
        val laidOut = spreads(3, twoPage = true, coverAlone = false).map { spreadLayout(it, ReadingDirection.RTL) }
        assertEquals(listOf(listOf(2, 1), listOf(3)), laidOut)
    }

    @Test
    fun ltrLayoutNeverReordersAndRtlTwiceIsIdentity() {
        for (spread in spreads(9, twoPage = true, coverAlone = true)) {
            assertEquals(spread, spreadLayout(spread, ReadingDirection.LTR))
            assertEquals(spread, spreadLayout(spreadLayout(spread, ReadingDirection.RTL), ReadingDirection.RTL))
        }
        assertEquals(emptyList<Int>(), spreadLayout(emptyList(), ReadingDirection.RTL))
    }

    @Test
    fun spreadIndexOfIsDirectionIndependentAndFindsBothSidesOfSpread() {
        val s = spreads(7, twoPage = true, coverAlone = true)
        assertEquals(1, spreadIndexOf(s, 2))
        assertEquals(1, spreadIndexOf(s, 3))
        assertEquals(3, spreadIndexOf(s, 7))
    }

    @Test
    fun spreadIndexOfOutOfRangePagesFallBackToFirstSpread() {
        val s = spreads(4, twoPage = true, coverAlone = true)
        assertEquals(0, spreadIndexOf(s, 0))
        assertEquals(0, spreadIndexOf(s, -3))
        assertEquals(0, spreadIndexOf(s, 99))
        assertEquals(0, spreadIndexOf(emptyList(), 1))
    }

    @Test
    fun keptPageFromSettleUsesFirstPageOfSettledSpread() {
        val s = spreads(5, twoPage = true, coverAlone = true)
        assertEquals(1, keptPageFromSettle(s, 0))
        assertEquals(4, keptPageFromSettle(s, 2))
        assertNull(keptPageFromSettle(s, -1))
        assertNull(keptPageFromSettle(s, s.size))
        assertNull(keptPageFromSettle(emptyList(), 0))
    }

    @Test
    fun switchingToSinglePageFromSecondPageOfSpreadKeepsSpreadFirstPage() {
        val two = spreads(6, twoPage = true, coverAlone = true)
        val kept = keptPageFromSettle(two, spreadIndexOf(two, 3))
        assertEquals(2, kept)
        assertEquals(1, spreadIndexOf(spreads(6, twoPage = false, coverAlone = true), 2))
    }

    @Test
    fun twoPageDecisionMatrix() {
        val fold = IntRect(400, 0, 420, 800)
        val postures = listOf(FoldPosture.Flat, FoldPosture.Book(fold), FoldPosture.Tabletop(fold))
        for (width in listOf(Int.MIN_VALUE, -1, 0, 599, 600, 601, Int.MAX_VALUE)) for (mode in TwoPageMode.entries) {
            for (posture in postures) {
                val expected = when (posture) {
                    is FoldPosture.Tabletop -> false
                    is FoldPosture.Book -> mode != TwoPageMode.AlwaysSingle
                    FoldPosture.Flat -> mode == TwoPageMode.AlwaysDouble || (mode == TwoPageMode.Auto && width >= 600)
                }
                assertEquals("$mode $width $posture", expected, shouldUseTwoPage(mode, width, posture))
            }
        }
    }
}
