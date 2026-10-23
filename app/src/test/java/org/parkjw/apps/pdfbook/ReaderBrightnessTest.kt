package org.parkjw.apps.pdfbook

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderBrightnessTest {
    @Test
    fun percentIsClampedToSliderRange() {
        assertEquals(1, normalizeReaderBrightnessPercent(-10))
        assertEquals(1, normalizeReaderBrightnessPercent(0))
        assertEquals(50, normalizeReaderBrightnessPercent(50))
        assertEquals(100, normalizeReaderBrightnessPercent(101))
    }

    @Test
    fun sliderSnapsToWholePercent() {
        assertEquals(38, snapReaderBrightnessPercent(37.6f))
        assertEquals(1, snapReaderBrightnessPercent(0f))
        assertEquals(100, snapReaderBrightnessPercent(200f))
        assertEquals(DEFAULT_READER_BRIGHTNESS_PERCENT, snapReaderBrightnessPercent(Float.NaN))
    }

    @Test
    fun followSystemUsesNoWindowOverride() {
        assertEquals(
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            readerBrightnessOverride(followSystem = true, percent = 40),
            0f,
        )
    }

    @Test
    fun manualLevelMapsToWindowBrightness() {
        assertEquals(0.4f, readerBrightnessOverride(followSystem = false, percent = 40), 0f)
        assertEquals(0.01f, readerBrightnessOverride(followSystem = false, percent = 0), 0f)
        assertEquals(1f, readerBrightnessOverride(followSystem = false, percent = 120), 0f)
    }
}
