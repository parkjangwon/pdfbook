package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageFitTest {
    private val screenW = 2400f
    private val screenH = 1080f
    private val widthFitContentW = 2400f
    private val widthFitContentH = 3600f

    private val portraitW = 1080f
    private val portraitH = 2400f
    private val heightFitContentW = 1600f
    private val heightFitContentH = 2400f

    @Test
    fun screenFitNeverOverflows() {
        assertFalse(pageFitOverflows(PageFit.Screen, screenW, screenH, widthFitContentW, widthFitContentH))
        assertEquals(0f to 0f, pageFitMaxOffset(PageFit.Screen, screenW, screenH, widthFitContentW, widthFitContentH))
        assertEquals(1f, pageFitBaseScale(PageFit.Screen, screenW, screenH, 800f, 600f))
        assertEquals(100 to 200, pageFitRenderSize(PageFit.Screen, 100, 200))
    }

    @Test
    fun twoPageFallsBackToScreen() {
        assertEquals(PageFit.Screen, effectivePageFit(PageFit.Width, twoPage = true))
        assertEquals(PageFit.Height, effectivePageFit(PageFit.Height, twoPage = false))
    }

    @Test
    fun widthFitOverflowsOnlyVertically() {
        assertTrue(pageFitOverflows(PageFit.Width, screenW, screenH, widthFitContentW, widthFitContentH))
        assertFalse(pageFitOverflows(PageFit.Width, screenW, screenH, widthFitContentW, screenH))
        assertFalse(pageFitOverflows(PageFit.Height, screenW, screenH, widthFitContentW, widthFitContentH))
    }

    @Test
    fun fitRenderSizeLeavesOneAxisOpen() {
        assertEquals(2400 to 7200, pageFitRenderSize(PageFit.Width, 2400, 1080))
        assertEquals(7200 to 2400, pageFitRenderSize(PageFit.Height, 1080, 2400))
    }

    @Test
    fun baseScaleUndoesTheFitShrink() {
        assertEquals(3600f / 1080f, pageFitBaseScale(PageFit.Width, screenW, screenH, widthFitContentW, widthFitContentH))
        assertEquals(1600f / 1080f, pageFitBaseScale(PageFit.Height, portraitW, portraitH, heightFitContentW, heightFitContentH))
    }

    @Test
    fun croppedWidthFitFillsTheScreenWidth() {
        val viewportW = 1000f
        val viewportH = 800f
        val croppedW = 900f
        val croppedH = 2000f
        val (drawnW, drawnH) = pageFitDrawnSize(PageFit.Width, viewportW, viewportH, croppedW, croppedH)
        assertEquals(viewportW, drawnW, 0.5f)
        assertTrue(drawnH > viewportH)
        val (maxX, maxY) = pageFitMaxOffset(PageFit.Width, viewportW, viewportH, drawnW, drawnH)
        assertEquals(0f, maxX)
        assertEquals((drawnH - viewportH) / 2f, maxY, 0.5f)
        // The layer times ContentScale.Fit reaches one drawn pixel per cropped bitmap pixel.
        val layer = pageFitBaseScale(PageFit.Width, viewportW, viewportH, croppedW, croppedH)
        val inner = minOf(viewportW / croppedW, viewportH / croppedH)
        assertEquals(drawnW / croppedW, inner * layer, 0.001f)
    }

    @Test
    fun croppedHeightFitFillsTheScreenHeight() {
        val viewportW = 1080f
        val viewportH = 2400f
        val croppedW = 1500f
        val croppedH = 2200f
        val (drawnW, drawnH) = pageFitDrawnSize(PageFit.Height, viewportW, viewportH, croppedW, croppedH)
        assertEquals(viewportH, drawnH, 0.5f)
        assertTrue(drawnW > viewportW)
        val (maxX, maxY) = pageFitMaxOffset(PageFit.Height, viewportW, viewportH, drawnW, drawnH)
        assertEquals((drawnW - viewportW) / 2f, maxX, 0.5f)
        assertEquals(0f, maxY)
    }

    @Test
    fun uncroppedDrawnSizeIsTheBitmapFromTheSlot() {
        assertEquals(2400f to 3600f, pageFitDrawnSize(PageFit.Width, screenW, screenH, 2400f, 3600f))
        assertEquals(1600f to 2400f, pageFitDrawnSize(PageFit.Height, portraitW, portraitH, 1600f, 2400f))
    }

    @Test
    fun widthFitStartsAndEndsAtTheTopAndBottom() {
        val start = pageFitStartState(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH)
        assertEquals(0f, start.offsetX)
        assertEquals(1260f, start.offsetY)
        val end = pageFitEndState(PageFit.Width, ReadingDirection.RTL, portraitW, portraitH, widthFitContentW, widthFitContentH)
        assertEquals(-600f, end.offsetY)
    }

    @Test
    fun heightFitStartsAtTheReadingDirectionSide() {
        val rtl = pageFitStartState(PageFit.Height, ReadingDirection.RTL, portraitW, portraitH, heightFitContentW, heightFitContentH)
        assertEquals(-260f, rtl.offsetX)
        assertEquals(0f, rtl.offsetY)
        val ltr = pageFitStartState(PageFit.Height, ReadingDirection.LTR, portraitW, portraitH, heightFitContentW, heightFitContentH)
        assertEquals(260f, ltr.offsetX)
        val rtlEnd = pageFitEndState(PageFit.Height, ReadingDirection.RTL, portraitW, portraitH, heightFitContentW, heightFitContentH)
        assertEquals(260f, rtlEnd.offsetX)
    }

    @Test
    fun widthFitMovesOneScreenThenStops() {
        var state = pageFitStartState(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH)
        state = state.scrollWithinPage(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH, forward = true)
        assertEquals(288f, state.offsetY)
        state = state.scrollWithinPage(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH, forward = true)
        assertEquals(-684f, state.offsetY)
        state = state.scrollWithinPage(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH, forward = true)
        assertEquals(-1260f, state.offsetY)
        val pastEnd = state.scrollWithinPage(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH, forward = true)
        assertEquals(state, pastEnd)
    }

    @Test
    fun backwardAtStartStaysPutAndTurnsThePage() {
        val start = pageFitStartState(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH)
        assertEquals(
            start,
            start.scrollWithinPage(PageFit.Width, ReadingDirection.RTL, screenW, screenH, widthFitContentW, widthFitContentH, forward = false),
        )
    }

    @Test
    fun heightFitScrollsTowardTheReadingDirection() {
        val rtlStart = pageFitStartState(PageFit.Height, ReadingDirection.RTL, portraitW, portraitH, heightFitContentW, heightFitContentH)
        val rtlNext = rtlStart.scrollWithinPage(PageFit.Height, ReadingDirection.RTL, portraitW, portraitH, heightFitContentW, heightFitContentH, forward = true)
        assertEquals(260f, rtlNext.offsetX)
        val ltrStart = pageFitStartState(PageFit.Height, ReadingDirection.LTR, portraitW, portraitH, heightFitContentW, heightFitContentH)
        val ltrNext = ltrStart.scrollWithinPage(PageFit.Height, ReadingDirection.LTR, portraitW, portraitH, heightFitContentW, heightFitContentH, forward = true)
        assertEquals(-260f, ltrNext.offsetX)
    }

    @Test
    fun fitDecodeFallsBackToScreen() {
        assertEquals(PageFit.Width, pageFitOrDefault("Width"))
        assertEquals(PageFit.Screen, pageFitOrDefault(null))
        assertEquals(PageFit.Screen, pageFitOrDefault("Original"))
    }
}
