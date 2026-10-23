package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoToPageTest {
    @Test
    fun acceptsPagesInsideRange() {
        assertEquals(1, parsePageInput("1", 120))
        assertEquals(80, parsePageInput("80", 120))
        assertEquals(120, parsePageInput("120", 120))
        assertEquals(7, parsePageInput(" 7 ", 120))
    }

    @Test
    fun rejectsEmptyOrNonNumericInput() {
        assertNull(parsePageInput("", 120))
        assertNull(parsePageInput("   ", 120))
        assertNull(parsePageInput("abc", 120))
        assertNull(parsePageInput("1.5", 120))
        assertNull(parsePageInput("-3", 120))
    }

    @Test
    fun rejectsOutOfRangeInput() {
        assertNull(parsePageInput("0", 120))
        assertNull(parsePageInput("121", 120))
    }

    @Test
    fun rejectsWhenTheBookHasNoPages() {
        assertNull(parsePageInput("1", 0))
        assertNull(parsePageInput("1", -1))
    }

    @Test
    fun mapsPageToTheSpreadContainingIt() {
        val singleSpreads = spreads(120, twoPage = false, coverAlone = true)
        assertEquals(79, spreadIndexOf(singleSpreads, 80))

        val twoPageSpreads = spreads(10, twoPage = true, coverAlone = true)
        assertEquals(
            listOf(listOf(1), listOf(2, 3), listOf(4, 5), listOf(6, 7), listOf(8, 9), listOf(10)),
            twoPageSpreads,
        )
        assertEquals(3, spreadIndexOf(twoPageSpreads, 7))
        assertEquals(4, spreadIndexOf(twoPageSpreads, 8))
        assertEquals(5, spreadIndexOf(twoPageSpreads, 10))
    }
}
