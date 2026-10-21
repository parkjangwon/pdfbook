package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldLayoutTest {
    private val window = IntSize(800, 700)
    private val vFold = IntRect(390, 0, 410, 700)
    private val hFold = IntRect(0, 340, 800, 360)

    @Test
    fun bookHalvesAlignToFold() {
        val halves = bookHalves(window, vFold)
        assertEquals(IntRect(0, 0, 390, 700), halves.first)
        assertEquals(IntRect(410, 0, 800, 700), halves.second)
    }

    @Test
    fun tabletopHalvesAlignToFold() {
        val halves = tabletopHalves(window, hFold)
        assertEquals(IntRect(0, 0, 800, 340), halves.first)
        assertEquals(IntRect(0, 360, 800, 700), halves.second)
    }

    @Test
    fun exclusionZoneExpandsFold() {
        val zone = foldExclusionZone(vFold, marginPx = 12, verticalHinge = true)
        assertEquals(IntRect(378, 0, 422, 700), zone)
        assertTrue(rectsOverlap(zone, IntRect(380, 10, 385, 20)))
        assertFalse(rectsOverlap(zone, IntRect(0, 0, 10, 10)))
    }

    @Test
    fun padWidthsMenuIsPointSixOfSide() {
        val (side, menu, side2) = tabletopPadWidths(available = 260f, gap = 8f)
        assertEquals(side, side2, 0.01f)
        assertEquals(side * 0.6f, menu, 0.01f)
        assertEquals(260f, side + menu + side2 + 16f, 0.01f)
    }

    @Test
    fun bookSingleHalfFollowsDirection() {
        val halves = bookHalves(window, vFold)
        assertEquals(halves.first, bookSingleHalf(halves, ReadingDirection.LTR))
        assertEquals(halves.second, bookSingleHalf(halves, ReadingDirection.RTL))
    }
}
