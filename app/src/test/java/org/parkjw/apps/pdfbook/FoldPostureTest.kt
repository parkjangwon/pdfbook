package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldPostureTest {
    @Test
    fun emptyOrFlatIsFlat() {
        assertEquals(FoldPosture.Flat, postureOf(emptyList()))
        assertEquals(
            FoldPosture.Flat,
            postureOf(listOf(FoldFeatureInput(halfOpened = false, verticalHinge = true, 0, 0, 10, 100))),
        )
    }

    @Test
    fun halfOpenedVerticalIsBook() {
        val posture = postureOf(
            listOf(FoldFeatureInput(halfOpened = true, verticalHinge = true, 400, 0, 420, 800)),
        )
        assertTrue(posture is FoldPosture.Book)
        assertEquals(IntRect(400, 0, 420, 800), (posture as FoldPosture.Book).foldBounds)
    }

    @Test
    fun halfOpenedHorizontalIsTabletop() {
        val posture = postureOf(
            listOf(FoldFeatureInput(halfOpened = true, verticalHinge = false, 0, 340, 800, 360)),
        )
        assertTrue(posture is FoldPosture.Tabletop)
        assertEquals(IntRect(0, 340, 800, 360), (posture as FoldPosture.Tabletop).foldBounds)
    }

    @Test
    fun firstHalfOpenedWins() {
        val posture = postureOf(
            listOf(
                FoldFeatureInput(false, true, 0, 0, 1, 1),
                FoldFeatureInput(true, false, 0, 100, 200, 120),
            ),
        )
        assertTrue(posture is FoldPosture.Tabletop)
    }
}
