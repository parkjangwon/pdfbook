package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomPanTest {
    @Test
    fun unscaledResetsToOrigin() {
        assertEquals(0f to 0f, clampZoomOffset(40f, -20f, scale = 1f, 200f, 400f))
    }

    @Test
    fun clampsToScaledBounds() {
        assertEquals(50f to 100f, clampZoomOffset(50f, 100f, scale = 2f, 200f, 400f))
        assertEquals(100f to -200f, clampZoomOffset(999f, -999f, scale = 2f, 200f, 400f))
        assertEquals(-100f to 200f, clampZoomOffset(-999f, 999f, scale = 2f, 200f, 400f))
    }

    @Test
    fun invalidViewportResets() {
        assertEquals(0f to 0f, clampZoomOffset(10f, 10f, scale = 2f, 0f, 400f))
    }

    @Test
    fun pinchLatchIgnoresTinySingleFrameUntilCumulative() {
        val first = PinchLatchState().advance(1.01f)
        assertFalse(first.latched)
        assertEquals(1.01f, first.cumulativeZoom, 0.0001f)
        val second = first.advance(1.02f)
        assertTrue(second.latched)
        assertEquals(1.01f * 1.02f, second.cumulativeZoom, 0.0001f)
    }

    @Test
    fun pinchLatchStaysTrueAfterThreshold() {
        val latched = PinchLatchState().advance(1.05f)
        assertTrue(latched.latched)
        assertTrue(latched.advance(1.001f).latched)
        assertTrue(latched.advance(0.999f).latched)
    }

    @Test
    fun pinchLatchDetectsPinchIn() {
        assertFalse(PinchLatchState().advance(0.99f).latched)
        assertTrue(PinchLatchState().advance(0.97f).latched)
    }

    @Test
    fun pinchClampsBelowMinimum() {
        val zoomed = ZoomState(scale = 2f).applyPinch(0.1f, 100f, 200f, 200f, 400f)
        assertEquals(MinZoomScale, zoomed.scale, 0f)
        assertEquals(0f, zoomed.offsetX, 0f)
        assertEquals(0f, zoomed.offsetY, 0f)
    }

    @Test
    fun pinchClampsAboveMaximum() {
        val zoomed = ZoomState().applyPinch(10f, 100f, 200f, 200f, 400f)
        assertEquals(MaxZoomScale, zoomed.scale, 0f)
    }

    @Test
    fun pinchZoomsAroundCentroid() {
        val zoomed = ZoomState().applyPinch(2f, 150f, 200f, 200f, 400f)
        assertEquals(2f, zoomed.scale, 0f)
        assertEquals(-50f, zoomed.offsetX, 0.01f)
        assertEquals(0f, zoomed.offsetY, 0.01f)
    }

    @Test
    fun panForbiddenAtIdentityScale() {
        val next = ZoomState().applyPan(30f, -20f, 200f, 400f)
        assertEquals(ZoomState(), next)
    }

    @Test
    fun panClampsOffsetToBounds() {
        val next = ZoomState(scale = 2f).applyPan(999f, -999f, 200f, 400f)
        assertEquals(100f, next.offsetX, 0f)
        assertEquals(-200f, next.offsetY, 0f)
    }

    @Test
    fun applyGestureMatchesPinchThenPan() {
        val base = ZoomState(scale = 2f, offsetX = 20f, offsetY = -10f)
        val expected = base
            .applyPinch(1.5f, 100f, 200f, 200f, 400f)
            .applyPan(5f, -5f, 200f, 400f)
        val combined = base.applyGesture(1.5f, 100f, 200f, 5f, -5f, 200f, 400f)
        assertEquals(expected, combined)
    }

    @Test
    fun applyGestureKeepsPinchWhenPanFollows() {
        val base = ZoomState()
        val combined = base.applyGesture(2f, 150f, 200f, 10f, 0f, 200f, 400f)
        val lostUpdate = base.applyPan(10f, 0f, 200f, 400f)
        assertEquals(2f, combined.scale, 0f)
        assertEquals(-40f, combined.offsetX, 0.01f)
        assertEquals(MinZoomScale, lostUpdate.scale, 0f)
        assertEquals(0f, lostUpdate.offsetX, 0f)
    }

    @Test
    fun allowsPageTurnOnlyAtMinimumScale() {
        assertTrue(ZoomState().allowsPageTurn)
        assertFalse(ZoomState(scale = 1.01f).allowsPageTurn)
        assertFalse(ZoomState(scale = MaxZoomScale).allowsPageTurn)
    }

    @Test
    fun doubleTapTogglesZoom() {
        val zoomed = ZoomState().toggleDoubleTap()
        assertEquals(DoubleTapZoomScale, zoomed.scale, 0f)
        assertEquals(ZoomState(), zoomed.toggleDoubleTap())
    }

    @Test
    fun doubleTapFromPartialZoomResets() {
        assertEquals(ZoomState(), ZoomState(scale = 3f, offsetX = 10f, offsetY = -5f).toggleDoubleTap())
    }

    @Test
    fun resetClearsOffset() {
        assertEquals(ZoomState(), ZoomState(scale = 3f, offsetX = 12f, offsetY = -8f).reset())
    }

    @Test
    fun clampToViewportAfterManualOffset() {
        val clamped = ZoomState(scale = 2f, offsetX = 500f, offsetY = -500f)
            .clampToViewport(200f, 400f)
        assertEquals(100f, clamped.offsetX, 0f)
        assertEquals(-200f, clamped.offsetY, 0f)
    }

    @Test
    fun readingStartRtlIsTopRight() {
        val start = ZoomState(2f, 30f, -40f).atReadingStart(ReadingDirection.RTL, 200f, 400f)
        assertEquals(ZoomState(2f, -100f, 200f), start)
    }

    @Test
    fun readingStartLtrIsTopLeft() {
        val start = ZoomState(3f).atReadingStart(ReadingDirection.LTR, 200f, 400f)
        assertEquals(ZoomState(3f, 200f, 400f), start)
    }

    @Test
    fun readingStartAtBaseScaleResets() {
        assertEquals(ZoomState(), ZoomState().atReadingStart(ReadingDirection.RTL, 200f, 400f))
    }

    @Test
    fun afterPageTurnKeepsScaleOnlyWhenEnabled() {
        val zoomed = ZoomState(2f, 10f, 10f)
        assertEquals(ZoomState(), zoomed.afterPageTurn(false, ReadingDirection.RTL, 200f, 400f))
        assertEquals(ZoomState(2f, -100f, 200f), zoomed.afterPageTurn(true, ReadingDirection.RTL, 200f, 400f))
        assertEquals(ZoomState(), ZoomState().afterPageTurn(true, ReadingDirection.LTR, 200f, 400f))
    }
}
