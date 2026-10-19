package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JumpBackTest {
    private val single = spreads(120, twoPage = false, coverAlone = true)
    private val double = spreads(120, twoPage = true, coverAlone = true)

    @Test
    fun jb1LongSeekRemembersPageBefore() {
        assertEquals(JumpBack(page = 12, shownAt = 79), jumpBackAfterSeek(single, 11, 79))
    }

    @Test
    fun jb2ReturnOffersRoundTrip() {
        val step = jumpBackStep(single, JumpBack(page = 12, shownAt = 79), 79)
        assertEquals(11, step.targetIndex)
        assertEquals(JumpBack(page = 80, shownAt = 11), step.next)
    }

    @Test
    fun jb4ShortSeekOfTwoPagesOrLessShowsNothing() {
        assertNull(jumpBackAfterSeek(single, 11, 13))
        assertNull(jumpBackAfterSeek(single, 11, 9))
        assertNull(jumpBackAfterSeek(single, 11, 11))
        assertEquals(JumpBack(page = 12, shownAt = 14), jumpBackAfterSeek(single, 11, 14))
    }

    @Test
    fun twoPageRemembersSpreadByFrontPage() {
        val from = spreadIndexOf(double, 13)
        val to = spreadIndexOf(double, 81)
        val jump = jumpBackAfterSeek(double, from, to)
        assertEquals(JumpBack(page = 12, shownAt = to), jump)
        val step = jumpBackStep(double, jump!!, to)
        assertEquals(from, step.targetIndex)
        assertEquals(JumpBack(page = 80, shownAt = from), step.next)
    }

    @Test
    fun twoPageNeighbourSpreadIsShort() {
        assertNull(jumpBackAfterSeek(double, spreadIndexOf(double, 12), spreadIndexOf(double, 14)))
    }

    @Test
    fun outOfRangeIndexesShowNothing() {
        assertNull(jumpBackAfterSeek(single, -1, 50))
        assertNull(jumpBackAfterSeek(single, 0, 500))
        assertNull(jumpBackAfterSeek(emptyList(), 0, 5))
    }

    @Test
    fun jb3PageTurnAwayFromShownSpreadHides() {
        val jump = JumpBack(page = 12, shownAt = 79)
        assertFalse(jumpBackHiddenBySettle(jump, 79))
        assertTrue(jumpBackHiddenBySettle(jump, 80))
        assertFalse(jumpBackHiddenBySettle(null, 80))
    }

    @Test
    fun nextBookCardAndSheetsTakePriority() {
        val jump = JumpBack(page = 12, shownAt = 79)
        assertTrue(jumpBackVisible(jump, null, sheetOpen = false))
        assertFalse(jumpBackVisible(jump, EndNotice.NoNextBook, sheetOpen = false))
        assertFalse(jumpBackVisible(jump, null, sheetOpen = true))
        assertFalse(jumpBackVisible(null, null, sheetOpen = false))
    }

    @Test
    fun timingsMatchDesign() {
        assertEquals(5_000L, JUMP_BACK_MILLIS)
        assertEquals(150, END_NOTICE_FADE_MILLIS)
    }
}
