package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class TapZoneWidthTest {
    @Test
    fun defaultIsThirtyThreePercentAndKeepsThirds() {
        assertEquals(33, DEFAULT_TAP_ZONE_SIDE_PERCENT)
        assertEquals(1f / 3f, tapZoneSideFraction(DEFAULT_TAP_ZONE_SIDE_PERCENT), 0f)
    }

    @Test
    fun rangeIsTwentyToFortyFive() {
        assertEquals(20, TAP_ZONE_SIDE_MIN_PERCENT)
        assertEquals(45, TAP_ZONE_SIDE_MAX_PERCENT)
        assertEquals(20, normalizeTapZoneSidePercent(0))
        assertEquals(20, normalizeTapZoneSidePercent(-10))
        assertEquals(45, normalizeTapZoneSidePercent(50))
        assertEquals(45, normalizeTapZoneSidePercent(100))
    }

    @Test
    fun valuesSnapToFivePercentSteps() {
        assertEquals(5, TAP_ZONE_SIDE_STEP_PERCENT)
        assertEquals(listOf(20, 25, 30, 35, 40, 45), (20..45 step 5).map { normalizeTapZoneSidePercent(it) })
        assertEquals(20, snapTapZoneSidePercent(22.4f))
        assertEquals(25, snapTapZoneSidePercent(22.5f))
        assertEquals(35, snapTapZoneSidePercent(33.4f))
        assertEquals(30, snapTapZoneSidePercent(32.4f))
        assertEquals(40, normalizeTapZoneSidePercent(38))
        assertEquals(45, snapTapZoneSidePercent(Float.POSITIVE_INFINITY))
        assertEquals(DEFAULT_TAP_ZONE_SIDE_PERCENT, snapTapZoneSidePercent(Float.NaN))
    }

    @Test
    fun onlyDefaultIsOffTheFivePercentGrid() {
        assertEquals(33, normalizeTapZoneSidePercent(33))
        assertEquals(30, normalizeTapZoneSidePercent(32))
        assertEquals(35, normalizeTapZoneSidePercent(34))
    }

    @Test
    fun fractionFollowsPercent() {
        assertEquals(0.2f, tapZoneSideFraction(20), 1e-6f)
        assertEquals(0.45f, tapZoneSideFraction(45), 1e-6f)
        assertEquals(0.45f, tapZoneSideFraction(90), 1e-6f)
    }

    @Test
    fun widerSidesMoveTapBoundaries() {
        val wide = tapZoneSideFraction(45)
        assertEquals(TapZone.Previous, tapZoneAt(440f, 1000f, ReadingDirection.LTR, wide))
        assertEquals(TapZone.Menu, tapZoneAt(460f, 1000f, ReadingDirection.LTR, wide))
        assertEquals(TapZone.Next, tapZoneAt(560f, 1000f, ReadingDirection.LTR, wide))
        assertEquals(TapZone.Next, tapZoneAt(440f, 1000f, ReadingDirection.RTL, wide))
        assertEquals(TapZone.Previous, tapZoneAt(560f, 1000f, ReadingDirection.RTL, wide))
    }

    @Test
    fun narrowerSidesWidenMenu() {
        val narrow = tapZoneSideFraction(20)
        assertEquals(TapZone.Previous, tapZoneAt(190f, 1000f, ReadingDirection.LTR, narrow))
        assertEquals(TapZone.Menu, tapZoneAt(210f, 1000f, ReadingDirection.LTR, narrow))
        assertEquals(TapZone.Menu, tapZoneAt(790f, 1000f, ReadingDirection.LTR, narrow))
        assertEquals(TapZone.Next, tapZoneAt(810f, 1000f, ReadingDirection.LTR, narrow))
        assertEquals(TapZone.Next, tapZoneAt(190f, 1000f, ReadingDirection.RTL, narrow))
        assertEquals(TapZone.Previous, tapZoneAt(810f, 1000f, ReadingDirection.RTL, narrow))
    }

    @Test
    fun spansStayContiguousAtEveryStep() {
        for (percent in listOf(20, 25, 30, 33, 35, 40, 45)) {
            for (direction in ReadingDirection.entries) {
                val spans = tapZoneSpans(direction, tapZoneSideFraction(percent))
                assertEquals(0f, spans.first().start, 0f)
                assertEquals(spans[0].end, spans[1].start, 0f)
                assertEquals(spans[1].end, spans[2].start, 0f)
                assertEquals(1f, spans.last().end, 0f)
                assertEquals(spans[0].width, spans[2].width, 1e-6f)
            }
        }
    }
}
