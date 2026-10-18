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
}
