package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldEdgeTest {
    private val window = IntSize(800, 700)

    @Test
    fun halvesClampFoldOutsideWindow() {
        val book = bookHalves(window, IntRect(-20, 0, 900, 700))
        assertEquals(IntRect(0, 0, 0, 700), book.first)
        assertEquals(IntRect(800, 0, 800, 700), book.second)
        val table = tabletopHalves(window, IntRect(0, -5, 800, 900))
        assertEquals(IntRect(0, 0, 800, 0), table.first)
        assertEquals(IntRect(0, 700, 800, 700), table.second)
    }

    @Test
    fun zeroWidthFoldSplitsWindowExactly() {
        val halves = bookHalves(window, IntRect(400, 0, 400, 700))
        assertEquals(IntRect(0, 0, 400, 700), halves.first)
        assertEquals(IntRect(400, 0, 800, 700), halves.second)
    }

    @Test
    fun exclusionZoneClampsAtOriginOnly() {
        assertEquals(IntRect(0, 0, 25, 700), foldExclusionZone(IntRect(5, 0, 13, 700), 12, true))
        assertEquals(IntRect(0, 328, 800, 372), foldExclusionZone(IntRect(0, 340, 800, 360), 12, false))
        assertEquals(IntRect(0, 0, 800, 372), foldExclusionZone(IntRect(0, 5, 800, 360), 12, false))
        assertEquals(IntRect(788, 0, 822, 700), foldExclusionZone(IntRect(800, 0, 810, 700), 12, true))
        val fold = IntRect(390, 0, 410, 700)
        assertEquals(fold, foldExclusionZone(fold, 0, true))
    }

    @Test
    fun touchingRectsDoNotOverlap() {
        val a = IntRect(0, 0, 10, 10)
        assertFalse(rectsOverlap(a, IntRect(10, 0, 20, 10)))
        assertFalse(rectsOverlap(a, IntRect(0, 10, 10, 20)))
        assertTrue(rectsOverlap(a, a))
        assertTrue(rectsOverlap(a, IntRect(2, 2, 4, 4)))
    }

    @Test
    fun padWidthsKeepInvariantAndClampWhenTooNarrow() {
        listOf(260f to 8f, 100f to 0f, 1000f to 24f).forEach { (avail, gap) ->
            val (s, m, s2) = tabletopPadWidths(avail, gap)
            assertEquals(avail, s + m + s2 + 2f * gap, 0.01f)
        }
        assertEquals(Triple(0f, 0f, 0f), tabletopPadWidths(10f, 8f))
        assertEquals(Triple(0f, 0f, 0f), tabletopPadWidths(0f, 0f))
        val (side, menu, _) = tabletopPadWidths(90f, 0f, menuRatio = 0f)
        assertEquals(45f, side, 0.01f)
        assertEquals(0f, menu, 0f)
    }

    @Test
    fun spreadGapEqualsFold() {
        val fold = IntRect(390, 0, 410, 700)
        assertEquals(fold, bookSpreadGap(fold))
    }

    @Test
    fun postureUsesFirstHalfOpenedFeatureOnly() {
        val posture = postureOf(
            listOf(
                FoldFeatureInput(false, false, 0, 0, 1, 1),
                FoldFeatureInput(true, true, 400, 0, 420, 800),
                FoldFeatureInput(true, false, 0, 100, 200, 120),
            ),
        )
        assertEquals(FoldPosture.Book(IntRect(400, 0, 420, 800)), posture)
    }
}
