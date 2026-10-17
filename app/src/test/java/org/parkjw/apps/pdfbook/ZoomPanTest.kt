package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
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
}
