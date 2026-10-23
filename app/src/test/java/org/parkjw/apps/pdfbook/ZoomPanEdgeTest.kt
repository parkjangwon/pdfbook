package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomPanEdgeTest {
    private val w = 200f
    private val h = 400f

    @Test
    fun boundsGrowWithScale() {
        assertEquals(300f to -600f, clampZoomOffset(999f, -999f, 4f, w, h))
        assertEquals(50f to -100f, clampZoomOffset(60f, -120f, 1.5f, w, h))
        val (x, y) = clampZoomOffset(50f, 50f, 1.01f, w, h)
        assertEquals(1f, x, 0.01f)
        assertEquals(2f, y, 0.01f)
    }

    @Test
    fun subMinimumScaleOrBadViewportResets() {
        assertEquals(0f to 0f, clampZoomOffset(10f, 10f, 0.5f, w, h))
        assertEquals(0f to 0f, clampZoomOffset(10f, 10f, 2f, -1f, h))
        assertEquals(0f to 0f, clampZoomOffset(10f, 10f, 2f, w, 0f))
    }

    @Test
    fun pinchAtLimitsOnlyClamps() {
        val max = ZoomState(MaxZoomScale, 300f, 600f)
        assertEquals(max, max.applyPinch(2f, 0f, 0f, w, h))
        assertEquals(ZoomState(), ZoomState().applyPinch(0.5f, 10f, 10f, w, h))
    }

    @Test
    fun zoomOutKeepsOffsetInsideNewBounds() {
        val out = ZoomState(MaxZoomScale, 300f, 600f).applyPinch(0.5f, 100f, 200f, w, h)
        assertEquals(ZoomState(2f, 100f, 200f), out)
    }

    @Test
    fun pinchAtCornerAnchorsThatCorner() {
        assertEquals(ZoomState(2f, 100f, 200f), ZoomState().applyPinch(2f, 0f, 0f, w, h))
        assertEquals(ZoomState(2f, -100f, -200f), ZoomState().applyPinch(2f, w, h, w, h))
    }

    @Test
    fun repeatedPinchAtCenterReachesMaxWithoutDrift() {
        val z = ZoomState().applyPinch(2f, 100f, 200f, w, h).applyPinch(3f, 100f, 200f, w, h)
        assertEquals(MaxZoomScale, z.scale, 0f)
        assertEquals(0f, z.offsetX, 0.01f)
        assertEquals(0f, z.offsetY, 0.01f)
    }

    @Test
    fun panStaysAtEdgesAndReverses() {
        val edge = ZoomState(2f, 100f, 200f)
        assertEquals(edge, edge.applyPan(10f, 10f, w, h))
        assertEquals(-100f, edge.applyPan(-250f, 0f, w, h).offsetX, 0f)
        assertEquals(
            ZoomState(MaxZoomScale, 300f, -600f),
            ZoomState(MaxZoomScale).applyPan(500f, -700f, w, h),
        )
    }

    @Test
    fun panWithoutViewportCollapsesOffsetButKeepsScale() {
        assertEquals(ZoomState(2f), ZoomState(2f, 5f, 5f).applyPan(1f, 1f, 0f, 0f))
    }

    @Test
    fun gestureWithoutZoomChangeAtMinimumIsNoOp() {
        assertEquals(ZoomState(), ZoomState().applyGesture(1f, 50f, 50f, 40f, 40f, w, h))
    }

    @Test
    fun gestureThatPinchesToMinimumDropsItsPan() {
        assertEquals(ZoomState(), ZoomState(2f).applyGesture(0.5f, 100f, 200f, 50f, 50f, w, h))
    }

    @Test
    fun doubleTapTargetIsClampedAndCentered() {
        assertEquals(MaxZoomScale, ZoomState().toggleDoubleTap(10f).scale, 0f)
        assertEquals(ZoomState(), ZoomState().toggleDoubleTap(0.5f))
        assertEquals(ZoomState(), ZoomState(scale = 1.001f).toggleDoubleTap())
        val zoomed = ZoomState().toggleDoubleTap()
        assertEquals(0f, zoomed.offsetX, 0f)
        assertEquals(0f, zoomed.offsetY, 0f)
    }

    @Test
    fun belowMinimumScaleStillAllowsPageTurn() {
        assertTrue(ZoomState(scale = 0.5f).allowsPageTurn)
        assertFalse(ZoomState(scale = 1.0001f).allowsPageTurn)
    }

    @Test
    fun readingStartScalesWithZoomAndHandlesNoViewport() {
        assertEquals(
            ZoomState(MaxZoomScale, -300f, 600f),
            ZoomState(MaxZoomScale).atReadingStart(ReadingDirection.RTL, w, h),
        )
        assertEquals(ZoomState(2f), ZoomState(2f).atReadingStart(ReadingDirection.LTR, 0f, 0f))
        assertEquals(
            ZoomState(2f, 100f, 200f),
            ZoomState(2f).afterPageTurn(true, ReadingDirection.LTR, w, h),
        )
    }

    @Test
    fun pinchLatchStaysLatchedAfterReturningToStart() {
        val back = PinchLatchState().advance(1.5f).advance(1f / 1.5f)
        assertTrue(back.latched)
        assertEquals(1f, back.cumulativeZoom, 0.0001f)
    }

    @Test
    fun pinchLatchHonoursCustomThresholdAndIgnoresNoChange() {
        assertFalse(PinchLatchState().advance(1.5f, threshold = 0.6f).latched)
        assertFalse(PinchLatchState().advance(1f).advance(1f).latched)
    }
}
