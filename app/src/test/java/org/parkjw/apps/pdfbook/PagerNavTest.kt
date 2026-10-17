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

    @Test
    fun ltrSliderMapsEnds() {
        assertEquals(0f, pageToSlider(1, 7, ReadingDirection.LTR))
        assertEquals(6f, pageToSlider(7, 7, ReadingDirection.LTR))
        assertEquals(1, sliderToPage(0f, 7, ReadingDirection.LTR))
        assertEquals(7, sliderToPage(6f, 7, ReadingDirection.LTR))
        assertEquals(4, sliderToPage(3f, 7, ReadingDirection.LTR))
    }

    @Test
    fun rtlSliderReverses() {
        assertEquals(6f, pageToSlider(1, 7, ReadingDirection.RTL))
        assertEquals(0f, pageToSlider(7, 7, ReadingDirection.RTL))
        assertEquals(1, sliderToPage(6f, 7, ReadingDirection.RTL))
        assertEquals(7, sliderToPage(0f, 7, ReadingDirection.RTL))
        assertEquals(4, sliderToPage(3f, 7, ReadingDirection.RTL))
    }

    @Test
    fun sliderRoundTrip() {
        for (page in 1..7) {
            val ltr = pageToSlider(page, 7, ReadingDirection.LTR)
            assertEquals(page, sliderToPage(ltr, 7, ReadingDirection.LTR))
            val rtl = pageToSlider(page, 7, ReadingDirection.RTL)
            assertEquals(page, sliderToPage(rtl, 7, ReadingDirection.RTL))
        }
    }

    @Test
    fun togglesDirection() {
        assertEquals(ReadingDirection.LTR, toggledDirection(ReadingDirection.RTL))
        assertEquals(ReadingDirection.RTL, toggledDirection(ReadingDirection.LTR))
    }

    @Test
    fun newBookUsesDefaultDirection() {
        assertEquals(ReadingDirection.LTR, resolveReadingDirection(null, ReadingDirection.LTR))
        assertEquals(ReadingDirection.RTL, resolveReadingDirection(null, ReadingDirection.RTL))
    }

    @Test
    fun existingBookKeepsSavedDirection() {
        assertEquals(
            ReadingDirection.RTL,
            resolveReadingDirection(ReadingDirection.RTL, ReadingDirection.LTR),
        )
        assertEquals(
            ReadingDirection.LTR,
            resolveReadingDirection(ReadingDirection.LTR, ReadingDirection.RTL),
        )
    }
}
