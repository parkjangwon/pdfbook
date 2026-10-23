package org.parkjw.apps.pdfbook

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSystemBarsTest {
    @Test
    fun barsHideOnlyWhileReadingWithoutOverlay() {
        assertTrue(
            readerHidesSystemBars(
                inReader = true,
                fullScreenReading = true,
                overlayVisible = false,
            ),
        )
    }

    @Test
    fun overlayShowsBars() {
        assertFalse(
            readerHidesSystemBars(
                inReader = true,
                fullScreenReading = true,
                overlayVisible = true,
            ),
        )
    }

    @Test
    fun settingOffShowsBars() {
        assertFalse(
            readerHidesSystemBars(
                inReader = true,
                fullScreenReading = false,
                overlayVisible = false,
            ),
        )
    }

    @Test
    fun outsideReaderShowsBars() {
        assertFalse(
            readerHidesSystemBars(
                inReader = false,
                fullScreenReading = true,
                overlayVisible = false,
            ),
        )
    }
}
