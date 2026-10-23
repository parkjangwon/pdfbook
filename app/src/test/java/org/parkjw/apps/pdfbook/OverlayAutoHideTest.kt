package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayAutoHideTest {
    @Test
    fun overlayOpeningStartsFullCountdown() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(1_000L)
        assertEquals(
            OVERLAY_AUTO_HIDE_MILLIS,
            timer.nextDelay(1_000L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
    }

    @Test
    fun longerRecommendedTimeoutDelaysHiding() {
        val timer = OverlayAutoHideTimer(timeoutMillis = { 120_000L })
        timer.onInteraction(0L)
        assertEquals(
            120_000L,
            timer.nextDelay(0L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
        assertEquals(
            70_000L,
            timer.nextDelay(50_000L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
        assertEquals(
            0L,
            timer.nextDelay(120_000L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
    }

    @Test
    fun interactionResetsCountdown() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        timer.onInteraction(3_500L)
        assertEquals(
            OVERLAY_AUTO_HIDE_MILLIS,
            timer.nextDelay(3_500L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
        assertEquals(
            1_000L,
            timer.nextDelay(6_500L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
        assertEquals(
            0L,
            timer.nextDelay(9_000L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
    }

    @Test
    fun pausedWhileSliderDragging() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(
            timer.nextDelay(0L, sheetOpen = false, sliderDragging = true, touchExploration = false),
        )
    }

    @Test
    fun pausedWhileSheetOpen() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(
            timer.nextDelay(0L, sheetOpen = true, sliderDragging = false, touchExploration = false),
        )
    }

    @Test
    fun pausedWhileTouchExplorationEnabled() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(
            timer.nextDelay(0L, sheetOpen = false, sliderDragging = false, touchExploration = true),
        )
    }

    @Test
    fun pausedWhilePointerHeld() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(
            timer.nextDelay(
                0L,
                sheetOpen = false,
                sliderDragging = false,
                touchExploration = false,
                pointerPressed = true,
            ),
        )
    }

    @Test
    fun pausedWhileNotResumed() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(
            timer.nextDelay(
                0L,
                sheetOpen = false,
                sliderDragging = false,
                touchExploration = false,
                resumed = false,
            ),
        )
    }

    @Test
    fun pauseEndingRestartsTheCountdown() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(1_000L)
        assertNull(
            timer.nextDelay(2_000L, sheetOpen = true, sliderDragging = false, touchExploration = false),
        )
        // The screen restarts the countdown once the pause condition goes away.
        timer.onInteraction(9_000L)
        assertEquals(
            OVERLAY_AUTO_HIDE_MILLIS,
            timer.nextDelay(9_000L, sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
    }
}
