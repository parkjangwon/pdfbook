package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpreadCropScaleTest {
    @Test
    fun bothHalvesGetTheSameScaleEvenWithDifferentOuterMargins() {
        val left = SpreadPageMetrics(
            width = 1000,
            height = 1400,
            crop = CropMargins(left = 0.10f, top = 0.05f, right = 0f, bottom = 0.05f),
        )
        val right = SpreadPageMetrics(
            width = 1000,
            height = 1400,
            crop = CropMargins(left = 0f, top = 0.05f, right = 0.20f, bottom = 0.05f),
        )
        val scale = spreadCropScale(listOf(left, right), listOf(600, 600), 1200)
        assertEquals(minOf(600f / 900f, 600f / 800f, 1200f / 1260f), scale)
        // Drawn heights stay equal even though the cropped widths differ.
        assertEquals(left.croppedHeight() * scale!!, right.croppedHeight() * scale, 0.001f)
        assertEquals(840f, left.croppedHeight() * scale, 0.5f)
    }

    @Test
    fun noScaleUntilBothPagesAreMeasured() {
        val left = SpreadPageMetrics(1000, 1400, CropMargins(left = 0.1f))
        assertNull(spreadCropScale(listOf(left, null), listOf(600, 600), 1200))
        assertNull(spreadCropScale(listOf(left), listOf(600), 1200))
    }

    @Test
    fun rtlSpreadLayoutKeepsTheVisualLeftFirst() {
        assertEquals(listOf(3, 2), spreadLayout(listOf(2, 3), ReadingDirection.RTL))
        val margins = alignedTwoPageMargins(
            CropMargins(left = 0.10f, top = 0.05f),
            CropMargins(right = 0.20f, top = 0.05f),
        )
        // layout[0] is the visually left page in both directions: it crops the left half and the
        // right page crops the right half. Both share the smaller outer margin, so the halves are
        // the same width and neither page loses content.
        assertEquals(0.10f, margins.first.left)
        assertEquals(0f, margins.first.right)
        assertEquals(0f, margins.second.left)
        assertEquals(0.10f, margins.second.right)
    }
}
