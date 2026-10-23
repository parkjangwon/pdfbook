package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderEdgeTest {
    private fun sorted(vararg names: String) = names.toList().sortedWith(NaturalOrder)

    @Test
    fun emptyStrings() {
        assertEquals(0, NaturalOrder.compare("", ""))
        assertTrue(NaturalOrder.compare("", "a") < 0)
        assertTrue(NaturalOrder.compare("", "0") < 0)
        assertEquals(listOf("", "1", " "), sorted(" ", "1", ""))
    }

    @Test
    fun allZeroRunsOrderByLengthDescending() {
        assertEquals(listOf("000", "00", "0"), sorted("0", "000", "00"))
        assertEquals(listOf("a000", "a0", "a1"), sorted("a1", "a0", "a000"))
    }

    @Test
    fun zerosOnlyBreakTiesAfterEverythingElse() {
        assertEquals(listOf("01a", "1a", "1b"), sorted("1b", "1a", "01a"))
        assertEquals(listOf("v01.2", "v1.2", "v1.10"), sorted("v1.10", "v1.2", "v01.2"))
    }

    @Test
    fun firstLeadingZeroTieDecidesWhenSeveralRunsTie() {
        assertEquals(listOf("01-1", "1-01"), sorted("1-01", "01-1"))
        assertEquals(listOf("01-01", "01-1", "1-01", "1-1"), sorted("1-1", "1-01", "01-1", "01-01"))
    }

    @Test
    fun nonAsciiDigitsAreOrdinaryCharactersNotNumbers() {
        // Fullwidth and Arabic-Indic digits are compared as text, so "１０" sorts before "２".
        assertEquals(listOf("１０", "２"), sorted("２", "１０"))
        assertEquals(listOf("٠١", "٢"), sorted("٢", "٠١"))
    }

    @Test
    fun asciiDigitsSortBeforeNonAsciiDigits() {
        assertTrue(NaturalOrder.compare("9", "１") < 0)
        assertTrue(NaturalOrder.compare("１", "9") > 0)
        assertEquals(listOf("a9", "a１"), sorted("a１", "a9"))
    }

    @Test
    fun koreanSortsByCodePointWithNumbersByValue() {
        assertEquals(
            listOf("가 2", "가 10", "나", "다 1"),
            sorted("다 1", "가 10", "나", "가 2"),
        )
        assertEquals(listOf("제2권", "제10권", "제11권"), sorted("제11권", "제2권", "제10권"))
    }

    @Test
    fun digitsThenLatinThenHangul() {
        assertEquals(listOf("1권", "a", "가"), sorted("가", "a", "1권"))
    }

    @Test
    fun koreanFileNamesWithPaddedAndUnpaddedVolumes() {
        assertEquals(
            listOf("원피스 01권", "원피스 2권", "원피스 003권", "원피스 10권"),
            sorted("원피스 10권", "원피스 003권", "원피스 2권", "원피스 01권"),
        )
    }

    @Test
    fun caseOnlyDifferenceFallsBackToCodePointOrder() {
        assertEquals(listOf("A1", "a1"), sorted("a1", "A1"))
        assertEquals(listOf("Ab 2", "ab 2"), sorted("ab 2", "Ab 2"))
    }

    @Test
    fun leadingWhitespaceSortsAfterDigitsButBeforeLetters() {
        assertEquals(listOf("1", " 1", "a"), sorted("a", " 1", "1"))
        assertEquals(listOf("1 a", "1a"), sorted("1a", "1 a"))
    }

    @Test
    fun emojiAndSurrogatePairsDoNotBreakOrdering() {
        val names = listOf("📘 2", "📘 10", "📗 1", "a", "1", "가")
        val expected = listOf("1", "a", "가", "📗 1", "📘 2", "📘 10")
        val result = names.shuffled(java.util.Random(7)).sortedWith(NaturalOrder)
        assertEquals(expected, result)
        for (x in names) for (y in names) {
            assertEquals(Integer.signum(NaturalOrder.compare(x, y)), -Integer.signum(NaturalOrder.compare(y, x)))
        }
    }

    @Test
    fun veryLongDigitRunsWithLeadingZerosCompareByValue() {
        val nines = "9".repeat(5_000)
        val tenPow = "1" + "0".repeat(5_000)
        assertEquals(listOf("0$nines", tenPow), sorted(tenPow, "0$nines"))
        assertTrue(NaturalOrder.compare("0".repeat(5_000) + "7", "8") < 0)
    }

    @Test
    fun numberVersusTextAtSamePositionIsNotMixedUp() {
        assertTrue(NaturalOrder.compare("1a", "a1") < 0)
        assertTrue(NaturalOrder.compare("a1", "1a") > 0)
        assertTrue(NaturalOrder.compare("a", "a0") < 0)
    }
}
