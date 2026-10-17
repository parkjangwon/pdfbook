package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class PagerNavTest {
    @Test
    fun advancesFromTargetNotCurrent() {
        assertEquals(3, pagerTargetIndex(targetPage = 2, delta = 1, pageCount = 10))
        assertEquals(4, pagerTargetIndex(targetPage = 3, delta = 1, pageCount = 10))
    }

    @Test
    fun clampsToBounds() {
        assertEquals(0, pagerTargetIndex(0, -1, 5))
        assertEquals(4, pagerTargetIndex(4, 1, 5))
        assertEquals(0, pagerTargetIndex(0, 1, 0))
    }
}
