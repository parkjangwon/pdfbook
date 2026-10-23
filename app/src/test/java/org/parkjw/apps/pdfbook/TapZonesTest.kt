package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class TapZonesTest {
    @Test
    fun ltrOrdersPreviousMenuNextFromLeft() {
        val zones = tapZoneSpans(ReadingDirection.LTR).map { it.zone }
        assertEquals(listOf(TapZone.Previous, TapZone.Menu, TapZone.Next), zones)
    }

    @Test
    fun rtlOrdersNextMenuPreviousFromLeft() {
        val zones = tapZoneSpans(ReadingDirection.RTL).map { it.zone }
        assertEquals(listOf(TapZone.Next, TapZone.Menu, TapZone.Previous), zones)
    }

    @Test
    fun defaultSpansSplitWidthIntoThirds() {
        val spans = tapZoneSpans(ReadingDirection.LTR)
        assertEquals(0f, spans[0].start, 0f)
        assertEquals(1f / 3f, spans[0].end, 1e-6f)
        assertEquals(2f / 3f, spans[2].start, 1e-6f)
        assertEquals(1f, spans[2].end, 0f)
        assertEquals(1f, spans.sumOf { it.width.toDouble() }.toFloat(), 1e-6f)
    }

    @Test
    fun ltrTapPositionsMapToZones() {
        assertEquals(TapZone.Previous, tapZoneAt(0f, 900f, ReadingDirection.LTR))
        assertEquals(TapZone.Previous, tapZoneAt(299f, 900f, ReadingDirection.LTR))
        assertEquals(TapZone.Menu, tapZoneAt(450f, 900f, ReadingDirection.LTR))
        assertEquals(TapZone.Next, tapZoneAt(601f, 900f, ReadingDirection.LTR))
        assertEquals(TapZone.Next, tapZoneAt(900f, 900f, ReadingDirection.LTR))
    }

    @Test
    fun rtlTapPositionsFlipPreviousAndNext() {
        assertEquals(TapZone.Next, tapZoneAt(10f, 900f, ReadingDirection.RTL))
        assertEquals(TapZone.Menu, tapZoneAt(450f, 900f, ReadingDirection.RTL))
        assertEquals(TapZone.Previous, tapZoneAt(890f, 900f, ReadingDirection.RTL))
    }

    @Test
    fun outOfBoundsTapsClampToEdgeZones() {
        assertEquals(TapZone.Previous, tapZoneAt(-50f, 900f, ReadingDirection.LTR))
        assertEquals(TapZone.Next, tapZoneAt(1200f, 900f, ReadingDirection.LTR))
        assertEquals(TapZone.Menu, tapZoneAt(10f, 0f, ReadingDirection.LTR))
    }

    @Test
    fun zonesTurnPagesInReadingOrder() {
        assertEquals(-1, TapZone.Previous.pageDelta)
        assertEquals(0, TapZone.Menu.pageDelta)
        assertEquals(1, TapZone.Next.pageDelta)
    }

    @Test
    fun snapClampsRoundsAndHandlesNaN() {
        assertEquals(DEFAULT_TAP_ZONE_SIDE_PERCENT, snapTapZoneSidePercent(Float.NaN))
        assertEquals(20, snapTapZoneSidePercent(-5f))
        assertEquals(45, snapTapZoneSidePercent(Float.POSITIVE_INFINITY))
        assertEquals(35, snapTapZoneSidePercent(33f))
        assertEquals(30, snapTapZoneSidePercent(28f))
    }

    @Test
    fun normalizeKeepsDefaultAndSnapsOthers() {
        assertEquals(33, normalizeTapZoneSidePercent(33))
        assertEquals(20, normalizeTapZoneSidePercent(0))
        assertEquals(45, normalizeTapZoneSidePercent(100))
        assertEquals(1f / 3f, tapZoneSideFraction(33), 0f)
        assertEquals(0.25f, tapZoneSideFraction(25), 0f)
        assertEquals(0.45f, tapZoneSideFraction(99), 0f)
    }

    @Test
    fun sideFractionIsCoercedAndBoundariesBelongToNextZone() {
        val wide = tapZoneSpans(ReadingDirection.LTR, 0.9f)
        assertEquals(0.5f, wide[0].end, 0f)
        assertEquals(0f, wide[1].width, 0f)
        assertEquals(TapZone.Next, tapZoneAt(450f, 900f, ReadingDirection.LTR, 0.9f))
        assertEquals(TapZone.Menu, tapZoneAt(450f, 900f, ReadingDirection.LTR, 0.25f))
        assertEquals(TapZone.Menu, tapZoneAt(225f, 900f, ReadingDirection.LTR, 0.25f))
        assertEquals(TapZone.Next, tapZoneAt(675f, 900f, ReadingDirection.LTR, 0.25f))
        assertEquals(TapZone.Menu, tapZoneAt(450f, 900f, ReadingDirection.LTR, -1f))
        assertEquals(TapZone.Menu, tapZoneAt(10f, -5f, ReadingDirection.RTL))
    }

    @Test
    fun onlyMenuWaitsForDoubleTapAtBaseScale() {
        assertEquals(false, TapZone.Previous.acceptsDoubleTapAtBaseScale)
        assertEquals(true, TapZone.Menu.acceptsDoubleTapAtBaseScale)
        assertEquals(false, TapZone.Next.acceptsDoubleTapAtBaseScale)
    }
}
