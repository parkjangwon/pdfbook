package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneFingerZoomTest {
    private fun detector(
        slop: Float = 10f,
        doubleTapMillis: Long = 300L,
        holdMillis: Long = 500L,
    ) = OneFingerZoomDetector(
        doubleTapMillis = doubleTapMillis,
        holdMillis = holdMillis,
        touchSlop = slop,
    )

    @Test
    fun singleTapFiresOnlyAfterDoubleTapWindow() {
        val d = detector()
        assertEquals(emptyList<OneFingerZoomCommand>(), d.down(100f, 100f))
        assertEquals(emptyList<OneFingerZoomCommand>(), d.up(100f, 100f))
        assertEquals(OneFingerZoomPhase.AwaitSecondDown, d.phase)
        assertEquals(listOf(OneFingerZoomCommand.Tap), d.timeout())
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun quickDoubleTapEmitsDoubleTap() {
        val d = detector()
        d.down(100f, 100f)
        d.up(100f, 100f)
        assertEquals(emptyList<OneFingerZoomCommand>(), d.down(102f, 98f))
        assertEquals(OneFingerZoomPhase.SecondDown, d.phase)
        assertEquals(listOf(OneFingerZoomCommand.DoubleTap), d.up(102f, 98f))
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun secondTapHeldEmitsHoldStartNotDoubleTap() {
        val d = detector()
        d.down(100f, 100f)
        d.up(100f, 100f)
        d.down(120f, 140f)
        assertEquals(listOf(OneFingerZoomCommand.HoldStart(120f, 140f)), d.timeout())
        assertEquals(OneFingerZoomPhase.Holding, d.phase)
        assertEquals(listOf(OneFingerZoomCommand.HoldEnd), d.up(120f, 140f))
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun heldFingerPansAndReleaseEndsHold() {
        val d = detector()
        d.down(100f, 100f)
        d.up(100f, 100f)
        d.down(100f, 100f)
        d.timeout()
        assertEquals(listOf(OneFingerZoomCommand.HoldPan(30f, -10f)), d.move(130f, 90f))
        assertEquals(listOf(OneFingerZoomCommand.HoldPan(10f, 5f)), d.move(140f, 95f))
        assertEquals(listOf(OneFingerZoomCommand.HoldEnd), d.up(140f, 95f))
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun holdKeepsJitterUnderSlop() {
        val d = detector(slop = 10f)
        d.down(100f, 100f)
        d.up(100f, 100f)
        d.down(100f, 100f)
        assertEquals(emptyList<OneFingerZoomCommand>(), d.move(104f, 103f))
        assertEquals(OneFingerZoomPhase.SecondDown, d.phase)
        assertEquals(listOf(OneFingerZoomCommand.HoldStart(104f, 103f)), d.timeout())
    }

    @Test
    fun longFirstPressTapsOnReleaseAndNeverStartsHold() {
        val d = detector()
        d.down(100f, 100f)
        assertEquals(emptyList<OneFingerZoomCommand>(), d.timeout())
        assertEquals(OneFingerZoomPhase.FirstDown, d.phase)
        assertNull(d.timeoutMillis())
        assertEquals(listOf(OneFingerZoomCommand.Tap), d.up(100f, 100f))
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun firstTapDragCancelsTapAndZoom() {
        val d = detector(slop = 10f)
        d.down(100f, 100f)
        d.move(140f, 100f)
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
        assertEquals(emptyList<OneFingerZoomCommand>(), d.timeout())
        assertEquals(emptyList<OneFingerZoomCommand>(), d.up(140f, 100f))
    }

    @Test
    fun secondTapDragCancelsZoom() {
        val d = detector(slop = 10f)
        d.down(100f, 100f)
        d.up(100f, 100f)
        d.down(100f, 100f)
        d.move(100f, 160f)
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
        assertEquals(emptyList<OneFingerZoomCommand>(), d.timeout())
        assertEquals(emptyList<OneFingerZoomCommand>(), d.up(100f, 160f))
    }

    @Test
    fun cancelWhileHoldingEndsHold() {
        val d = detector()
        d.down(100f, 100f)
        d.up(100f, 100f)
        d.down(100f, 100f)
        d.timeout()
        assertEquals(listOf(OneFingerZoomCommand.HoldEnd), d.cancel())
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun cancelBeforeHoldEmitsNothing() {
        val d = detector()
        d.down(100f, 100f)
        d.up(100f, 100f)
        d.down(100f, 100f)
        assertEquals(emptyList<OneFingerZoomCommand>(), d.cancel())
        assertEquals(OneFingerZoomPhase.Idle, d.phase)
    }

    @Test
    fun timeoutUsesPhaseDuration() {
        val d = detector(doubleTapMillis = 280L, holdMillis = 420L)
        assertNull(d.timeoutMillis())
        d.down(0f, 0f)
        assertEquals(420L, d.timeoutMillis())
        d.up(0f, 0f)
        assertEquals(280L, d.timeoutMillis())
        d.down(0f, 0f)
        assertEquals(420L, d.timeoutMillis())
        d.timeout()
        assertNull(d.timeoutMillis())
    }

    @Test
    fun holdZoomUsesDoubleTapScaleAroundHeldPoint() {
        val zoomed = oneFingerZoomHoldStart(ZoomState(), 150f, 200f, 200f, 400f)
        assertEquals(DoubleTapZoomScale, zoomed.scale, 0f)
        assertEquals(-50f, zoomed.offsetX, 0.01f)
        assertEquals(0f, zoomed.offsetY, 0.01f)
    }

    @Test
    fun holdZoomKeepsExistingZoomAsBase() {
        val base = ZoomState(scale = 3f, offsetX = 12f, offsetY = -8f)
        assertEquals(base, oneFingerZoomHoldStart(base, 10f, 20f, 200f, 400f))
    }
}
