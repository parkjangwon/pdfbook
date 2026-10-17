package dev.parkjw.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class SpreadLayoutTest {
    @Test
    fun ltrKeepsOrder() {
        assertEquals(listOf(2, 3), spreadLayout(listOf(2, 3), ReadingDirection.LTR))
    }

    @Test
    fun rtlPutsLowerPageOnRight() {
        assertEquals(listOf(3, 2), spreadLayout(listOf(2, 3), ReadingDirection.RTL))
    }

    @Test
    fun singlePageUnchanged() {
        assertEquals(listOf(1), spreadLayout(listOf(1), ReadingDirection.RTL))
        assertEquals(listOf(1), spreadLayout(listOf(1), ReadingDirection.LTR))
    }
}
