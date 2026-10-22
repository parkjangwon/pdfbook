package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayAutoHideTest {
    @Test
    fun expiryIsTimeoutAfterLastInteraction() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(1_000L)
        assertEquals(
            1_000L + OVERLAY_AUTO_HIDE_MILLIS,
            timer.update(sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
    }

    @Test
    fun interactionResetsCountdown() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        timer.onInteraction(3_500L)
        timer.update(sheetOpen = false, sliderDragging = false, touchExploration = false)
        assertFalse(timer.expiredAt(7_499L))
        assertTrue(timer.expiredAt(7_500L))
    }

    @Test
    fun pausedWhileSliderDragging() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(timer.update(sheetOpen = false, sliderDragging = true, touchExploration = false))
        assertFalse(timer.expiredAt(100_000L))
    }

    @Test
    fun pausedWhileSheetOpen() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(timer.update(sheetOpen = true, sliderDragging = false, touchExploration = false))
        assertFalse(timer.expiredAt(100_000L))
    }

    @Test
    fun pausedWhileTouchExplorationEnabled() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(0L)
        assertNull(timer.update(sheetOpen = false, sliderDragging = false, touchExploration = true))
        assertFalse(timer.expiredAt(100_000L))
    }

    @Test
    fun resumesAfterPauseEnds() {
        val timer = OverlayAutoHideTimer()
        timer.onInteraction(1_000L)
        assertNull(timer.update(sheetOpen = true, sliderDragging = false, touchExploration = false))
        assertEquals(
            1_000L + OVERLAY_AUTO_HIDE_MILLIS,
            timer.update(sheetOpen = false, sliderDragging = false, touchExploration = false),
        )
    }
}
