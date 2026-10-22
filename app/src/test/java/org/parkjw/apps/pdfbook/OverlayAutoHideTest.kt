package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayAutoHideTest {
    @Test
    fun returnsDefaultTimeoutWhenIdle() {
        assertEquals(
            OVERLAY_AUTO_HIDE_MILLIS,
            overlayAutoHideDelayMillis(
                sheetOpen = false,
                sliderDragging = false,
                touchExplorationEnabled = false,
            ),
        )
    }

    @Test
    fun pausesWhileSliderDragging() {
        assertNull(
            overlayAutoHideDelayMillis(
                sheetOpen = false,
                sliderDragging = true,
                touchExplorationEnabled = false,
            ),
        )
    }

    @Test
    fun pausesWhileSheetOpen() {
        assertNull(
            overlayAutoHideDelayMillis(
                sheetOpen = true,
                sliderDragging = false,
                touchExplorationEnabled = false,
            ),
        )
    }

    @Test
    fun pausesWhileTouchExplorationEnabled() {
        assertNull(
            overlayAutoHideDelayMillis(
                sheetOpen = false,
                sliderDragging = false,
                touchExplorationEnabled = true,
            ),
        )
    }
}
