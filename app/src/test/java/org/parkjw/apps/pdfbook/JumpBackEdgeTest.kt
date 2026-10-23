package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JumpBackEdgeTest {
    private val single = spreads(120, twoPage = false, coverAlone = true)
    private val double = spreads(120, twoPage = true, coverAlone = true)

    @Test
    fun backwardSeekRemembersLaterPage() {
        assertEquals(JumpBack(page = 80, shownAt = 11), jumpBackAfterSeek(single, 79, 11))
    }

    @Test
    fun emptySpreadIsIgnored() {
        assertNull(jumpBackAfterSeek(listOf(emptyList(), listOf(9)), 0, 1))
    }

    @Test
    fun twoPageThresholdCountsPagesNotSpreads() {
        assertEquals(JumpBack(page = 2, shownAt = 3), jumpBackAfterSeek(double, 1, 3))
        assertNull(jumpBackAfterSeek(double, 0, 1))
        assertEquals(JumpBack(page = 1, shownAt = 2), jumpBackAfterSeek(double, 0, 2))
    }

    @Test
    fun stepRoundTripReturnsToOriginalOffer() {
        val first = jumpBackStep(single, JumpBack(page = 12, shownAt = 79), 79)
        val second = jumpBackStep(single, first.next!!, first.targetIndex)
        assertEquals(79, second.targetIndex)
        assertEquals(JumpBack(page = 12, shownAt = 79), second.next)
    }

    @Test
    fun stepToCurrentSpreadOffersNothing() {
        val step = jumpBackStep(single, JumpBack(page = 12, shownAt = 79), 11)
        assertEquals(11, step.targetIndex)
        assertNull(step.next)
    }

    @Test
    fun stepWithUnknownPageFallsBackToFirstSpread() {
        val step = jumpBackStep(single, JumpBack(page = 999, shownAt = 5), 40)
        assertEquals(0, step.targetIndex)
        assertEquals(JumpBack(page = 41, shownAt = 0), step.next)
    }

    @Test
    fun stepFromOutOfRangeIndexHasNoReturnOffer() {
        val step = jumpBackStep(single, JumpBack(page = 12, shownAt = 79), 500)
        assertEquals(11, step.targetIndex)
        assertNull(step.next)
    }

    @Test
    fun visibleOnlyWhenNothingElseCompetes() {
        val jump = JumpBack(page = 12, shownAt = 79)
        assertFalse(jumpBackVisible(jump, EndNotice.NoNextBook, sheetOpen = true))
        assertTrue(jumpBackVisible(jump, null, sheetOpen = false))
    }
}
